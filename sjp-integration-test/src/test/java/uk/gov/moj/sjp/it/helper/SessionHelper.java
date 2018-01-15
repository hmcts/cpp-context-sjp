package uk.gov.moj.sjp.it.helper;

import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.moj.sjp.it.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.sjp.it.util.QueueUtil.retrieveMessageAsJsonObject;

import uk.gov.justice.services.messaging.DefaultJsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.sjp.it.util.QueueUtil;

import java.util.Optional;
import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

public class SessionHelper implements AutoCloseable {

    private final RestClient restClient;
    private MessageConsumer privateMessageConsumer;
    private MessageConsumer publicMessageConsumer;

    public SessionHelper() {
        restClient = new RestClient();
        privateMessageConsumer = QueueUtil.privateEvents.createConsumerForMultipleSelectors("sjp.events.case-assigned");
        publicMessageConsumer = QueueUtil.publicEvents.createConsumerForMultipleSelectors("public.sjp.session-started");
    }

    public Response startSession(final UUID sessionId, final UUID userId, final String courtCode, final Optional<String> magistrate) {
        final JsonObjectBuilder payloadBuilder = Json.createObjectBuilder()
                .add("courtCode", courtCode);
        magistrate.ifPresent(m -> payloadBuilder.add("magistrate", m));

        final String resource = getWriteUrl(String.format("/session/%s/", sessionId));
        final String contentType = "application/vnd.sjp.start-session+json";

        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(USER_ID, userId);

        return restClient.postCommand(resource, contentType, payloadBuilder.build().toString(), headers);
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
