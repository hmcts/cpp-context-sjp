package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.EventSelector.EVENT_SELECTOR_PLEA_CANCELLED;
import static uk.gov.moj.sjp.it.EventSelector.PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseById;
import static uk.gov.moj.sjp.it.util.QueueUtil.retrieveMessage;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.RestPoller;
import uk.gov.moj.sjp.it.util.QueueUtil;

import javax.jms.MessageConsumer;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.jayway.restassured.path.json.JsonPath;

public class CancelPleaHelper extends AbstractTestHelper {

    private CaseSjpHelper caseSjpHelper;
    private String offenceId;
    private final String writeUrl;

    public CancelPleaHelper(final CaseSjpHelper caseSjpHelper) {
        this(caseSjpHelper, EVENT_SELECTOR_PLEA_CANCELLED, PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED);
    }

    public CancelPleaHelper(final CaseSjpHelper caseSjpHelper, final String privateEvent, final String publicEvent) {
        this.caseSjpHelper = caseSjpHelper;
        this.offenceId = caseSjpHelper.getSingleOffenceId();
        privateEventsConsumer = QueueUtil.privateEvents.createConsumer(privateEvent);
        publicEventsConsumer = QueueUtil.publicEvents.createConsumer(publicEvent);
        writeUrl = String.format("/cases/%s/offences/%s/pleas", caseSjpHelper.getCaseId(), offenceId);
    }

    public void cancelPlea(final Response.StatusType expectedStatus) {

        final MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(HeaderConstants.USER_ID, USER_ID);

        final String contentType = "application/vnd.sjp.cancel-plea+json";
        final String payload = "{}";
        final Response response = restClient.postCommand(getWriteUrl(writeUrl), contentType, payload, map);
        assertThat(response.getStatus(), equalTo(expectedStatus.getStatusCode()));
    }

    public void verifyInPublicTopic() {
        assertEventData(publicEventsConsumer);
    }

    private void assertEventData(final MessageConsumer publicEventsConsumer) {
        final JsonPath message = retrieveMessage(publicEventsConsumer);
        assertThat(message.get("caseId"), equalTo(caseSjpHelper.caseId));
        assertThat(message.get("offenceId"), equalTo(offenceId));
    }

    public void verifyPleaCancelled() {
        RestPoller.poll(getCaseById(caseSjpHelper.getCaseId())).until(
                status().is(OK),
                payload().isJson(allOf(
                        withoutJsonPath("defendant.offences[0].plea"),
                        withoutJsonPath("defendant.offences[0].pleaMethod")))
        );
    }

    public void verifyInterpreterCancelled() {
        RestPoller.poll(getCaseById(caseSjpHelper.getCaseId())).until(
                status().is(OK),
                payload().isJson(withoutJsonPath("defendants[0].interpreter.language"))
        );
    }

}
