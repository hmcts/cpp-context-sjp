package uk.gov.moj.cpp.sjp.event.processor;

import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.resulting.event.DecisionToReferCaseForCourtHearingSaved;
import uk.gov.moj.cpp.sjp.ReferCaseForCourtHearing;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class CourtReferralProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtReferralProcessor.class);

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("public.resulting.decision-to-refer-case-for-court-hearing-saved")
    public void decisionToReferCaseForCourtHearingSaved(final Envelope<DecisionToReferCaseForCourtHearingSaved> event) {
        final DecisionToReferCaseForCourtHearingSaved decisionToReferCaseForCourtHearingSaved = event.payload();

        final ReferCaseForCourtHearing commandPayload = ReferCaseForCourtHearing.referCaseForCourtHearing()
                .withCaseId(decisionToReferCaseForCourtHearingSaved.getCaseId())
                .withSessionId(decisionToReferCaseForCourtHearingSaved.getSessionId())
                .withReferralReasonId(decisionToReferCaseForCourtHearingSaved.getReferralReasonId())
                .withHearingTypeId(decisionToReferCaseForCourtHearingSaved.getHearingTypeId())
                .withEstimatedHearingDuration(decisionToReferCaseForCourtHearingSaved.getEstimatedHearingDuration())
                .withListingNotes(decisionToReferCaseForCourtHearingSaved.getListingNotes())
                .withRequestedAt(decisionToReferCaseForCourtHearingSaved.getDecisionSavedAt())
                .build();

        final JsonEnvelope command = enveloper.withMetadataFrom(envelopeFrom(metadataFrom(event.metadata()), NULL), "sjp.command.refer-case-for-court-hearing")
                .apply(commandPayload);

        sender.send(command);
    }

    @Handles("sjp.events.case-referred-for-court-hearing")
    public void caseReferredForCourtHearing(final Envelope<CaseReferredForCourtHearing> envelope) {
        LOGGER.info("Case referred for court hearing {}", envelope.payload());
    }

}
