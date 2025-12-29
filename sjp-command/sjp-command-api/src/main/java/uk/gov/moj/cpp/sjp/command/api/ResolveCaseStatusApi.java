package uk.gov.moj.cpp.sjp.command.api;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
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
public class ResolveCaseStatusApi {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("sjp.resolve-case-status")
    public void resolveCaseStatus(final JsonEnvelope resolveCaseStatusCommand) {
        final JsonObject commandPayload = resolveCaseStatusCommand.payloadAsJsonObject();
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add("caseId", commandPayload.getString("caseId"));

        sender.send(enveloper
                .withMetadataFrom(resolveCaseStatusCommand,
                        "sjp.command.resolve-case-status")
                .apply(jsonObjectBuilder.build()));
    }
}
