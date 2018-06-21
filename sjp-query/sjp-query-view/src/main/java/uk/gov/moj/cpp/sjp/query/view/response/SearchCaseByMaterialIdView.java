package uk.gov.moj.cpp.sjp.query.view.response;


import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;

import java.util.UUID;

public class SearchCaseByMaterialIdView {

    private UUID caseId;
    private ProsecutingAuthority prosecutingAuthority;

    public SearchCaseByMaterialIdView(UUID caseId, ProsecutingAuthority prosecutingAuthority) {
        this.caseId = caseId;
        this.prosecutingAuthority = prosecutingAuthority;
    }

    public SearchCaseByMaterialIdView() {
        this(null, null);
    }

    public UUID getCaseId() {
        return caseId;
    }

    public ProsecutingAuthority getProsecutingAuthority() {
        return prosecutingAuthority;
    }
}
