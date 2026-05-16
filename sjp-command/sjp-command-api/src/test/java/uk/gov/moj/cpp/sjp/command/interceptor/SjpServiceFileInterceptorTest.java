package uk.gov.moj.cpp.sjp.command.interceptor;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.sjp.filestore.azure.StoragePath.internal;

import uk.gov.justice.services.adapter.rest.multipart.DefaultFileInputDetails;
import uk.gov.justice.services.adapter.rest.multipart.FileInputDetails;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.core.interceptor.InterceptorChain;
import uk.gov.justice.services.core.interceptor.InterceptorContext;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.filestore.azure.FileStorer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SjpServiceFileInterceptorTest {

    private static final String FIELD_NAME = "FieldName";
    private static final String FILE_CONTENT = "some test data for my input stream";

    @Mock
    private FileStorer fileStorer;

    @Mock
    private DocumentTypeValidator documentTypeValidator;

    @Mock
    private InterceptorContext interceptorContext;

    @Mock
    private InterceptorContext interceptorContextExpected;

    @Mock
    private InterceptorChain interceptorChain;

    @InjectMocks
    SjpServiceFileInterceptor sjpServiceFileInterceptor;

    @Test
    public void shouldPassSjpServiceFileInterceptorValidationAndUpload() throws Exception {
        final String fileName = "some-file.txt";
        final UUID storedFileId = randomUUID();
        final JsonEnvelope originalEnvelope = envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("test.command").withUserId(randomUUID().toString()).build(),
                createObjectBuilder().build()
        );

        final List<FileInputDetails> fileInputDetailsList = getFileInputDetails(fileName);
        when(documentTypeValidator.isValid(fileName)).thenReturn(true);
        when(interceptorContext.getInputParameter(FileInputDetails.FILE_INPUT_DETAILS_LIST)).thenReturn(of(fileInputDetailsList));
        when(interceptorContext.inputEnvelope()).thenReturn(originalEnvelope);

        final ArgumentCaptor<UUID> correlationIdCaptor = ArgumentCaptor.forClass(UUID.class);
        final ArgumentCaptor<InputStream> contentCaptor = ArgumentCaptor.forClass(InputStream.class);
        when(fileStorer.store(eq(internal()), correlationIdCaptor.capture(), eq(fileName), contentCaptor.capture()))
                .thenReturn(storedFileId);

        final ArgumentCaptor<JsonEnvelope> envelopeCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);
        when(interceptorContext.copyWithInput(envelopeCaptor.capture())).thenReturn(interceptorContextExpected);
        when(interceptorChain.processNext(interceptorContextExpected)).thenReturn(interceptorContextExpected);

        sjpServiceFileInterceptor.process(interceptorContext, interceptorChain);

        verify(interceptorChain).processNext(interceptorContextExpected);
        assertThat(correlationIdCaptor.getValue(), is(notNullValue()));
        assertArrayEquals(FILE_CONTENT.getBytes(UTF_8), contentCaptor.getValue().readAllBytes());

        final JsonEnvelope modifiedEnvelope = envelopeCaptor.getValue();
        assertThat(modifiedEnvelope.payloadAsJsonObject().getString(FIELD_NAME), is(storedFileId.toString()));
    }

    @Test
    public void shouldNotPassSjpServiceFileInterceptorValidationAndUpload() {
        when(interceptorContext.getInputParameter(FileInputDetails.FILE_INPUT_DETAILS_LIST)).thenReturn(empty());

        sjpServiceFileInterceptor.process(interceptorContext, interceptorChain);

        verify(interceptorChain).processNext(interceptorContext);
    }

    @Test
    public void shouldFailSjpServiceFileInterceptorValidationAndUpload() {
        final String fileName = "some-file.xyz";
        final List<FileInputDetails> fileInputDetailsList = getFileInputDetails(fileName);
        when(documentTypeValidator.isValid(fileName)).thenReturn(false);
        when(interceptorContext.getInputParameter(FileInputDetails.FILE_INPUT_DETAILS_LIST)).thenReturn(of(fileInputDetailsList));

        assertThrows(ForbiddenRequestException.class, () -> sjpServiceFileInterceptor.process(interceptorContext, interceptorChain));
    }

    @Test
    public void shouldThrowSjpDocumentUploadExceptionWhenStreamCloseFails() {
        final String fileName = "some-file.txt";
        final InputStream failingCloseStream = new InputStream() {
            @Override
            public int read() {
                return -1;
            }
            @Override
            public void close() throws IOException {
                throw new IOException("close failed");
            }
        };

        final List<FileInputDetails> failingInputDetails = new ArrayList<>();
        failingInputDetails.add(new DefaultFileInputDetails(fileName, FIELD_NAME, failingCloseStream));
        when(documentTypeValidator.isValid(fileName)).thenReturn(true);
        when(interceptorContext.getInputParameter(FileInputDetails.FILE_INPUT_DETAILS_LIST)).thenReturn(of(failingInputDetails));
        final ArgumentCaptor<UUID> correlationIdCaptor = ArgumentCaptor.forClass(UUID.class);
        final ArgumentCaptor<InputStream> streamCaptor = ArgumentCaptor.forClass(InputStream.class);
        when(fileStorer.store(eq(internal()), correlationIdCaptor.capture(), eq(fileName), streamCaptor.capture())).thenReturn(randomUUID());

        assertThrows(SjpDocumentUploadException.class, () -> sjpServiceFileInterceptor.process(interceptorContext, interceptorChain));
    }

    private List<FileInputDetails> getFileInputDetails(final String fileName) {
        final List<FileInputDetails> fileInputDetailsList = new ArrayList<>();
        fileInputDetailsList.add(new DefaultFileInputDetails(fileName, FIELD_NAME, new ByteArrayInputStream(FILE_CONTENT.getBytes(UTF_8))));
        return fileInputDetailsList;
    }
}
