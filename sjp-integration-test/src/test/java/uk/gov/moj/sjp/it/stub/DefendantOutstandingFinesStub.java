package uk.gov.moj.sjp.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaults;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;
import uk.gov.justice.services.common.http.HeaderConstants;

import com.github.tomakehurst.wiremock.client.WireMock;
import uk.gov.justice.services.test.utils.core.http.RequestParams;

import javax.ws.rs.core.Response;

public class DefendantOutstandingFinesStub {
    public static void stubStagingEnforcementOutstandingFines(final String firstName,
                                                        final String lastName,
                                                        final String dateOfBirth,
                                                        final String nationalInsuranceNumber, final String jsonResponse) {

        InternalEndpointMockUtils.stubPingFor("stagingenforcement-query-api");

        final String urlPath = "/stagingenforcement-service/query/api/rest/stagingenforcement/defendant/outstanding-fines";
        stubFor(get(urlPathEqualTo(urlPath))
                .withQueryParam("firstname", WireMock.equalTo(firstName))
                .withQueryParam("lastname", WireMock.equalTo(lastName))
                .withQueryParam("dob", WireMock.equalTo(dateOfBirth))
                .withQueryParam("ninumber", WireMock.equalTo(nationalInsuranceNumber))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, "application/vnd.stagingenforcement.defendant.outstanding-fines+json")
                        .withBody(jsonResponse)));
        waitForStubToBeReady(
                String.format("%s?firstname=%s&lastname=%s&dob=%s&ninumber=%s", urlPath, firstName, lastName, dateOfBirth, nationalInsuranceNumber),
                "application/vnd.stagingenforcement.defendant.outstanding-fines+json");

    }

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final String BASE_URI = "http://" + HOST + ":8080";

    public static void waitForStubToBeReady(String resource, String mediaType) {
        waitForStubToBeReady(resource, mediaType, Response.Status.OK);
    }

    public static void waitForStubToBeReady(String resource, String mediaType, Response.Status expectedStatus) {
        final RequestParams requestParams = requestParams(BASE_URI + resource, mediaType).build();
        pollWithDefaults(requestParams).until(status().is(expectedStatus));
    }
}
