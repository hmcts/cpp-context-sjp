package uk.gov.moj.cpp.sjp.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class UpdateDefendantDetailsApi {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("sjp.update-defendant-details")
    public void updateDefendantDetails(final JsonEnvelope envelope) {
        sender.send(enveloper.withMetadataFrom(envelope, "sjp.command.update-defendant-details").apply(envelope.payloadAsJsonObject()));
    }

    @Handles("sjp.update-defendant-national-insurance-number")
    public void updateDefendantNationalInsuranceNumber(final JsonEnvelope envelope) {
        sender.send(enveloper.withMetadataFrom(envelope, "sjp.command.update-defendant-national-insurance-number").apply(envelope.payloadAsJsonObject()));
    }
}
