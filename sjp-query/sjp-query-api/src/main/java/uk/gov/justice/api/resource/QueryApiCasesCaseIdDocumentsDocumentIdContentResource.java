
package uk.gov.justice.api.resource;

import uk.gov.justice.services.common.http.HeaderConstants;

import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@Path("cases/{caseId}/documents/{documentId}/content")
public interface QueryApiCasesCaseIdDocumentsDocumentIdContentResource {

    @GET
    @Produces({"application/vnd.sjp.query.case-document-content+json"})
    Response getDocumentContent(
            @PathParam("caseId") UUID caseId,
            @PathParam("documentId") UUID documentId,
            @HeaderParam(HeaderConstants.USER_ID) UUID userId);
}
