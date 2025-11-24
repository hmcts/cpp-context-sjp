package uk.gov.moj.cpp.sjp.domain.aggregate.domain;

import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.Priority;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;

import static uk.gov.moj.cpp.sjp.domain.Priority.HIGH;
import static uk.gov.moj.cpp.sjp.domain.Priority.LOW;
import static uk.gov.moj.cpp.sjp.domain.Priority.MEDIUM;

public final class SessionRules {
    private SessionRules(){}

    public static Priority getPriority(final CaseAggregateState state) {
        if (state.isSetAside() || state.withdrawalRequestedOnAllOffences()||state.hasPendingApplication()) { // for multiple offences
            return HIGH;
        }
        if (!state.getOffenceIdsWithPleas().isEmpty()) {
            return MEDIUM;
        }
        return LOW;
    }

    public static SessionType getSessionType(final CaseReadinessReason readinessReason,
                                             final boolean postConviction,
                                             final boolean setAside,final boolean hasPendingApplication) {
        if (setAside || postConviction || hasPendingApplication) {
            return SessionType.MAGISTRATE;
        }
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
