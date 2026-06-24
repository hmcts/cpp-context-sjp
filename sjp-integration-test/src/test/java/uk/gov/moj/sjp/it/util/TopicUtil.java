package uk.gov.moj.sjp.it.util;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.wrap;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static uk.gov.moj.sjp.it.Constants.PRIVATE_ACTIVE_MQ_TOPIC;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_ACTIVE_MQ_TOPIC;
import static uk.gov.moj.sjp.it.util.OptionalPresent.ifPresent;

import uk.gov.justice.services.messaging.DefaultJsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.json.JsonObject;

import io.restassured.path.json.JsonPath;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQTopic;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicUtil implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopicUtil.class);

    private static final String EVENT_SELECTOR_TEMPLATE = "CPPNAME IN ('%s')";

    private static final String QUEUE_URI = System.getProperty("queueUri", "tcp://localhost:61616");

    private static final long RETRIEVE_TIMEOUT = 20000;
    private static final long MESSAGE_RETRIEVE_TRIAL_TIMEOUT = 10000;

    private Connection connection;

    private Session session;

    private Topic topic;

    public static final TopicUtil privateEvents = new TopicUtil(PRIVATE_ACTIVE_MQ_TOPIC);

    public static final TopicUtil publicEvents = new TopicUtil(PUBLIC_ACTIVE_MQ_TOPIC);

    private TopicUtil(final String topicName) {
        try {
            LOGGER.info("Artemis URI: {}", QUEUE_URI);
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(QUEUE_URI);
            connection = factory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            topic = new ActiveMQTopic(topicName);
        } catch (JMSException e) {
            LOGGER.error("Fatal error initialising Artemis", e);
            throw new RuntimeException(e);
        }
    }

    public MessageConsumer createConsumer(final String eventSelector) {
        try {
            return session.createConsumer(topic, String.format(EVENT_SELECTOR_TEMPLATE, eventSelector));
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public MessageConsumer createConsumerForMultipleSelectors(final String... eventSelectors) {
        final Function<String, String> wrapInQuotes = (str) -> wrap(str, "'");

        final String eventSelectorsExpression = Arrays.stream(eventSelectors)
                .map(wrapInQuotes)
                .collect(joining(",", "CPPNAME IN (", ")"));

        try {
            return session.createConsumer(topic, eventSelectorsExpression);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonPath retrieveMessage(final MessageConsumer consumer) {
        return retrieveMessage(consumer, RETRIEVE_TIMEOUT).orElse(null);
    }

    public static Optional<JsonPath> retrieveMessage(final MessageConsumer consumer, long customTimeOutInMillis) {
        return ifPresent(retrieveMessageAsString(consumer, customTimeOutInMillis),
                (x) -> Optional.of(new JsonPath(x))
        ).orElse(Optional::<JsonPath>empty);
    }

    public static Optional<JsonObject> retrieveMessageAsJsonObject(final MessageConsumer consumer) {
        return ifPresent(retrieveMessageAsString(consumer, RETRIEVE_TIMEOUT),
                (x) -> Optional.of(createReader(new StringReader(x)).readObject())
        ).orElse(Optional::<JsonObject>empty);
    }

    public static Optional<JsonEnvelope> retrieveMessageAsJsonEnvelope(final MessageConsumer messageConsumer) {
        return retrieveMessageAsJsonObject(messageConsumer)
                .map(event -> new DefaultJsonObjectEnvelopeConverter().asEnvelope(event));
    }

    public static Optional<String> retrieveMessageAsString(final MessageConsumer consumer, long customTimeOutInMillis) {
        try {
            TextMessage message = (TextMessage) consumer.receive(customTimeOutInMillis);
            if (message == null) {
                LOGGER.error("No message retrieved using consumer with selector {}", consumer.getMessageSelector());
                return Optional.empty();
            }
            return Optional.of(message.getText());
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonPath retrieveMessage(final MessageConsumer consumer, final Matcher matchers) {
        final long startTime = System.currentTimeMillis();
        JsonPath message;
        do {
            message = retrieveMessage(consumer, RETRIEVE_TIMEOUT).orElse(null);
            if (ofNullable(message).isPresent()) {
                if (matchers.matches(message.prettify())) {
                    return message;
                }
            }
        } while (MESSAGE_RETRIEVE_TRIAL_TIMEOUT > (System.currentTimeMillis() - startTime));
        return null;
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }
}
