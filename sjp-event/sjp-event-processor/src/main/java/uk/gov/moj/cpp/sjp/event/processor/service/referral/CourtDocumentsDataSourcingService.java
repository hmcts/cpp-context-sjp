package uk.gov.moj.cpp.sjp.event.processor.service.referral;

import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.normalizeSpace;
import static org.apache.commons.lang3.StringUtils.upperCase;

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
import java.util.function.Function;

import javax.inject.Inject;
import javax.json.JsonObject;

public class CourtDocumentsDataSourcingService {

    @Inject
    private MaterialService materialService;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private CourtDocumentsViewHelper courtDocumentsHelper;

    public List<CourtDocumentView> createCourtDocumentViews(
            final CaseReferredForCourtHearing caseReferredForCourtHearing,
            final CaseDetails caseDetails,
            final JsonEnvelope emptyEnvelopeWithReferralEventMetadata) {

        final Map<String, UUID> documentTypeToDocumentTypeId =
                getDocumentsTypes(caseReferredForCourtHearing.getReferredAt().toLocalDate(),
                        emptyEnvelopeWithReferralEventMetadata);

        final Map<UUID, MaterialView> documentIdToMaterialView =
                createDocumentIdToMaterialView(caseDetails, emptyEnvelopeWithReferralEventMetadata);

        final Map<UUID, UUID> documentIdToDocumentTypeId =
                createDocumentIdToDocumentTypeId(documentTypeToDocumentTypeId, caseDetails);

        return courtDocumentsHelper.createCourtDocumentViews(
                caseDetails,
                documentIdToMaterialView,
                documentIdToDocumentTypeId);
    }

    private Map<UUID, UUID> createDocumentIdToDocumentTypeId(
            final Map<String, UUID> documentsMetadata,
            final CaseDetails caseDetails) {

        return caseDetails
                .getCaseDocuments()
                .stream()
                .collect(toMap(
                        Document::getId,
                        toDocumentMetadataId(documentsMetadata))
                );
    }

    private Function<Document, UUID> toDocumentMetadataId(final Map<String, UUID> documentsMetadata) {
        return document -> {
            final String documentType = CaseDocumentTypeHelper.getDocumentType(document.getDocumentType());
            return documentsMetadata.getOrDefault(normalizedDocumentType(documentType), null);
        };
    }

    private Map<String, UUID> getDocumentsTypes(
            final LocalDate date,
            final JsonEnvelope emptyEnvelopeWithCourtDocumentsMetadata) {

        return referenceDataService.getDocumentTypeAccess(date,
                emptyEnvelopeWithCourtDocumentsMetadata)
                .getJsonArray("documentsTypeAccess")
                .getValuesAs(JsonObject.class)
                .stream()
                .collect(toMap(
                        metadata -> normalizedDocumentType(metadata.getString("section")),
                        metadata -> fromString(metadata.getString("id"))
                ));
    }

    private Map<UUID, MaterialView> createDocumentIdToMaterialView(
            final CaseDetails caseDetails,
            final JsonEnvelope emptyEnvelopeWithReferralEventMetadata) {

        return caseDetails
                .getCaseDocuments()
                .stream()
                .collect(
                        toMap(Document::getId,
                                document -> createMaterialView(
                                        document.getMaterialId(),
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

    private String normalizedDocumentType(final String documentType) {
        return normalizeSpace(upperCase(documentType));
    }
}
