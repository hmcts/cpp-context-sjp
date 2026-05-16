package uk.gov.justice.api.resource;

import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.status;
import static uk.gov.justice.api.resource.DefaultQueryApiTransparencyReportContentFileIdResource.PDF_CONTENT_TYPE;
import static uk.gov.moj.cpp.sjp.filestore.azure.StoragePath.internal;

import java.util.UUID;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobRange;

public class ResourceUtility {

    private static final long MAX_BLOB_SIZE_BYTES = 1_000_000_000L;

    static Response getResponse(final BlobContainerClient blobContainerClient, final UUID fileId, final String fileName) {
        final BlobClient blobClient = blobContainerClient.getBlobClient(internal().blobName(fileId));
        if (!blobClient.exists()) {
            throw new RuntimeException("No file present for fileId: " + fileId);
        }
        final StreamingOutput streamingOutput = output ->
                blobClient.downloadStreamWithResponse(output, new BlobRange(0, MAX_BLOB_SIZE_BYTES), null, null, false, null, null);
        return status(OK)
                .entity(streamingOutput)
                .header(CONTENT_TYPE, PDF_CONTENT_TYPE)
                .header(CONTENT_DISPOSITION, fileName + fileId + ".pdf")
                .build();
    }
}
