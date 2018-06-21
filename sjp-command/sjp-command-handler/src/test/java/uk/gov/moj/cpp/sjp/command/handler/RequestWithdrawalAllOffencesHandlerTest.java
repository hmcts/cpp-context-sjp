package uk.gov.moj.cpp.sjp.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class RequestWithdrawalAllOffencesHandlerTest extends CaseCommandHandlerTest {

    @InjectMocks
    private RequestWithdrawalAllOffencesHandler requestWithdrawalAllOffencesOffHandler;

    @Test
    public void shouldRequestWithdrawalAllOffences() throws EventStreamException {
        when(caseAggregate.requestWithdrawalAllOffences()).thenReturn(events);

        requestWithdrawalAllOffencesOffHandler.requestWithdrawalAllOffences(jsonEnvelope);

        verify(caseAggregate).requestWithdrawalAllOffences();
    }
}
