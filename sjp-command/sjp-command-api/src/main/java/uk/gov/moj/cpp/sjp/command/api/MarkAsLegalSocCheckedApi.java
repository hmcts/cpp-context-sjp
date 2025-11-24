package uk.gov.moj.cpp.sjp.command.api;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

@ServiceComponent(COMMAND_API)
public class MarkAsLegalSocCheckedApi {

    @Inject
    private Sender sender;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("sjp.mark-as-legal-soc-checked")
    public void markAsLegalSocChecked(final JsonEnvelope envelope) {

        final JsonObject payload = objectToJsonObjectConverter.convert(envelope.payload());
        sender.send(Envelope.envelopeFrom(metadataFrom(envelope.metadata())
                        .withName("sjp.command.mark-as-legal-soc-checked").build(), payload));

    }

}
