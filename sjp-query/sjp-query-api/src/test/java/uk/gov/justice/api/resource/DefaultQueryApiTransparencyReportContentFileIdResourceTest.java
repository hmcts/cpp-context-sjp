package uk.gov.justice.api.resource;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.api.resource.DefaultQueryApiTransparencyReportContentFileIdResource.PDF_CONTENT_TYPE;
import static uk.gov.justice.api.resource.DefaultQueryApiTransparencyReportContentFileIdResource.TRANSPARENCY_REPORT_CONTENT_QUERY_NAME;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.moj.cpp.sjp.filestore.azure.StoragePath.internal;

import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.core.interceptor.InterceptorContext;

import java.util.UUID;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class DefaultQueryApiTransparencyReportContentFileIdResourceTest {

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobClient blobClient;

    @Mock
    private InterceptorChainProcessor interceptorChainProcessor;

    @InjectMocks
    private DefaultQueryApiTransparencyReportContentFileIdResource underTest;

    @Captor
    private ArgumentCaptor<InterceptorContext> interceptorContextCaptor;

    private final UUID userId = randomUUID();
    private final UUID fileId = randomUUID();

    @Test
    public void shouldReturnValidResponseWhenTheFileIdIsValid() {
        when(blobContainerClient.getBlobClient(internal().blobName(fileId))).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(true);

        final Response response = underTest.getTransparencyReportContentByFileId(fileId, userId);

        assertThat(response.getEntity(), instanceOf(StreamingOutput.class));
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString(CONTENT_TYPE), is(PDF_CONTENT_TYPE));
        assertThat(response.getHeaderString(CONTENT_DISPOSITION), is("attachment;filename=TransparencyReport_" + fileId + ".pdf"));

        verifyInterceptorChainExecution();
    }

    @Test
    public void shouldThrowExceptionWhenTheFileIdIsNotValid() {
        when(blobContainerClient.getBlobClient(internal().blobName(fileId))).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(false);

        assertThrows(RuntimeException.class, () -> underTest.getTransparencyReportContentByFileId(fileId, userId));
    }

    private void verifyInterceptorChainExecution() {
        verify(interceptorChainProcessor).process(interceptorContextCaptor.capture());

        assertThat(interceptorContextCaptor.getValue().inputEnvelope(),
                jsonEnvelope(metadata()
                                .withName(TRANSPARENCY_REPORT_CONTENT_QUERY_NAME)
                                .withUserId(userId.toString()),
                        payload().isJson(allOf(
                                withJsonPath("$.fileId", equalTo(fileId.toString()))
                        ))
                ));
    }
}
