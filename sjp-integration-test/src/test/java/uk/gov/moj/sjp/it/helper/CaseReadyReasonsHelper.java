package uk.gov.moj.sjp.it.helper;

import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

public class CaseReadyReasonsHelper {

    public static void requestCaseMarkedReady(final UUID caseId, final String reason) {

        final JsonObject caseMarkedReadyForDecisionPayload = Json.createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("reason", reason)
                .add("markedAt", now(UTC).toString()).build();

        final JsonEnvelope caseMarkedReadyForDecisionEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-marked-ready-for-decision").withStreamId(UUID.randomUUID()).withVersion(1L), caseMarkedReadyForDecisionPayload);

        try (final MessageProducerClient messageProducer = new MessageProducerClient()) {
            messageProducer.startProducer("sjp.event");
            messageProducer.sendMessage("sjp.events.case-marked-ready-for-decision", caseMarkedReadyForDecisionEvent);
        }
    }

    public static void requestCaseUnmarkedReady(final UUID caseId) {

        final JsonObject caseUnmarkedReadyForDecisionPayload = Json.createObjectBuilder()
                .add("caseId", caseId.toString()).build();

        final JsonEnvelope caseUnmarkedReadyForDecisionEvent = envelopeFrom(
                metadataWithRandomUUID("sjp.events.case-unmarked-ready-for-decision").withStreamId(UUID.randomUUID()).withVersion(1L),
                caseUnmarkedReadyForDecisionPayload);

        try (final MessageProducerClient messageProducer = new MessageProducerClient()) {
            messageProducer.startProducer("sjp.event");
            messageProducer.sendMessage("sjp.events.case-unmarked-ready-for-decision", caseUnmarkedReadyForDecisionEvent);
        }
    }

}
