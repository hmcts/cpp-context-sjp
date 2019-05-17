package uk.gov.justice.api.resource;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.api.resource.DefaultQueryApiTransparencyReportContentFileIdResource.PDF_CONTENT_TYPE;
import static uk.gov.justice.api.resource.DefaultQueryApiTransparencyReportContentFileIdResource.TRANSPARENCY_REPORT_CONTENT_QUERY_NAME;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;

import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.core.interceptor.InterceptorContext;
import uk.gov.justice.services.fileservice.api.FileRetriever;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.domain.FileReference;
import uk.gov.moj.cpp.systemusers.ServiceContextSystemUserProvider;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class DefaultQueryApiTransparencyReportContentFileIdResourceTest {

    @Mock
    private FileRetriever fileRetriever;

    @Mock
    private InterceptorChainProcessor interceptorChainProcessor;

    @Mock
    private ServiceContextSystemUserProvider serviceContextSystemUserProvider;

    @InjectMocks
    private DefaultQueryApiTransparencyReportContentFileIdResource underTest;

    @Captor
    private ArgumentCaptor<InterceptorContext> interceptorContextCaptor;

    private final UUID userId = randomUUID();
    private final UUID fileId = randomUUID();
    private final UUID systemUserId = randomUUID();

    @Before
    public void init() {
        when(serviceContextSystemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));
    }

    @Test
    public void shouldReturnValidResponseWhenTheFileIdIsValid() throws FileServiceException {
        // given
        final FileReference fileReference = mock(FileReference.class);
        final Optional<FileReference> optionalFileReference = Optional.of(fileReference);
        final InputStream inputStream = mock(InputStream.class);

        when(fileReference.getContentStream()).thenReturn(inputStream);
        when(fileRetriever.retrieve(fileId)).thenReturn(optionalFileReference);

        // when
        final Response response = underTest.getTransparencyReportContentByFileId(fileId, userId);

        // then
        assertThat(response.getEntity(), is(inputStream));
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString(CONTENT_TYPE), is(PDF_CONTENT_TYPE));
        assertThat(response.getHeaderString(CONTENT_DISPOSITION), is("attachment;filename=TransparencyReport_" + fileId.toString() + ".pdf"));

        verifyInterceptorChainExecution();
    }

    @Test(expected = RuntimeException.class) // then
    public void shouldThrowExceptionWhenTheFileIdIsNotValid() throws FileServiceException {
        // given
        final Optional<FileReference> optionalFileReference = Optional.of(null);
        when(fileRetriever.retrieve(fileId)).thenReturn(optionalFileReference);

        // when
        underTest.getTransparencyReportContentByFileId(fileId, userId);
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