package uk.gov.justice.api.resource;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.core.interceptor.InterceptorContext.interceptorContextWithInput;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.fileservice.api.FileRetriever;
import uk.gov.justice.services.messaging.JsonEnvelope;

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

        return ResourceUtility.getResponse(fileRetriever, fileId, "attachment;filename=TransparencyReport_");
    }

}
