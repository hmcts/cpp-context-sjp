package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.CourtDocumentView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.DefendantDocumentView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.DocumentCategoryView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.MaterialView;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CourtDocumentsViewHelper {

    public List<CourtDocumentView> createCourtDocumentViews(
            final CaseDetails caseDetails,
            final Map<UUID, MaterialView> documentIdToMaterialView,
            final Map<UUID, UUID> documentIdToDocumentTypeId) {

        final DocumentCategoryView documentCategoryView =
                new DocumentCategoryView(
                        new DefendantDocumentView(
                                caseDetails.getId(),
                                singletonList(caseDetails.getDefendant().getId())));

        return caseDetails.getCaseDocuments().stream()
                .map(caseDocument -> new CourtDocumentView(
                        caseDocument.getId(),
                        documentCategoryView,
                        documentIdToMaterialView.get(caseDocument.getId()).getName(),
                        documentIdToDocumentTypeId.get(caseDocument.getId()),
                        documentIdToMaterialView.get(caseDocument.getId()).getMimeType(),
                        singletonList(documentIdToMaterialView.get(caseDocument.getId()))))
                .collect(toList());
    }
}
