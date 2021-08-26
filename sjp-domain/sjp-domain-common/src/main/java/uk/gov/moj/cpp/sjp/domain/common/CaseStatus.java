package uk.gov.moj.cpp.sjp.domain.common;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

@SuppressWarnings({"squid:S1067"})
public enum CaseStatus {
    NO_PLEA_RECEIVED,
    NO_PLEA_RECEIVED_READY_FOR_DECISION, // PIA
    PLEA_RECEIVED_READY_FOR_DECISION, // check the plea
    PLEA_RECEIVED_NOT_READY_FOR_DECISION,
    SET_ASIDE_READY_FOR_DECISION,
    WITHDRAWAL_REQUEST_READY_FOR_DECISION, // WITHDRAWAL_REQUESTED
    REFERRED_FOR_COURT_HEARING,
    COMPLETED,
    REOPENED_IN_LIBRA,
    COMPLETED_APPLICATION_PENDING,
    RELISTED,
    APPEALED,
    UNKNOWN;

    private static final Set<CaseStatus> NOT_ALLOWED_FROM_NON_READY = newHashSet(COMPLETED, REOPENED_IN_LIBRA, REFERRED_FOR_COURT_HEARING);
    private static final Set<CaseStatus> TERMINAL_STATUS = newHashSet(COMPLETED, REFERRED_FOR_COURT_HEARING, REOPENED_IN_LIBRA);

    private static final Set<CaseStatus> READY_STATUSES
            = newHashSet(NO_PLEA_RECEIVED_READY_FOR_DECISION,SET_ASIDE_READY_FOR_DECISION,
            PLEA_RECEIVED_READY_FOR_DECISION, WITHDRAWAL_REQUEST_READY_FOR_DECISION, COMPLETED, REOPENED_IN_LIBRA, COMPLETED_APPLICATION_PENDING);

    // In single offence times, the default case was NO_PLEA_RECEIVED_READY_FOR_DECISION
    // Now we throw an exception if a case is not covered
    public static final CaseStatus DEFAULT_STATUS = UNKNOWN;

    public static boolean isAReadyStatus(CaseStatus caseStatus) { // TODO rather should be on enum state ??
        return READY_STATUSES.contains(caseStatus);
    }

    public static boolean isAllowedStatusFromComplete(final CaseStatus caseStatus) {
        return caseStatus == REOPENED_IN_LIBRA || caseStatus == COMPLETED_APPLICATION_PENDING ;
    }

    public static boolean isAllowedStatusFromReopenedToLibra(final CaseStatus caseStatus) {
        return caseStatus == COMPLETED;
    }

    public static boolean isNotAllowedFromNonReady(final CaseStatus caseStatus) {
        return NOT_ALLOWED_FROM_NON_READY.contains(caseStatus);
    }

    public static boolean isTerminalStatus(final CaseStatus caseStatus) {
        return TERMINAL_STATUS.contains(caseStatus);
    }
}
