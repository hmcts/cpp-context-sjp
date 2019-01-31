package uk.gov.moj.sjp.it.producer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;
import static uk.gov.moj.sjp.it.Constants.EVENT_SELECTOR_TRIAL_REQUESTED;
import static uk.gov.moj.sjp.it.util.TopicUtil.retrieveMessage;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.sjp.it.util.TopicUtil;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.json.Json;
import javax.json.JsonObject;

import com.jayway.restassured.path.json.JsonPath;

public class TrialRequestedProducer {

    private static final String CASE_ID_PROPERTY = "caseId";

    private final UUID caseId;
    private final MessageConsumer privateEventsConsumer;

    public TrialRequestedProducer(final UUID caseId) {
        this.caseId = caseId;
        privateEventsConsumer = TopicUtil.privateEvents.createConsumer(EVENT_SELECTOR_TRIAL_REQUESTED);
    }

    public void verifyInActiveMQ() {
        JsonPath jsonResponse = retrieveMessage(privateEventsConsumer);
        assertThat(jsonResponse.get(CASE_ID_PROPERTY), equalTo(caseId.toString()));
    }

    public void produceTrialRequestedEvent(ZonedDateTime updatedDate, int eventSequenceId) {
        final JsonObject trialRequestedPayload = Json.createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("updatedDate", ZonedDateTimes.toString(updatedDate))
                .build();

        final JsonEnvelope trialRequestedEvent = envelopeFrom(
                metadataWithRandomUUID(EVENT_SELECTOR_TRIAL_REQUESTED)
                        .withStreamId(caseId)
                        .withVersion(eventSequenceId),
                trialRequestedPayload);

        try (final MessageProducerClient producerClient = new MessageProducerClient()) {
            producerClient.startProducer("sjp.event");
            producerClient.sendMessage(EVENT_SELECTOR_TRIAL_REQUESTED, trialRequestedEvent);
        }
    }

}
