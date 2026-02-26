package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.testutils.AggregateHelper;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.CaseNotFound;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidAdded;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidUpdated;
import uk.gov.moj.cpp.sjp.event.ProsecutionAuthorityAccessDenied;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AddDatesToAvoidTest extends CaseAggregateBaseTest {
    private static final String DATES_TO_AVOID = "12th July 2018";
    private static final String DATES_TO_AVOID_UPDATED = "13th August 2018";
    public static final String ALL = "ALL";

    @Test
    public void datesToAvoidAddedEvent() {
        //when
        final List<Object> events = caseAggregate.addDatesToAvoid(DATES_TO_AVOID, ALL, null).collect(toList());
        final DatesToAvoidAdded datesToAvoidAdded = (DatesToAvoidAdded) events.get(0);

        //then
        assertThatDatesToAvoidAddedEventWasRaised(events, datesToAvoidAdded);
    }

    @Test
    public void raiseDatesToAvoidUpdatedEvent() {
        //given
        datesToAvoidAddedEvent(DATES_TO_AVOID);

        //when
        final List<Object> datesToAvoidUpdatedEvents = caseAggregate.addDatesToAvoid(DATES_TO_AVOID_UPDATED, ALL, null).collect(toList());
        final DatesToAvoidUpdated datesToAvoidUpdated = (DatesToAvoidUpdated) datesToAvoidUpdatedEvents.get(0);

        //then
        assertThatDatesToAvoidUpdatedEventWasRaised(datesToAvoidUpdatedEvents, datesToAvoidUpdated);
    }

    private void assertThatDatesToAvoidAddedEventWasRaised(List<Object> datesToAvoidAddedEvents, DatesToAvoidAdded datesToAvoidAdded) {
        assertThat(datesToAvoidAddedEvents, hasSize(1));
        assertThat(datesToAvoidAdded.getCaseId(), equalTo(caseId));
        assertThat(datesToAvoidAdded.getDatesToAvoid(), equalTo(DATES_TO_AVOID));
    }

    private void assertThatDatesToAvoidUpdatedEventWasRaised(List<Object> datesToAvoidUpdatedEvents, DatesToAvoidUpdated datesToAvoidUpdated) {
        assertThat(datesToAvoidUpdatedEvents, hasSize(1));
        assertThat(datesToAvoidUpdated.getCaseId(), equalTo(caseId));
        assertThat(datesToAvoidUpdated.getDatesToAvoid(), equalTo(DATES_TO_AVOID_UPDATED));
    }

    private DatesToAvoidAdded datesToAvoidAddedEvent(final String datesToAvoid) {
        final List<Object> datesToAvoidAddedEvents = caseAggregate.addDatesToAvoid(datesToAvoid, ALL, null).collect(toList());
        return (DatesToAvoidAdded) datesToAvoidAddedEvents.get(0);
    }

    @Test
    public void caseCompletedDoesNotAcceptDatesToAvoid() {
        Mockito.when(session.getSessionType()).thenReturn(SessionType.DELEGATED_POWERS);
        //given a completed case
        AggregateHelper.saveDecision(caseAggregate, aCase, session, VerdictType.FOUND_NOT_GUILTY);

        //when
        final List<Object> events = caseAggregate.addDatesToAvoid(DATES_TO_AVOID, ALL, null).collect(toList());

        //then
        assertThat(events, hasSize(1));
        final CaseUpdateRejected datesToAvoidReceived = (CaseUpdateRejected) events.get(0);
        assertThat(datesToAvoidReceived.getCaseId(), equalTo(aCase.getId()));
        assertThat(datesToAvoidReceived.getReason(), equalTo(CaseUpdateRejected.RejectReason.CASE_COMPLETED));
    }

    @Test
    public void caseAssignedDoesNotAcceptDatesToAvoid() {
        //given an assigned case
        caseAggregate.assignCase(randomUUID(), clock.now(), CaseAssignmentType.DELEGATED_POWERS_DECISION);

        //when
        final List<Object> events = caseAggregate.addDatesToAvoid(DATES_TO_AVOID, ALL, null).collect(toList());

        //then
        assertThat(events, hasSize(1));
        final CaseUpdateRejected datesToAvoidReceived = (CaseUpdateRejected) events.get(0);
        assertThat(datesToAvoidReceived.getCaseId(), equalTo(aCase.getId()));
        assertThat(datesToAvoidReceived.getReason(), equalTo(CaseUpdateRejected.RejectReason.CASE_ASSIGNED));
    }

    @Test
    public void caseNotFound() {
        //when
        final List<Object> events = new CaseAggregate().addDatesToAvoid(DATES_TO_AVOID, ALL, null).collect(toList());

        //then
        assertThat(events, hasSize(1));
        final CaseNotFound datesToAvoidReceived = (CaseNotFound) events.get(0);
        assertThat(datesToAvoidReceived.getCaseId(), nullValue());
        assertThat(datesToAvoidReceived.getDescription(), equalTo("Add dates to avoid"));
    }

    @Test
    public void prosecutionAuthorityAccessAllowedForAgent() {
        //given an assigned case
        caseAggregate.assignCase(randomUUID(), clock.now(), CaseAssignmentType.DELEGATED_POWERS_DECISION);
        caseAggregate.getState().setAssigneeId(null);

        //when
        final List<Object> events = caseAggregate.addDatesToAvoid(DATES_TO_AVOID, "TVL", Arrays.asList("TFL")).collect(toList());

        //then
        assertThat(events, hasSize(1));
        final DatesToAvoidAdded datesToAvoidAdded = (DatesToAvoidAdded) events.get(0);
        assertThat(datesToAvoidAdded.getCaseId(), equalTo(caseId));
        assertThat(datesToAvoidAdded.getDatesToAvoid(), equalTo(DATES_TO_AVOID));
    }

    @Test
    public void prosecutionAuthorityAccessDenied() {
        //given an assigned case
        caseAggregate.assignCase(randomUUID(), clock.now(), CaseAssignmentType.DELEGATED_POWERS_DECISION);
        caseAggregate.getState().setAssigneeId(null);

        //when
        final List<Object> events = caseAggregate.addDatesToAvoid(DATES_TO_AVOID, "TVL", Arrays.asList("XYZ")).collect(toList());

        //then
        assertThat(events, hasSize(1));
        final ProsecutionAuthorityAccessDenied prosecutionAuthorityAccessDenied = (ProsecutionAuthorityAccessDenied) events.get(0);

        assertThat(prosecutionAuthorityAccessDenied.getCaseAuthority(), equalTo("TFL"));
        assertThat(prosecutionAuthorityAccessDenied.getAgentProsecutorAuthorityAccess().size(), equalTo(2));
        assertThat(prosecutionAuthorityAccessDenied.getAgentProsecutorAuthorityAccess().get(0), equalTo("XYZ"));
        assertThat(prosecutionAuthorityAccessDenied.getAgentProsecutorAuthorityAccess().get(1), equalTo("TVL"));
    }
}
