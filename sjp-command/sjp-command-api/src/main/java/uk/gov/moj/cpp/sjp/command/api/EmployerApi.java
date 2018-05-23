package uk.gov.moj.cpp.sjp.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(COMMAND_API)
public class EmployerApi {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("sjp.update-employer")
    public void updateEmployer(final JsonEnvelope envelope) {
        final JsonObject payloadAsJsonObject = envelope.payloadAsJsonObject();
        //TODO ATCM-3151: when the UI is adapted remove this code below
        final JsonObjectBuilder employerDetails = JsonObjects.createObjectBuilderWithFilter(payloadAsJsonObject,
                key -> !("caseId".equals(key) || "defendantId".equals(key)));


        final JsonObject payload = Json.createObjectBuilder()
                .add("caseId", payloadAsJsonObject.getString("caseId"))
                .add("defendantId", payloadAsJsonObject.getString("defendantId"))
                .add("employer", employerDetails)
                .build();

        sender.send(enveloper.withMetadataFrom(envelope, "sjp.command.update-employer")
                .apply(payload));
    }

    @Handles("sjp.delete-employer")
    public void deleteEmployer(final JsonEnvelope envelope) {
        sender.send(enveloper.withMetadataFrom(envelope, "sjp.command.delete-employer").apply(envelope.payloadAsJsonObject()));
    }

}
