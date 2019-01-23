package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseAdjournedForLaterSjpHearingRecorded;
import uk.gov.moj.cpp.sjp.event.CaseNotFound;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;

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

        if (!state.isCaseIdEqualTo(caseId)) {
            LOGGER.error("Mismatch of IDs in aggregate: {} != {}", state.getCaseId(), caseId);
            return Stream.of(new CaseNotFound(null, "Record case adjournment to later date"));
        }
        return Stream.of(
                new CaseAdjournedForLaterSjpHearingRecorded(
                        adjournedTo,
                        caseId,
                        sessionId),
                new CaseUnassigned(caseId));
    }
}