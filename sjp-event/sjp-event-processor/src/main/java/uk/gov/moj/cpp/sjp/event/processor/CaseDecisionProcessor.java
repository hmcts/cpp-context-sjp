package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class CaseDecisionProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseDecisionProcessor.class);

    @Inject
    private Sender sender;

    private static final String PUBLIC_CASE_DECISION_SAVED_EVENT = "public.sjp.case-decision-saved";

    @Handles(DecisionSaved.EVENT_NAME)
    public void handleCaseDecisionSaved(final JsonEnvelope jsonEnvelope) {
        final JsonObject savedDecision = jsonEnvelope.payloadAsJsonObject();

        LOGGER.info("Received Case decision saved message for caseId {}", savedDecision.getString(EventProcessorConstants.CASE_ID));

        sender.send(envelop(savedDecision)
                .withName(PUBLIC_CASE_DECISION_SAVED_EVENT)
                .withMetadataFrom(jsonEnvelope));
    }
}
