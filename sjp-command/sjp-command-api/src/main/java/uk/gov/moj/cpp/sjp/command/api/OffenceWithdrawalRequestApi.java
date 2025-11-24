package uk.gov.moj.cpp.sjp.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_API)
public class OffenceWithdrawalRequestApi {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("sjp.set-offences-withdrawal-requests-status")
    public void setOffenceWithdrawalRequestsStatus(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        sender.send(enveloper.withMetadataFrom(envelope, "sjp.command.set-offences-withdrawal-requests-status").apply(payload));
    }
}
