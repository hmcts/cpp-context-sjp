package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.session.CaseAlreadyAssigned;
import uk.gov.moj.cpp.sjp.event.session.CaseAssigned;
import uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.CaseAssignmentType.MAGISTRATE_DECISION;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.APPLICATION_PENDING;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PIA;
import static uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected.RejectReason;
import static uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected.RejectReason.CASE_COMPLETED;
import static uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected.RejectReason.CASE_NOT_READY;

public class CaseCoreHandlerAssignmentTest {

    @Spy
    private Clock clock = new StoppedClock(ZonedDateTime.now(UTC));

    private final UUID assigneeId = randomUUID();
    private final UUID caseId = randomUUID();
    private final ZonedDateTime assignedAt = clock.now();

    private CaseAggregateState caseAggregateState;

    @BeforeEach
    public void onceBeforeEachTest() {
        caseAggregateState = new CaseAggregateState();
        caseAggregateState.setCaseId(caseId);
    }

    @Test
    public void shouldRejectCaseAssignmentIfCaseIsNotReady() {
        final Stream<Object> eventStream = CaseCoreHandler.INSTANCE.assignCaseToUser(assigneeId, assignedAt, caseAggregateState);

        final List<Object> eventList = eventStream.collect(toList());

        thenTheCaseAssignmentIsRejected(CASE_NOT_READY, eventList);
    }
    @Test
    public void shouldRejectCaseAssignmentIfCaseIsCompletedAndNoPendingApplication() {

        caseAggregateState.markCaseCompleted();
        final Stream<Object> eventStream = CaseCoreHandler.INSTANCE.assignCase(assigneeId, assignedAt,MAGISTRATE_DECISION, caseAggregateState);

        final List<Object> eventList = eventStream.collect(toList());

        thenTheCaseAssignmentIsRejected(CASE_COMPLETED, eventList);
    }

    @Test
    public void shouldAssignCaseIfCaseIsReadyAndCompletedAndHasPendingApplication() {
        caseAggregateState.markReady(clock.now(), APPLICATION_PENDING);

        final Stream<Object> eventStream = CaseCoreHandler.INSTANCE.assignCase(assigneeId, assignedAt,MAGISTRATE_DECISION, caseAggregateState);

        final List<Object> eventList = eventStream.collect(toList());

        thenCaseIsAssignedToUser(caseId, assigneeId, assignedAt, eventList);
    }

    @Test
    public void shouldRejectCaseAssignmentIfCaseIsAlreadyAssignedToTheUser() {
        caseAggregateState.markReady(clock.now(), PIA);

        caseAggregateState.setAssigneeId(assigneeId);

        final Stream<Object> eventStream = CaseCoreHandler.INSTANCE.assignCaseToUser(assigneeId, assignedAt, caseAggregateState);

        final List<Object> eventList = eventStream.collect(toList());

        thenTheCaseIsAlreadyAssigned(eventList);
    }

    @Test
    public void shouldAssignCaseIfCaseIsAssignedToAnotherUser() {
        final UUID oldAssigneeId = randomUUID();

        caseAggregateState.markReady(clock.now(), PIA);

        caseAggregateState.setAssigneeId(oldAssigneeId);

        final Stream<Object> eventStream = CaseCoreHandler.INSTANCE.assignCaseToUser(assigneeId, assignedAt, caseAggregateState);

        final List<Object> eventList = eventStream.collect(toList());

        thenCaseIsReassignedToUser(caseId, assigneeId, assignedAt, eventList);
    }

    @Test
    public void shouldAssignCaseIfCaseIsReadyAndNotAssignedToAnyUser() {
        caseAggregateState.markReady(clock.now(), PIA);

        final Stream<Object> eventStream = CaseCoreHandler.INSTANCE.assignCaseToUser(assigneeId, assignedAt, caseAggregateState);

        final List<Object> eventList = eventStream.collect(toList());

        thenCaseIsAssignedToUser(caseId, assigneeId, assignedAt, eventList);
    }

    private void thenTheCaseAssignmentIsRejected(final RejectReason rejectReason, final List<Object> eventList) {
        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(CaseAssignmentRejected.class),
                Matchers.<CaseAssignmentRejected>hasProperty("reason", is(rejectReason)))));
    }

    private void thenTheCaseIsAlreadyAssigned(final List<Object> eventList) {
        assertThat(eventList, hasItem(
                Matchers.instanceOf(CaseAlreadyAssigned.class)));
    }

    private void thenCaseIsAssignedToUser(final UUID caseId,
                                          final UUID assigneeId,
                                          final ZonedDateTime assignedAt,
                                          final List<Object> eventList) {

        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(CaseAssigned.class),
                Matchers.<CaseCompleted>hasProperty("caseId", is(caseId)),
                Matchers.<CaseCompleted>hasProperty("assigneeId", is(assigneeId)),
                Matchers.<CaseCompleted>hasProperty("assignedAt", is(assignedAt))
        )));
    }

    private void thenCaseIsReassignedToUser(final UUID caseId,
                                            final UUID assigneeId,
                                            final ZonedDateTime assignedAt,
                                            final List<Object> eventList) {

        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(CaseUnassigned.class),
                Matchers.<CaseCompleted>hasProperty("caseId", is(caseId))
        )));

        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(CaseAssigned.class),
                Matchers.<CaseCompleted>hasProperty("caseId", is(caseId)),
                Matchers.<CaseCompleted>hasProperty("assigneeId", is(assigneeId)),
                Matchers.<CaseCompleted>hasProperty("assignedAt", is(assignedAt))
        )));
    }
}
