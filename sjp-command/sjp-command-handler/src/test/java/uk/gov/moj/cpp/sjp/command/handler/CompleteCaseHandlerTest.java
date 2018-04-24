package uk.gov.moj.cpp.sjp.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CompleteCaseHandlerTest extends CaseCommandHandlerTest {

    @InjectMocks
    private CompleteCaseHandler completeCaseHandler;

    @Test
    public void shouldCompleteCase() throws EventStreamException {
        when(caseAggregate.completeCase()).thenReturn(events);
        completeCaseHandler.completeCase(jsonEnvelope);
        verify(caseAggregate).completeCase();
    }

}