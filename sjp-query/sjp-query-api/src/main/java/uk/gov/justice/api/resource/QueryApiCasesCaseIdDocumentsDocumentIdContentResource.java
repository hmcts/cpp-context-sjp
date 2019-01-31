
package uk.gov.justice.api.resource;

import uk.gov.justice.services.common.http.HeaderConstants;

import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("cases/{caseId}/documents/{documentId}/content")
public interface QueryApiCasesCaseIdDocumentsDocumentIdContentResource {

    @GET
    @Produces({"application/vnd.sjp.query.case-document-content+json"})
    Response getDocumentContent(
            @PathParam("caseId") UUID caseId,
            @PathParam("documentId") UUID documentId,
            @HeaderParam(HeaderConstants.USER_ID) UUID userId);
}
