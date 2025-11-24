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
public class DefendantDetailsController extends RegionAwareController {

    static final String COMMAND_NAME = "sjp.command.update-defendant-details";

    @Inject
    private Sender sender;

    @Handles(COMMAND_NAME)
    public void updateDefendantDetails(final JsonEnvelope envelope) {
        final JsonObject newPayload = enrichJsonWithRegion(envelope.payloadAsJsonObject());
        sender.send(envelop(newPayload)
                .withName(COMMAND_NAME)
                .withMetadataFrom(envelope));
    }

}
