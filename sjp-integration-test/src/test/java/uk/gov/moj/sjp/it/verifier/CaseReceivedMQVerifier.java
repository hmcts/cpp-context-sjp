package uk.gov.moj.sjp.it.verifier;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.sjp.it.EventSelector.EVENT_SELECTOR_CASE_RECEIVED;
import static uk.gov.moj.sjp.it.util.QueueUtil.retrieveMessage;

import uk.gov.moj.sjp.it.util.QueueUtil;

import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;

import com.jayway.restassured.path.json.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaseReceivedMQVerifier implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseReceivedMQVerifier.class);
    
    private MessageConsumer privateEventsConsumer;

    public CaseReceivedMQVerifier() {
        privateEventsConsumer = QueueUtil.privateEvents.createConsumer(EVENT_SELECTOR_CASE_RECEIVED);
    }

    public void verifyInPrivateActiveMQ(final UUID caseId, final String caseUrn) {
        JsonPath jsonResponse = retrieveMessage(privateEventsConsumer);
        assertThat(jsonResponse.get("caseId"), is(caseId.toString()));
        assertThat(jsonResponse.get("urn"), is(caseUrn));
    }

    @Override
    public void close() {
        try {
            privateEventsConsumer.close();
        } catch (JMSException e) {
            LOGGER.warn("Unable to close consumer.", e);
        }
    }
}
