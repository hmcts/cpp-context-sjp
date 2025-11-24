package uk.gov.moj.cpp.sjp.domain.common;

import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.UNKNOWN;

import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;

import com.google.common.base.Objects;

public class CaseState {
    public static final CaseState INVALID_CASE_STATE = new CaseState(CaseStatus.UNKNOWN, CaseReadinessReason.UNKNOWN);

    private CaseStatus caseStatus;
    private CaseReadinessReason caseReadinessReason;

    public CaseState(final CaseStatus caseStatus, final CaseReadinessReason caseReadinessReason) {
        this.caseStatus = caseStatus;
        this.caseReadinessReason = caseReadinessReason;
    }

    public CaseState(final CaseStatus caseStatus) {
        this.caseStatus = caseStatus;
        this.caseReadinessReason = UNKNOWN;
    }

    public CaseStatus getCaseStatus() {
        return caseStatus;
    }

    public CaseReadinessReason getCaseReadinessReason() {
        return caseReadinessReason;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CaseState caseState = (CaseState) o;
        return caseStatus == caseState.caseStatus &&
                caseReadinessReason == caseState.caseReadinessReason;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(caseStatus, caseReadinessReason);
    }
}
