package uk.gov.moj.sjp.it.producer;

import static uk.gov.moj.cpp.resulting.event.DecisionToReferCaseForCourtHearingSaved.decisionToReferCaseForCourtHearingSaved;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_ACTIVE_MQ_TOPIC;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SELECTOR_CASE_REFER_FOR_COURT_HEARING_IN_RESULTING;
import static uk.gov.moj.sjp.it.util.JsonHelper.toJsonObject;

import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.cpp.resulting.event.DecisionToReferCaseForCourtHearingSaved;

import java.time.ZonedDateTime;
import java.util.UUID;

public class DecisionToReferCaseForCourtHearingSavedProducer {

    private final UUID caseId;
    private final UUID decisionId;
    private final UUID sessionId;
    private final UUID referralReasonId;
    private final UUID hearingTypeId;
    private final Integer estimatedHearingDuration;
    private final String listingNotes;
    private final ZonedDateTime savedAt;

    public DecisionToReferCaseForCourtHearingSavedProducer(final UUID caseId,
                                                           final UUID decisionId,
                                                           final UUID sessionId,
                                                           final UUID referralReasonId,
                                                           final UUID hearingTypeId,
                                                           final Integer estimatedHearingDuration,
                                                           final String listingNotes,
                                                           final ZonedDateTime savedAt) {
        this.caseId = caseId;
        this.decisionId = decisionId;
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
                .withDecisionId(decisionId)
                .withSessionId(sessionId)
                .withReferralReasonId(referralReasonId)
                .withHearingTypeId(hearingTypeId)
                .withEstimatedHearingDuration(estimatedHearingDuration)
                .withListingNotes(listingNotes)
                .withDecisionSavedAt(savedAt)
                .build();

        try (final MessageProducerClient messageProducer = new MessageProducerClient()) {
            messageProducer.startProducer(PUBLIC_ACTIVE_MQ_TOPIC);
            messageProducer.sendMessage(PUBLIC_EVENT_SELECTOR_CASE_REFER_FOR_COURT_HEARING_IN_RESULTING, toJsonObject(decisionToReferCaseForCourtHearingSaved));
        }
    }
}
