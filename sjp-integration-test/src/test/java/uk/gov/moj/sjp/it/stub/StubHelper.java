package uk.gov.moj.sjp.it.stub;


import static com.jayway.awaitility.Awaitility.waitAtMost;
import static com.jayway.awaitility.Duration.TEN_SECONDS;
import static javax.ws.rs.client.Entity.entity;
import static uk.gov.justice.services.test.utils.core.rest.ResteasyClientBuilderFactory.clientBuilder;

import uk.gov.justice.services.test.utils.core.rest.RestClient;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class StubHelper {
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final String BASE_URI = "http://" + HOST + ":8080";

    public static void waitForPostStubToBeReady(final String resource, final String mediaType, final Response.Status expectedStatus) {
        final RestClient restClient = new RestClient();
        waitAtMost(TEN_SECONDS).until(() -> restClient.postCommand(BASE_URI + resource, mediaType, "").getStatus() == expectedStatus.getStatusCode());
    }

    public static void waitForGetStubToBeReady(final String resource, final String mediaType, final Response.Status expectedStatus) {
        final RestClient restClient = new RestClient();
        waitAtMost(TEN_SECONDS).until(() -> restClient.query(BASE_URI + resource, mediaType).getStatus() == expectedStatus.getStatusCode());
    }

    public static void waitForPutStubToBeReady(final String resource, final String contentType, final Response.Status expectedStatus) {
        waitAtMost(TEN_SECONDS)
                .until(() -> put(BASE_URI + resource, contentType)
                        .getStatus() == expectedStatus.getStatusCode());
    }

    private static Response put(final String url, final String contentType) {
        return clientBuilder().build()
                .target(url)
                .request()
                .put(entity("", MediaType.valueOf(contentType)));
    }

}
