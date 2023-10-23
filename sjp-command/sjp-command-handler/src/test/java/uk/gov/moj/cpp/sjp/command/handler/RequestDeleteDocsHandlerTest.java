package uk.gov.moj.cpp.sjp.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RequestDeleteDocsHandlerTest extends CaseCommandHandlerTest {
    @InjectMocks
    private RequestDeleteDocsHandler requestDeleteDocsHandler;

    @Test
    public void testExpireDefendantResponseTimer() throws EventStreamException {
        when(caseAggregate.deleteDocs()).thenReturn(events);
        requestDeleteDocsHandler.deleteDocs(jsonEnvelope);

        verify(caseAggregate).deleteDocs();
    }
}