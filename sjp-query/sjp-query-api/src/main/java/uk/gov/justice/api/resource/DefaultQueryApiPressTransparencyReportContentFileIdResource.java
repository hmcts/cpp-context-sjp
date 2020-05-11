package uk.gov.justice.api.resource;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
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

import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

@Adapter(Component.QUERY_API)
public class DefaultQueryApiPressTransparencyReportContentFileIdResource implements QueryApiPressTransparencyReportContentFileIdResource {

  private static final String PRESS_TRANSPARENCY_REPORT_CONTENT_QUERY_NAME = "sjp.query.transparency-report-content";
  private static final String PDF_CONTENT_TYPE = "application/pdf";

  @Inject
  private FileRetriever fileRetriever;

  @Inject
  InterceptorChainProcessor interceptorChainProcessor;

  @Context
  HttpHeaders headers;

  @Override
  public Response getPressTransparencyReportContentByFileId(final UUID fileId, final UUID userId) {
    final JsonEnvelope envelope = envelopeFrom(
            metadataBuilder()
                    .withId(randomUUID())
                    .withName(PRESS_TRANSPARENCY_REPORT_CONTENT_QUERY_NAME)
                    .withUserId(userId.toString())
                    .build(),
            createObjectBuilder()
                    .add("fileId", fileId.toString())
                    .build()
    );

    interceptorChainProcessor.process(interceptorContextWithInput(envelope));

    return getResponse(fileId);
  }

  @SuppressWarnings("squid:S00112")
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
      responseBuilder.header(CONTENT_DISPOSITION, "attachment;filename=PressTransparencyReport_" + fileId.toString() + ".pdf");
    } else {
      responseBuilder = status(NOT_FOUND);
    }

    return responseBuilder.build();
  }
}
