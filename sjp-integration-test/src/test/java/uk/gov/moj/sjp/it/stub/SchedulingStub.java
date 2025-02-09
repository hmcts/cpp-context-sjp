package uk.gov.moj.sjp.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_ACCEPTED;

import uk.gov.justice.services.common.http.HeaderConstants;

public class SchedulingStub {

    private static final String START_SJP_SESSION_URL = "/scheduling-service/command/api/rest/scheduling/start-sjp-session";
    private static final String END_SJP_SESSION_URL = "/scheduling-service/command/api/rest/scheduling/end-sjp-session";

    public static void stubStartSjpSessionCommand() {
        stubFor(post(urlPathEqualTo(START_SJP_SESSION_URL))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    public static void stubEndSjpSessionCommand() {
        stubFor(post(urlPathEqualTo(END_SJP_SESSION_URL))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }
}
