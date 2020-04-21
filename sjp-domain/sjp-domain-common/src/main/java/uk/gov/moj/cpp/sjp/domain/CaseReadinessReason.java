package uk.gov.moj.cpp.sjp.domain;

public enum CaseReadinessReason {
    PIA,
    PLEADED_GUILTY, // all the offences are gulty
    PLEADED_NOT_GUILTY,
    PLEADED_GUILTY_REQUEST_HEARING,
    WITHDRAWAL_REQUESTED,
    SET_ASIDE,
    UNKNOWN;


    public static final CaseReadinessReason DEFAULT_STATUS = UNKNOWN;
}
