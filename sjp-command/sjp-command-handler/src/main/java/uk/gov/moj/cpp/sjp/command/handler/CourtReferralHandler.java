package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.RecordCaseReferralForCourtHearingRejection;
import uk.gov.moj.cpp.sjp.ReferCaseForCourtHearing;

@ServiceComponent(Component.COMMAND_HANDLER)
public class CourtReferralHandler extends CaseCommandHandler {

    @Handles("sjp.command.refer-case-for-court-hearing")
    public void referCaseForCourtHearing(final Envelope<ReferCaseForCourtHearing> command) throws EventStreamException {
        final ReferCaseForCourtHearing referCaseForCourtHearing = command.payload();

        applyToCaseAggregate(referCaseForCourtHearing.getCaseId(), command, aggregate -> aggregate.referCaseForCourtHearing(
                referCaseForCourtHearing.getCaseId(),
                referCaseForCourtHearing.getDecisionId(),
                referCaseForCourtHearing.getSessionId(),
                referCaseForCourtHearing.getLegalAdviser(),
                referCaseForCourtHearing.getListingDetails()
        ));
    }

    @Handles("sjp.command.record-case-referral-for-court-hearing-rejection")
    public void recordCaseReferralForCourtHearingRejection(final Envelope<RecordCaseReferralForCourtHearingRejection> command) throws EventStreamException {
        final RecordCaseReferralForCourtHearingRejection courtReferralRejection = command.payload();

        applyToCaseAggregate(courtReferralRejection.getCaseId(), command, aggregate -> aggregate.recordCaseReferralForCourtHearingRejection(
                courtReferralRejection.getCaseId(),
                courtReferralRejection.getRejectionReason(),
                courtReferralRejection.getRejectedAt()
        ));
    }
}
