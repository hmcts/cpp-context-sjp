package uk.gov.moj.cpp.sjp.command.service;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;

public class SessionService {

    @Inject
    @ServiceComponent(COMMAND_CONTROLLER)
    private Requester requester;

    public JsonObject getLatestAocpSessionDetails(final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().build();
        final JsonEnvelope request = envelopeFrom(metadataFrom(envelope.metadata()).withName("sjp.query.latest-aocp-session"), payload);
        final Envelope<JsonObject> response = requester.requestAsAdmin(request, JsonObject.class);
        return response.payload();
    }
}
