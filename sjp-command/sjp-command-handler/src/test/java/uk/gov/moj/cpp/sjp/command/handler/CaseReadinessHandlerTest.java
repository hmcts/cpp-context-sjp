package uk.gov.moj.cpp.sjp.command.handler;

import static java.time.ZonedDateTime.now;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PIA;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseReadinessHandlerTest extends CaseCommandHandlerTest {

    @InjectMocks
    private CaseReadinessHandler caseReadinessHandler;

    @Test
    public void shouldHandleMarkCaseReadyForDecisionCommand() throws EventStreamException {
        final CaseReadinessReason readinessReason = PIA;
        final ZonedDateTime markedAt = now();

        when(jsonObject.getString("reason")).thenReturn(readinessReason.name());
        when(jsonObject.getString("markedAt")).thenReturn(markedAt.toString());
        when(caseAggregate.markCaseReadyForDecision(readinessReason, markedAt)).thenReturn(events);

        caseReadinessHandler.markCaseReadyForDecision(jsonEnvelope);

        verify(jsonObject).getString("reason");
        verify(jsonObject).getString("markedAt");
        verify(caseAggregate).markCaseReadyForDecision(readinessReason, markedAt);
    }

    @Test
    public void shouldHandleUnmarkCaseReadyForDecisionCommand() throws EventStreamException {
        final LocalDate expectedDateReady = LocalDate.now();
        when(jsonObject.getString("expectedDateReady")).thenReturn(expectedDateReady.toString());
        when(caseAggregate.unmarkCaseReadyForDecision(expectedDateReady)).thenReturn(events);

        caseReadinessHandler.unmarkCaseReadyForDecision(jsonEnvelope);

        verify(jsonObject).getString("expectedDateReady");
        verify(caseAggregate).unmarkCaseReadyForDecision(expectedDateReady);
    }

}
