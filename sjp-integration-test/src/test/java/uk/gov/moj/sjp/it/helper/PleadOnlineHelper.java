package uk.gov.moj.sjp.it.helper;

import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;
import uk.gov.moj.sjp.it.util.HttpClientUtil;
import uk.gov.moj.sjp.it.util.TopicUtil;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.ws.rs.core.Response;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SET_PLEAS;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseById;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaults;
import static uk.gov.moj.sjp.it.util.TopicUtil.retrieveMessage;

import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;
import uk.gov.moj.sjp.it.util.HttpClientUtil;
import uk.gov.moj.sjp.it.util.TopicUtil;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.ws.rs.core.Response;

import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PleadOnlineHelper implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PleadOnlineHelper.class);

    private final String writeUrl;

    private final UUID defendantId;

    private MessageConsumer publicEventsConsumer;

    private MessageConsumer privateEventsConsumer;

    public PleadOnlineHelper(final UUID caseId, final UUID defendantId) {
        this.defendantId = defendantId;
        publicEventsConsumer = TopicUtil.publicEvents.createConsumer(PUBLIC_EVENT_SET_PLEAS);
        writeUrl = String.format("/cases/%s/defendants/%s/plead-online", caseId, defendantId);
    }

    public PleadOnlineHelper(final UUID caseId, final UUID defendantId, final UUID pcqId) {
        this.defendantId = defendantId;
        publicEventsConsumer = TopicUtil.publicEvents.createConsumer(PUBLIC_EVENT_SET_PLEAS);
        privateEventsConsumer = TopicUtil.privateEvents.createConsumer("sjp.events.defendant-details-updated");
        writeUrl = String.format("/cases/%s/defendants/%s/plead-online-pcq-visited", caseId, defendantId);
    }

    public PleadOnlineHelper(final UUID caseId) {
        publicEventsConsumer = TopicUtil.publicEvents.createConsumer(PUBLIC_EVENT_SET_PLEAS);
        defendantId = UUID.fromString(CasePoller.pollUntilCaseByIdIsOk(caseId).getString("defendant.id"));
        writeUrl = String.format("/cases/%s/defendants/%s/plead-online", caseId, defendantId);
    }

    public static Response getOnlinePlea(final String caseId, final String defendantId, final UUID userId) {
        final String resource = format("/cases/%s/defendants/%s/defendants-online-plea", caseId, defendantId);
        final String contentType = "application/vnd.sjp.query.defendants-online-plea+json";
        return HttpClientUtil.makeGetCall(resource, contentType, userId);
    }

    public static String getOnlinePlea(final String caseId, final String defendantId, final Matcher<Object> jsonMatcher, final UUID userId) {
        return await().atMost(20, TimeUnit.SECONDS).until(() -> {
            final Response onlinePlea = getOnlinePlea(caseId, defendantId, userId);
            if (onlinePlea.getStatus() != OK.getStatusCode()) {
                fail("Polling interrupted, please fix the error before continue. Status code: " + onlinePlea.getStatus());
            }

            return onlinePlea.readEntity(String.class);
        }, jsonMatcher);
    }

    public static void verifyOnlinePleaReceivedAndUpdatedCaseDetailsFlag(final UUID caseId, final boolean onlinePleaReceived) {
        pollWithDefaults(getCaseById(caseId))
                .timeout(20, TimeUnit.SECONDS)
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
        return HttpClientUtil.getPostCallResponse(writeUrl, "application/vnd.sjp.plead-online+json", payload, httpStatus);
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

    public void verifyInPrivateTopic(UUID caseId, UUID defendantId, UUID pcqId) {
        assertPrivateEventData(caseId, defendantId, pcqId);
    }

    public JsonPath verifyPleaUpdated(final UUID caseId, final PleaType pleaType, final PleaMethod pleaMethod) {
        return verifyPleaUpdated(caseId, pleaType, pleaMethod, 0);
    }

    public JsonPath verifyPleaUpdated(final UUID caseId, final PleaType pleaType, final PleaMethod pleaMethod, final int index) {
        return CasePoller.pollUntilCaseByIdIsOk(caseId,
                allOf(
                        withJsonPath(format("defendant.offences[%d].plea", index), is(pleaType.name())),
                        withJsonPath(format("defendant.offences[%d].pleaMethod", index), is(pleaMethod.name())),
                        withJsonPath(format("defendant.offences[%d].pleaDate", index), notNullValue()),
                        withJsonPath("onlinePleaReceived", is(PleaMethod.ONLINE.equals(pleaMethod)))
                )
        );
    }

    private void assertEventData(final UUID caseId, final UUID offenceId, final PleaType pleaType, final String denialReason) {
        final JsonPath message = retrieveMessage(publicEventsConsumer);
        assertThat(message.get("caseId"), equalTo(caseId.toString()));
        assertThat(message.get("pleas[0].offenceId"), equalTo(offenceId.toString()));
        assertThat(message.get("pleas[0].pleaType"), equalTo(pleaType.name()));
        assertThat(message.get("denialReason"), equalTo(denialReason));
    }

    private void assertPrivateEventData(final UUID caseId, final UUID defendantId, final UUID pcqId) {
        final JsonPath message = retrieveMessage(privateEventsConsumer);
        assertThat(message.get("caseId"), equalTo(caseId.toString()));
        assertThat(message.get("defendantId"), equalTo(defendantId.toString()));
        if (nonNull(pcqId)) {
            assertThat(message.get("pcqId"), equalTo(pcqId.toString()));
        } else {
            assertThat(message.get("pcqId"), is(nullValue()));
        }
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
