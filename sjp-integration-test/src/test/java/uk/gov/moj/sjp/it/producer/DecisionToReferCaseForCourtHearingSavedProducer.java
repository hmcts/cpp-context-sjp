package uk.gov.moj.sjp.it.producer;

import static uk.gov.moj.cpp.resulting.event.DecisionToReferCaseForCourtHearingSaved.decisionToReferCaseForCourtHearingSaved;
import static uk.gov.moj.sjp.it.util.JsonHelper.toJsonObject;

import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.cpp.resulting.event.DecisionToReferCaseForCourtHearingSaved;

import java.time.ZonedDateTime;
import java.util.UUID;

public class DecisionToReferCaseForCourtHearingSavedProducer {

    private final UUID caseId;
    private final UUID sessionId;
    private final UUID referralReasonId;
    private final UUID hearingTypeId;
    private final Integer estimatedHearingDuration;
    private final String listingNotes;
    private final ZonedDateTime savedAt;

    public DecisionToReferCaseForCourtHearingSavedProducer(final UUID caseId,
                                                           final UUID sessionId,
                                                           final UUID referralReasonId,
                                                           final UUID hearingTypeId,
                                                           final Integer estimatedHearingDuration,
                                                           final String listingNotes,
                                                           final ZonedDateTime savedAt) {
        this.caseId = caseId;
        this.sessionId = sessionId;
        this.referralReasonId = referralReasonId;
        this.hearingTypeId = hearingTypeId;
        this.estimatedHearingDuration = estimatedHearingDuration;
        this.listingNotes = listingNotes;
        this.savedAt = savedAt;
    }

    public void saveDecisionToReferCaseForCourtHearing() {

        final DecisionToReferCaseForCourtHearingSaved decisionToReferCaseForCourtHearingSaved = decisionToReferCaseForCourtHearingSaved()
                .withCaseId(caseId)
                .withSessionId(sessionId)
                .withReferralReasonId(referralReasonId)
                .withHearingTypeId(hearingTypeId)
                .withEstimatedHearingDuration(estimatedHearingDuration)
                .withListingNotes(listingNotes)
                .withDecisionSavedAt(savedAt)
                .build();

        try (final MessageProducerClient messageProducer = new MessageProducerClient()) {
            messageProducer.startProducer("public.event");
            messageProducer.sendMessage("public.resulting.decision-to-refer-case-for-court-hearing-saved", toJsonObject(decisionToReferCaseForCourtHearingSaved));
        }
    }
}
