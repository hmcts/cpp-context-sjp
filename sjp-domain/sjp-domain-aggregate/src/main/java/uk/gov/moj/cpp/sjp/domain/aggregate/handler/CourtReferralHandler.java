package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.Objects.isNull;
import static uk.gov.moj.cpp.sjp.event.CaseReferralForCourtHearingRejectionRecorded.caseReferralForCourtHearingRejectionRecorded;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseNotFound;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

public class CourtReferralHandler {

    public static final CourtReferralHandler INSTANCE = new CourtReferralHandler();

    private CourtReferralHandler() {
    }

    public Stream<Object> recordCaseReferralForCourtHearingRejection(
            final UUID caseId,
            final String rejectionReason,
            final ZonedDateTime rejectionTimestamp,
            final CaseAggregateState state) {

        final Stream.Builder<Object> streamBuilder = Stream.builder();

        if (isNull(state.getCaseId())) {
            streamBuilder.add(new CaseNotFound(caseId, "Referral rejection recording of non existing case rejected"));
        } else if (state.isCaseReferredForCourtHearing()) {
            streamBuilder.add(caseReferralForCourtHearingRejectionRecorded()
                    .withCaseId(caseId)
                    .withRejectionReason(rejectionReason)
                    .withRejectedAt(rejectionTimestamp)
                    .build());
        }

        return streamBuilder.build();
    }
}
