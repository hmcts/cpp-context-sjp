package uk.gov.moj.cpp.sjp.command.handler;

import static javax.json.Json.createObjectBuilder;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.filestore.azure.FileIngestor;
import uk.gov.moj.cpp.sjp.filestore.azure.StoragePath;

import java.net.URI;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IngestFileCommandHandlerTest {

    @Mock
    private FileIngestor fileIngestor;

    @InjectMocks
    private IngestFileCommandHandler handler;

    @Test
    public void shouldIngestFileFromPayload() {
        final UUID fileId = UUID.randomUUID();
        final UUID correlationId = UUID.randomUUID();
        final String filename = "transparency_report_2026-05-15.pdf";
        final String sourceUri = "https://storage.example.com/container/blob?sig=abc123";

        final JsonEnvelope command = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("sjp.command.ingest-file"),
                createObjectBuilder()
                        .add("fileId", fileId.toString())
                        .add("correlationId", correlationId.toString())
                        .add("filename", filename)
                        .add("sourceUri", sourceUri));

        handler.ingestFile(command);

        verify(fileIngestor).ingest(
                StoragePath.internal(),
                fileId,
                correlationId,
                filename,
                URI.create(sourceUri));
    }

    @Test
    public void shouldPassSourceUriAsUriToIngestor() {
        final UUID fileId = UUID.randomUUID();
        final UUID correlationId = UUID.randomUUID();
        final String filename = "report.pdf";
        final String sourceUri = "https://storage.example.com/container/other?sig=xyz";

        final JsonEnvelope command = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("sjp.command.ingest-file"),
                createObjectBuilder()
                        .add("fileId", fileId.toString())
                        .add("correlationId", correlationId.toString())
                        .add("filename", filename)
                        .add("sourceUri", sourceUri));

        handler.ingestFile(command);

        verify(fileIngestor).ingest(
                StoragePath.internal(),
                fileId,
                correlationId,
                filename,
                URI.create(sourceUri));
    }
}
