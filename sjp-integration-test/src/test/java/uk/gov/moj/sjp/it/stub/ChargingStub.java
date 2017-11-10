package uk.gov.moj.sjp.it.stub;


import org.json.JSONObject;
import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;
import static uk.gov.moj.sjp.it.util.WiremockTestHelper.waitForStubToBeReady;

public class ChargingStub extends StubUtil {

    private static final String CHARGING_QUERY_API_REVIEWS_URL = "/charging-query-api/query/api/rest/charging/reviews";
    private static final String CHARGING_QUERY_API_REVIEWS_MEDIA_TYPE = "application/vnd.charging.query.reviews+json";

    private static final String CHARGING_QUERY_API_REVIEW_URL = "/charging-query-api/query/api/rest/charging/review";
    private static final String CHARGING_QUERY_API_REVIEW_MEDIA_TYPE = "application/vnd.charging.query.review+json";

    public static void stubGetReviewsByIds(String caseId) {
        InternalEndpointMockUtils.stubPingFor("charging-query-api");

        final String ids = "7e2f843e-d639-40b3-8611-8015f3a18958";
        stubFor(get(urlPathEqualTo(CHARGING_QUERY_API_REVIEWS_URL))
                .withQueryParam("ids", equalTo(ids))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", DEFAULT_JSON_CONTENT_TYPE)
                        .withBody(getPayload("stub-data/charging-unassigned-review-case-ids.json").replace("CASE_ID", caseId))));

        waitForStubToBeReady(withQueryParam(CHARGING_QUERY_API_REVIEWS_URL, "ids", ids), CHARGING_QUERY_API_REVIEWS_MEDIA_TYPE);
    }

    public static void stubGetReviewById(String caseId, String reviewStatus) {
        InternalEndpointMockUtils.stubPingFor("charging-query-api");

        JSONObject responsePayload = new JSONObject(getPayload("stub-data/charging-query-review.json"));
        responsePayload.put("caseId", caseId);
        responsePayload.put("reviewStatus", reviewStatus);

        stubFor(get(urlPathEqualTo(CHARGING_QUERY_API_REVIEW_URL))
                .withQueryParam("caseId", equalTo(caseId))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", DEFAULT_JSON_CONTENT_TYPE)
                        .withBody(responsePayload.toString())));

        waitForStubToBeReady(withQueryParam(CHARGING_QUERY_API_REVIEW_URL, "caseId", caseId), CHARGING_QUERY_API_REVIEW_MEDIA_TYPE);
    }
}
