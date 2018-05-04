package uk.gov.moj.sjp.it.producer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;
import static uk.gov.moj.sjp.it.EventSelector.EVENT_SELECTOR_PLEA_UPDATED;
import static uk.gov.moj.sjp.it.util.QueueUtil.retrieveMessage;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.sjp.it.util.QueueUtil;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.json.Json;
import javax.json.JsonObject;

import com.jayway.restassured.path.json.JsonPath;

public class PleaUpdatedProducer {

    private static final String CASE_ID_PROPERTY = "caseId";

    private final UUID caseId;
    private final MessageConsumer privateEventsConsumer = QueueUtil.privateEvents.createConsumer(EVENT_SELECTOR_PLEA_UPDATED);

    public PleaUpdatedProducer(final UUID caseId) {
        this.caseId = caseId;
    }

    public void verifyInActiveMQ() {
        JsonPath jsonResponse = retrieveMessage(privateEventsConsumer);
        assertThat(jsonResponse.get(CASE_ID_PROPERTY), equalTo(caseId.toString()));
    }

    public void producePleaUpdatedEvent(final UUID offenceId, final PleaType plea, final PleaMethod pleaMethod,
                                        final ZonedDateTime updatedDate, final int eventSequenceId) {
        final JsonObject pleaUpdatedPayload = Json.createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("offenceId", offenceId.toString())
                .add("plea", plea.name())
                .add("pleaMethod", pleaMethod.toString())
                .add("updatedDate", ZonedDateTimes.toString(updatedDate))
                .build();

        final JsonEnvelope pleaUpdatedEvent = envelopeFrom(
                metadataWithRandomUUID(EVENT_SELECTOR_PLEA_UPDATED)
                        .withStreamId(caseId)
                        .withVersion(eventSequenceId),
                pleaUpdatedPayload);

        try (final MessageProducerClient producerClient = new MessageProducerClient()) {
            producerClient.startProducer("sjp.event");
            producerClient.sendMessage(EVENT_SELECTOR_PLEA_UPDATED, pleaUpdatedEvent);
        }
    }

}
