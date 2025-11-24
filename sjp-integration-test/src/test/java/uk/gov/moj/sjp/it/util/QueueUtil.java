package uk.gov.moj.sjp.it.util;

import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static uk.gov.justice.services.test.utils.core.messaging.QueueUriProvider.queueUri;

import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

public class QueueUtil {

    public static void sendToQueue(final String queueName, final JsonEnvelope jsonEnvelope) {

        try (final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(queueUri());
             final Connection connection = factory.createConnection()) {

            connection.start();

            try (Session session = connection.createSession(false, AUTO_ACKNOWLEDGE);
                 MessageProducer messageProducer = session.createProducer(session.createQueue(queueName))) {

                final TextMessage message = session.createTextMessage();
                message.setText(jsonEnvelope.asJsonObject().toString());
                message.setStringProperty("CPPNAME", jsonEnvelope.metadata().name());
                messageProducer.send(message);
            }
        } catch (final JMSException e) {
            throw new RuntimeException(e);
        }
    }

}
