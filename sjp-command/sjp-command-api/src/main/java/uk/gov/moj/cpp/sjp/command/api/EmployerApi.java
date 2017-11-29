package uk.gov.moj.cpp.sjp.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class EmployerApi {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("sjp.update-employer")
    public void updateEmployer(final JsonEnvelope envelope) {
        sender.send(enveloper.withMetadataFrom(envelope, "sjp.command.update-employer").apply(envelope.payloadAsJsonObject()));
    }

    @Handles("sjp.delete-employer")
    public void deleteEmployer(final JsonEnvelope envelope) {
        sender.send(enveloper.withMetadataFrom(envelope, "sjp.command.delete-employer").apply(envelope.payloadAsJsonObject()));
    }

}
