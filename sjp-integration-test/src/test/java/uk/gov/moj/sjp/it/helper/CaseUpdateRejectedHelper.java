package uk.gov.moj.sjp.it.helper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.util.QueueUtil.retrieveMessage;

import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.moj.sjp.it.util.QueueUtil;

import javax.jms.MessageConsumer;

import com.jayway.restassured.path.json.JsonPath;

public class CaseUpdateRejectedHelper implements AutoCloseable {

    private static final String CASE_ID = "caseId";

    private CaseSjpHelper caseSjpHelper;
    private MessageConsumerClient publicConsumer = new MessageConsumerClient();
    private MessageConsumer privateEventsConsumer;
    private MessageConsumer publicEventsConsumer;

    public CaseUpdateRejectedHelper(CaseSjpHelper caseSjpHelper, String privateEvent, String publicEvent) {
        this.caseSjpHelper = caseSjpHelper;
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
        assertThat(messageInQueue.get(CASE_ID), equalTo(caseSjpHelper.getCaseId()));
        assertThat(messageInQueue.get("reason"), equalTo(reasonExpected));
    }

    @Override
    public void close() {
        publicConsumer.close();
    }
}
