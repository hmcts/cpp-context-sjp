package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.Objects.isNull;
import static uk.gov.moj.cpp.sjp.event.CaseNoteAdded.caseNoteAdded;
import static uk.gov.moj.cpp.sjp.event.CaseReferralForCourtHearingRejectionRecorded.caseReferralForCourtHearingRejectionRecorded;
import static uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing.caseReferredForCourtHearing;
import static uk.gov.moj.cpp.sjp.event.CaseUpdateRejected.RejectReason.CASE_REFERRED_FOR_COURT_HEARING;

import uk.gov.justice.json.schemas.domains.sjp.ListingDetails;
import uk.gov.justice.json.schemas.domains.sjp.Note;
import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseNotFound;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class CourtReferralHandler {

    public static final CourtReferralHandler INSTANCE = new CourtReferralHandler();

    private CourtReferralHandler() {
    }

    public Stream<Object> referCaseForCourtHearing(final UUID caseId,
                                                   final UUID decisionId,
                                                   final UUID sessionId,
                                                   final User legalAdviser,
                                                   final ListingDetails listingDetails,
                                                   final CaseAggregateState state) {

        final Stream.Builder<Object> streamBuilder = Stream.builder();

        if (isNull(state.getCaseId())) {
            streamBuilder.add(new CaseNotFound(caseId, "Referral of non existing case for court hearing rejected"));
        } else if (state.isCaseReferredForCourtHearing()) {
            streamBuilder.add(new CaseUpdateRejected(caseId, CASE_REFERRED_FOR_COURT_HEARING));
        } else {

            final Optional<Note> listingNotes = Optional.ofNullable(listingDetails.getListingNotes());

            streamBuilder.add(caseReferredForCourtHearing()
                    .withCaseId(state.getCaseId())
                    .withUrn(state.getUrn())
                    .withSessionId(sessionId)
                    .withReferralReasonId(listingDetails.getReferralReasonId())
                    .withHearingTypeId(listingDetails.getHearingTypeId())
                    .withEstimatedHearingDuration(listingDetails.getEstimatedHearingDuration())
                    .withListingNotes(listingNotes.map(Note::getText).orElse(null))
                    .withReferredAt(listingDetails.getRequestedAt())
                    .build());

            listingNotes.ifPresent(note -> streamBuilder.add(caseNoteAdded()
                    .withCaseId(state.getCaseId())
                    .withAuthor(legalAdviser)
                    .withDecisionId(decisionId)
                    .withNote(note)
                    .build()));
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
