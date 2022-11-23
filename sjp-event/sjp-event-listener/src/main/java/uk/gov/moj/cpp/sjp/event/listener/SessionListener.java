package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.event.session.AocpSessionEnded;
import uk.gov.moj.cpp.sjp.event.session.AocpSessionStarted;
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

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    private static final String USER_ID = "userId";
    private static final String SESSION_ID = "sessionId";
    private static final String COURT_HOUSE_CODE = "courtHouseCode";
    private static final String COURT_HOUSE_NAME = "courtHouseName";
    private static final String LEGAL_JUSTICE_AREA_NATIONAL_COURT_CODE = "localJusticeAreaNationalCourtCode";
    private static final String STARTED_AT = "startedAt";

    @Transactional
    @Handles(DelegatedPowersSessionStarted.EVENT_NAME)
    public void handleDelegatedPowersSessionStarted(final JsonEnvelope delegatedPowersSessionStartedEvent) {
        final JsonObject delegatedPowersSessionStarted = delegatedPowersSessionStartedEvent.payloadAsJsonObject();

        final Session session = new Session(
                UUID.fromString(delegatedPowersSessionStarted.getString(SESSION_ID))
                , UUID.fromString(delegatedPowersSessionStarted.getString(USER_ID))
                , delegatedPowersSessionStarted.getString(COURT_HOUSE_CODE)
                , delegatedPowersSessionStarted.getString(COURT_HOUSE_NAME)
                , delegatedPowersSessionStarted.getString(LEGAL_JUSTICE_AREA_NATIONAL_COURT_CODE)
                , ZonedDateTime.parse(delegatedPowersSessionStarted.getString(STARTED_AT))

        );
        sessionRepository.save(session);
    }

    @Transactional
    @Handles(MagistrateSessionStarted.EVENT_NAME)
    public void handleMagistrateSessionStarted(final JsonEnvelope magistrateSessionStartedEvent) {
        final JsonObject magistrateSessionStartedJson = magistrateSessionStartedEvent.payloadAsJsonObject();
        final MagistrateSessionStarted magistrateSessionStarted = jsonObjectConverter.convert(magistrateSessionStartedJson, MagistrateSessionStarted.class);

        final Session session = new Session(
                UUID.fromString(magistrateSessionStartedJson.getString(SESSION_ID))
                , UUID.fromString(magistrateSessionStartedJson.getString(USER_ID))
                , magistrateSessionStartedJson.getString(COURT_HOUSE_CODE)
                , magistrateSessionStartedJson.getString(COURT_HOUSE_NAME)
                , magistrateSessionStartedJson.getString(LEGAL_JUSTICE_AREA_NATIONAL_COURT_CODE)
                , magistrateSessionStartedJson.getString("magistrate")
                , ZonedDateTime.parse(magistrateSessionStartedJson.getString(STARTED_AT))
        );

        magistrateSessionStarted.getLegalAdviser().ifPresent(legalAdviser -> session.setLegalAdviserUserId(legalAdviser.getUserId()));

        sessionRepository.save(session);
    }

    @Transactional
    @Handles(AocpSessionStarted.EVENT_NAME)
    public void handleAocpSessionStarted(final JsonEnvelope event) {
        final JsonObject aocpSession = event.payloadAsJsonObject();

        final Session session = new Session(
                UUID.fromString(aocpSession.getString(SESSION_ID))
                , UUID.fromString(aocpSession.getString(USER_ID))
                , aocpSession.getString(COURT_HOUSE_CODE)
                , aocpSession.getString(COURT_HOUSE_NAME)
                , aocpSession.getString(LEGAL_JUSTICE_AREA_NATIONAL_COURT_CODE)
                , ZonedDateTime.parse(aocpSession.getString(STARTED_AT))

        );
        session.setType(SessionType.AOCP);
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

    @Transactional
    @Handles(AocpSessionEnded.EVENT_NAME)
    public void handleAocpSessionEnded(final JsonEnvelope envelope) {
        endSession(envelope);
    }

    private void endSession(final JsonEnvelope sessionEndedEvent) {
        final JsonObject sessionEnded = sessionEndedEvent.payloadAsJsonObject();
        final UUID sessionId = UUID.fromString(sessionEnded.getString(SESSION_ID));
        final ZonedDateTime endedAt = ZonedDateTime.parse(sessionEnded.getString("endedAt"));

        sessionRepository.findBy(sessionId).setEndedAt(endedAt);
    }

}
