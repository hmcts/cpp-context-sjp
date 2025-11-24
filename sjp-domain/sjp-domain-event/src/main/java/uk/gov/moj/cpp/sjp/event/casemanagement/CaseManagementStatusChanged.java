package uk.gov.moj.cpp.sjp.event.casemanagement;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.common.CaseManagementStatus;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

@Event(CaseManagementStatusChanged.EVENT_NAME)
public class CaseManagementStatusChanged {

    public static final String EVENT_NAME = "sjp.events.case-management-status-changed";

    private final UUID caseId;
    private final CaseManagementStatus caseManagementStatus;

    @JsonCreator
    public CaseManagementStatusChanged(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("caseManagementStatus") final CaseManagementStatus caseManagementStatus) {
        this.caseId = caseId;
        this.caseManagementStatus = caseManagementStatus;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public CaseManagementStatus getCaseManagementStatus() {
        return caseManagementStatus;
    }

    @Override
    public String toString() {
        return "CaseManagementStatusChanged{" +
                "caseId=" + caseId +
                ", caseManagementStatus=" + caseManagementStatus +
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
        final CaseManagementStatusChanged that = (CaseManagementStatusChanged) o;
        return Objects.equal(caseId, that.caseId) &&
                Objects.equal(caseManagementStatus, that.caseManagementStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(caseId, caseManagementStatus);
    }
}
