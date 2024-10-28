package uk.gov.moj.cpp.sjp.event.processor.service.referral;

import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.normalizeSpace;
import static org.apache.commons.lang3.StringUtils.upperCase;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Document;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ApplicationDocumentView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.CourtDocumentView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.DefendantDocumentView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.DocumentCategoryView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.MaterialView;
import uk.gov.moj.cpp.sjp.event.processor.service.MaterialService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers.CaseDocumentTypeHelper;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

public class CourtDocumentsDataSourcingService {

    private static final String FINANCIAL_MEANS = "FINANCIAL_MEANS";
    private static final String APPLICATION_DOCUMENT_TYPE = "APPLICATION";
    private static final Set<String> EXCLUDED_DOCUMENT_TYPES = new HashSet<>();

    static {
        EXCLUDED_DOCUMENT_TYPES.add("RESULT_ORDER");
        EXCLUDED_DOCUMENT_TYPES.add("EMPLOYER_ATTACHMENT_TO_EARNINGS");
        EXCLUDED_DOCUMENT_TYPES.add("ELECTRONIC_NOTIFICATIONS");
    }

    @Inject
    private MaterialService materialService;

    @Inject
    private ReferenceDataService referenceDataService;

    public List<CourtDocumentView> createCourtDocumentViews(final ZonedDateTime referredAt,
                                                            final CaseDetails caseDetails,
                                                            final JsonEnvelope envelope) {

        final LocalDate date = referredAt.toLocalDate();
        final List<DocumentTypeAccess> referenceDataDocumentTypes = referenceDataService.getDocumentTypeAccess(date, envelope);
        final List<CourtDocumentView> results = new ArrayList<>();
        final List<Document> caseDocumentsToBeSent = getCaseDocumentsForReferral(caseDetails);
        for (final Document caseDocument : caseDocumentsToBeSent) {

            final MaterialView material = getMaterial(caseDocument.getMaterialId(), envelope);
            final UUID documentTypeId = getDocumentTypeId(caseDocument.getDocumentType(), referenceDataDocumentTypes);
            final DocumentCategoryView documentCategory = createDocumentCategory(caseDetails, caseDocument.getDocumentType());

            final CourtDocumentView courtDocument = new CourtDocumentView(
                    caseDocument.getId(),
                    documentCategory,
                    material.getName(),
                    documentTypeId,
                    material.getMimeType(),
                    isFinancialMeans(caseDocument.getDocumentType()),
                    singletonList(material));

            results.add(courtDocument);
        }

        return results;
    }

    public List<CourtDocumentView> createCourtDocumentViews(final LocalDate date,
                                                            final CaseDetails caseDetails,
                                                            final JsonEnvelope envelope) {

        final List<DocumentTypeAccess> referenceDataDocumentTypes = referenceDataService.getDocumentTypeAccess(date, envelope);
        final List<CourtDocumentView> results = new ArrayList<>();
        final List<Document> caseDocumentsToBeSent = getCaseDocumentsForReferral(caseDetails);
        for (final Document caseDocument : caseDocumentsToBeSent) {

            final MaterialView material = getMaterial(caseDocument.getMaterialId(), envelope);
            final UUID documentTypeId = getDocumentTypeId(caseDocument.getDocumentType(), referenceDataDocumentTypes);
            final DocumentCategoryView documentCategory = createDocumentCategory(caseDetails, caseDocument.getDocumentType());

            final CourtDocumentView courtDocument = new CourtDocumentView(
                    caseDocument.getId(),
                    documentCategory,
                    material.getName(),
                    documentTypeId,
                    material.getMimeType(),
                    isFinancialMeans(caseDocument.getDocumentType()),
                    singletonList(material));

            results.add(courtDocument);
        }

        return results;
    }

    private MaterialView getMaterial(final UUID materialId, final JsonEnvelope envelope) {
        final JsonObject materialMetadata = materialService.getMaterialMetadata(materialId, envelope);

        return new MaterialView(
                materialId,
                materialMetadata.getString("fileName"),
                ZonedDateTime.parse(materialMetadata.getString("materialAddedDate")),
                materialMetadata.getString("mimeType"));
    }

    private UUID getDocumentTypeId(final String sjpDocumentType, final List<DocumentTypeAccess> referenceDataDocumentTypes) {
        final String referenceDataDocumentType = normalizedDocumentType(CaseDocumentTypeHelper.getDocumentType(sjpDocumentType));
        return referenceDataDocumentTypes
                .stream()
                .filter(documentTypeAccess -> normalizedDocumentType(documentTypeAccess.getDocumentType()).equals(referenceDataDocumentType))
                .findFirst()
                .map(DocumentTypeAccess::getDocumentTypeId)
                .orElse(null);
    }

    private DocumentCategoryView createDocumentCategory(final CaseDetails caseDetails, final String documentType) {
        final UUID caseId = caseDetails.getId();
        final UUID defendantId = caseDetails.getDefendant().getId();

        if (APPLICATION_DOCUMENT_TYPE.equals(documentType)) {
            return new DocumentCategoryView(null, new ApplicationDocumentView(
                    caseDetails.getCaseApplication().getApplicationId(),
                    defendantId,
                    caseId
            ));
        }

        return new DocumentCategoryView(new DefendantDocumentView(caseId, singletonList(defendantId)), null);
    }

    private Boolean isFinancialMeans(final String documentType) {
        if (isNull(documentType) || isEmpty(documentType)) {
            return Boolean.FALSE;
        }
        return documentType.contentEquals(FINANCIAL_MEANS);
    }

    private String normalizedDocumentType(final String documentType) {
        return normalizeSpace(upperCase(documentType));
    }

    private List<Document> getCaseDocumentsForReferral(final CaseDetails caseDetails) {

        return caseDetails.getCaseDocuments()
                .stream()
                .filter(caseDoc -> ! checkIfInExcludedSet(caseDoc))
                .filter(caseDoc -> !(caseDetails.getCaseApplication() == null && APPLICATION_DOCUMENT_TYPE.equals(caseDoc.getDocumentType())))
                .collect(Collectors.toList());
    }

    private boolean checkIfInExcludedSet(Document caseDoc) {
        return EXCLUDED_DOCUMENT_TYPES.contains(caseDoc.getDocumentType()) ||
                EXCLUDED_DOCUMENT_TYPES.stream().anyMatch(e -> caseDoc.getDocumentType().contains(e));
    }
}
