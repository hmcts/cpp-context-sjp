package uk.gov.justice.api.resource;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.status;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.core.interceptor.InterceptorContext.interceptorContextWithInput;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.api.service.CourtExtractDataService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

@Stateless
@Adapter(QUERY_API)
public class DefaultQueryApiCasesCaseIdDocumentsCourtExtractResource implements QueryApiCasesCaseIdDocumentsCourtExtractResource {

    private static final Logger LOGGER = getLogger(DefaultQueryApiCasesCaseIdDocumentsCourtExtractResource.class);

    private static final String MIME_TYPE_PDF = "application/pdf";

    public static final String QUERY_ACTION_NAME = "sjp.query.case-court-extract";

    private static final String EXTRACT_FILE_NAME = "court_extract.pdf";

    @Inject
    private InterceptorChainProcessor interceptorChainProcessor;

    @Inject
    private CourtExtractDataService courtExtractDataService;

    @Override
    public Response getCasesByCaseIdDocumentsCourtExtract(final UUID caseId, final UUID userId) {

        final JsonEnvelope initialQueryEnvelope = buildRequestEnvelope(caseId, userId);

        interceptorChainProcessor.process(interceptorContextWithInput(initialQueryEnvelope));

        final Optional<JsonObject> courtExtractData = courtExtractDataService.getCourtExtractData(initialQueryEnvelope);

        return courtExtractData.map(courtData -> {

            try(final InputStream documentInputStream = new ByteArrayInputStream(courtExtractDataService.generatePdfDocument(courtData))) {
                return status(OK).entity(documentInputStream)
                    .header(CONTENT_TYPE, MIME_TYPE_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + EXTRACT_FILE_NAME + "\"")
                    .build();
            } catch (final IOException e) {
                LOGGER.error(e.getMessage(), e);
                return status(INTERNAL_SERVER_ERROR).build();
            }

        }).orElseGet(() -> status(NOT_FOUND).build());

    }

    private JsonEnvelope buildRequestEnvelope(final UUID caseId, UUID userId) {
        return envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(QUERY_ACTION_NAME)
                        .withUserId(userId.toString())
                        .build(),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .build());
    }
}
