package uk.gov.moj.sjp.it.helper;

import uk.gov.moj.sjp.it.util.HttpClientUtil;

import java.util.UUID;

import javax.ws.rs.core.Response;

import com.jayway.restassured.path.json.JsonPath;

public class CaseProsecutingAuthorityHelper {

    public static String getProsecutingAuthority(final UUID caseId) {
        final String contentType = "application/vnd.sjp.query.case-prosecuting-authority+json";
        final String url = String.format("/cases/%s/prosecuting-authority", caseId);
        final Response response = HttpClientUtil.makeGetCall(url, contentType);
        return new JsonPath(response.readEntity(String.class)).getString("prosecutingAuthority");
    }
}
