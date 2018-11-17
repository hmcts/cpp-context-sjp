package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.Objects.isNull;
import static uk.gov.moj.cpp.sjp.event.CaseReferralForCourtHearingRejectionRecorded.caseReferralForCourtHearingRejectionRecorded;
import static uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing.caseReferredForCourtHearing;
import static uk.gov.moj.cpp.sjp.event.CaseUpdateRejected.RejectReason.CASE_REFERRED_FOR_COURT_HEARING;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseNotFound;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

public class CourtReferralHandler {

    public static final CourtReferralHandler INSTANCE = new CourtReferralHandler();

    private CourtReferralHandler() {
    }

    @SuppressWarnings("squid:S00107")
    public Stream<Object> referCaseForCourtHearing(final UUID caseId,
                                                   final UUID sessionId,
                                                   final UUID referralReasonId,
                                                   final UUID hearingTypeId,
                                                   final String listingNotes,
                                                   final Integer estimatedHearingDuration,
                                                   final ZonedDateTime referredAt,
                                                   final CaseAggregateState state) {

        final Stream.Builder<Object> streamBuilder = Stream.builder();

        if (isNull(state.getCaseId())) {
            streamBuilder.add(new CaseNotFound(caseId, "Referral of non existing case for court hearing rejected"));
        } else if (state.isCaseReferredForCourtHearing()) {
            streamBuilder.add(new CaseUpdateRejected(caseId, CASE_REFERRED_FOR_COURT_HEARING));
        } else {
            streamBuilder.add(caseReferredForCourtHearing()
                    .withCaseId(state.getCaseId())
                    .withSessionId(sessionId)
                    .withReferralReasonId(referralReasonId)
                    .withHearingTypeId(hearingTypeId)
                    .withEstimatedHearingDuration(estimatedHearingDuration)
                    .withListingNotes(listingNotes)
                    .withReferredAt(referredAt)
                    .build());
        }

        return streamBuilder.build();
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
