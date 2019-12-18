package uk.gov.moj.cpp.sjp.query.view.response;

import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;

import java.time.LocalDate;

public class CaseSummaryView {

    private final String id;
    private final String urn;
    private final DefendantSummaryView defendant;
    private final ProsecutingAuthority prosecutingAuthority;
    private final LocalDate postingDate;

    public CaseSummaryView(final CaseDetail caseDetail) {
        this.id = caseDetail.getId().toString();
        this.urn = caseDetail.getUrn();
        this.prosecutingAuthority = caseDetail.getProsecutingAuthority();
        this.defendant = new DefendantSummaryView(caseDetail.getDefendant());
        this.postingDate = caseDetail.getPostingDate();
    }

    public String getId() {
        return id;
    }

    public String getUrn() {
        return urn;
    }

    public DefendantSummaryView getDefendant() {
        return defendant;
    }

    public ProsecutingAuthority getProsecutingAuthority() {
        return prosecutingAuthority;
    }

    public LocalDate getPostingDate() {
        return postingDate;
    }
}
