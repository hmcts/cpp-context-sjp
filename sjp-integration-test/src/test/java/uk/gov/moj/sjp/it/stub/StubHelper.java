package uk.gov.moj.sjp.it.stub;


import static org.awaitility.Awaitility.waitAtMost;
import static org.awaitility.Durations.TEN_SECONDS;

import uk.gov.justice.services.test.utils.core.rest.RestClient;

import javax.ws.rs.core.Response;

public class StubHelper {
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final String BASE_URI = "http://" + HOST + ":8080";

    public static void waitForStubToBeReady(final String resource, final String mediaType) {
        waitForGetStubToBeReady(resource, mediaType, Response.Status.OK);
    }

    public static void waitForPostStubToBeReady(final String resource, final String mediaType, final Response.Status expectedStatus) {
        final RestClient restClient = new RestClient();
        waitAtMost(TEN_SECONDS).until(() -> restClient.postCommand(BASE_URI + resource, mediaType, "").getStatus() == expectedStatus.getStatusCode());
    }

    public static void waitForGetStubToBeReady(final String resource, final String mediaType, final Response.Status expectedStatus) {
        final RestClient restClient = new RestClient();
        waitAtMost(TEN_SECONDS).until(() -> restClient.query(BASE_URI + resource, mediaType).getStatus() == expectedStatus.getStatusCode());
    }

}
