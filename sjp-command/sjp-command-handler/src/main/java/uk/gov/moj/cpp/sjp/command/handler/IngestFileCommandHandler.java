package uk.gov.moj.cpp.sjp.command.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.filestore.azure.FileIngestor;
import uk.gov.moj.cpp.sjp.filestore.azure.StoragePath;

import java.net.URI;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_HANDLER)
public class IngestFileCommandHandler {

    private static final StoragePath BLOB_PATH = StoragePath.internal();

    @Inject
    private FileIngestor fileIngestor;

    @Handles("sjp.command.ingest-file")
    public void ingestFile(final JsonEnvelope command) {
        final JsonObject payload = command.payloadAsJsonObject();
        fileIngestor.ingest(
                BLOB_PATH,
                UUID.fromString(payload.getString("fileId")),
                UUID.fromString(payload.getString("correlationId")),
                payload.getString("filename"),
                URI.create(payload.getString("sourceUri")));
    }
}
