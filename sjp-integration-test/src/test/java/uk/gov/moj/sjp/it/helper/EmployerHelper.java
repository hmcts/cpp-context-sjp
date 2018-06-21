package uk.gov.moj.sjp.it.helper;

import static com.jayway.awaitility.Awaitility.await;
import static java.lang.String.format;
import static uk.gov.moj.sjp.it.util.QueueUtil.retrieveMessageAsJsonObject;

import uk.gov.justice.services.messaging.DefaultJsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.sjp.it.util.HttpClientUtil;
import uk.gov.moj.sjp.it.util.QueueUtil;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.hamcrest.Matcher;

public class EmployerHelper implements AutoCloseable {

    private MessageConsumer messageConsumer;

    public EmployerHelper() {
        messageConsumer = QueueUtil.publicEvents.createConsumerForMultipleSelectors(
                "public.sjp.employer-updated", "public.sjp.employer-deleted", "public.sjp.case-update-rejected");
    }

    public void updateEmployer(final UUID caseId, final String defendantId, final JsonObject payload) {
        final String resource = String.format("/cases/%s/defendant/%s/employer", caseId, defendantId);
        final String contentType = "application/vnd.sjp.update-employer+json";
        HttpClientUtil.makePostCall(resource, contentType, payload.toString());
    }

    public Response getEmployer(final String defendantId) {
        final String resource = format("/defendant/%s/employer", defendantId);
        final String contentType = "application/vnd.sjp.query.employer+json";
        return HttpClientUtil.makeGetCall(resource, contentType);
    }

    public String getEmployer(final String defendantId, final Matcher<Object> jsonMatcher) {
        return await().atMost(20, TimeUnit.SECONDS).until(() -> getEmployer(defendantId).readEntity(String.class), jsonMatcher);
    }

    public void deleteEmployer(final String caseId, final String defendantId) {
        final String resource = String.format("/cases/%s/defendant/%s/employer", caseId, defendantId);
        final String contentType = "application/vnd.sjp.delete-employer+json";
        HttpClientUtil.makePostCall(resource, contentType, null);
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
