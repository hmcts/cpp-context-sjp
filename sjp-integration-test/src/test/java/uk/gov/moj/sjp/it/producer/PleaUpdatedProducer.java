package uk.gov.moj.sjp.it.producer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.OFFENCE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.PLEA;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.PLEA_METHOD;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.UPDATED_DATE;
import static uk.gov.moj.sjp.it.Constants.EVENT_SELECTOR_PLEA_UPDATED;
import static uk.gov.moj.sjp.it.Constants.SJP_EVENT;
import static uk.gov.moj.sjp.it.util.TopicUtil.retrieveMessage;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.sjp.it.util.TopicUtil;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.json.Json;
import javax.json.JsonObject;

import io.restassured.path.json.JsonPath;

public class PleaUpdatedProducer {

    private static final String CASE_ID_PROPERTY = "caseId";

    private final UUID caseId;
    private final MessageConsumer privateEventsConsumer = TopicUtil.privateEvents.createConsumer(EVENT_SELECTOR_PLEA_UPDATED);

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
                .add(CASE_ID, caseId.toString())
                .add(OFFENCE_ID, offenceId.toString())
                .add(PLEA, plea.name())
                .add(PLEA_METHOD, pleaMethod.toString())
                .add(UPDATED_DATE, ZonedDateTimes.toString(updatedDate))
                .build();

        final JsonEnvelope pleaUpdatedEvent = envelopeFrom(
                metadataWithRandomUUID(EVENT_SELECTOR_PLEA_UPDATED)
                        .withStreamId(caseId)
                        .withVersion(eventSequenceId),
                pleaUpdatedPayload);

        try (final MessageProducerClient producerClient = new MessageProducerClient()) {
            producerClient.startProducer(SJP_EVENT);
            producerClient.sendMessage(EVENT_SELECTOR_PLEA_UPDATED, pleaUpdatedEvent);
        }
    }

}
