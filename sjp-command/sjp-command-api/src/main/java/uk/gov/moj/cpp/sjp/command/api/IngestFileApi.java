package uk.gov.moj.cpp.sjp.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class IngestFileApi {

    @Inject
    private Sender sender;

    @Handles("sjp.ingest-file")
    public void ingestFile(final JsonEnvelope envelope) {
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("sjp.command.ingest-file")
                .build();
        sender.send(envelopeFrom(metadata, envelope.payload()));
    }
}
