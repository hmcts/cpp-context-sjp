package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

public enum CaseReadinessEventRaised {
    NONE,
    CASE_MARKED_READY_FOR_DECISION_RAISED,
    CASE_UNMARKED_READY_FOR_DECISION_RAISED,
    CASE_EXPECTED_DATE_READY_CHANGED_RAISED;
}