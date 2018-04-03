package uk.gov.moj.sjp.it.helper;

import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;

import uk.gov.justice.services.messaging.DefaultJsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.cpp.sjp.domain.SessionType;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

public class AssignmentHelper implements AutoCloseable {

    private MessageConsumerClient privateMessageConsumerClient;
    private MessageConsumerClient publicMessageConsumerClient;

    public AssignmentHelper() {
        privateMessageConsumerClient = new MessageConsumerClient();
        publicMessageConsumerClient = new MessageConsumerClient();
        privateMessageConsumerClient.startConsumer("sjp.events.case-assigned", "sjp.event");
        publicMessageConsumerClient.startConsumer("public.sjp.case-not-assigned", "public.event");
    }

    //TODO ATCM-2955 use command to request case assignment
    public static void requestCaseAssignment(final UUID sessionId, final UUID userId, final String localJusticeAreaNationalCourtCode, final SessionType sessionType) {

        final JsonObject caseAssignmentRequestPayload = Json.createObjectBuilder()
                .add("session", Json.createObjectBuilder()
                        .add("id", sessionId.toString())
                        .add("type", sessionType.name())
                        .add("userId", userId.toString())
                        .add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode)
                ).build();

        final JsonEnvelope caseAssignmentRequestedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-assignment-requested"), caseAssignmentRequestPayload);

        try (final MessageProducerClient messageProducer = new MessageProducerClient()) {
            messageProducer.startProducer("sjp.event");
            messageProducer.sendMessage("sjp.events.case-assignment-requested", caseAssignmentRequestedEvent);
        }
    }

    public JsonEnvelope getCaseNotAssignedEvent() {
        final String message = publicMessageConsumerClient.retrieveMessage().get();
        return new DefaultJsonObjectEnvelopeConverter().asEnvelope(message);
    }

    public JsonEnvelope getCaseAssignedEvent() {
        final String message = privateMessageConsumerClient.retrieveMessage().get();
        return new DefaultJsonObjectEnvelopeConverter().asEnvelope(message);
    }

    @Override
    public void close() throws Exception {
        privateMessageConsumerClient.close();
        publicMessageConsumerClient.close();
    }
}
