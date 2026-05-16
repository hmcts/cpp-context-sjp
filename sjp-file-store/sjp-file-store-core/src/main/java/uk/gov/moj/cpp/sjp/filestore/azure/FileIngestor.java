package uk.gov.moj.cpp.sjp.filestore.azure;

import static com.azure.core.util.Context.NONE;
import static java.util.Map.of;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.options.BlobCopyFromUrlOptions;

import java.net.URI;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

@ApplicationScoped
public class FileIngestor {

    @Inject
    @SuppressWarnings("squid:S1312")
    private Logger logger;

    @Inject
    private BlobContainerClient blobContainerClient;

    @Inject
    private AzureBlobConfiguration azureBlobConfiguration;

    public void ingest(final StoragePath storagePath,
                       final UUID fileId,
                       final UUID correlationId,
                       final String filename,
                       final URI sourceUri) {
        final String blobName = storagePath.blobName(fileId);
        blobContainerClient.getBlobClient(blobName)
                .copyFromUrlWithResponse(
                        new BlobCopyFromUrlOptions(sourceUri.toString())
                                .setMetadata(of("correlation_id", correlationId.toString(),
                                        "filename", filename)),
                        azureBlobConfiguration.getTransferTimeout(), NONE);
        logger.info("Ingested blob '{}' sourceUri='{}' correlationId='{}' filename='{}'",
                blobName, sourceUri, correlationId, filename);
    }
}
