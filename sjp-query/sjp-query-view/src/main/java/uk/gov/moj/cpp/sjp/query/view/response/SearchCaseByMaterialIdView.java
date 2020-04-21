package uk.gov.moj.cpp.sjp.query.view.response;

import java.util.UUID;

public class SearchCaseByMaterialIdView {

    private UUID caseId;
    private String prosecutingAuthority;

    public SearchCaseByMaterialIdView(UUID caseId, String prosecutingAuthority) {
        this.caseId = caseId;
        this.prosecutingAuthority = prosecutingAuthority;
    }

    public SearchCaseByMaterialIdView() {
        this(null, null);
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getProsecutingAuthority() {
        return prosecutingAuthority;
    }
}
