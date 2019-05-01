package uk.gov.justice.api.resource;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.status;
import static uk.gov.justice.services.core.interceptor.InterceptorContext.interceptorContextWithInput;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.fileservice.api.FileRetriever;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.domain.FileReference;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

@SuppressWarnings({"squid:S00112"})
@Stateless
@Adapter(Component.QUERY_API)
public class DefaultQueryApiTransparencyReportContentFileIdResource implements QueryApiTransparencyReportContentFileIdResource {

    public static final String TRANSPARENCY_REPORT_CONTENT_QUERY_NAME = "sjp.query.transparency-report-content";
    public static final String PDF_CONTENT_TYPE = "application/pdf";

    @Inject
    private FileRetriever fileRetriever;

    @Inject
    private InterceptorChainProcessor interceptorChainProcessor;

    @Override
    public Response getTransparencyReportContentByFileId(final UUID fileId, final UUID userId) {

        final JsonEnvelope envelope = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(TRANSPARENCY_REPORT_CONTENT_QUERY_NAME)
                        .withUserId(userId.toString())
                        .build(),
                createObjectBuilder()
                        .add("fileId", fileId.toString())
                        .build()
        );

        interceptorChainProcessor.process(interceptorContextWithInput(envelope));

        return getResponse(fileId);
    }

    private Response getResponse(final UUID fileId) {
        final Optional<FileReference> fileRetrieverOptional;

        try {
            fileRetrieverOptional = fileRetriever.retrieve(fileId);
        } catch (FileServiceException fileServiceException) {
            throw new RuntimeException(fileServiceException);
        }

        final InputStream contentStream;
        final Response.ResponseBuilder responseBuilder;

        if (fileRetrieverOptional.isPresent()) {
            final FileReference fileReference = fileRetrieverOptional.get();
            contentStream =  fileReference.getContentStream();
            responseBuilder = status(OK).entity(contentStream);
            responseBuilder.header(CONTENT_TYPE, PDF_CONTENT_TYPE);
            responseBuilder.header(CONTENT_DISPOSITION, "attachment;filename=TransparencyReport_" + fileId.toString() + ".pdf");
        } else {
            throw new RuntimeException("No File present for the fileId : " + fileId.toString());
        }

        return responseBuilder.build();
    }
}
