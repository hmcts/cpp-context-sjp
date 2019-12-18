package uk.gov.moj.cpp.sjp.domain.aggregate.domain;

import static uk.gov.moj.cpp.sjp.domain.Priority.HIGH;
import static uk.gov.moj.cpp.sjp.domain.Priority.LOW;
import static uk.gov.moj.cpp.sjp.domain.Priority.MEDIUM;

import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.Priority;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;

public final class SessionRules {
    private SessionRules(){}

    public static Priority getPriority(final CaseAggregateState state) {
        if (state.withdrawalRequestedOnAllOffences()) { // for multiple offences
            return HIGH;
        }
        if (!state.getOffenceIdsWithPleas().isEmpty()) {
            return MEDIUM;
        }
        return LOW;
    }

    public static SessionType getSessionType(final CaseReadinessReason readinessReason) {
        final SessionType sessionType;
        switch (readinessReason) {
            case PIA:
            case PLEADED_GUILTY:
                sessionType = SessionType.MAGISTRATE;
                break;
            default:
                sessionType = SessionType.DELEGATED_POWERS;
        }
        return sessionType;
    }
}
