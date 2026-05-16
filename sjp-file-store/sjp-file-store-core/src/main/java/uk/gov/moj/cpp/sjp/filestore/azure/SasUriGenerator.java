package uk.gov.moj.cpp.sjp.filestore.azure;

import uk.gov.justice.services.common.util.UtcClock;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.UserDelegationKey;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

@ApplicationScoped
public class SasUriGenerator {

    @Inject
    private BlobContainerClient blobContainerClient;

    @Inject
    private BlobServiceClient blobServiceClient;

    @Inject
    private AzureBlobConfiguration azureBlobConfiguration;

    @Inject
    private UtcClock utcClock;

    @Inject
    @SuppressWarnings("squid:S1312")
    private Logger logger;

    private volatile UserDelegationKey cachedDelegationKey;
    private volatile OffsetDateTime delegationKeyExpiresAt;

    public URI generateReadUri(final StoragePath storagePath, final UUID fileId) {
        final String blobName = storagePath.blobName(fileId);
        final BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
        final OffsetDateTime expiresAt = utcClock.now().toOffsetDateTime().plus(azureBlobConfiguration.getSasExpiry());
        final BlobSasPermission permissions = new BlobSasPermission().setReadPermission(true);
        final BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(expiresAt, permissions);
        final URI sasUri;
        if (azureBlobConfiguration.hasConnectionString()) {
            sasUri = URI.create(blobClient.getBlobUrl() + "?" + blobClient.generateSas(sasValues));
        } else {
            sasUri = URI.create(blobClient.getBlobUrl() + "?" + blobClient.generateUserDelegationSas(sasValues, getDelegationKey()));
        }
        logger.info("Generated SAS URI blobName='{}'", blobName);
        return sasUri;
    }

    private UserDelegationKey getDelegationKey() {
        if (cachedDelegationKey != null
                && utcClock.now().toOffsetDateTime().isBefore(
                        delegationKeyExpiresAt.minus(azureBlobConfiguration.getDelegationKeyRefreshThreshold()))) {
            return cachedDelegationKey;
        }
        synchronized (this) {
            if (cachedDelegationKey != null
                    && utcClock.now().toOffsetDateTime().isBefore(
                            delegationKeyExpiresAt.minus(azureBlobConfiguration.getDelegationKeyRefreshThreshold()))) {
                return cachedDelegationKey;
            }
            final OffsetDateTime start = utcClock.now().toOffsetDateTime();
            final OffsetDateTime expiry = start.plus(azureBlobConfiguration.getSasExpiry().plusHours(1L));
            logger.info("Fetching new UserDelegationKey expiresAt='{}'", expiry);
            final UserDelegationKey newKey = blobServiceClient.getUserDelegationKey(start, expiry);
            delegationKeyExpiresAt = expiry;
            cachedDelegationKey = newKey;
            return newKey;
        }
    }
}
