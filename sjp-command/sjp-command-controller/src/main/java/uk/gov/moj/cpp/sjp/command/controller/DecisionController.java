package uk.gov.moj.cpp.sjp.command.controller;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.command.service.CaseService;
import uk.gov.moj.cpp.sjp.command.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.command.service.SessionService;
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

    @Inject
    private SessionService sessionService;

    private static final String REFER_FOR_COURT_HEARING = "REFER_FOR_COURT_HEARING";
    private static final String USER_ID = "userId";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";

    @Handles("sjp.command.controller.save-decision")
    public void saveDecision(final JsonEnvelope caseDecisionCommand) {
        final JsonObject payload = caseDecisionCommand.payloadAsJsonObject();

        final JsonObject userDetails = userService.getCallingUserDetails(caseDecisionCommand);

        final JsonObjectBuilder enrichedPayload = createObjectBuilder(payload)
                .add("decisionId", randomUUID().toString())
                .add("savedBy", createObjectBuilder()
                        .add(USER_ID, userDetails.getJsonString(USER_ID))
                        .add(FIRST_NAME, userDetails.getString(FIRST_NAME))
                        .add(LAST_NAME, userDetails.getString(LAST_NAME)));

        if (payload.containsKey("offenceDecisions")) {
            final JsonArray offenceDecisions = payload.getJsonArray("offenceDecisions");
            final JsonArray offenceDecisionsWithId = addOffenceDecisionId(offenceDecisions).build();
            final JsonArray offenceDecisionsWithReferralReason = addReferralReason(offenceDecisionsWithId).build();
            enrichedPayload.add("offenceDecisions", offenceDecisionsWithReferralReason);
        }

        addDefendantCourtDetails(payload, enrichedPayload);

        sender.send(envelop(enrichedPayload.build())
                .withName("sjp.command.save-decision")
                .withMetadataFrom(caseDecisionCommand));
    }

    @Handles("sjp.command.controller.expire-defendant-aocp-response-timer")
    public void handleAocpAcceptanceResponseTimeExpiredRequested(final JsonEnvelope command) {

        final JsonObject payload = command.payloadAsJsonObject();
        final JsonObject userDetails = userService.getCallingUserDetails(command);
        final JsonObjectBuilder enrichedPayload = createObjectBuilder(payload)
                .add("decisionId", randomUUID().toString())
                .add("savedBy", createObjectBuilder()
                        .add(USER_ID, userDetails.getJsonString(USER_ID))
                        .add(FIRST_NAME, userDetails.getString(FIRST_NAME))
                        .add(LAST_NAME, userDetails.getString(LAST_NAME)));

        addDefendantCourtDetails(payload, enrichedPayload);

        final JsonObject response= sessionService.getLatestAocpSessionDetails(command);
        if(nonNull(response)) {
            enrichedPayload.add("sessionId", response.getString("sessionId"));
        }


        sender.send(envelop(enrichedPayload.build())
                .withName("sjp.command.expire-defendant-aocp-response-timer")
                .withMetadataFrom(command));

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

    private JsonArrayBuilder addReferralReason(final JsonArray offenceDecisions) {
        final JsonArrayBuilder decisionsBuilder = createArrayBuilder();
        offenceDecisions.getValuesAs(JsonObject.class)
                .forEach(offenceDecision -> {
                    final String type = offenceDecision.getString("type");
                    if (type.equals(REFER_FOR_COURT_HEARING)) {
                        final JsonObjectBuilder referralDecisionWithReason = createObjectBuilder(offenceDecision);
                        final String referralReasonId = offenceDecision.getString("referralReasonId");
                        referenceDataService.getReferralReason(referralReasonId)
                                .map(referralReasonRefData ->
                                        ofNullable(referralReasonRefData.getString("subReason", null))
                                                .map(subReason -> format("%s (%s)", referralReasonRefData.getString("reason"), subReason))
                                                .orElse(referralReasonRefData.getString("reason"))
                                ).ifPresent(reason -> referralDecisionWithReason.add("referralReason", reason));

                        decisionsBuilder.add(referralDecisionWithReason.build());
                    } else {
                        decisionsBuilder.add(offenceDecision);
                    }
                });

        return decisionsBuilder;
    }
}
