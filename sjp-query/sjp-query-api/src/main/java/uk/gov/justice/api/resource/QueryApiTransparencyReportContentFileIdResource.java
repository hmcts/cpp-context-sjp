package uk.gov.justice.api.resource;

import uk.gov.justice.services.common.http.HeaderConstants;

import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;


@Path("transparency-report/content/{fileId}")
public interface QueryApiTransparencyReportContentFileIdResource {

    @GET
    @Produces({"application/vnd.sjp.query.transparency-report-content+json"})
    Response getTransparencyReportContentByFileId(
            @PathParam("fileId") UUID fileId,
            @HeaderParam(HeaderConstants.USER_ID) UUID userId);
}

