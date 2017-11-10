package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataFrom;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAdded;
import uk.gov.moj.cpp.sjp.event.listener.converter.CaseDocumentAddedToCaseDocument;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseDocumentRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@SuppressWarnings("WeakerAccess")
@RunWith(MockitoJUnitRunner.class)
public class CaseUpdatedListenerTest {

    private UUID caseId = randomUUID();

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject payload;

    @Mock
    private CaseDocumentAddedToCaseDocument caseDocumentConverter;

    @Mock
    private CaseDocumentAdded caseDocumentEvent;

    @Mock
    private CaseDetail caseDetail;

    @Mock
    private DefendantDetail defendantDetail;

    @Mock
    private CaseDocument caseDocument;

    @Mock
    private CaseDocumentRepository caseDocumentRepository;


    @InjectMocks
    private CaseUpdatedListener listener;

    @Test
    public void shouldUpdateCompletedStatusOnCase() {
        JsonObject caseCompletedJson = createObjectBuilder().build();
        when(jsonObjectToObjectConverter.convert(caseCompletedJson, CaseCompleted.class)).thenReturn(new CaseCompleted(caseId));
        Metadata metadata = metadataFrom(createObjectBuilder().add("id", randomUUID().toString()).add("name", "in").build());
        JsonEnvelope envelopeIn = envelopeFrom(metadata, caseCompletedJson);

        listener.caseCompleted(envelopeIn);

        verify(caseRepository).completeCase(caseId);
    }


    @Test
    public void shouldAddDocument() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CaseDocumentAdded.class)).thenReturn(caseDocumentEvent);
        when(caseDocumentConverter.convert(caseDocumentEvent)).thenReturn(caseDocument);
        when(caseDocument.getCaseId()).thenReturn(UUID.randomUUID());
        when(caseDocumentEvent.getCaseId()).thenReturn(caseId.toString());
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);

        listener.addCaseDocument(envelope);

        verify(caseDocumentRepository).save(caseDocument);
    }

    @Test
    public void shouldAddDocumentBeforeCaseArrived() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CaseDocumentAdded.class)).thenReturn(caseDocumentEvent);
        when(caseDocumentConverter.convert(caseDocumentEvent)).thenReturn(caseDocument);
        when(caseDocumentEvent.getCaseId()).thenReturn(caseId.toString());
        when(caseRepository.findBy(caseId)).thenReturn(null);

        listener.addCaseDocument(envelope);

        verify(caseDocumentRepository).save(caseDocument);
    }


}
