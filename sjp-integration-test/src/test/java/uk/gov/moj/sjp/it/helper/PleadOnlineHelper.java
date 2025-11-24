package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static javax.ws.rs.core.Response.Status.OK;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SET_PLEAS;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseById;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.getPostCallResponse;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makeGetCall;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.TIMEOUT_IN_SECONDS;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaults;
import static uk.gov.moj.sjp.it.util.TopicUtil.retrieveMessage;

import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.sjp.it.model.PleasView;
import uk.gov.moj.sjp.it.util.HttpClientUtil;
import uk.gov.moj.sjp.it.util.TopicUtil;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PleadOnlineHelper implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PleadOnlineHelper.class);

    private final String writeUrl;

    private final UUID defendantId;

    private MessageConsumer publicEventsConsumer;

    public PleadOnlineHelper(final UUID caseId, final UUID defendantId) {
        this.defendantId = defendantId;
        publicEventsConsumer = TopicUtil.publicEvents.createConsumer(PUBLIC_EVENT_SET_PLEAS);
        writeUrl = String.format("/cases/%s/defendants/%s/plead-online", caseId, defendantId);
    }

    public PleadOnlineHelper(final UUID caseId, final UUID defendantId, final UUID pcqId) {
        this.defendantId = defendantId;
        publicEventsConsumer = TopicUtil.publicEvents.createConsumer(PUBLIC_EVENT_SET_PLEAS);
        writeUrl = String.format("/cases/%s/defendants/%s/plead-online-pcq-visited", caseId, defendantId);
    }

    public PleadOnlineHelper(final UUID caseId) {
        publicEventsConsumer = TopicUtil.publicEvents.createConsumer(PUBLIC_EVENT_SET_PLEAS);
        defendantId = UUID.fromString(pollUntilCaseByIdIsOk(caseId).getString("defendant.id"));
        writeUrl = String.format("/cases/%s/defendants/%s/plead-online", caseId, defendantId);
    }

    public static Response getOnlinePlea(final String caseId, final String defendantId, final UUID userId) {
        final String resource = format("/cases/%s/defendants/%s/defendants-online-plea", caseId, defendantId);
        final String contentType = "application/vnd.sjp.query.defendants-online-plea+json";
        return makeGetCall(resource, contentType, userId);
    }


    public static String getOnlinePlea(final String caseId, final String defendantId, final Matcher<Object> jsonMatcher, final UUID userId) {
        return await().atMost(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS).until(() -> {
            final Response onlinePlea = getOnlinePlea(caseId, defendantId, userId);
            if (onlinePlea.getStatus() != OK.getStatusCode()) {
                fail("Polling interrupted, please fix the error before continue. Status code: " + onlinePlea.getStatus());
            }

            String response = onlinePlea.readEntity(String.class);
            ObjectMapper objectMapper = new ObjectMapper();
            PleasView pleasView = objectMapper.readValue(response, PleasView.class);
            return objectMapper.writeValueAsString(pleasView.getPleas().get(0));
        }, jsonMatcher);
    }

    public static String getOnlinePleaAocpAccepted(final String caseId, final String defendantId, final Matcher<Object> jsonMatcher, final UUID userId) {
        return await().atMost(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS).until(() -> {
            final Response onlinePlea = getOnlinePlea(caseId, defendantId, userId);
            if (onlinePlea.getStatus() != OK.getStatusCode()) {
                fail("Polling interrupted, please fix the error before continue. Status code: " + onlinePlea.getStatus());
            }

            String response = onlinePlea.readEntity(String.class);
            ObjectMapper objectMapper = new ObjectMapper();
            PleasView pleasView = objectMapper.readValue(response, PleasView.class);
            return objectMapper.writeValueAsString(pleasView.getPleas().get(0));
        }, jsonMatcher);
    }

    public static void verifyOnlinePleaReceivedAndUpdatedCaseDetailsFlag(final UUID caseId, final boolean onlinePleaReceived) {
        pollWithDefaults(getCaseById(caseId))
                .timeout(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(
                                withJsonPath("$.onlinePleaReceived", is(onlinePleaReceived))
                        )
                );

    }

    private void pleadOnline(final String payload, final String contentType, final Response.Status httpStatus) {
        LOGGER.info("Request payload: {}", payload);
        HttpClientUtil.makePostCall(writeUrl, contentType, payload, httpStatus);
    }

    public String pleadOnline(final String payload, final Response.Status httpStatus) {
        return getPostCallResponse(writeUrl, "application/vnd.sjp.plead-online+json", payload, httpStatus);
    }

    public String pleadOnlineAocp(final String payload, final Response.Status httpStatus) {
        return getPostCallResponse(writeUrl, "application/vnd.sjp.plead-aocp-online+json", payload, httpStatus);
    }

    public void pleadOnline(final String payload) {
        pleadOnline(payload, "application/vnd.sjp.plead-online+json", Response.Status.ACCEPTED);
    }

    public void pleadOnlinePcqVisited(final String payload) {
        pleadOnline(payload, "application/vnd.sjp.plead-online-pcq-visited+json", Response.Status.ACCEPTED);
    }

    public UUID getCaseDefendantId() {
        return defendantId;
    }

    public void verifyInPublicTopic(final UUID caseId, final UUID offenceId, final PleaType pleaType, final String denialReason) {
        assertEventData(caseId, offenceId, pleaType, denialReason);
    }

    public JsonPath verifyPleaUpdated(final UUID caseId, final Matcher[] matchers) {
        return pollUntilCaseByIdIsOk(caseId,
                allOf(matchers)
        );
    }

    private void assertEventData(final UUID caseId, final UUID offenceId, final PleaType pleaType, final String denialReason) {
        final JsonPath message = retrieveMessage(publicEventsConsumer);
        assertThat(message.get("caseId"), equalTo(caseId.toString()));
        assertThat(message.get("pleas[0].offenceId"), equalTo(offenceId.toString()));
        assertThat(message.get("pleas[0].pleaType"), equalTo(pleaType.name()));
        assertThat(message.get("denialReason"), equalTo(denialReason));
    }

    @Override
    public void close() {
        try {
            publicEventsConsumer.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

}
