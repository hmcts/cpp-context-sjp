package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.event.CaseAdjournedToLaterSjpHearingRecorded.caseAdjournedToLaterSjpHearingRecorded;
import static uk.gov.moj.cpp.sjp.event.CaseAdjournmentToLaterSjpHearingElapsed.caseAdjournmentToLaterSjpHearingElapsed;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseNotFound;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

public class CaseAdjournmentHandlerTest {

    private UUID existingCaseId;
    private CaseAggregateState caseAggregateState;

    @Before
    public void init() {
        existingCaseId = randomUUID();
        caseAggregateState = new CaseAggregateState();
        caseAggregateState.setCaseId(existingCaseId);

    }

    @Test
    public void shouldCreateCaseNotFoundEventWhenRecordingAdjournmentForDifferentCase() {
        final UUID nonExistingCaseId = randomUUID();
        final Stream<Object> eventStream = CaseAdjournmentHandler.INSTANCE.recordCaseAdjournedToLaterSjpHearing(
                nonExistingCaseId,
                null,
                null,
                caseAggregateState);

        assertThat(eventStream.collect(toList()), contains(new CaseNotFound(nonExistingCaseId, "Record case adjourned to later sjp hearing")));
    }

    @Test
    public void shouldCreateCaseNotFoundEventWhenRecordingAdjournmentElapsedForDifferentCase() {
        final UUID nonExistingCaseId = randomUUID();

        final Stream<Object> eventStream = CaseAdjournmentHandler.INSTANCE.recordCaseAdjournmentToLaterSjpHearingElapsed(
                nonExistingCaseId,
                ZonedDateTime.now(),
                caseAggregateState);

        assertThat(eventStream.collect(toList()), contains(new CaseNotFound(nonExistingCaseId, "Record case adjournment to later sjp hearing elapsed")));
    }

    @Test
    public void shouldCreateCaseAdjournedToLaterSjpHearingRecordedAndCaseUnassignedEvents() {
        final UUID sessionId = randomUUID();
        final LocalDate adjournedTo = LocalDate.now();

        final Stream<Object> eventStream = CaseAdjournmentHandler.INSTANCE.recordCaseAdjournedToLaterSjpHearing(
                existingCaseId,
                sessionId,
                adjournedTo,
                caseAggregateState);

        assertThat(eventStream.collect(toList()), containsInAnyOrder(
                caseAdjournedToLaterSjpHearingRecorded()
                        .withCaseId(existingCaseId)
                        .withAdjournedTo(adjournedTo)
                        .withSessionId(sessionId).build(),
                new CaseUnassigned(existingCaseId)));
    }

    @Test
    public void shouldCreateCaseAdjournmentToLaterSjpHearingElapsedEvent() {
        final ZonedDateTime elapsedAt = ZonedDateTime.now();

        final Stream<Object> eventStream = CaseAdjournmentHandler.INSTANCE.recordCaseAdjournmentToLaterSjpHearingElapsed(
                existingCaseId,
                elapsedAt,
                caseAggregateState);

        assertThat(eventStream.collect(toList()), contains(
                caseAdjournmentToLaterSjpHearingElapsed()
                        .withCaseId(existingCaseId)
                        .withElapsedAt(elapsedAt)
                        .build()));
    }
}
