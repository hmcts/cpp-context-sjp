package uk.gov.moj.cpp.sjp.command.interceptor;

import uk.gov.justice.services.adapter.rest.interceptor.MultipleFileInputDetailsService;
import uk.gov.justice.services.adapter.rest.interceptor.ResultsHandler;
import uk.gov.justice.services.adapter.rest.multipart.FileInputDetails;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.core.interceptor.Interceptor;
import uk.gov.justice.services.core.interceptor.InterceptorChain;
import uk.gov.justice.services.core.interceptor.InterceptorContext;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

public class SjpServiceFileInterceptor implements Interceptor {

    @Inject
    DocumentTypeValidator documentTypeValidator;

    @Inject
    MultipleFileInputDetailsService multipleFileInputDetailsService;

    @Inject
    ResultsHandler resultsHandler;

    public InterceptorContext process(final InterceptorContext interceptorContext, final InterceptorChain interceptorChain) {
        final Optional<Object> inputParameterOptional = interceptorContext.getInputParameter("fileInputDetailsList");
        if (inputParameterOptional.isPresent()) {
            final List<FileInputDetails> fileInputDetails = (List) inputParameterOptional.get();
            for (final FileInputDetails filedetails : fileInputDetails) {
                final String fileNameWithExtention = filedetails.getFileName();
                if (!documentTypeValidator.isValid(fileNameWithExtention)) {
                    throw new ForbiddenRequestException("Allowed only doc|docx|jpg|jpeg|pdf|txt extensions");
                }
            }
            final Map<String, UUID> results = this.multipleFileInputDetailsService.storeFileDetails(fileInputDetails);
            final JsonEnvelope inputEnvelope = this.resultsHandler.addResultsTo(interceptorContext.inputEnvelope(), results);
            return interceptorChain.processNext(interceptorContext.copyWithInput(inputEnvelope));
        } else {
            return interceptorChain.processNext(interceptorContext);
        }
    }
}