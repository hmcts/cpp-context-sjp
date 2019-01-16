package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseAdjournedForLaterSjpHearingRecorded;
import uk.gov.moj.cpp.sjp.event.CaseNotFound;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaseAdjournmentHandler {

    public static final CaseAdjournmentHandler INSTANCE = new CaseAdjournmentHandler();

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseAdjournmentHandler.class);

    private CaseAdjournmentHandler() {
    }

    public Stream<Object> recordCaseAdjournedToLaterSjpHearing(final UUID caseId,
                                                               final UUID sessionId,
                                                               final LocalDate adjournedTo,
                                                               final CaseAggregateState state) {

        final Object event;

        if (!state.isCaseIdEqualTo(caseId)) {
            LOGGER.error("Mismatch of IDs in aggregate: {} != {}", state.getCaseId(), caseId);
            event = new CaseNotFound(null, "Record case adjournment to later date");
        } else {
            event = new CaseAdjournedForLaterSjpHearingRecorded(
                    adjournedTo,
                    caseId,
                    sessionId);
        }

        return Stream.of(event);
    }
}
