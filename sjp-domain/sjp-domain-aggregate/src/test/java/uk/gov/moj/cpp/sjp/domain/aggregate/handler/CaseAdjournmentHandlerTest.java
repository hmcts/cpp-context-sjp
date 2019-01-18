package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Test;

public class CaseAdjournmentHandlerTest {

    private static final UUID CASE_ID = randomUUID();

    @Test
    public void shouldCreateCaseNotFoundEventIfCaseIdDoesNotMatch() {
        final CaseAggregateState caseAggregateState = new CaseAggregateState();
        caseAggregateState.setCaseId(randomUUID());

        final Stream<Object> eventStream = CaseAdjournmentHandler.INSTANCE.recordCaseAdjournedToLaterSjpHearing(
                CASE_ID,
                null,
                null,
                caseAggregateState);

        assertThat(
                eventStream.findFirst().get(),
                hasProperty("description", is("Record case adjournment to later date")));
    }

    @Test
    public void shouldCreateCaseAdjournedToLaterSjpHearingRecorded() {
        final CaseAggregateState caseAggregateState = new CaseAggregateState();
        caseAggregateState.setCaseId(CASE_ID);

        final UUID sessionId = randomUUID();
        final LocalDate adjournedTo = LocalDate.now();

        final Stream<Object> eventStream = CaseAdjournmentHandler.INSTANCE.recordCaseAdjournedToLaterSjpHearing(
                CASE_ID,
                sessionId,
                adjournedTo,
                caseAggregateState);

        Object event = eventStream.findFirst().get();
        assertThat(event, allOf(
                hasProperty("adjournedTo", is(adjournedTo)),
                hasProperty("caseId", is(CASE_ID)),
                hasProperty("sessionId", is(sessionId))));
    }
}
