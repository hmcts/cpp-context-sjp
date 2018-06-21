package uk.gov.moj.cpp.sjp.query.view.response;

import uk.gov.moj.cpp.sjp.persistence.entity.PendingDatesToAvoid;

import java.util.List;
import java.util.stream.Collectors;

public class CasesPendingDatesToAvoidView {

    private final List<CasePendingDatesToAvoidView> cases;

    private final int count;

    public CasesPendingDatesToAvoidView(final List<PendingDatesToAvoid> cases) {
        this.cases = cases.stream().map(CasePendingDatesToAvoidView::new).collect(Collectors.toList());
        this.count = this.cases.size();
    }

    public List<CasePendingDatesToAvoidView> getCases() {
        return this.cases;
    }

    public int getCount() {
        return this.count;
    }
}