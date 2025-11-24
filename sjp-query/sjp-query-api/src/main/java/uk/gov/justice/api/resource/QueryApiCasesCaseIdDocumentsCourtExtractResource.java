package uk.gov.justice.api.resource;

import uk.gov.justice.services.common.http.HeaderConstants;

import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("cases/{caseId}/documents/court-extract")
public interface QueryApiCasesCaseIdDocumentsCourtExtractResource {

    @GET
    @Produces("application/vnd.sjp.query.case-court-extract+json")
    Response getCasesByCaseIdDocumentsCourtExtract(@PathParam("caseId") UUID caseId,
                                                   @HeaderParam(HeaderConstants.USER_ID) UUID userId);

}
