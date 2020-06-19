package uk.gov.moj.cpp.sjp.event.processor.service.referral;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.json.schemas.domains.sjp.queries.Defendant.defendant;
import static uk.gov.justice.json.schemas.domains.sjp.queries.Offence.offence;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing.caseReferredForCourtHearing;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Document;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.MaterialView;
import uk.gov.moj.cpp.sjp.event.processor.service.MaterialService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers.CourtDocumentsViewHelper;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CourtDocumentsDataSourcingServiceTest {

    private static final UUID MATERIAL_ID = randomUUID();
    private static final UUID DOCUMENT_ID = randomUUID();
    private static final UUID DOCUMENT_TYPE_ID = randomUUID();
    private static final UUID CASE_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID OFFENCE_ID = randomUUID();
    private static final ZonedDateTime MATERIAL_ADDED_DATE = ZonedDateTime.now();
    private static final ZonedDateTime REFERRED_AT = ZonedDateTime.now();
    private static final String MATERIAL_FILE_NAME = "Material Name";
    private static final String MIME_TYPE = "Mime Type";
    private static final String SJP_DOCUMENT_TYPE = "SJPN";
    private static final String CC_DOCUMENT_TYPE = "Case Summary";
    @Mock
    private MaterialService materialService;
    @Mock
    private ReferenceDataService referenceDataService;
    @Mock
    private CourtDocumentsViewHelper courtDocumentsViewHelper;
    @InjectMocks
    private CourtDocumentsDataSourcingService courtDocumentsDataSourcingService;

    @Test
    public void shouldCreateCourtDocumentViews() {
        final JsonEnvelope emptyEnvelopeWithReferralEventMetadata =
                envelopeFrom(metadataWithRandomUUID("sjp.events.case-referred-for-court-hearing"), NULL);

        final CaseReferredForCourtHearing caseReferredForCourtHearing = caseReferredForCourtHearing()
                .withCaseId(CASE_ID)
                .withReferredAt(REFERRED_AT)
                .build();

        final CaseDetails caseDetails = createCaseDetails();

        final JsonObject materialDataMock = createObjectBuilder()
                .add("fileName", MATERIAL_FILE_NAME)
                .add("materialAddedDate", MATERIAL_ADDED_DATE.toString())
                .add("mimeType", MIME_TYPE)
                .build();

        final JsonObject documentsMetadataMock = createDocumentsMetadata();

        final Map<UUID, MaterialView> expectedMaterialMetaDataMap = createDocumentIdToMaterialView();
        final Map<UUID, UUID> expectedDocumentMetaDataMap = createDocumentIdToDocumentTypeId();

        when(materialService.getMaterialMetadata(MATERIAL_ID, emptyEnvelopeWithReferralEventMetadata)).thenReturn(materialDataMock);
        when(referenceDataService.getDocumentTypeAccess(MATERIAL_ADDED_DATE.toLocalDate(), emptyEnvelopeWithReferralEventMetadata)).thenReturn(documentsMetadataMock);

        courtDocumentsDataSourcingService.createCourtDocumentViews(caseReferredForCourtHearing, caseDetails, emptyEnvelopeWithReferralEventMetadata);

        verify(courtDocumentsViewHelper).createCourtDocumentViews(caseDetails, expectedMaterialMetaDataMap, expectedDocumentMetaDataMap);
    }

    private JsonObject createDocumentsMetadata() {
        return createObjectBuilder()
                .add("documentsTypeAccess", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("section", CC_DOCUMENT_TYPE)
                                .add("id", DOCUMENT_TYPE_ID.toString())))
                .build();
    }

    private CaseDetails createCaseDetails() {
        return CaseDetails.caseDetails()
                .withId(CASE_ID)
                .withDefendant(defendant()
                        .withId(DEFENDANT_ID)
                        .withOffences(
                                singletonList(offence().withId(OFFENCE_ID).build()))
                        .build())
                .withCaseDocuments(singletonList(
                        Document.document()
                                .withId(DOCUMENT_ID)
                                .withDocumentType(SJP_DOCUMENT_TYPE)
                                .withMaterialId(MATERIAL_ID)
                                .build()
                ))
                .build();
    }

    private Map<UUID, UUID> createDocumentIdToDocumentTypeId() {
        return ImmutableMap.of(DOCUMENT_ID, DOCUMENT_TYPE_ID);
    }

    private Map<UUID, MaterialView> createDocumentIdToMaterialView() {
        return ImmutableMap.of(DOCUMENT_ID, new MaterialView(MATERIAL_ID
                , MATERIAL_FILE_NAME, MATERIAL_ADDED_DATE, MIME_TYPE));
    }
}
