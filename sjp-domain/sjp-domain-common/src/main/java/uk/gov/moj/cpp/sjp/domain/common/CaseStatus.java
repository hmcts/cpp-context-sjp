package uk.gov.moj.cpp.sjp.domain.common;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

public enum CaseStatus {
    NO_PLEA_RECEIVED,
    NO_PLEA_RECEIVED_READY_FOR_DECISION,
    WITHDRAWAL_REQUEST_READY_FOR_DECISION,
    PLEA_RECEIVED_READY_FOR_DECISION,
    PLEA_RECEIVED_NOT_READY_FOR_DECISION,
    REFERRED_FOR_COURT_HEARING,
    COMPLETED,
    REOPENED_IN_LIBRA;

    private CaseStatus guiltyPleaReceived() {
        if (this.equals(WITHDRAWAL_REQUEST_READY_FOR_DECISION)) {
            return WITHDRAWAL_REQUEST_READY_FOR_DECISION;
        } else {
            return PLEA_RECEIVED_READY_FOR_DECISION;
        }
    }

    private CaseStatus notGuiltyPleaReceived() {
        if (this.equals(WITHDRAWAL_REQUEST_READY_FOR_DECISION)) {
            return WITHDRAWAL_REQUEST_READY_FOR_DECISION;
        } else {
            return PLEA_RECEIVED_NOT_READY_FOR_DECISION;
        }
    }

    public CaseStatus pleaReceived(PleaType pleaType, String datesToAvoid) {
        if (pleaType == PleaType.NOT_GUILTY && isBlank(datesToAvoid)) {
            return notGuiltyPleaReceived();
        } else if (pleaType == PleaType.NOT_GUILTY && isNotBlank(datesToAvoid)) {
            return guiltyPleaReceived();
        } else if (pleaType == PleaType.GUILTY || pleaType == PleaType.GUILTY_REQUEST_HEARING) {
            return guiltyPleaReceived();
        }
        return this;
    }

    public CaseStatus cancelPlea(final Boolean provedInAbsence) {
        if (this != CaseStatus.WITHDRAWAL_REQUEST_READY_FOR_DECISION) {
            return ofNullable(provedInAbsence).map(pia -> pia ? NO_PLEA_RECEIVED_READY_FOR_DECISION : NO_PLEA_RECEIVED).orElse(this);
        }
        return this;
    }

    public CaseStatus markReadyCase(CaseReadinessReason newReadinessReason, String datesToAvoid) {
        if (this == CaseStatus.WITHDRAWAL_REQUEST_READY_FOR_DECISION) {
            if (newReadinessReason == CaseReadinessReason.PIA) {
                return NO_PLEA_RECEIVED_READY_FOR_DECISION;
            } else if (newReadinessReason == CaseReadinessReason.PLEADED_GUILTY
                    || newReadinessReason == CaseReadinessReason.PLEADED_GUILTY_REQUEST_HEARING) {
                return PLEA_RECEIVED_READY_FOR_DECISION;
            } else if (newReadinessReason == CaseReadinessReason.PLEADED_NOT_GUILTY && isNotBlank(datesToAvoid)) {
                return PLEA_RECEIVED_READY_FOR_DECISION;
            }
        } else if (CaseReadinessReason.PIA.equals(newReadinessReason)) {
            //When no plea received and certificate of service date >= 28 days
            return NO_PLEA_RECEIVED_READY_FOR_DECISION;
        } else if (CaseReadinessReason.PLEADED_NOT_GUILTY.equals(newReadinessReason)) {
            //When plea not guilty received and plea updated date > 10 days (activiti makes the decision)
            return PLEA_RECEIVED_READY_FOR_DECISION;
        }
        return this;
    }

    public CaseStatus unmarkReadyCase(PleaType pleaType, CaseReadinessReason readinessReason, String datesToAvoid) {
        if (this == CaseStatus.WITHDRAWAL_REQUEST_READY_FOR_DECISION) {
            if (PleaType.NOT_GUILTY == pleaType && isBlank(datesToAvoid)) {
                return PLEA_RECEIVED_NOT_READY_FOR_DECISION;
            } else if (PleaType.NOT_GUILTY == pleaType && isNotBlank(datesToAvoid)) {
                return PLEA_RECEIVED_READY_FOR_DECISION;
            } else if (pleaType == null) {
                return NO_PLEA_RECEIVED;
            }
        }//When the case from guilty plea received to not guilty plea
        else if (readinessReason == CaseReadinessReason.PLEADED_NOT_GUILTY && isBlank(datesToAvoid)) {
            return PLEA_RECEIVED_NOT_READY_FOR_DECISION;
        }
        return this;
    }

}
