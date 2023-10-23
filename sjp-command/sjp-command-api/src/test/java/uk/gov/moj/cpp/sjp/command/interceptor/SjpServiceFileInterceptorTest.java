package uk.gov.moj.cpp.sjp.command.interceptor;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.adapter.rest.interceptor.MultipleFileInputDetailsService;
import uk.gov.justice.services.adapter.rest.interceptor.ResultsHandler;
import uk.gov.justice.services.adapter.rest.multipart.DefaultFileInputDetails;
import uk.gov.justice.services.adapter.rest.multipart.FileInputDetails;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.core.interceptor.InterceptorChain;
import uk.gov.justice.services.core.interceptor.InterceptorContext;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SuppressWarnings("squid:S2187")
@RunWith(MockitoJUnitRunner.class)
public class SjpServiceFileInterceptorTest extends TestCase {

    @Mock
    private JsonEnvelope value;

    @Mock
    private JsonEnvelope inputEnvelope;

    @Mock
    private Map<String, UUID> results;

    @Mock
    ResultsHandler resultsHandler;

    @Mock
    private MultipleFileInputDetailsService multipleFileInputDetailsService;

    @Mock
    private DocumentTypeValidator documentTypeValidator;

    @Mock
    private InterceptorContext interceptorContext;

    @Mock
    private InterceptorChain interceptorChain;

    @Mock
    private InterceptorContext interceptorContextExpected;

    @InjectMocks
    SjpServiceFileInterceptor sjpServiceFileInterceptor;

    @Test
    public void shouldPassSjpServiceFileInterceptorValidationAndUpload() throws IOException {
        final String fileName = "fileName.txt";

        final List<FileInputDetails> fileInputDetailsList = getFileInputDetails(fileName);
        final Optional<Object> fileInputDetails = of(fileInputDetailsList);
        when(multipleFileInputDetailsService.storeFileDetails(fileInputDetailsList)).thenReturn(results);
        when(documentTypeValidator.isValid(fileName)).thenReturn(true);
        when(interceptorContext.getInputParameter("fileInputDetailsList")).thenReturn(fileInputDetails);

        final InterceptorContext outputInterceptorContext = this.interceptorContext.copyWithInput(inputEnvelope);

        when(interceptorContext.inputEnvelope()).thenReturn(value);
        when(resultsHandler.addResultsTo(value, results)).thenReturn(inputEnvelope);

        when(interceptorChain.processNext(outputInterceptorContext)).thenReturn(outputInterceptorContext);

        sjpServiceFileInterceptor.process(interceptorContext, interceptorChain);
        verify(interceptorChain).processNext(outputInterceptorContext);
    }

    @Test
    public void shouldNotPassSjpServiceFileInterceptorValidationAndUpload() throws IOException {
        when(interceptorContext.getInputParameter("fileInputDetailsList")).thenReturn(empty());
        final InterceptorContext outputInterceptorContext = this.interceptorContext.copyWithInput(inputEnvelope);
        when(interceptorChain.processNext(outputInterceptorContext)).thenReturn(null);

        sjpServiceFileInterceptor.process(interceptorContext, interceptorChain);
        verify(interceptorChain).processNext(interceptorContext);
    }

    @Test(expected = ForbiddenRequestException.class)
    public void shouldFailSjpServiceFileInterceptorValidationAndUpload() throws IOException {
        final String fileName = "fileName.exe";
        final List<FileInputDetails> fileInputDetailsList = getFileInputDetails(fileName);
        final Optional<Object> fileInputDetails = of(fileInputDetailsList);
        when(multipleFileInputDetailsService.storeFileDetails(fileInputDetailsList)).thenReturn(results);
        when(documentTypeValidator.isValid(fileName)).thenReturn(false);
        when(interceptorContext.getInputParameter("fileInputDetailsList")).thenReturn(fileInputDetails);
        final InterceptorContext outputInterceptorContext = this.interceptorContext.copyWithInput(inputEnvelope);

        when(interceptorContext.inputEnvelope()).thenReturn(value);
        when(resultsHandler.addResultsTo(value, results)).thenReturn(inputEnvelope);

        when(interceptorChain.processNext(outputInterceptorContext)).thenReturn(outputInterceptorContext);

        sjpServiceFileInterceptor.process(interceptorContext, interceptorChain);
    }
    private List<FileInputDetails> getFileInputDetails(final String fileName) throws IOException {
        final String FIELD_NAME = "FieldName";
        final InputStream is = IOUtils.toInputStream("some test data for my input stream", "UTF-8");
        final FileInputDetails fileInputDetails
                = new DefaultFileInputDetails(fileName, FIELD_NAME, is);
        final List<FileInputDetails> defaultFileInputDetails = new ArrayList();
        defaultFileInputDetails.add(fileInputDetails);
        return defaultFileInputDetails;
    }
}