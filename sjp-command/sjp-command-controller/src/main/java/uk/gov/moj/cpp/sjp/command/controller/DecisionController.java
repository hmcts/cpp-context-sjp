package uk.gov.moj.cpp.sjp.command.controller;

import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.command.service.CaseService;
import uk.gov.moj.cpp.sjp.command.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.command.service.UserService;

import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(COMMAND_CONTROLLER)
public class DecisionController {

    @Inject
    private Sender sender;

    @Inject
    private UserService userService;

    @Inject
    private CaseService caseService;

    @Inject
    private ReferenceDataService referenceDataService;

    @Handles("sjp.command.controller.save-decision")
    public void saveDecision(final JsonEnvelope caseDecisionCommand) {
        final JsonObject payload = caseDecisionCommand.payloadAsJsonObject();

        final JsonObject userDetails = userService.getCallingUserDetails(caseDecisionCommand);


        final JsonObjectBuilder enrichedPayload = createObjectBuilder(payload)
                .add("decisionId", randomUUID().toString())
                .add("savedBy", createObjectBuilder()
                        .add("userId", userDetails.getJsonString("userId"))
                        .add("firstName", userDetails.getString("firstName"))
                        .add("lastName", userDetails.getString("lastName")));

        if(payload.containsKey("offenceDecisions")){
            final JsonArray offenceDecisions = payload.getJsonArray("offenceDecisions");
            final JsonArray offenceDecisionsWithId = addOffenceDecisionId(offenceDecisions).build();
            enrichedPayload.add("offenceDecisions", offenceDecisionsWithId);
        }

        addDefendantCourtDetails(payload, enrichedPayload);

        sender.send(envelop(enrichedPayload.build())
                .withName("sjp.command.save-decision")
                .withMetadataFrom(caseDecisionCommand));
    }

    private void addDefendantCourtDetails(final JsonObject payload, final JsonObjectBuilder enrichedPayload) {
        final JsonObject caseDetails = caseService.getCaseDetails(payload.getString("caseId"));
        getDefendantPostCode(caseDetails)
                .map(defendantPostCode -> referenceDataService.getEnforcementArea(defendantPostCode))
                .map(this::getDefendantCourtDetails)
                .ifPresent(defendantCourtDetails -> {
                    final JsonObjectBuilder defendantDetails = createObjectBuilder();
                    defendantDetails.add("court", defendantCourtDetails);
                    enrichedPayload.add("defendant", defendantDetails);
                });
    }

    private JsonObject getDefendantCourtDetails(final Optional<JsonObject> enforcementArea) {
        return enforcementArea
                .map(value -> value.getJsonObject("localJusticeArea"))
                .map(localJusticeArea -> createObjectBuilder()
                            .add("nationalCourtCode", localJusticeArea.getString("nationalCourtCode"))
                            .add("nationalCourtName", localJusticeArea.getString("name"))
                            .build())
                .orElse(null);
    }

    private Optional<String> getDefendantPostCode(JsonObject caseDetails) {
        return ofNullable(caseDetails.getJsonObject("defendant")
                .getJsonObject("personalDetails"))
                .map(personalDetails -> personalDetails.getJsonObject("address"))
                .map(address -> address.getString("postcode", null));
    }

    private JsonArrayBuilder addOffenceDecisionId(JsonArray offenceDecisions) {
        final JsonArrayBuilder decisionsWithIdBuilder = createArrayBuilder();
        offenceDecisions.getValuesAs(JsonObject.class).forEach(offenceDecision ->
                decisionsWithIdBuilder.add(createObjectBuilder(offenceDecision)
                    .add("id", randomUUID().toString())
                    .build()
        ));
        return decisionsWithIdBuilder;
    }
}
