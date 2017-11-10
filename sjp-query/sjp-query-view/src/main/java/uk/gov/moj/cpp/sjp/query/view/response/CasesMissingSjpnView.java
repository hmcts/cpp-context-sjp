package uk.gov.moj.cpp.sjp.query.view.response;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class CasesMissingSjpnView {
    public final List<String> ids;
    public final int count;

    public CasesMissingSjpnView(List<String> caseIds, int totalCount) {
        this.ids = ImmutableList.copyOf(caseIds);
        this.count = totalCount;
    }
}
