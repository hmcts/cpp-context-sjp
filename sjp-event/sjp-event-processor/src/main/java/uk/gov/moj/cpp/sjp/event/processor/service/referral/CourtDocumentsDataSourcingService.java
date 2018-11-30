package uk.gov.moj.cpp.sjp.event.processor.service.referral;

import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toMap;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Document;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.CourtDocumentView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.MaterialView;
import uk.gov.moj.cpp.sjp.event.processor.service.MaterialService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers.CaseDocumentTypeHelper;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers.CourtDocumentsViewHelper;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class CourtDocumentsDataSourcingService {

    @Inject
    private MaterialService materialService;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private CourtDocumentsViewHelper courtDocumentsHelper;

    @Inject
    private CaseDocumentTypeHelper caseDocumentTypeHelper;

    public List<CourtDocumentView> createCourtDocumentViews(
            final CaseReferredForCourtHearing caseReferredForCourtHearing,
            final CaseDetails caseDetails,
            final JsonEnvelope emptyEnvelopeWithReferralEventMetadata) {

        final Map<String, UUID> documentsMetadata =
                getDocumentsMetadata(caseReferredForCourtHearing.getReferredAt().toLocalDate(),
                        emptyEnvelopeWithReferralEventMetadata);

        final Map<String, MaterialView> documentIdToMaterialView =
                createDocumentIdToMaterialView(caseDetails, emptyEnvelopeWithReferralEventMetadata);

        final Map<String, UUID> documentIdToDocumentTypeId =
                createDocumentIdToDocumentTypeId(documentsMetadata, caseDetails);

        return courtDocumentsHelper.createCourtDocumentViews(
                caseDetails,
                documentIdToMaterialView,
                documentIdToDocumentTypeId);
    }

    private Map<String, UUID> createDocumentIdToDocumentTypeId(
            final Map<String, UUID> documentsMetadata,
            final CaseDetails caseDetails) {

        return caseDetails
                .getCaseDocuments()
                .stream()
                .collect(
                        toMap(Document::getId,
                                document -> documentsMetadata.get(caseDocumentTypeHelper
                                        .getDocumentType(document.getDocumentType().trim()))));
    }

    private Map<String, UUID> getDocumentsMetadata(
            final LocalDate date,
            final JsonEnvelope emptyEnvelopeWithCourtDocumentsMetadata) {

        return referenceDataService.getDocumentMetadata(date,
                emptyEnvelopeWithCourtDocumentsMetadata)
                .getJsonArray("documentsMetadata")
                .getValuesAs(JsonObject.class)
                .stream()
                .collect(toMap(
                        metadata -> metadata.getString("documentType"),
                        metadata -> fromString(metadata.getString("uuid"))));
    }

    private Map<String, MaterialView> createDocumentIdToMaterialView(
            final CaseDetails caseDetails,
            final JsonEnvelope emptyEnvelopeWithReferralEventMetadata) {

        return caseDetails
                .getCaseDocuments()
                .stream()
                .collect(
                        toMap(Document::getId,
                                document -> createMaterialView(
                                        fromString(document.getMaterialId()),
                                        emptyEnvelopeWithReferralEventMetadata)));
    }

    private MaterialView createMaterialView(
            UUID materialId,
            final JsonEnvelope emptyEnvelopeWithReferralEventMetadata) {
        final JsonObject materialMetadata =
                materialService.getMaterialMetadata(
                        materialId,
                        emptyEnvelopeWithReferralEventMetadata);

        return new MaterialView(
                materialId,
                materialMetadata.getString("fileName"),
                ZonedDateTime.parse(materialMetadata.getString("materialAddedDate")),
                materialMetadata.getString("mimeType"));
    }
}
