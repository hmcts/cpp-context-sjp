package uk.gov.moj.cpp.sjp.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.sjp.command.api.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.command.utils.UUIDHelper;
import uk.gov.moj.cpp.sjp.domain.SessionCourt;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(COMMAND_API)
public class SessionApi {

    private static final String SESSION_ID = "sessionId";

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("sjp.start-session")
    public void startSession(final JsonEnvelope startSessionCommand) {

        final JsonObject commandPayload = startSessionCommand.payloadAsJsonObject();
        final String courtHouseOUCode = commandPayload.getString("courtHouseOUCode");

        final SessionCourt sessionCourt = referenceDataService.getCourtByCourtHouseOUCode(courtHouseOUCode, startSessionCommand)
                .orElseThrow(() -> new BadRequestException(String.format("Court house with ou code %s not found", courtHouseOUCode)));

        final JsonObjectBuilder startSessionBuilder = Json.createObjectBuilder()
                .add(SESSION_ID, commandPayload.getString(SESSION_ID))
                .add("courtHouseCode", courtHouseOUCode)
                .add("courtHouseName", sessionCourt.getCourtHouseName())
                .add("localJusticeAreaNationalCourtCode", sessionCourt.getLocalJusticeAreaNationalCourtCode());

        JsonObjects.getString(commandPayload, "magistrate")
                .ifPresent(magistrate -> startSessionBuilder.add("magistrate", magistrate));

        sender.send(enveloper.withMetadataFrom(startSessionCommand, "sjp.command.start-session").apply(startSessionBuilder.build()));
    }

    @Handles("sjp.end-session")
    public void endSession(final JsonEnvelope envelope) {
        validateSessionId(envelope);
        sender.send(enveloper.withMetadataFrom(envelope, "sjp.command.end-session").apply(envelope.payloadAsJsonObject()));
    }

    @Handles("sjp.assign-next-case")
    public void assignNextCase(final JsonEnvelope assignCaseCommand) {
        sender.send(enveloper.withMetadataFrom(assignCaseCommand, "sjp.command.assign-next-case").apply(assignCaseCommand.payloadAsJsonObject()));
    }

    @Handles("sjp.assign-case")
    public void assignCase(final JsonEnvelope assignCaseCommand) {
        sender.send(enveloper.withMetadataFrom(assignCaseCommand, "sjp.command.controller.assign-case").apply(assignCaseCommand.payloadAsJsonObject()));
    }

    @Handles("sjp.unassign-case")
    public void unassignCase(final JsonEnvelope unassignCaseCommand) {
        sender.send(enveloper.withMetadataFrom(unassignCaseCommand, "sjp.command.unassign-case").apply(unassignCaseCommand.payloadAsJsonObject()));
    }

    private void validateSessionId(final JsonEnvelope envelope) {
        final String sessionId = envelope.payloadAsJsonObject().getString(SESSION_ID, null);
        if (!UUIDHelper.isUuid(sessionId)) {
            throw new BadRequestException("Invalid sessionId provided");
        }
    }
}
