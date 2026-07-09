package uk.gov.moj.cpp.sjp.command.controller;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.command.service.UserService;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(COMMAND_CONTROLLER)
public class ApplicationDecisionController {

    @Inject
    private Sender sender;

    @Inject
    private UserService userService;

    @Handles("sjp.command.controller.save-application-decision")
    public void saveApplicationDecision(final JsonEnvelope commandEnvelope) {
        final JsonObject payload = commandEnvelope.payloadAsJsonObject();
        final JsonObject userDetails = userService.getCallingUserDetails(commandEnvelope);

        final JsonObjectBuilder enrichedPayload = createObjectBuilder(payload)
                .add("savedBy", createObjectBuilder()
                        .add("userId", userDetails.getJsonString("userId"))
                        .add("firstName", userDetails.getString("firstName"))
                        .add("lastName", userDetails.getString("lastName")));

        sender.send(envelopeFrom(
                metadataFrom(commandEnvelope.metadata())
                        .withName("sjp.command.handler.save-application-decision"),
                enrichedPayload.build())
        );
    }

}
