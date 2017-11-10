package uk.gov.moj.cpp.sjp.command.handler;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.moj.cpp.sjp.domain.command.CompleteCase;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CompleteCaseHandlerTest extends CaseCommandHandlerTest {

    @InjectMocks
    private CompleteCaseHandler completeCaseHandler;

    @Mock
    private CompleteCase completeCase;

    @Test
    public void shouldCompleteCase() throws EventStreamException {
        when(converter.convert(jsonObject, CompleteCase.class)).thenReturn(completeCase);
        when(caseAggregate.completeCase(completeCase)).thenReturn(events);

        completeCaseHandler.completeCase(jsonEnvelope);

        verify(converter).convert(jsonObject, CompleteCase.class);
        verify(caseAggregate).completeCase(completeCase);
    }

}