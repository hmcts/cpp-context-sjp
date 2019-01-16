package uk.gov.moj.cpp.sjp.command.handler;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseAdjournmentHandlerTest extends CaseCommandHandlerTest {

    @InjectMocks
    private CaseAdjournmentHandler caseAdjournmentHandler;

    @Test
    public void shouldRecordCaseAdjournedToLaterSjpHearing() throws EventStreamException {
        final UUID sessionId = randomUUID();
        final LocalDate adjournedTo = LocalDate.now();

        when(jsonObject.getString("caseId")).thenReturn(CASE_ID.toString());
        when(jsonObject.getString("sjpSessionId")).thenReturn(sessionId.toString());
        when(jsonObject.getString("adjournedTo")).thenReturn(adjournedTo.toString());

        when(caseAggregate.recordCaseAdjournedToLaterSjpHearing(CASE_ID, sessionId, adjournedTo)).thenReturn(events);

        caseAdjournmentHandler.recordCaseAdjournedToLaterSjpHearing(jsonEnvelope);

        verify(jsonObject).getString("sjpSessionId");
        verify(jsonObject).getString("adjournedTo");
        verify(caseAggregate).recordCaseAdjournedToLaterSjpHearing(CASE_ID, sessionId, adjournedTo);
    }
}
