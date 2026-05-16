package uk.gov.moj.cpp.sjp.command.interceptor;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.sjp.filestore.azure.StoragePath.internal;

import uk.gov.justice.services.adapter.rest.multipart.FileInputDetails;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.core.interceptor.Interceptor;
import uk.gov.justice.services.core.interceptor.InterceptorChain;
import uk.gov.justice.services.core.interceptor.InterceptorContext;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.filestore.azure.FileStorer;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObjectBuilder;

public class SjpServiceFileInterceptor implements Interceptor {

    @Inject
    DocumentTypeValidator documentTypeValidator;

    @Inject
    private FileStorer fileStorer;

    @Override
    public InterceptorContext process(final InterceptorContext interceptorContext, final InterceptorChain interceptorChain) {
        final Optional<Object> inputParameterOptional = interceptorContext.getInputParameter(FileInputDetails.FILE_INPUT_DETAILS_LIST);
        if (inputParameterOptional.isPresent()) {
            final List<FileInputDetails> fileInputDetailsList = (List<FileInputDetails>) inputParameterOptional.get();
            for (final FileInputDetails fileDetails : fileInputDetailsList) {
                if (!documentTypeValidator.isValid(fileDetails.getFileName())) {
                    throw new ForbiddenRequestException("Allowed only doc|docx|jpg|jpeg|pdf|txt extensions");
                }
            }
            final Map<String, UUID> results = storeFiles(fileInputDetailsList);
            final JsonEnvelope modifiedEnvelope = addResultsToEnvelope(interceptorContext.inputEnvelope(), results);
            return interceptorChain.processNext(interceptorContext.copyWithInput(modifiedEnvelope));
        } else {
            return interceptorChain.processNext(interceptorContext);
        }
    }

    private Map<String, UUID> storeFiles(final List<FileInputDetails> fileInputDetailsList) {
        final Map<String, UUID> results = new HashMap<>();
        for (final FileInputDetails fileDetails : fileInputDetailsList) {
            try (final InputStream inputStream = fileDetails.getInputStream()) {
                final UUID fileId = fileStorer.store(internal(), randomUUID(), fileDetails.getFileName(), inputStream);
                results.put(fileDetails.getFieldName(), fileId);
            } catch (final IOException e) {
                throw new SjpDocumentUploadException("Failed to store uploaded file: " + fileDetails.getFileName(), e);
            }
        }
        return results;
    }

    private JsonEnvelope addResultsToEnvelope(final JsonEnvelope envelope, final Map<String, UUID> results) {
        final JsonObjectBuilder payloadBuilder = createObjectBuilder(envelope.payloadAsJsonObject());
        results.forEach((fieldName, fileId) -> payloadBuilder.add(fieldName, fileId.toString()));
        return envelopeFrom(metadataFrom(envelope.metadata()), payloadBuilder.build());
    }
}
