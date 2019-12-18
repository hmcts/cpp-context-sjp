package uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.casemarkedready;

public enum CaseReadinessReason {
    PIA,
    PLEADED_GUILTY,
    PLEADED_NOT_GUILTY,
    PLEADED_GUILTY_REQUEST_HEARING,
    WITHDRAWAL_REQUESTED,
    UNKNOWN;

    public static final CaseReadinessReason DEFAULT_STATUS = UNKNOWN;
}