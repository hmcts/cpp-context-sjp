package uk.gov.moj.sjp.it.producer;

import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;

import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

public final class ReferToCourtHearingProducer {

    private ReferToCourtHearingProducer() {
    }

    public static void rejectCaseReferral(UUID caseId, String rejectionReason) {
        final JsonObject payload = Json.createObjectBuilder()
                .add("courtReferral", Json.createObjectBuilder()
                        .add("sjpReferral", Json.createObjectBuilder()
                                .add("noticeDate", now(UTC).minusDays(10).toString())
                                .add("referralDate", now(UTC).minusDays(1).toString())
                                .build())
                        .add("prosecutionCases", Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add("id", caseId.toString()))
                                .build())
                        .build())
                .add("rejectedReason", rejectionReason)
                .build();

        try (final MessageProducerClient producerClient = new MessageProducerClient()) {
            producerClient.startProducer("public.event");
            producerClient.sendMessage("public.progression.refer-prosecution-cases-to-court-rejected", payload);
        }
    }

}
