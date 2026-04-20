package uk.gov.moj.cpp.sjp.command.api;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator.AdjournDecisionValidator.validateAdjournDecision;
import static uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator.ApplicationDecisionValidator.validateApplicationDecision;
import static uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator.DischargeDecisionValidator.validateDischargeDecision;
import static uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator.FinancialImpositionValidator.validateFinancialImposition;
import static uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator.FinancialPenaltyDecisionValidator.validateFinancialPenaltyDecision;
import static uk.gov.moj.cpp.sjp.command.utils.NullSafeJsonObjectHelper.notNull;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.ADJOURN;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISCHARGE;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.FINANCIAL_PENALTY;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.command.api.service.CaseService;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(COMMAND_API)
public class DecisionApi {

    @Inject
    private Clock clock;

    @Inject
    private Sender sender;

    @Inject
    private CaseService caseService;

    private static final String EXCISE_PENALTY = "Excise penalty";

    @Handles("sjp.save-decision")
    public void saveDecision(final JsonEnvelope saveDecisionCommand) {
        final JsonObject payload = saveDecisionCommand.payloadAsJsonObject();
        boolean allOffencesAreExcisePenalty = false;
        List<JsonObject> offences = new ArrayList<>();

        final List<JsonObject> financialDecisions = payload.getJsonArray("offenceDecisions").getValuesAs(JsonObject.class).stream()
                .filter(offenceDecision -> asList(DISCHARGE, FINANCIAL_PENALTY).contains(DecisionType.valueOf(offenceDecision.getString("type")))).collect(toList());

        if(!financialDecisions.isEmpty()) {
            final JsonObject caseDetails = caseService.getCaseDetails(saveDecisionCommand);
            offences = caseDetails.getJsonObject("defendant").getJsonArray("offences").getValuesAs(JsonObject.class);
            allOffencesAreExcisePenalty = allOffencesAreExcisePenalty(financialDecisions, offences);
        }

        validateDecision(payload, offences);
        validateFinancialImposition(payload, allOffencesAreExcisePenalty);

        final JsonObject enrichedPayload = enrichDecision(saveDecisionCommand);
        sender.send(envelopeFrom(metadataFrom(saveDecisionCommand.metadata()).withName("sjp.command.controller.save-decision").build(), enrichedPayload));
    }

    private boolean allOffencesAreExcisePenalty(final List<JsonObject> offenceDecisions, final List<JsonObject> offences) {
        return offenceDecisions.stream()
                .map(financialOffenceDecision -> getOffence(financialOffenceDecision, offences))
                .allMatch(offence -> offence.isPresent() && nonNull(offence.get().get("penaltyType"))
                        && EXCISE_PENALTY.equals(offence.get().getString("penaltyType")));
    }

    private Optional<JsonObject> getOffence(final JsonObject offenceDecision, final List<JsonObject> offences) {
        String offenceId;
        if(notNull("offenceId", offenceDecision)) {
            offenceId = offenceDecision.getString("offenceId");
        } else {
            offenceId = offenceDecision.getJsonObject("offenceDecisionInformation").getString("offenceId");
        }
        return offences.stream()
                .filter(offenceValue -> offenceId.equals(offenceValue.getString("id")))
                .findFirst();
    }

    private void validateDecision(final JsonObject payload, final List<JsonObject> offences) {
        payload
                .getJsonArray("offenceDecisions")
                .getValuesAs(JsonObject.class)
                .forEach(offenceDecision -> {
                    final DecisionType decisionType = DecisionType.valueOf(offenceDecision.getString("type"));
                    if(FINANCIAL_PENALTY.equals(decisionType)) {
                        ofNullable(offenceDecision.getJsonObject("offenceDecisionInformation"))
                                .map(offenceDecisionInformation -> offenceDecisionInformation.getString("offenceId"))
                                .ifPresent(offenceId -> {
                                    final Optional<JsonObject> offence = offences
                                            .stream()
                                            .filter(offenceValue -> offenceId.equals(offenceValue.getString("id")))
                                            .findFirst();
                                    validateFinancialPenaltyDecision(offenceDecision, offence);
                                });
                    } else if (ADJOURN.equals(decisionType)) {
                        validateAdjournDecision(offenceDecision);
                    } else if (DISCHARGE.equals(decisionType)) {
                        validateDischargeDecision(offenceDecision);
                    }
                });
    }

    private JsonObject enrichDecision(final JsonEnvelope envelope) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder(envelope.payloadAsJsonObject());
        jsonObjectBuilder.add("savedAt", clock.now().toString());
        return jsonObjectBuilder.build();
    }

    @Handles("sjp.save-application-decision")
    public void saveApplicationDecision(final JsonEnvelope decisionEnvelope) {
        final JsonObject applicationDecision = decisionEnvelope.payloadAsJsonObject();
        validateApplicationDecision(applicationDecision);
        sender.send(
                envelopeFrom(
                    metadataFrom(decisionEnvelope.metadata())
                    .withName("sjp.command.controller.save-application-decision")
                    .build(),
                applicationDecision));
    }


    @Handles("sjp.case-complete-bdf")
    public void caseCompleteBdf(final JsonEnvelope jsonEnvelope) {
        final JsonObject jsonObject = jsonEnvelope.payloadAsJsonObject();
        sender.send(
                envelopeFrom(
                        metadataFrom(jsonEnvelope.metadata())
                                .withName("sjp.command.case-complete-bdf")
                                .build(),
                        jsonObject));
    }
}
