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
public class PleadAocpOnlineController extends RegionAwareController {

    static final String COMMAND_NAME = "sjp.command.plead-aocp-online";
    private static final String PERSONAL_DETAILS = "personalDetails";

    @Inject
    private Sender sender;

    @Handles(COMMAND_NAME)
    public void pleadOnline(final JsonEnvelope envelope) {
        final JsonObject personDetails = enrichJsonWithRegion(envelope.payloadAsJsonObject().getJsonObject(PERSONAL_DETAILS));
        final JsonObject newPayload = updateJsonObject(envelope.payloadAsJsonObject(), PERSONAL_DETAILS, personDetails);
        sender.send(envelop(newPayload)
                .withName(COMMAND_NAME)
                .withMetadataFrom(envelope));
    }
}
