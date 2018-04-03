package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.event.processor.service.SchedulingService;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class SessionProcessor {

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private SchedulingService schedulingService;

    @Handles("sjp.events.magistrate-session-started")
    public void magistrateSessionStarted(final JsonEnvelope magistrateSessionStartedEvent) {

        final JsonObject magistrateSessionStarted = magistrateSessionStartedEvent.payloadAsJsonObject();

        final UUID sessionId = UUID.fromString(magistrateSessionStarted.getString("sessionId"));
        final String magistrate = magistrateSessionStarted.getString("magistrate");
        final String courtHouseName = magistrateSessionStarted.getString("courtHouseName");
        final String localJusticeAreaNationalCourtCode = magistrateSessionStarted.getString("localJusticeAreaNationalCourtCode");

        schedulingService.startMagistrateSession(magistrate, sessionId, courtHouseName, localJusticeAreaNationalCourtCode, magistrateSessionStartedEvent);
        emitPublicSessionStartedEvent(sessionId, courtHouseName, localJusticeAreaNationalCourtCode, SessionType.MAGISTRATE, magistrateSessionStartedEvent);
    }

    @Handles("sjp.events.delegated-powers-session-started")
    public void delegatedPowersSessionStarted(final JsonEnvelope delegatedPowersSessionStartedEvent) {
        final JsonObject magistrateSessionStarted = delegatedPowersSessionStartedEvent.payloadAsJsonObject();

        final UUID sessionId = UUID.fromString(magistrateSessionStarted.getString("sessionId"));
        final String courtHouseName = magistrateSessionStarted.getString("courtHouseName");
        final String localJusticeAreaNationalCourtCode = magistrateSessionStarted.getString("localJusticeAreaNationalCourtCode");

        schedulingService.startDelegatedPowersSession(sessionId, courtHouseName, localJusticeAreaNationalCourtCode, delegatedPowersSessionStartedEvent);
        emitPublicSessionStartedEvent(sessionId, courtHouseName, localJusticeAreaNationalCourtCode, SessionType.DELEGATED_POWERS, delegatedPowersSessionStartedEvent);
    }

    @Handles("sjp.events.delegated-powers-session-ended")
    public void delegatedPowersSessionEnded(final JsonEnvelope delegatedPowersSessionEnded) {
        final UUID sessionId = UUID.fromString(delegatedPowersSessionEnded.payloadAsJsonObject().getString("sessionId"));
        schedulingService.endSession(sessionId, delegatedPowersSessionEnded);
    }

    @Handles("sjp.events.magistrate-session-ended")
    public void magistrateSessionEnded(final JsonEnvelope magistrateSessionEnded) {
        final UUID sessionId = UUID.fromString(magistrateSessionEnded.payloadAsJsonObject().getString("sessionId"));
        schedulingService.endSession(sessionId, magistrateSessionEnded);
    }

    private void emitPublicSessionStartedEvent(final UUID sessionId, final String courtHouseName, final String localJusticeAreaNationalCourtCode, final SessionType sessionType, final JsonEnvelope event) {
        final JsonObject payload = Json.createObjectBuilder()
                .add("sessionId", sessionId.toString())
                .add("courtHouseName", courtHouseName)
                .add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode)
                .add("type", sessionType.name())
                .build();

        sender.send(enveloper.withMetadataFrom(event, "public.sjp.session-started").apply(payload));
    }
}
