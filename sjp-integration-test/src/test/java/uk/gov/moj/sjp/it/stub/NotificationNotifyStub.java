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
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.moj.sjp.it.stub.StubHelper.waitForPostStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.sjp.it.util.JsonHelper;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.json.JsonObject;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.jayway.awaitility.Duration;
import org.json.JSONObject;

public class NotificationNotifyStub {

    public static final String COMMAND_URL = "/notificationnotify-service/command/api/rest/notificationnotify/notifications/";
    public static final String COMMAND_MEDIA_TYPE = "application/vnd.notificationnotify.email+json";

    public static void stubNotifications() {
        InternalEndpointMockUtils.stubPingFor("notificationnotify-service");

        stubFor(post(urlPathMatching(COMMAND_URL + ".*"))
                .withHeader(CONTENT_TYPE, equalTo(COMMAND_MEDIA_TYPE))
                .willReturn(aResponse().withStatus(ACCEPTED.getStatusCode())
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));

        waitForPostStubToBeReady(COMMAND_URL + randomUUID(), COMMAND_MEDIA_TYPE, ACCEPTED);
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
                        .filter(str -> ! str.equals(""))
                        .map(JSONObject::new)
                        .anyMatch(commandPayloadPredicate));
    }

    public static JsonObject verifyNotification(final UUID notificationId, final String email) {
        final List<JsonObject> notifications = waitAtMost(Duration.TEN_SECONDS)
                .until(() -> findAll(postRequestedFor(urlPathMatching(COMMAND_URL + notificationId.toString()))
                                .withHeader(CONTENT_TYPE, equalTo(COMMAND_MEDIA_TYPE)))
                                .stream()
                                .map(LoggedRequest::getBodyAsString)
                                .filter(str -> ! str.equals(""))
                                .map(JsonHelper::getJsonObject)
                                .filter(commandPayload -> commandPayload.getString("sendToAddress").equals(email))
                                .collect(Collectors.toList())
                        , hasSize(1));

        return notifications.get(0);
    }

    public static void publishNotificationFailedPublicEvent(final UUID notificationId) {
        final JsonObject payload = createObjectBuilder()
                .add("notificationId", notificationId.toString())
                .add("failedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .add("errorMessage", "An error has occurred")
                .build();

        try (final MessageProducerClient producerClient = new MessageProducerClient()) {
            producerClient.startProducer("public.event");
            producerClient.sendMessage("public.notificationnotify.events.notification-failed", payload);
        }
    }

    public static void publishNotificationSentPublicEvent(final UUID notificationId) {
        final JsonObject payload = createObjectBuilder()
                .add("notificationId", notificationId.toString())
                .add("sentTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .build();

        try (final MessageProducerClient producerClient = new MessageProducerClient()) {
            producerClient.startProducer("public.event");
            producerClient.sendMessage("public.notificationnotify.events.notification-sent", payload);
        }
    }
}
