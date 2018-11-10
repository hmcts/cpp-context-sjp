package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.ReferCaseForCourtHearing;

@ServiceComponent(Component.COMMAND_HANDLER)
public class CourtReferralHandler extends CaseCommandHandler {

    @Handles("sjp.command.refer-case-for-court-hearing")
    public void referCaseForCourtHearing(final Envelope<ReferCaseForCourtHearing> command) throws EventStreamException {
        final ReferCaseForCourtHearing referCaseForCourtHearing = command.payload();

        applyToCaseAggregate(referCaseForCourtHearing.getCaseId(), command, aggregate -> aggregate.referCaseForCourtHearing(
                referCaseForCourtHearing.getCaseId(),
                referCaseForCourtHearing.getSessionId(),
                referCaseForCourtHearing.getReferralReasonId(),
                referCaseForCourtHearing.getHearingTypeId(),
                referCaseForCourtHearing.getEstimatedHearingDuration(),
                referCaseForCourtHearing.getListingNotes(),
                referCaseForCourtHearing.getRequestedAt()
        ));
    }
}
