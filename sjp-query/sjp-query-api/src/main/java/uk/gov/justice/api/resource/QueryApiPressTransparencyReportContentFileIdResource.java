package uk.gov.justice.api.resource;

import uk.gov.justice.services.common.http.HeaderConstants;

import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@Path("press-transparency-report/content/{fileId}")
public interface QueryApiPressTransparencyReportContentFileIdResource {
  @GET
  @Produces("application/vnd.sjp.query.press-transparency-report-content+json")
  Response getPressTransparencyReportContentByFileId(
          @PathParam("fileId") UUID fileId,
          @HeaderParam(HeaderConstants.USER_ID) UUID userId);
}
