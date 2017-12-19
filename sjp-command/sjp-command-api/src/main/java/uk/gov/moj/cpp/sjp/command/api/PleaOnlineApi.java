package uk.gov.moj.cpp.sjp.command.api;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import org.slf4j.Logger;

@ServiceComponent(COMMAND_API)
public class PleaOnlineApi {

    private static final Logger LOGGER = getLogger(PleaOnlineApi.class);

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Inject
    private ObjectToJsonValueConverter objectToJsonValueConverter;

    @Handles("sjp.plea-online")
    public void pleaOnline(final JsonEnvelope envelope) {
        sender.send(enveloper.withMetadataFrom(envelope, "sjp.command.plea-online").apply(envelope.payloadAsJsonObject()));
    }
}
