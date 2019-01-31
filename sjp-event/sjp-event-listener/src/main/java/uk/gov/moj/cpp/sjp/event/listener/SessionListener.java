package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.session.DelegatedPowersSessionEnded;
import uk.gov.moj.cpp.sjp.event.session.DelegatedPowersSessionStarted;
import uk.gov.moj.cpp.sjp.event.session.MagistrateSessionEnded;
import uk.gov.moj.cpp.sjp.event.session.MagistrateSessionStarted;
import uk.gov.moj.cpp.sjp.persistence.entity.Session;
import uk.gov.moj.cpp.sjp.persistence.repository.SessionRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class SessionListener {

    @Inject
    private SessionRepository sessionRepository;

    @Transactional
    @Handles(DelegatedPowersSessionStarted.EVENT_NAME)
    public void handleDelegatedPowersSessionStarted(final JsonEnvelope delegatedPowersSessionStartedEvent) {
        final JsonObject delegatedPowersSessionStarted = delegatedPowersSessionStartedEvent.payloadAsJsonObject();

        final Session session = new Session(
                UUID.fromString(delegatedPowersSessionStarted.getString("sessionId"))
                , UUID.fromString(delegatedPowersSessionStarted.getString("userId"))
                , delegatedPowersSessionStarted.getString("courtHouseCode")
                , delegatedPowersSessionStarted.getString("courtHouseName")
                , delegatedPowersSessionStarted.getString("localJusticeAreaNationalCourtCode")
                , ZonedDateTime.parse(delegatedPowersSessionStarted.getString("startedAt"))

        );
        sessionRepository.save(session);
    }

    @Transactional
    @Handles(MagistrateSessionStarted.EVENT_NAME)
    public void handleMagistrateSessionStarted(final JsonEnvelope magistrateSessionStartedEvent) {
        final JsonObject magistrateSessionStarted = magistrateSessionStartedEvent.payloadAsJsonObject();

        final Session session = new Session(
                UUID.fromString(magistrateSessionStarted.getString("sessionId"))
                , UUID.fromString(magistrateSessionStarted.getString("userId"))
                , magistrateSessionStarted.getString("courtHouseCode")
                , magistrateSessionStarted.getString("courtHouseName")
                , magistrateSessionStarted.getString("localJusticeAreaNationalCourtCode")
                , magistrateSessionStarted.getString("magistrate")
                , ZonedDateTime.parse(magistrateSessionStarted.getString("startedAt"))
        );
        sessionRepository.save(session);
    }

    @Transactional
    @Handles(DelegatedPowersSessionEnded.EVENT_NAME)
    public void handleDelegatedPowersSessionEnded(final JsonEnvelope delegatedPowersSessionEnded) {
        endSession(delegatedPowersSessionEnded);
    }

    @Transactional
    @Handles(MagistrateSessionEnded.EVENT_NAME)
    public void handleMagistrateSessionEnded(final JsonEnvelope magistrateSessionEnded) {
        endSession(magistrateSessionEnded);
    }

    private void endSession(final JsonEnvelope sessionEndedEvent) {
        final JsonObject sessionEnded = sessionEndedEvent.payloadAsJsonObject();
        final UUID sessionId = UUID.fromString(sessionEnded.getString("sessionId"));
        final ZonedDateTime endedAt = ZonedDateTime.parse(sessionEnded.getString("endedAt"));

        sessionRepository.findBy(sessionId).setEndedAt(endedAt);
    }

}
