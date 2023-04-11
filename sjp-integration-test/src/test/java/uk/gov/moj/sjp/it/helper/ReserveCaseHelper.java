package uk.gov.moj.sjp.it.helper;

import static uk.gov.moj.sjp.it.Constants.EVENT_CASE_ALREADY_RESERVED;
import static uk.gov.moj.sjp.it.Constants.EVENT_CASE_RESERVED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_CASE_ALREADY_RESERVED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_CASE_RESERVED;
import static uk.gov.moj.sjp.it.Constants.EVENT_CASE_ALREADY_UNRESERVED;
import static uk.gov.moj.sjp.it.Constants.EVENT_CASE_UNRESERVED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_CASE_ALREADY_UNRESERVED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_CASE_UNRESERVED;
import static uk.gov.moj.sjp.it.util.TopicUtil.retrieveMessageAsJsonObject;


import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import uk.gov.justice.services.messaging.DefaultJsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.sjp.it.util.TopicUtil;

public class ReserveCaseHelper implements AutoCloseable {

    private final MessageConsumer privateMessageConsumer;
    private final MessageConsumer publicMessageConsumer;

    public ReserveCaseHelper() {
        privateMessageConsumer = TopicUtil.privateEvents.createConsumerForMultipleSelectors(EVENT_CASE_RESERVED, EVENT_CASE_ALREADY_RESERVED, EVENT_CASE_UNRESERVED, EVENT_CASE_ALREADY_UNRESERVED);
        publicMessageConsumer = TopicUtil.publicEvents.createConsumerForMultipleSelectors(PUBLIC_CASE_RESERVED, PUBLIC_CASE_ALREADY_RESERVED, PUBLIC_CASE_UNRESERVED, PUBLIC_CASE_ALREADY_UNRESERVED);
    }

    @Override
    public void close() throws JMSException {
        privateMessageConsumer.close();
        publicMessageConsumer.close();
    }

    public JsonEnvelope getEventFromTopic() {
        return getEventFromTopic(privateMessageConsumer);
    }

    public JsonEnvelope getPublicEventFromTopic() {
        return getEventFromTopic(publicMessageConsumer);
    }

    private JsonEnvelope getEventFromTopic(final MessageConsumer messageConsumer) {
        return retrieveMessageAsJsonObject(messageConsumer)
                .map(event -> new DefaultJsonObjectEnvelopeConverter().asEnvelope(event)).orElse(null);
    }
}
