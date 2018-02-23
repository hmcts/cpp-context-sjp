package uk.gov.moj.sjp.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.ACCEPTED;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

public class NotifyStub {

    public static final String COMMAND_URL = "/notificationnotify-command-api/command/api/rest/notificationnotify/notifications/";
    public static final String COMMAND_MEDIA_TYPE = "application/vnd.notificationnotify.email+json";

    public static void stubNotifications() {
        InternalEndpointMockUtils.stubPingFor("notificationnotify-command-api");

        stubFor(post(urlPathMatching(COMMAND_URL + ".*"))
                .withHeader("Content-Type", equalTo(COMMAND_MEDIA_TYPE))
                .willReturn(aResponse().withStatus(ACCEPTED.getStatusCode())
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)));
    }

    public static void verifyNotification(final String email, final String urn) {

        verify(postRequestedFor(urlPathMatching(COMMAND_URL + ".*")));
    }
}
