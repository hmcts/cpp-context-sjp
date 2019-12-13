package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

@Event(CaseStatusChanged.EVENT_NAME)
public class CaseStatusChanged {

    public static final String EVENT_NAME = "sjp.events.case-status-changed";

    private final UUID caseId;
    private final CaseStatus caseStatus;

    @JsonCreator
    public CaseStatusChanged(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("caseStatus") final CaseStatus caseStatus) {
        this.caseId = caseId;
        this.caseStatus = caseStatus;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public CaseStatus getCaseStatus() {
        return caseStatus;
    }

    @Override
    public String toString() {
        return "CaseStatusChanged{" +
                "caseId=" + caseId +
                ", caseStatus=" + caseStatus +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CaseStatusChanged that = (CaseStatusChanged) o;
        return Objects.equal(caseId, that.caseId) &&
                Objects.equal(caseStatus, that.caseStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(caseId, caseStatus);
    }
}
