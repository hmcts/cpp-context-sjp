package uk.gov.moj.cpp.sjp.event.processor.service.referral;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.json.schemas.domains.sjp.queries.Defendant.defendant;
import static uk.gov.justice.json.schemas.domains.sjp.queries.Offence.offence;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing.caseReferredForCourtHearing;

import uk.gov.justice.json.schemas.domains.sjp.CaseApplication;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Document;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.CourtDocumentView;
import uk.gov.moj.cpp.sjp.event.processor.service.MaterialService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtDocumentsDataSourcingServiceTest {

    private static final UUID MATERIAL_ID = randomUUID();
    private static final UUID DOCUMENT_ID = randomUUID();
    private static final UUID CASE_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID OFFENCE_ID = randomUUID();
    private static final UUID APPLICATION_ID = randomUUID();
    private static final ZonedDateTime MATERIAL_ADDED_DATE = ZonedDateTime.now();
    private static final ZonedDateTime REFERRED_AT = ZonedDateTime.now();
    private static final String MATERIAL_FILE_NAME = "Material Name";
    private static final String MIME_TYPE = "Mime Type";
    private static final UUID CASE_SUMMARY_DOCUMENT_TYPE_ID = randomUUID();
    private static final UUID APPLICATIONS_DOCUMENT_TYPE_ID = randomUUID();
    private static final String SJP_DOCUMENT_TYPE = "SJPN";
    private static final String SJP_APPLICATION_DOCUMENT_TYPE = "APPLICATION";
    private static final String RESULT_ORDER = "RESULT_ORDER";
    private static final String EMPLOYER_ATTACHMENT_TO_EARNINGS = "EMPLOYER_ATTACHMENT_TO_EARNINGS";

    @Mock
    private MaterialService materialService;
    @Mock
    private ReferenceDataService referenceDataService;
    @InjectMocks
    private CourtDocumentsDataSourcingService courtDocumentsDataSourcingService;

    private JsonEnvelope envelope;

    @BeforeEach
    public void setUp() {
        envelope = envelope();
    }

    @Test
    public void shouldCreateCourtDocumentWithSjpDocumentType() {
        final JsonObject materialDataMock = createObjectBuilder()
                .add("fileName", MATERIAL_FILE_NAME)
                .add("materialAddedDate", MATERIAL_ADDED_DATE.toString())
                .add("mimeType", MIME_TYPE)
                .build();
        when(materialService.getMaterialMetadata(MATERIAL_ID, envelope)).thenReturn(materialDataMock);
        mockDocumentTypeAccessService();

        final CaseReferredForCourtHearing caseReferral = caseReferredForCourtHearing()
                .withCaseId(CASE_ID)
                .withReferredAt(REFERRED_AT)
                .build();

        final CaseDetails caseDetails = caseDetails().withCaseDocuments(singletonList(
                Document.document()
                        .withId(DOCUMENT_ID)
                        .withDocumentType(SJP_DOCUMENT_TYPE)
                        .withMaterialId(MATERIAL_ID)
                        .build()
        )).build();

        final List<CourtDocumentView> courtDocuments = courtDocumentsDataSourcingService.createCourtDocumentViews(caseReferral.getReferredAt(), caseDetails, envelope);

        assertThat(courtDocuments, hasSize(1));
        final CourtDocumentView actual = courtDocuments.get(0);
        assertThat(actual.getCourtDocumentId(), is(DOCUMENT_ID));
        assertThat(actual.getDocumentTypeId(), is(CASE_SUMMARY_DOCUMENT_TYPE_ID));
        assertThat(actual.getName(), is(MATERIAL_FILE_NAME));
        assertThat(actual.getMimeType(), is(MIME_TYPE));
        assertThat(actual.getContainsFinancialMeans(), is(false));
        assertThat(actual.getDocumentCategory().getApplicationDocument(), is(nullValue()));
        assertThat(actual.getDocumentCategory().getDefendantDocument(), is(notNullValue()));
        assertThat(actual.getDocumentCategory().getDefendantDocument().getProsecutionCaseId(), is(CASE_ID));
        assertThat(actual.getDocumentCategory().getDefendantDocument().getDefendants(), containsInAnyOrder(DEFENDANT_ID));
        assertThat(actual.getDocumentCategory().getApplicationDocument(), is(nullValue()));
        assertThat(actual.getMaterials(), hasSize(1));
    }

    @Test
    public void caseDoesNotHaveAnApplication() {
        final JsonObject materialDataMock = createObjectBuilder()
                .add("fileName", MATERIAL_FILE_NAME)
                .add("materialAddedDate", MATERIAL_ADDED_DATE.toString())
                .add("mimeType", MIME_TYPE)
                .build();
        when(materialService.getMaterialMetadata(MATERIAL_ID, envelope)).thenReturn(materialDataMock);
        mockDocumentTypeAccessService();

        final CaseReferredForCourtHearing caseReferral = caseReferredForCourtHearing()
                .withCaseId(CASE_ID)
                .withReferredAt(REFERRED_AT)
                .build();

        final CaseDetails caseDetails = caseDetails()
                .withCaseDocuments(singletonList(Document.document()
                        .withId(DOCUMENT_ID)
                        .withDocumentType(SJP_APPLICATION_DOCUMENT_TYPE)
                        .withMaterialId(MATERIAL_ID)
                        .build()))
                .withCaseApplication(CaseApplication.caseApplication().withApplicationId(APPLICATION_ID).build())
                .build();

        final List<CourtDocumentView> courtDocuments = courtDocumentsDataSourcingService.createCourtDocumentViews(caseReferral.getReferredAt(), caseDetails, envelope);

        assertThat(courtDocuments, hasSize(1));
        final CourtDocumentView actual = courtDocuments.get(0);
        assertThat(actual.getCourtDocumentId(), is(DOCUMENT_ID));
        assertThat(actual.getDocumentTypeId(), is(APPLICATIONS_DOCUMENT_TYPE_ID));
        assertThat(actual.getName(), is(MATERIAL_FILE_NAME));
        assertThat(actual.getMimeType(), is(MIME_TYPE));
        assertThat(actual.getContainsFinancialMeans(), is(false));
        assertThat(actual.getDocumentCategory().getDefendantDocument(), is(nullValue()));
        assertThat(actual.getDocumentCategory().getApplicationDocument(), is(notNullValue()));
        assertThat(actual.getDocumentCategory().getApplicationDocument().getApplicationId(), is(APPLICATION_ID));
        assertThat(actual.getDocumentCategory().getApplicationDocument().getDefendants(), containsInAnyOrder(DEFENDANT_ID));
        assertThat(actual.getDocumentCategory().getApplicationDocument().getProsecutionCaseId(), is(CASE_ID));
    }

    @Test
    public void shouldFilterOutExcludedDocumentTypes() {
        final JsonObject materialDataMock = createObjectBuilder()
                .add("fileName", MATERIAL_FILE_NAME)
                .add("materialAddedDate", MATERIAL_ADDED_DATE.toString())
                .add("mimeType", MIME_TYPE)
                .build();
        mockDocumentTypeAccessService();

        final CaseReferredForCourtHearing caseReferral = caseReferredForCourtHearing()
                .withCaseId(CASE_ID)
                .withReferredAt(REFERRED_AT)
                .build();

        final List<Document> caseDocuments = new ArrayList<>();
        caseDocuments.add(Document.document()
                .withId(randomUUID())
                .withDocumentType(EMPLOYER_ATTACHMENT_TO_EARNINGS)
                .withMaterialId(MATERIAL_ID)
                .build());
        caseDocuments.add(Document.document()
                .withId(randomUUID())
                .withDocumentType(RESULT_ORDER)
                .withMaterialId(MATERIAL_ID)
                .build());

        final CaseDetails caseDetails = caseDetails().withCaseDocuments(caseDocuments).build();

        final List<CourtDocumentView> courtDocuments = courtDocumentsDataSourcingService.createCourtDocumentViews(caseReferral.getReferredAt(), caseDetails, envelope);

        assertThat(courtDocuments, hasSize(0));

    }

    @Test
    public void shouldFilterOutApplicationDocumentTypeIfNoCurrentApplication() {
        final JsonObject materialDataMock = createObjectBuilder()
                .add("fileName", MATERIAL_FILE_NAME)
                .add("materialAddedDate", MATERIAL_ADDED_DATE.toString())
                .add("mimeType", MIME_TYPE)
                .build();
        mockDocumentTypeAccessService();

        final CaseReferredForCourtHearing caseReferral = caseReferredForCourtHearing()
                .withCaseId(CASE_ID)
                .withReferredAt(REFERRED_AT)
                .build();

        final List<Document> caseDocuments = new ArrayList<>();
        caseDocuments.add(Document.document()
                .withId(randomUUID())
                .withDocumentType(SJP_APPLICATION_DOCUMENT_TYPE)
                .withMaterialId(MATERIAL_ID)
                .build());
        caseDocuments.add(Document.document()
                .withId(randomUUID())
                .withDocumentType(SJP_APPLICATION_DOCUMENT_TYPE)
                .withMaterialId(MATERIAL_ID)
                .build());

        final CaseDetails caseDetails = caseDetails().withCaseDocuments(caseDocuments).build();

        final List<CourtDocumentView> courtDocuments = courtDocumentsDataSourcingService.createCourtDocumentViews(caseReferral.getReferredAt(), caseDetails, envelope);

        assertThat(courtDocuments, hasSize(0));

    }


    private void mockDocumentTypeAccessService() {
        final List<DocumentTypeAccess> documentsTypeAccess = Arrays.asList(
                new DocumentTypeAccess(CASE_SUMMARY_DOCUMENT_TYPE_ID, "Case Summary"),
                new DocumentTypeAccess(APPLICATIONS_DOCUMENT_TYPE_ID, "Applications")
        );

        when(referenceDataService.getDocumentTypeAccess(MATERIAL_ADDED_DATE.toLocalDate(), envelope)).thenReturn(documentsTypeAccess);
    }

    private CaseDetails.Builder caseDetails() {
        return CaseDetails.caseDetails()
                .withId(CASE_ID)
                .withDefendant(defendant()
                        .withId(DEFENDANT_ID)
                        .withOffences(singletonList(offence().withId(OFFENCE_ID).build()))
                        .build());
    }

    private JsonEnvelope envelope() {
        return envelopeFrom(metadataWithRandomUUID("sjp.events.case-referred-for-court-hearing"), NULL);
    }
}
