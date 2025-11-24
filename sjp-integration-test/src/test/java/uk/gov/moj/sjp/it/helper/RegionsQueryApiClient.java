package uk.gov.moj.sjp.it.helper;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.moj.sjp.it.util.HttpClientUtil;
import uk.gov.moj.sjp.it.util.JsonHelper;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

public class RegionsQueryApiClient {

    private RegionsQueryApiClient() {
    }

    public static JsonObject getRegions() {
        final String contentType = "application/vnd.sjp.query.regions+json";
        final String resource = "/regions";

        final Response response = HttpClientUtil.makeGetCall(resource, contentType);
        assertThat(response.getStatus(), is(200));

        return JsonHelper.getJsonObject(response.readEntity(String.class));
    }
}
