package uk.gov.moj.cpp.sjp.event.processor;

import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.sjp.RecordCaseReferralForCourtHearingRejection.recordCaseReferralForCourtHearingRejection;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.resulting.event.DecisionToReferCaseForCourtHearingSaved;
import uk.gov.moj.cpp.sjp.ReferCaseForCourtHearing;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class CourtReferralProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtReferralProcessor.class);

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Inject
    private Clock clock;

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

    @Handles("public.progression.refer-prosecution-cases-to-court-rejected")
    public void referToCourtHearingRejected(final JsonEnvelope event) {
        final JsonObject rejectionEvent = event.payloadAsJsonObject();
        final JsonObject rejectedCourtReferral = rejectionEvent.getJsonObject("courtReferral");

        if (rejectedCourtReferral.containsKey("sjpReferral")) {

            final JsonObject caseDetails = rejectedCourtReferral.getJsonArray("prosecutionCases").getJsonObject(0);
            final String rejectionReason = rejectionEvent.getString("rejectedReason");

            final JsonEnvelope command = enveloper.withMetadataFrom(
                    envelopeFrom(metadataFrom(event.metadata()), NULL),
                    "sjp.command.record-case-referral-for-court-hearing-rejection")
                    .apply(recordCaseReferralForCourtHearingRejection()
                            .withCaseId(UUID.fromString(caseDetails.getString("id")))
                            .withRejectionReason(rejectionReason)
                            .withRejectedAt(clock.now())
                            .build());

            sender.send(command);
        }
    }

    @Handles("sjp.events.case-referred-for-court-hearing")
    public void caseReferredForCourtHearing(final Envelope<CaseReferredForCourtHearing> envelope) {
        LOGGER.info("Case referred for court hearing {}", envelope.payload());
    }

}
