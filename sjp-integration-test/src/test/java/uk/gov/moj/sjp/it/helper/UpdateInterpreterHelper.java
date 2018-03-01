package uk.gov.moj.sjp.it.helper;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.moj.sjp.it.util.QueueUtil.retrieveMessageAsJsonObject;

import uk.gov.justice.services.messaging.DefaultJsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.sjp.it.util.HttpClientUtil;
import uk.gov.moj.sjp.it.util.QueueUtil;

import java.util.concurrent.TimeUnit;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.hamcrest.Matcher;

public class UpdateInterpreterHelper implements AutoCloseable {

    private MessageConsumer messageConsumer;

    public UpdateInterpreterHelper() {
        messageConsumer = QueueUtil.publicEvents.createConsumer("public.sjp.case-update-rejected");
    }

    public void updateInterpreter(final String caseId, final String defendantId, final JsonObject payload) {
        final String resource = String.format("/cases/%s/defendants/%s", caseId, defendantId);
        final String contentType = "application/vnd.sjp.update-interpreter+json";
        HttpClientUtil.makePostCall(resource, contentType, payload.toString());
    }

    private Response getCase(final String caseId) {
        final String resource = format("/cases/%s", caseId.toString());
        final String contentType = "application/vnd.sjp.query.case+json";
        return HttpClientUtil.makeGetCall(resource, contentType);
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