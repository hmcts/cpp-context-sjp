package uk.gov.moj.sjp.it.helper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.util.QueueUtil.retrieveMessage;

import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.moj.sjp.it.util.QueueUtil;

import java.util.UUID;

import javax.jms.MessageConsumer;

import com.jayway.restassured.path.json.JsonPath;

public class CaseUpdateRejectedHelper implements AutoCloseable {

    private static final String CASE_ID = "caseId";

    private UUID caseId;
    private MessageConsumerClient publicConsumer = new MessageConsumerClient();
    private MessageConsumer privateEventsConsumer;
    private MessageConsumer publicEventsConsumer;

    public CaseUpdateRejectedHelper(UUID caseId, String privateEvent, String publicEvent) {
        this.caseId = caseId;
        privateEventsConsumer = QueueUtil.privateEvents.createConsumer(privateEvent);
        publicEventsConsumer = QueueUtil.publicEvents.createConsumer(publicEvent);
    }

    public void verifyCaseUpdateRejectedPrivateInActiveMQ(final String reasonExpected) {
        verifyCaseUpdateRejectedInActiveMQ(privateEventsConsumer, reasonExpected);
    }

    public void verifyCaseUpdateRejectedPublicInActiveMQ(final String reasonExpected) {
        verifyCaseUpdateRejectedInActiveMQ(publicEventsConsumer, reasonExpected);
    }

    private void verifyCaseUpdateRejectedInActiveMQ(final MessageConsumer messageConsumer, String reasonExpected) {
        final JsonPath messageInQueue = retrieveMessage(messageConsumer);
        assertThat(messageInQueue, notNullValue());
        assertThat(messageInQueue.get(CASE_ID), equalTo(caseId.toString()));
        assertThat(messageInQueue.get("reason"), equalTo(reasonExpected));
    }

    @Override
    public void close() {
        publicConsumer.close();
    }
}
