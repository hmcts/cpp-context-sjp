package uk.gov.moj.cpp.sjp.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class ResubmitResultsApi {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("sjp.resubmit-results")
    public void resubmitResults(final JsonEnvelope requestTransparencyCommand) {
        sender.send(enveloper.withMetadataFrom(requestTransparencyCommand, "sjp.command.resubmit-results")
                .apply(requestTransparencyCommand.payloadAsJsonObject()));
    }
}
