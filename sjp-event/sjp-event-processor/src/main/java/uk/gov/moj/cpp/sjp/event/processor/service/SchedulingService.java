package uk.gov.moj.cpp.sjp.event.processor.service;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

public class SchedulingService {

    @Inject
    private Enveloper enveloper;

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    private Requester requester;

    public void startMagistrateSession(final String magistrate, final UUID sessionId, final String courtHouseName, final String localJusticeAreaNationalCourtCode, final JsonEnvelope envelope) {
        final JsonObject startMagistrateSessionPayload = createObjectBuilder()
                .add("id", sessionId.toString())
                .add("courtLocation", courtHouseName)
                .add("nationalCourtCode", localJusticeAreaNationalCourtCode)
                .add("magistrate", magistrate)
                .build();

        sender.send(enveloper.withMetadataFrom(envelope, "scheduling.command.start-sjp-session").apply(startMagistrateSessionPayload));
    }

    public void startDelegatedPowersSession(final UUID sessionId, final String courtHouseName, final String localJusticeAreaNationalCourtCode, final JsonEnvelope envelope) {
        final JsonObject startDelegatedPowersSessionPayload = createObjectBuilder()
                .add("id", sessionId.toString())
                .add("courtLocation", courtHouseName)
                .add("nationalCourtCode", localJusticeAreaNationalCourtCode)
                .build();

        sender.send(enveloper.withMetadataFrom(envelope, "scheduling.command.start-sjp-session").apply(startDelegatedPowersSessionPayload));
    }

    public void endSession(final UUID sessionId, final JsonEnvelope envelope) {
        final JsonObject endSessionPayload = createObjectBuilder()
                .add("id", sessionId.toString())
                .build();
        sender.send(enveloper.withMetadataFrom(envelope, "scheduling.command.end-sjp-session").apply(endSessionPayload));
    }

    public Optional<JsonObject> getSession(final UUID sessionId, final JsonEnvelope envelope) {
        final JsonObject requestPayload = Json.createObjectBuilder().add("sjpSessionId", sessionId.toString()).build();
        final JsonEnvelope queryEnvelope = enveloper
                .withMetadataFrom(envelope, "scheduling.query.sjp-session")
                .apply(requestPayload);

        final JsonValue responsePayload = requester.request(queryEnvelope).payload();
        return JsonValue.NULL.equals(responsePayload) ? Optional.empty() : Optional.of((JsonObject) responsePayload);
    }

}
