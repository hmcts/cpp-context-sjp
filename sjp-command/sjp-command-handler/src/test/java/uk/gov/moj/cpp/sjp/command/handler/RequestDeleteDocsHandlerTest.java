package uk.gov.moj.cpp.sjp.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RequestDeleteDocsHandlerTest extends CaseCommandHandlerTest {
    @InjectMocks
    private RequestDeleteDocsHandler requestDeleteDocsHandler;

    @BeforeEach
    void setUp() {
        super.setupMocks();
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonObject.getString(CaseCommandHandler.STREAM_ID)).thenReturn(CASE_ID.toString());
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(enveloper.withMetadataFrom(jsonEnvelope)).thenReturn(function);
        when(events.map(function)).thenReturn(jsonEvents);
    }

    @Test
    public void testExpireDefendantResponseTimer() throws EventStreamException {
        when(caseAggregate.deleteDocs()).thenReturn(events);
        requestDeleteDocsHandler.deleteDocs(jsonEnvelope);

        verify(caseAggregate).deleteDocs();
    }
}