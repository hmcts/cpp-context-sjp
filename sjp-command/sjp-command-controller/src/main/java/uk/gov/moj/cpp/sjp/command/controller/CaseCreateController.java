package uk.gov.moj.cpp.sjp.command.controller;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_CONTROLLER)
public class CaseCreateController extends RegionAwareController {

    private static final String DEFENDANT = "defendant";
    static final String COMMAND_NAME = "sjp.command.create-sjp-case";

    @Inject
    private Sender sender;

    @Handles(COMMAND_NAME)
    public void createSjpCase(final JsonEnvelope envelope) {
        final JsonObject newDefendantObject = enrichJsonWithRegion(envelope.payloadAsJsonObject().getJsonObject(DEFENDANT));
        final JsonObject newPayload = updateJsonObject(envelope.payloadAsJsonObject(), DEFENDANT, newDefendantObject);
        sender.send(envelop(newPayload)
                .withName(COMMAND_NAME)
                .withMetadataFrom(envelope));
    }
}
