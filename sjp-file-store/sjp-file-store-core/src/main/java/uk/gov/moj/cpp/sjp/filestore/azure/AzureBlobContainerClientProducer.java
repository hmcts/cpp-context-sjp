package uk.gov.moj.cpp.sjp.filestore.azure;

import com.azure.core.exception.HttpResponseException;
import com.azure.core.http.jdk.httpclient.JdkHttpClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * CDI producer that builds and exposes a {@link BlobContainerClient} for injection.
 *
 * <p>Authentication order:
 * <ol>
 *   <li>If {@code azure.filestore.connection-string} is set to a real connection string (not
 *       the {@code "DefaultAzureCredential"} sentinel default), uses it (local Azurite only).</li>
 *   <li>Otherwise, {@code DefaultAzureCredential} with the endpoint — on AKS this resolves
 *       to the pod's Workload Identity (Entra ID Federated Identity Credential).</li>
 * </ol>
 *
 * <p><strong>Why {@code @Dependent} on the producer method:</strong>
 * {@link BlobContainerClient} is {@code final} — Weld cannot proxy it, so
 * {@code @ApplicationScoped} on the producer would fail with WELD-001410.
 * {@code @Dependent} injects the real instance held by this {@code @ApplicationScoped} bean.
 */
@ApplicationScoped
public class AzureBlobContainerClientProducer {

    private static final int HTTP_CONFLICT = 409;

    @Inject
    private Logger logger;

    @Inject
    private AzureBlobConfiguration azureBlobConfiguration;

    private BlobServiceClient blobServiceClient;
    private BlobContainerClient blobContainerClient;

    @PostConstruct
    public void initialise() {
        blobServiceClient = buildBlobServiceClient(azureBlobConfiguration);
        blobContainerClient = blobServiceClient.getBlobContainerClient(azureBlobConfiguration.getContainerName());
        try {
            blobContainerClient.createIfNotExists();
        } catch (final HttpResponseException e) {
            if (e.getResponse() != null && e.getResponse().getStatusCode() == HTTP_CONFLICT) {
                logger.warn("BlobContainerClient.createIfNotExists returned 409 Conflict for container '{}' — container already exists",
                        azureBlobConfiguration.getContainerName());
            } else {
                throw new AzureBlobContainerClientCreationException(
                        "Failed to create BlobContainerClient for container '" + azureBlobConfiguration.getContainerName() + "'", e);
            }
        }
    }

    @Produces
    @Dependent
    public BlobContainerClient blobContainerClient() {
        return blobContainerClient;
    }

    @Produces
    @Dependent
    public BlobServiceClient blobServiceClient() {
        return blobServiceClient;
    }

    protected BlobServiceClient buildBlobServiceClient(final AzureBlobConfiguration configuration) {
        final JdkHttpClientBuilder httpClientBuilder = new JdkHttpClientBuilder()
                .connectionTimeout(configuration.getConnectionTimeout())
                .responseTimeout(configuration.getResponseTimeout());
        if (configuration.hasConnectionString()) {
            return new BlobServiceClientBuilder()
                    .httpClient(httpClientBuilder.build())
                    .connectionString(configuration.getConnectionString())
                    .buildClient();
        }
        return new BlobServiceClientBuilder()
                .httpClient(httpClientBuilder.build())
                .credential(new DefaultAzureCredentialBuilder().build())
                .endpoint(configuration.getEndpoint())
                .buildClient();
    }
}
