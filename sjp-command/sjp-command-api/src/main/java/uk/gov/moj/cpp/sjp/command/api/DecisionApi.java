package uk.gov.moj.cpp.sjp.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator.AdjournDecisionValidator.validateAdjournDecision;
import static uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator.DischargeDecisionValidator.validateDischargeDecision;
import static uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator.FinancialImpositionValidator.validateFinancialImposition;
import static uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator.FinancialPenaltyDecisionValidator.validateFinancialPenaltyDecision;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;

import java.util.List;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(COMMAND_API)
public class DecisionApi {

    @Inject
    private Clock clock;

    @Inject
    private Sender sender;

    @Handles("sjp.save-decision")
    public void saveDecision(final JsonEnvelope saveDecisionCommand) {
        validateDecision(saveDecisionCommand.payloadAsJsonObject());
        validateFinancialImposition(saveDecisionCommand.payloadAsJsonObject());
        final JsonObject payload = enrichDecision(saveDecisionCommand.payloadAsJsonObject());
        sender.send(envelopeFrom(metadataFrom(saveDecisionCommand.metadata()).withName("sjp.command.controller.save-decision").build(), payload));
    }

    private void validateDecision(final JsonObject decision) {
        final List<JsonObject> offenceDecisions = decision.getJsonArray("offenceDecisions").getValuesAs(JsonObject.class);
        offenceDecisions.forEach(offenceDecision ->  validateOffenceDecision(offenceDecision));
    }

    private void validateOffenceDecision(final JsonObject offenceDecision) {
        final DecisionType decisionType = DecisionType.valueOf(offenceDecision.getString("type"));

        switch (decisionType) {
            case ADJOURN:
                validateAdjournDecision(offenceDecision);
                break;
            case DISCHARGE:
                validateDischargeDecision(offenceDecision);
                break;
            case FINANCIAL_PENALTY:
                validateFinancialPenaltyDecision(offenceDecision);
                break;
            default:
        }
    }

    private JsonObject enrichDecision(JsonObject decision) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder(decision);
        jsonObjectBuilder.add("savedAt", clock.now().toString());
        return jsonObjectBuilder.build();
    }
}
