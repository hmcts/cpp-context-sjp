package uk.gov.moj.cpp.sjp.event.listener;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.Boolean.TRUE;
import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PIA;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.common.CaseManagementStatus.DONE;
import static uk.gov.moj.cpp.sjp.domain.common.CaseManagementStatus.IN_PROGRESS;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAdded;
import uk.gov.moj.cpp.sjp.event.casemanagement.UpdateCasesManagementStatus;
import uk.gov.moj.cpp.sjp.event.listener.converter.CaseDocumentAddedToCaseDocument;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;
import uk.gov.moj.cpp.sjp.persistence.entity.ReadyCase;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseDocumentRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.ReadyCaseRepository;

import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@SuppressWarnings("WeakerAccess")
@ExtendWith(MockitoExtension.class)
public class CaseUpdatedListenerTest {

    private static final String EVENTS_CASE_LISTED_IN_CRIMINAL_COURTS = "sjp.events.case-listed-in-criminal-courts";
    private static final String METHOD_CASE_LISTED_IN_CRIMINAL_COURTS = "updateCaseListedInCriminalCourts";

    private final UUID caseId = randomUUID();

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
    private CaseDetail caseDetail2;

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
        final ReadyCase readyCase = new ReadyCase(caseId, PIA, null, MAGISTRATE, 3, "TFL", now(), now());

        when(jsonObjectToObjectConverter.convert(caseCompletedEventPayload, CaseCompleted.class)).thenReturn(new CaseCompleted(caseId, newHashSet(randomUUID())));
        when(readyCaseRepository.findBy(caseId)).thenReturn(readyCase);
        listener.caseCompleted(envelopeIn);

        verify(caseRepository).completeCase(caseId);
        verify(readyCaseRepository).remove(readyCase);
    }

    @Test
    public void shouldUpdateCompletedStatusAndNotRemoveCaseReadinessIfDoesNotExist() {
        final JsonObject caseCompletedEventPayload = createObjectBuilder().build();
        final JsonEnvelope envelopeIn = envelopeFrom(metadataWithRandomUUID(CaseCompleted.EVENT_NAME), caseCompletedEventPayload);

        when(jsonObjectToObjectConverter.convert(caseCompletedEventPayload, CaseCompleted.class)).thenReturn(new CaseCompleted(caseId, newHashSet(randomUUID())));
        when(readyCaseRepository.findBy(caseId)).thenReturn(null);

        listener.caseCompleted(envelopeIn);

        verify(caseRepository).completeCase(caseId);
        verify(readyCaseRepository, never()).remove(any());
    }

    @Test
    public void shouldAddDocument() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CaseDocumentAdded.class)).thenReturn(caseDocumentEvent);
        when(caseDocumentConverter.convert(caseDocumentEvent)).thenReturn(caseDocument);

        listener.addCaseDocument(envelope);

        verify(caseDocumentRepository).save(caseDocument);
    }

    @Test
    public void shouldAddDocumentBeforeCaseArrived() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CaseDocumentAdded.class)).thenReturn(caseDocumentEvent);
        when(caseDocumentConverter.convert(caseDocumentEvent)).thenReturn(caseDocument);

        listener.addCaseDocument(envelope);

        verify(caseDocumentRepository).save(caseDocument);
    }

    @Test
    public void shouldHandleCaseListedInCriminalCourtsEvent() throws NoSuchMethodException {
        final Class<CaseUpdatedListener> caseUpdatedListenerClass = CaseUpdatedListener.class;

        final Method m = caseUpdatedListenerClass.getDeclaredMethod(METHOD_CASE_LISTED_IN_CRIMINAL_COURTS, JsonEnvelope.class);
        final Handles handles = m.getAnnotation(Handles.class);

        assertThat(EVENTS_CASE_LISTED_IN_CRIMINAL_COURTS, equalTo(handles.value()));
    }

    @Test
    public void shouldUpdateCaseListedInCriminalCourts() {
        final String hearingCourtName = "Carmarthen Magistrates' Court";
        final ZonedDateTime hearingTime = ZonedDateTime.parse("2018-12-28T11:53:04.693Z");
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(payload.getString("caseId")).thenReturn(caseId.toString());
        when(payload.getString("hearingCourtName")).thenReturn(hearingCourtName);
        when(payload.getString("hearingTime")).thenReturn(ZonedDateTimes.toString(hearingTime));
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);

        listener.updateCaseListedInCriminalCourts(envelope);

        verify(caseRepository).findBy(caseId);
        verify(caseDetail).setListedInCriminalCourts(TRUE);
        verify(caseDetail).setHearingCourtName(hearingCourtName);
        verify(caseDetail).setHearingTime(hearingTime);

    }

    @Test
    public void shouldUpdateCaseManagementStatus() {
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();

        final JsonObject caseManagementStatusChanged = createObjectBuilder().add("cases", createArrayBuilder()
                .add(createObjectBuilder()
                        .add("caseId", case1Id.toString())
                        .add("caseManagementStatus", IN_PROGRESS.toString()))
                .add(createObjectBuilder()
                        .add("caseId", case2Id.toString())
                        .add("caseManagementStatus", DONE.toString()))).build();

        final JsonEnvelope envelopeIn = envelopeFrom(metadataWithRandomUUID(UpdateCasesManagementStatus.EVENT_NAME), caseManagementStatusChanged);

        when(caseRepository.findBy(case1Id)).thenReturn(caseDetail);
        when(caseRepository.findBy(case2Id)).thenReturn(caseDetail2);

        listener.updateCaseManagementStatus(envelopeIn);

        verify(caseRepository).findBy(case1Id);
        verify(caseRepository).findBy(case2Id);
    }
}
