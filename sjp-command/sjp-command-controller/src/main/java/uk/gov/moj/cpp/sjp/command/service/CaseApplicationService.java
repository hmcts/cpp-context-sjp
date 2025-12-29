package uk.gov.moj.cpp.sjp.command.service;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

public class CaseApplicationService {

    @Inject
    @ServiceComponent(COMMAND_CONTROLLER)
    private Requester requester;

    public JsonObject getApplicationDetails(final JsonEnvelope envelope) {
        final JsonObject payload = createObjectBuilder().add("applicationId", envelope.payloadAsJsonObject().getJsonObject("courtApplication").getString("id", "")).build();

        final JsonEnvelope queryCaseEnvelope = JsonEnvelope.envelopeFrom(
                metadataFrom(envelope.metadata())
                        .withName("sjp.query.application").build(),
                payload);

        return requester.request(queryCaseEnvelope).payloadAsJsonObject();
    }

}
