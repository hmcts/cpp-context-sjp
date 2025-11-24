package uk.gov.moj.cpp.sjp.query.view.response;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class CasesMissingSjpnView {

    public final List<String> ids;

    public final List<CaseSummaryView> cases;

    public final int count;

    public CasesMissingSjpnView(List<String> caseIds, List<CaseSummaryView> cases, int totalCount) {
        this.ids = ImmutableList.copyOf(caseIds);
        this.cases = ImmutableList.copyOf(cases);
        this.count = totalCount;
    }
}
