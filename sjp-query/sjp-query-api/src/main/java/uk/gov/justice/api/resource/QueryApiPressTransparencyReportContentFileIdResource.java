package uk.gov.justice.api.resource;

import uk.gov.justice.services.common.http.HeaderConstants;

import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("press-transparency-report/content/{fileId}")
public interface QueryApiPressTransparencyReportContentFileIdResource {
  @GET
  @Produces("application/vnd.sjp.query.press-transparency-report-content+json")
  Response getPressTransparencyReportContentByFileId(
          @PathParam("fileId") UUID fileId,
          @HeaderParam(HeaderConstants.USER_ID) UUID userId);
}
