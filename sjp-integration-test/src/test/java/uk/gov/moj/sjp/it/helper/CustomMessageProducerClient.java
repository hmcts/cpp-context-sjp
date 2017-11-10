package uk.gov.moj.sjp.it.helper;


import static java.util.UUID.randomUUID;
import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static uk.gov.justice.services.messaging.DefaultJsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataOf;
import static uk.gov.justice.services.test.utils.core.messaging.QueueUriProvider.queueUri;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.json.JsonObject;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomMessageProducerClient implements AutoCloseable {

    private static final String USER_ID = randomUUID().toString();

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomMessageProducerClient.class);

    private static final String QUEUE_URI = queueUri();

    private Session session;
    private MessageProducer messageProducer;
    private Connection connection;

    public void startProducer(final String topicName) {

        try {
            final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(QUEUE_URI);
            connection = factory.createConnection();
            connection.start();

            session = connection.createSession(false, AUTO_ACKNOWLEDGE);
            final Destination destination = session.createTopic(topicName);
            messageProducer = session.createProducer(destination);
        } catch (final JMSException e) {
            close();
            throw new RuntimeException("Failed to create message producer to topic: '" + topicName + "', queue uri: '" + QUEUE_URI + "'", e);
        }
    }

    public void sendMessage(final String commandName, final JsonObject payload) {

        if (messageProducer == null) {
            close();
            throw new RuntimeException("Message producer not started. Please call startProducer(...) first.");
        }

        Metadata metadata = metadataOf(randomUUID(), commandName)
                .withUserId(USER_ID)
                .withClientCorrelationId(randomUUID().toString())
                .build();

        sendMessage(commandName, payload, metadata);
    }

    public void sendMessage(final String commandName, final JsonObject payload, final long lifecycleId) {

        if (messageProducer == null) {
            close();
            throw new RuntimeException("Message producer not started. Please call startProducer(...) first.");
        }

        Metadata metadata = metadataOf(randomUUID(), commandName)
                .withUserId(USER_ID)
                .withClientCorrelationId(Long.toString(lifecycleId))
                .build();

        sendMessage(commandName, payload, metadata);
    }

    private void sendMessage(String commandName, JsonObject payload, Metadata metadata) {
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata, payload);
        final String json = jsonEnvelope.toDebugStringPrettyPrint();

        try {
            final TextMessage message = session.createTextMessage();

            message.setText(json);
            message.setStringProperty("CPPNAME", commandName);

            messageProducer.send(message);
        } catch (JMSException e) {
            close();
            throw new RuntimeException("Failed to send message. commandName: '" + commandName + "', json: " + json, e);
        }
    }

    public void sendMessage(final JsonEnvelope jsonEnvelope, String commandName) {

        if (messageProducer == null) {
            close();
            throw new RuntimeException("Message producer not started. Please call startProducer(...) first.");
        }
        final String json = jsonEnvelope.toDebugStringPrettyPrint();
        try {
            final TextMessage message = session.createTextMessage();
            message.setText(json);
            message.setStringProperty("CPPNAME", commandName);
            LOGGER.info("Payload for command '{}'\n {}", commandName, json);
            messageProducer.send(message);
        } catch (JMSException e) {
            close();
            throw new RuntimeException("Failed to send message.  json: " + json, e);
        }
    }

    @Override
    public void close() {
        close(messageProducer);
        close(session);
        close(connection);

        session = null;
        messageProducer = null;
        connection = null;
    }

    private void close(final AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }
}