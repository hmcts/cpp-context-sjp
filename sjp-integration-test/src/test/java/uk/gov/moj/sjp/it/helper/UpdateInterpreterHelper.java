package uk.gov.moj.sjp.it.helper;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.moj.sjp.it.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.sjp.it.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.sjp.it.util.QueueUtil.retrieveMessageAsJsonObject;

import uk.gov.justice.services.messaging.DefaultJsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.sjp.it.util.QueueUtil;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.hamcrest.Matcher;

public class UpdateInterpreterHelper implements AutoCloseable {

    private final MultivaluedMap<String, Object> headers;
    private final RestClient restClient;
    private MessageConsumer messageConsumer;

    public UpdateInterpreterHelper() {
        restClient = new RestClient();
        headers = new MultivaluedHashMap<>();
        headers.add(USER_ID, UUID.randomUUID());
        messageConsumer = QueueUtil.publicEvents.createConsumer("public.structure.case-update-rejected");
    }

    public Response updateInterpreter(final String caseId, final String defendantId, final JsonObject payload) {
        final String resource = getWriteUrl(String.format("/cases/%s/defendants/%s", caseId, defendantId));
        final String contentType = "application/vnd.sjp.update-interpreter+json";
        return restClient.postCommand(resource, contentType, payload.toString(), headers);
    }

    private Response getCase(final String caseId) {
        final String resource = getReadUrl(format("/cases/%s", caseId.toString()));
        final String contentType = "application/vnd.sjp.query.case+json";
        return restClient.query(resource, contentType, headers);
    }

    public String pollForInterpreter(final String caseId, final String defendantId, final String expectedInterpreterLanguage) {
        final Matcher interpreterMatcher = allOf(
                withJsonPath("language", equalTo(expectedInterpreterLanguage)),
                withJsonPath("needed", equalTo(true))
        );
        return pollForInterpreter(caseId, defendantId, interpreterMatcher);
    }

    public String pollForEmptyInterpreter(final String caseId, final String defendantId) {
        final Matcher interpreterMatcher = allOf(
                withoutJsonPath("language"),
                withJsonPath("needed", equalTo(false))
        );
        return pollForInterpreter(caseId, defendantId, interpreterMatcher);
    }

    private String pollForInterpreter(final String caseId, final String defendantId, final Matcher interpreterMatcher) {
        return await().atMost(20, TimeUnit.SECONDS).until(() -> getCase(caseId.toString()).readEntity(String.class),
                isJson(withJsonPath("$.defendant",
                        isJson(allOf(
                                withJsonPath("id", is(defendantId.toString())),
                                withJsonPath("interpreter", isJson(interpreterMatcher)))
                        ))));
    }

    public JsonEnvelope getEventFromPublicTopic() {
        final String message = retrieveMessageAsJsonObject(messageConsumer).get().toString();
        return new DefaultJsonObjectEnvelopeConverter().asEnvelope(message);
    }

    @Override
    public void close() throws Exception {
        messageConsumer.close();
    }

}