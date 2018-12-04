package uk.gov.moj.cpp.sjp.event.listener;

import static java.lang.Boolean.TRUE;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PIA;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAdded;
import uk.gov.moj.cpp.sjp.event.listener.converter.CaseDocumentAddedToCaseDocument;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;
import uk.gov.moj.cpp.sjp.persistence.entity.ReadyCase;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseDocumentRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.ReadyCaseRepository;

import java.lang.reflect.Method;
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

    private static final String EVENTS_CASE_LISTED_IN_CRIMINAL_COURTS = "sjp.events.case-listed-in-criminal-courts";
    private static final String METHOD_CASE_LISTED_IN_CRIMINAL_COURTS = "updateCaseListedInCriminalCourts";

    private UUID caseId = randomUUID();

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private ReadyCaseRepository readyCaseRepository;

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
    private CaseDocument caseDocument;

    @Mock
    private CaseDocumentRepository caseDocumentRepository;

    @InjectMocks
    private CaseUpdatedListener listener;

    @Test
    public void shouldUpdateCompletedStatusAndRemoveCaseReadinessIfExists() {
        final JsonObject caseCompletedEventPayload = createObjectBuilder().build();
        final JsonEnvelope envelopeIn = envelopeFrom(metadataWithRandomUUID(CaseCompleted.EVENT_NAME), caseCompletedEventPayload);
        final ReadyCase readyCase = new ReadyCase(caseId, PIA);

        when(jsonObjectToObjectConverter.convert(caseCompletedEventPayload, CaseCompleted.class)).thenReturn(new CaseCompleted(caseId));
        when(readyCaseRepository.findBy(caseId)).thenReturn(readyCase);
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);
        listener.caseCompleted(envelopeIn);

        verify(caseRepository).completeCase(caseId);
        verify(readyCaseRepository).remove(readyCase);
    }

    @Test
    public void shouldUpdateCompletedStatusAndNotRemoveCaseReadinessIfDoesNotExist() {
        final JsonObject caseCompletedEventPayload = createObjectBuilder().build();
        final JsonEnvelope envelopeIn = envelopeFrom(metadataWithRandomUUID(CaseCompleted.EVENT_NAME), caseCompletedEventPayload);

        when(jsonObjectToObjectConverter.convert(caseCompletedEventPayload, CaseCompleted.class)).thenReturn(new CaseCompleted(caseId));
        when(readyCaseRepository.findBy(caseId)).thenReturn(null);
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);

        listener.caseCompleted(envelopeIn);

        verify(caseRepository).completeCase(caseId);
        verify(readyCaseRepository, never()).remove(any());
    }

    @Test
    public void shouldAddDocument() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CaseDocumentAdded.class)).thenReturn(caseDocumentEvent);
        when(caseDocumentConverter.convert(caseDocumentEvent)).thenReturn(caseDocument);
        when(caseDocument.getCaseId()).thenReturn(UUID.randomUUID());
        when(caseDocumentEvent.getCaseId()).thenReturn(caseId);
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);

        listener.addCaseDocument(envelope);

        verify(caseDocumentRepository).save(caseDocument);
    }

    @Test
    public void shouldAddDocumentBeforeCaseArrived() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CaseDocumentAdded.class)).thenReturn(caseDocumentEvent);
        when(caseDocumentConverter.convert(caseDocumentEvent)).thenReturn(caseDocument);
        when(caseDocumentEvent.getCaseId()).thenReturn(caseId);
        when(caseRepository.findBy(caseId)).thenReturn(null);

        listener.addCaseDocument(envelope);

        verify(caseDocumentRepository).save(caseDocument);
    }

    @Test
    public void shouldHandleCaseListedInCriminalCourtsEvent() throws NoSuchMethodException {
        Class<CaseUpdatedListener> caseUpdatedListenerClass = CaseUpdatedListener.class;

        Method m = caseUpdatedListenerClass.getDeclaredMethod(METHOD_CASE_LISTED_IN_CRIMINAL_COURTS, JsonEnvelope.class);
        Handles handles = m.getAnnotation(Handles.class);

        assertThat(EVENTS_CASE_LISTED_IN_CRIMINAL_COURTS, equalTo(handles.value()));
    }

    @Test
    public void shouldUpdateCaseListedInCriminalCourts() {

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(payload.getString("caseId")).thenReturn(caseId.toString());
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);

        listener.updateCaseListedInCriminalCourts(envelope);

        verify(caseRepository).findBy(caseId);
        verify(caseDetail).setListedInCriminalCourts(TRUE);

    }


}
