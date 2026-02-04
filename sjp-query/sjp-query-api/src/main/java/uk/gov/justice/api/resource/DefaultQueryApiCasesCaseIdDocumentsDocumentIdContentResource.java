
package uk.gov.justice.api.resource;

import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.fromStatusCode;
import static javax.ws.rs.core.Response.status;
import static uk.gov.justice.services.core.interceptor.InterceptorContext.interceptorContextWithInput;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.client.MaterialClient;
import uk.gov.moj.cpp.systemusers.ServiceContextSystemUserProvider;

import java.util.UUID;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * http endpoint adapter which overrides default framework adapter. It handles transfer of files
 * binaries between material and sjp context. Name of this class is after raml definition of
 * case-document-content query and need to be changed when raml definition changes. Class invoke
 * standard interceptor chain. Thanks to that all standard cross-cutting concerns like
 * authorisation, audit, performance metrics, feature toggles handling are handled in standard way.
 * At the end of interceptor chain, regular query handler is invoked and returns documents details
 * (materialId among others)
 */

@Stateless
@Adapter(Component.QUERY_API)
public class DefaultQueryApiCasesCaseIdDocumentsDocumentIdContentResource implements QueryApiCasesCaseIdDocumentsDocumentIdContentResource {

    static final String CASE_DOCUMENT_CONTENT_QUERY_NAME = "sjp.query.case-document-content";

    @Inject
    private ServiceContextSystemUserProvider serviceContextSystemUserProvider;

    @Inject
    private InterceptorChainProcessor interceptorChainProcessor;

    @Inject
    private MaterialClient materialClient;

    @Override
    public Response getDocumentContent(final UUID caseId, final UUID documentId, final UUID userId) {

        final JsonEnvelope documentQuery = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(CASE_DOCUMENT_CONTENT_QUERY_NAME)
                        .withUserId(userId.toString())
                        .build(),
                createObjectBuilder()
                        .add("documentId", documentId.toString())
                        .add("caseId", caseId.toString())
                        .build()
        );

        return interceptorChainProcessor.process(interceptorContextWithInput(documentQuery))
                .map(this::getDocumentContent)
                .orElse(status(NOT_FOUND).build());
    }

    private Response getDocumentContent(final JsonEnvelope document) {
        if (JsonValue.NULL.equals(document.payload())) {
            return null;
        } else {
            final UUID systemUser = serviceContextSystemUserProvider.getContextSystemUserId()
                    .orElseThrow(() -> new WebApplicationException("System user for sjp context not found"));

            final UUID materialId = UUID.fromString(document.payloadAsJsonObject().getJsonObject("caseDocument").getString("materialId"));
            final Response documentContentResponse = materialClient.getMaterialWithHeader(materialId, systemUser);
            final Response.Status documentContentResponseStatus = fromStatusCode(documentContentResponse.getStatus());

            if (OK.equals(documentContentResponseStatus)) {
                final String url = documentContentResponse.readEntity(String.class);
                final JsonObject jsonObject = createObjectBuilder()
                        .add("url", url)
                        .build();

                return Response
                        .status(OK)
                        .entity(jsonObject)
                        .header(CONTENT_TYPE, "application/json")
                        .build();

            }
            return Response
                    .fromResponse(documentContentResponse)
                    .build();
        }
    }
}
