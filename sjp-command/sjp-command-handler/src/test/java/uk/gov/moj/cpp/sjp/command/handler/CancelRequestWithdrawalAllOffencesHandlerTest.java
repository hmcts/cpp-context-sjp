package uk.gov.moj.cpp.sjp.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class CancelRequestWithdrawalAllOffencesHandlerTest extends CaseCommandHandlerTest {

    @InjectMocks
    private CancelRequestWithdrawalAllOffencesHandler cancelRequestWithdrawalAllOffencesOffHandler;

    @Test
    public void shouldCancelRequestWithdrawalAllOffences() throws EventStreamException {
        when(caseAggregate.cancelRequestWithdrawalAllOffences(CASE_ID)).thenReturn(events);

        cancelRequestWithdrawalAllOffencesOffHandler.cancelRequestWithdrawalAllOffences(jsonEnvelope);

        verify(caseAggregate).cancelRequestWithdrawalAllOffences(CASE_ID);
    }
}
