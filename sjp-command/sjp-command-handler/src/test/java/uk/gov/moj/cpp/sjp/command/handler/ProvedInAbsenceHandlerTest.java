package uk.gov.moj.cpp.sjp.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProvedInAbsenceHandlerTest extends CaseCommandHandlerTest {
    @InjectMocks
    private ProvedInAbsenceHandler provedInAbsenceHandler;

    @Test
    public void testExpireDefendantResponseTimer() throws EventStreamException {
        when(caseAggregate.expireDefendantResponseTimer()).thenReturn(events);
        provedInAbsenceHandler.expireDefendantResponseTimer(jsonEnvelope);

        verify(caseAggregate).expireDefendantResponseTimer();
    }
}