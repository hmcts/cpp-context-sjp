package uk.gov.justice.api.resource;

import uk.gov.justice.services.common.http.HeaderConstants;

import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;


@Path("transparency-report/content/{fileId}")
public interface QueryApiTransparencyReportContentFileIdResource {

    @GET
    @Produces({"application/vnd.sjp.query.transparency-report-content+json"})
    Response getTransparencyReportContentByFileId(
            @PathParam("fileId") UUID fileId,
            @HeaderParam(HeaderConstants.USER_ID) UUID userId);
}

