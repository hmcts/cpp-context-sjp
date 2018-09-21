package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SELECTOR_PLEA_UPDATED;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;
import static uk.gov.moj.sjp.it.util.QueueUtil.retrieveMessage;

import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;
import uk.gov.moj.sjp.it.util.QueueUtil;

import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import com.jayway.restassured.path.json.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdatePleaHelper implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdatePleaHelper.class);

    private static final String WRITE_URL_PATTERN = "/cases/%s/offences/%s/pleas";

    private MessageConsumer publicEventsConsumer;

    public UpdatePleaHelper() {
        this(PUBLIC_EVENT_SELECTOR_PLEA_UPDATED);
    }

    public UpdatePleaHelper(String publicEvent) {
        publicEventsConsumer = QueueUtil.publicEvents.createConsumer(publicEvent);
    }

    private String writeUrl(final UUID caseId, final UUID offenceId) {
        if (caseId == null || offenceId == null) {
            throw new IllegalArgumentException(String.format("Both values are required. CaseId '%s'. OffenceId: '%s'", caseId, offenceId));
        }
        return String.format(WRITE_URL_PATTERN, caseId, offenceId);
    }

    private void requestHttpCallWithPayloadAndStatus(
            final UUID caseId, final UUID offenceId, final String payload, final Response.Status expectedStatus) {

        LOGGER.info("Request payload: {}", new JsonPath(payload).prettify());

        makePostCall(writeUrl(caseId, offenceId), "application/vnd.sjp.update-plea+json", payload, expectedStatus);
    }

    public void updatePlea(final UUID caseId, final UUID offenceId, final JsonObject payload) {
        requestHttpCallWithPayloadAndStatus(caseId, offenceId, payload.toString(), Response.Status.ACCEPTED);
    }

    public void updatePlea(final UUID caseId, final UUID offenceId, final PleaType pleaType) {
        requestHttpCallWithPayloadAndStatus(caseId, offenceId, getPleaPayload(pleaType).toString(), Response.Status.ACCEPTED);
    }

    public void verifyInPublicTopic(final UUID caseId, final UUID offenceId, final PleaType pleaType, final String denialReason) {
        assertEventData(caseId, offenceId, pleaType, denialReason);
    }

    private void assertEventData(final UUID caseId, final UUID offenceId, final PleaType pleaType, final String denialReason) {
        final JsonPath message = retrieveMessage(publicEventsConsumer);
        assertThat(message.get("caseId"), equalTo(caseId.toString()));
        assertThat(message.get("offenceId"), equalTo(offenceId.toString()));
        assertThat(message.get("plea"), equalTo(pleaType.name()));
        assertThat(message.get("denialReason"), equalTo(denialReason));
    }

    public void verifyPleaUpdated(final UUID caseId, final PleaType pleaType, final PleaMethod pleaMethod) {
        CasePoller.pollUntilCaseByIdIsOk(caseId,
                allOf(
                        withJsonPath("defendant.offences[0].plea", is(pleaType.name())),
                        withJsonPath("defendant.offences[0].pleaMethod", is(pleaMethod.name()))
                )
        );
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

    public static JsonObject getPleaPayload(final PleaType pleaType) {
        final Boolean mandatoryFields = (PleaType.NOT_GUILTY.equals(pleaType) || PleaType.GUILTY_REQUEST_HEARING.equals(pleaType)) ? false : null;

        return getPleaPayload(pleaType, mandatoryFields, null, mandatoryFields);
    }

    public static JsonObject getPleaPayload(final PleaType pleaType, final Boolean interpreterRequired, final String interpreterLanguage, final Boolean speakWelsh) {
        final JsonObjectBuilder builder = createObjectBuilder()
                .add("plea", pleaType.name());
        if (interpreterRequired != null) {
            builder.add("interpreterRequired", interpreterRequired);
        }
        if (interpreterLanguage != null) {
            builder.add("interpreterLanguage", interpreterLanguage);
        }
        if (speakWelsh != null) {
            builder.add("speakWelsh", speakWelsh);
        }

        return builder.build();
    }
}
