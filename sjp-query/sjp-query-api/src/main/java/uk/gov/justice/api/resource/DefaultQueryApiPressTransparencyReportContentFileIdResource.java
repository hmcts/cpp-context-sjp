package uk.gov.justice.api.resource;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.interceptor.InterceptorContext.interceptorContextWithInput;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.fileservice.api.FileRetriever;
import uk.gov.justice.services.messaging.JsonEnvelope;

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

    return ResourceUtility.getResponse(fileRetriever, fileId, "attachment;filename=PressTransparencyReport_");
  }
}
