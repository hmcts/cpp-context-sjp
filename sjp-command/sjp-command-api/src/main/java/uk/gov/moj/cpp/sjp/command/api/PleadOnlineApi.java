package uk.gov.moj.cpp.sjp.command.api;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import org.slf4j.Logger;

@ServiceComponent(COMMAND_API)
public class PleadOnlineApi {

    private static final Logger LOGGER = getLogger(PleadOnlineApi.class);

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("sjp.plead-online")
    public void pleadOnline(final JsonEnvelope envelope) {
        sender.send(enveloper.withMetadataFrom(envelope, "sjp.command.plead-online").apply(envelope.payloadAsJsonObject()));
    }
}
