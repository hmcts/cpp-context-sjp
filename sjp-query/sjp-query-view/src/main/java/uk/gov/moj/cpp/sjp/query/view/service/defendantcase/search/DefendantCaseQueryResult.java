package uk.gov.moj.cpp.sjp.query.view.service.defendantcase.search;

import java.util.LinkedList;
import java.util.List;

public class DefendantCaseQueryResult {

    private final Integer totalResults;
    private final List<DefendantCase> cases = new LinkedList<>();

    public DefendantCaseQueryResult(final Integer totalResults, final List<DefendantCase> cases) {
        this.totalResults = totalResults;
        this.cases.addAll(cases);
    }

    public Integer getTotalResults() {
        return totalResults;
    }

    public List<DefendantCase> getCases() {
        return new LinkedList<>(cases);
    }

    @Override
    public String toString() {
        return "DefendantCaseResult{" +
                "totalResults=" + totalResults +
                ", cases=" + cases +
                '}';
    }
}
