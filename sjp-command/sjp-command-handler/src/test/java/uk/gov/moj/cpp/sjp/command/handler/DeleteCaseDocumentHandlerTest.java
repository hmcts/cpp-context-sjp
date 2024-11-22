package uk.gov.moj.cpp.sjp.command.handler;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeleteCaseDocumentHandlerTest extends CaseCommandHandlerTest {

    @InjectMocks
    private DeleteCaseDocumentHandler deleteCaseDocumentHandler;

    @BeforeEach
    void setUp() {
        super.setupMocks();
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonObject.getString(CaseCommandHandler.STREAM_ID)).thenReturn(CASE_ID.toString());
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(enveloper.withMetadataFrom(jsonEnvelope)).thenReturn(function);
        when(events.map(function)).thenReturn(jsonEvents);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
    }

    @Test
    public void shouldRaiseCaseDocumentDeletedEventWhenCaseNotLockedInSession() throws Exception {

        final UUID documentId = UUID.randomUUID();
        when(jsonObject.getString("documentId")).thenReturn(documentId.toString());
        when(caseAggregate.deleteCaseDocument(documentId)).thenReturn(events);

        deleteCaseDocumentHandler.deleteCaseDocument(jsonEnvelope);

        verify(jsonObject, atLeast(1)).getString("documentId");
        verify(caseAggregate).deleteCaseDocument(eq(documentId));
    }

}
