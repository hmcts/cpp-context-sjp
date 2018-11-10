package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseNotFound;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;
import uk.gov.moj.cpp.sjp.event.DefendantNotFound;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HandlerUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(HandlerUtils.class);

    private HandlerUtils() {
    }

    public static Optional<Stream<Object>> createRejectionEvents(final UUID userId,
                                                                 final String action,
                                                                 final UUID defendantId,
                                                                 final CaseAggregateState state) {
        Object event = null;
        if (isNull(state.getCaseId())) {
            LOGGER.warn("Case not found: {}", action);
            event = new CaseNotFound(null, action);
        } else if (nonNull(defendantId) && !state.hasDefendant(defendantId)) {
            LOGGER.warn("Defendant not found: {}", action);
            event = new DefendantNotFound(defendantId, action);
        } else if (nonNull(state.getAssigneeId()) && !state.isAssignee(userId)) {
            LOGGER.warn("Update rejected because case is assigned to another user: {}", action);
            event = new CaseUpdateRejected(state.getCaseId(), CaseUpdateRejected.RejectReason.CASE_ASSIGNED);
        } else if (state.isCaseCompleted()) {
            LOGGER.warn("Update rejected because case is already completed: {}", action);
            event = new CaseUpdateRejected(state.getCaseId(), CaseUpdateRejected.RejectReason.CASE_COMPLETED);
        } else if (state.isCaseReferredForCourtHearing()) {
            LOGGER.warn("Update rejected because case is referred to court for hearing: {}", action);
            event = new CaseUpdateRejected(state.getCaseId(), CaseUpdateRejected.RejectReason.CASE_REFERRED_FOR_COURT_HEARING);
        }

        return Optional.ofNullable(event).map(Stream::of);
    }

}
