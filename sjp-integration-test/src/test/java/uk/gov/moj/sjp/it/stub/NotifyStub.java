package uk.gov.moj.sjp.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.jayway.awaitility.Awaitility.waitAtMost;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.ACCEPTED;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.util.function.Predicate;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.jayway.awaitility.Duration;
import org.json.JSONObject;

public class NotifyStub {

    public static final String COMMAND_URL = "/notificationnotify-service/command/api/rest/notificationnotify/notifications/";
    public static final String COMMAND_MEDIA_TYPE = "application/vnd.notificationnotify.email+json";

    public static void stubNotifications() {
        InternalEndpointMockUtils.stubPingFor("notificationnotify-service");

        stubFor(post(urlPathMatching(COMMAND_URL + ".*"))
                .withHeader(CONTENT_TYPE, equalTo(COMMAND_MEDIA_TYPE))
                .willReturn(aResponse().withStatus(ACCEPTED.getStatusCode())
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    public static void verifyNotification(final String email, final String urn, final String templateId) {

        final Predicate<JSONObject> commandPayloadPredicate = commandPayload -> commandPayload.getString("sendToAddress").equals(email)
                && commandPayload.getJSONObject("personalisation").getString("urn").equals(urn)
                && commandPayload.getString("templateId").equals(templateId);

        waitAtMost(Duration.TEN_SECONDS).until(() ->
                findAll(postRequestedFor(urlPathMatching(COMMAND_URL + ".*"))
                        .withHeader(CONTENT_TYPE, equalTo(COMMAND_MEDIA_TYPE)))
                        .stream()
                        .map(LoggedRequest::getBodyAsString)
                        .map(JSONObject::new)
                        .anyMatch(commandPayloadPredicate));
    }
}
