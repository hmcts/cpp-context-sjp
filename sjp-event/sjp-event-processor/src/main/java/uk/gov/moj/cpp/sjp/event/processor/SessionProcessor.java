package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import uk.gov.moj.cpp.sjp.event.session.DelegatedPowersSessionStarted;
import uk.gov.moj.cpp.sjp.event.session.MagistrateSessionStarted;
import uk.gov.moj.cpp.sjp.event.session.ResetAocpSession;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class SessionProcessor {

    public static final String PUBLIC_SJP_SESSION_STARTED = "public.sjp.session-started";
    public static final String COURT_HOUSE_CODE = "courtHouseCode";
    private static final String SESSION_ID = "sessionId";

    private static final String AOCP_COURT_HOUSE_CODE = "B52CM00";
    private static final String AOCP_COURT_HOUSE_NAME = "Bristol Magistrates' Court";
    private static final String AOCP_COURT_LJA = "1450";
    public static final String COURT_HOUSE_NAME = "courtHouseName";
    public static final String LOCAL_JUSTICE_AREA_NATIONAL_COURT_CODE = "localJusticeAreaNationalCourtCode";

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private SjpService sjpService;

    @Handles(MagistrateSessionStarted.EVENT_NAME)
    public void magistrateSessionStarted(final JsonEnvelope magistrateSessionStartedEvent) {
        final JsonObject magistrateSessionStarted = magistrateSessionStartedEvent.payloadAsJsonObject();
        final UUID sessionId = UUID.fromString(magistrateSessionStarted.getString(SESSION_ID));
        final String courtHouseCode = magistrateSessionStarted.getString(COURT_HOUSE_CODE);
        final String courtHouseName = magistrateSessionStarted.getString(COURT_HOUSE_NAME);
        final String localJusticeAreaNationalCourtCode = magistrateSessionStarted.getString(LOCAL_JUSTICE_AREA_NATIONAL_COURT_CODE);

        emitPublicSessionStartedEvent(sessionId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, SessionType.MAGISTRATE, magistrateSessionStartedEvent);
    }

    @Handles(DelegatedPowersSessionStarted.EVENT_NAME)
    public void delegatedPowersSessionStarted(final JsonEnvelope delegatedPowersSessionStartedEvent) {
        final JsonObject delegatedPowersSessionStarted = delegatedPowersSessionStartedEvent.payloadAsJsonObject();
        final UUID sessionId = UUID.fromString(delegatedPowersSessionStarted.getString(SESSION_ID));
        final String courtHouseCode = delegatedPowersSessionStarted.getString(COURT_HOUSE_CODE);
        final String courtHouseName = delegatedPowersSessionStarted.getString(COURT_HOUSE_NAME);
        final String localJusticeAreaNationalCourtCode = delegatedPowersSessionStarted.getString(LOCAL_JUSTICE_AREA_NATIONAL_COURT_CODE);

        emitPublicSessionStartedEvent(sessionId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, SessionType.DELEGATED_POWERS, delegatedPowersSessionStartedEvent);
    }

    @Handles(ResetAocpSession.EVENT_NAME)
    public void aocpSessionResetRequested(final JsonEnvelope envelope) {
        final JsonObject response= sjpService.getLatestAocpSessionDetails(envelope);
        if(nonNull(response)) {
            endSession(envelope, response.getString(SESSION_ID));
        }
        startNewSession(envelope);
    }

    private void endSession(final JsonEnvelope envelope, String sessionId){
        final JsonObject payload = Json.createObjectBuilder()
                .add(SESSION_ID, sessionId).build();

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("sjp.command.end-session")
                .build();

        sender.send(envelopeFrom(metadata, payload));
    }

    private void startNewSession(final JsonEnvelope envelope){

        final JsonObject payload = Json.createObjectBuilder()
                .add(SESSION_ID, randomUUID().toString())
                .add(COURT_HOUSE_CODE , AOCP_COURT_HOUSE_CODE)
                .add(COURT_HOUSE_NAME, AOCP_COURT_HOUSE_NAME)
                .add(LOCAL_JUSTICE_AREA_NATIONAL_COURT_CODE, AOCP_COURT_LJA)
                .add("isAocpSession", true)
                .add("prosecutors", Json.createArrayBuilder().add("TFL").add("TVL").add("DVLA").build())
                .build();

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("sjp.command.start-session")
                .build();

        sender.send(envelopeFrom(metadata, payload));
    }

    private void emitPublicSessionStartedEvent(final UUID sessionId, final String courtHouseCode, final String courtHouseName, final String localJusticeAreaNationalCourtCode, final SessionType sessionType, final JsonEnvelope event) {
        final JsonObject payload = Json.createObjectBuilder()
                .add(SESSION_ID, sessionId.toString())
                .add(COURT_HOUSE_CODE, courtHouseCode)
                .add(COURT_HOUSE_NAME, courtHouseName)
                .add(LOCAL_JUSTICE_AREA_NATIONAL_COURT_CODE, localJusticeAreaNationalCourtCode)
                .add("type", sessionType.name())
                .build();

        sender.send(enveloper.withMetadataFrom(event, PUBLIC_SJP_SESSION_STARTED).apply(payload));
    }
}
