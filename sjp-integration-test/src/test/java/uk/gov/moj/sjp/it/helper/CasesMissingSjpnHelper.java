package uk.gov.moj.sjp.it.helper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makeGetCall;

import uk.gov.moj.sjp.it.util.JsonHelper;

import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

public class CasesMissingSjpnHelper {

    public static JsonObject getCasesMissingSjpn(UUID userId) {
        return getCasesMissingSjpn(userId, "/cases-missing-sjpn");
    }

    public static JsonObject getCasesMissingSjpn(UUID userId, int limit) {
        String url = "/cases-missing-sjpn" + String.format("/?limit=%d", limit);
        return getCasesMissingSjpn(userId, url);
    }

    public static JsonObject getCasesMissingSjpnPostedDaysAgo(UUID userId, int postedDaysAgo) {
        String url = "/cases-missing-sjpn" + String.format("/?daysSincePosting=%d", postedDaysAgo);
        return getCasesMissingSjpn(userId, url);
    }

    public static JsonObject getCasesMissingSjpnPostedDaysAgo(UUID userId, int postedDaysAgo, int limit) {
        String url = "/cases-missing-sjpn" + String.format("/?daysSincePosting=%d&limit=%d", postedDaysAgo, limit);
        return getCasesMissingSjpn(userId, url);
    }

    private static JsonObject getCasesMissingSjpn(UUID userId, String url) {
        Response response = makeGetCall(url, "application/vnd.sjp.query.cases-missing-sjpn+json", userId);
        assertThat(response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));
        return JsonHelper.getJsonObject(response.readEntity(String.class));
    }
}
