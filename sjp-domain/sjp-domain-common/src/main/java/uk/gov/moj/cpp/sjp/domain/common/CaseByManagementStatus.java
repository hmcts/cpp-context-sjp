package uk.gov.moj.cpp.sjp.domain.common;

import java.io.Serializable;
import java.util.UUID;

public class CaseByManagementStatus implements Serializable {

    private final UUID caseId;
    private final CaseManagementStatus caseManagementStatus;

    public CaseByManagementStatus(final UUID caseId, final CaseManagementStatus caseManagementStatus) {
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
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
