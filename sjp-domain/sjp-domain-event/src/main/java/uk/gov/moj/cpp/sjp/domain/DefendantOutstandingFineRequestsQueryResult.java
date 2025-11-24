package uk.gov.moj.cpp.sjp.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DefendantOutstandingFineRequestsQueryResult {

    private List<DefendantOutstandingFineRequest> defendantDetails = new ArrayList<>();

    @JsonCreator
    public DefendantOutstandingFineRequestsQueryResult(@JsonProperty("defendantDetails") final List<DefendantOutstandingFineRequest> defendantDetails) {
        this.defendantDetails = Collections.unmodifiableList(defendantDetails);
    }

    public DefendantOutstandingFineRequestsQueryResult() {
    }

    public List<DefendantOutstandingFineRequest> getDefendantDetails() {
        return Collections.unmodifiableList(defendantDetails);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DefendantOutstandingFineRequestsQueryResult that = (DefendantOutstandingFineRequestsQueryResult) o;
        return Objects.equals(defendantDetails, that.defendantDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(defendantDetails);
    }
}