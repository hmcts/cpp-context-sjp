package uk.gov.moj.cpp.sjp.command.api;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(COMMAND_API)
public class SetDatesToAvoidRequiredApi {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("sjp.set-dates-to-avoid-required")
    public void setDatesToAvoidRequired(final JsonEnvelope setDateToAvoidRequiredCommand) {
        final JsonObject commandPayload = setDateToAvoidRequiredCommand.payloadAsJsonObject();
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add("caseId", commandPayload.getString("caseId"));

        sender.send(enveloper
                .withMetadataFrom(setDateToAvoidRequiredCommand,
                        "sjp.command.set-dates-to-avoid-required")
                .apply(jsonObjectBuilder.build()));
    }
}
