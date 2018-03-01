package uk.gov.moj.sjp.it.helper;

import static uk.gov.moj.sjp.it.util.QueueUtil.retrieveMessageAsJsonObject;

import uk.gov.justice.services.messaging.DefaultJsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.sjp.it.util.HttpClientUtil;
import uk.gov.moj.sjp.it.util.QueueUtil;

import java.util.Optional;
import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

public class SessionHelper implements AutoCloseable {

    private MessageConsumer privateMessageConsumer;
    private MessageConsumer publicMessageConsumer;

    public SessionHelper() {
        privateMessageConsumer = QueueUtil.privateEvents.createConsumerForMultipleSelectors("sjp.events.case-assigned");
        publicMessageConsumer = QueueUtil.publicEvents.createConsumerForMultipleSelectors("public.sjp.session-started");
    }

    public void startSession(final UUID sessionId, final UUID userId, final String courtCode, final Optional<String> magistrate) {
        final JsonObjectBuilder payloadBuilder = Json.createObjectBuilder()
                .add("courtCode", courtCode);
        magistrate.ifPresent(m -> payloadBuilder.add("magistrate", m));

        final String resource = String.format("/session/%s/", sessionId);
        final String contentType = "application/vnd.sjp.start-session+json";

        HttpClientUtil.makePostCall(userId, resource, contentType, payloadBuilder.build().toString(), Response.Status.ACCEPTED);
    }

    public JsonEnvelope getEventFromPublicTopic() {
        final String message = retrieveMessageAsJsonObject(publicMessageConsumer).get().toString();
        return new DefaultJsonObjectEnvelopeConverter().asEnvelope(message);
    }

    public JsonEnvelope getEventFromPrivateTopic() {
        final String message = retrieveMessageAsJsonObject(privateMessageConsumer).get().toString();
        return new DefaultJsonObjectEnvelopeConverter().asEnvelope(message);
    }

    @Override
    public void close() throws Exception {
        privateMessageConsumer.close();
        publicMessageConsumer.close();
    }
}
