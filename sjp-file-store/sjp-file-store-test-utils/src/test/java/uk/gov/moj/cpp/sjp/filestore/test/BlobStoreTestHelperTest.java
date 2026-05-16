package uk.gov.moj.cpp.sjp.filestore.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BlobStoreTestHelperTest {

    // Standard Azurite well-known test credential — not a real secret.
    private static final String AZURITE_CONNECTION_STRING = azuriteConnectionString();

    private static String azuriteConnectionString() {
        return "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;"
                + "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq"
                + "/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://localhost:10000/devstoreaccount1;";
    }

    private static final String CONTAINER_NAME = "blobhelper-it-" + UUID.randomUUID().toString().substring(0, 8);

    private BlobStoreTestHelper helper;

    @BeforeEach
    public void setUp() {
        helper = BlobStoreTestHelper.forConnectionStringAndContainer(AZURITE_CONNECTION_STRING, CONTAINER_NAME);
    }

    @AfterEach
    public void tearDown() {
        BlobStoreTestHelper.forConnectionStringAndContainer(AZURITE_CONNECTION_STRING, CONTAINER_NAME);
    }

    @Test
    public void shouldReturnGeneratedFileIdOnUpload() {
        final UUID fileId = helper.upload("internal", "report.pdf", "content".getBytes());

        assertThat(fileId, notNullValue());
    }

    @Test
    public void shouldConfirmBlobExistsAfterUpload() {
        final UUID fileId = helper.upload("internal", "report.pdf", "content".getBytes());

        assertThat(helper.exists("internal", fileId), is(true));
    }

    @Test
    public void shouldReturnFalseWhenBlobDoesNotExist() {
        assertThat(helper.exists("internal", UUID.randomUUID()), is(false));
    }

    @Test
    public void shouldDownloadUploadedContent() {
        final byte[] content = "hello sjp".getBytes();
        final UUID fileId = helper.upload("internal", "doc.txt", content);

        final Optional<byte[]> downloaded = helper.download("internal", fileId);

        assertThat(downloaded.isPresent(), is(true));
        assertThat(downloaded.get(), is(content));
    }

    @Test
    public void shouldReturnEmptyWhenDownloadingNonExistentBlob() {
        final Optional<byte[]> downloaded = helper.download("internal", UUID.randomUUID());

        assertThat(downloaded.isPresent(), is(false));
    }

    @Test
    public void shouldDeleteBlobSuccessfully() {
        final UUID fileId = helper.upload("internal", "temp.pdf", "bytes".getBytes());

        helper.delete("internal", fileId);

        assertThat(helper.exists("internal", fileId), is(false));
    }

    @Test
    public void shouldReturnContainerName() {
        assertThat(helper.containerName(), is(CONTAINER_NAME));
    }

    @Test
    public void shouldReturnWorkingHelperViaForLocalAzurite() {
        final String containerName = "blobhelper-local-" + UUID.randomUUID().toString().substring(0, 8);
        final BlobStoreTestHelper localHelper = BlobStoreTestHelper.forLocalAzurite("localhost", containerName);

        final UUID fileId = localHelper.upload("internal", "test.pdf", "content".getBytes());

        assertThat(localHelper.exists("internal", fileId), is(true));
    }

    @Test
    public void shouldReplaceLocalEndpointWithDockerEndpointInSasUri() {
        final UUID fileId = helper.upload("internal", "report.pdf", "content".getBytes());
        final String localBase = "http://localhost:10000/devstoreaccount1";
        final String dockerBase = "http://cpp-azurite:10000/devstoreaccount1";

        final String sasUri = helper.generateDockerAccessibleSasUri("internal", fileId, localBase, dockerBase);

        assertThat(sasUri, startsWith(dockerBase));
        assertThat(sasUri, containsString("?"));
    }
}
