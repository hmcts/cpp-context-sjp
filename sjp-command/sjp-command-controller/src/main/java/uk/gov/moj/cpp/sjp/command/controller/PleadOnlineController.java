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
public class PleadOnlineController extends RegionAwareController {

    static final String COMMAND_NAME = "sjp.command.plead-online";
    private static final String PERSONAL_DETAILS = "personalDetails";

    private static final String LEGAL_ENTITY = "legalEntityDefendant";

    @Inject
    private Sender sender;

    @Handles(COMMAND_NAME)
    public void pleadOnline(final JsonEnvelope envelope) {
        if (envelope.payloadAsJsonObject().containsKey(PERSONAL_DETAILS)) {
            final JsonObject personDetails = enrichJsonWithRegion(envelope.payloadAsJsonObject().getJsonObject(PERSONAL_DETAILS));
            final JsonObject newPayload = updateJsonObject(envelope.payloadAsJsonObject(), PERSONAL_DETAILS, personDetails);
            sender.send(envelop(newPayload)
                    .withName(COMMAND_NAME)
                    .withMetadataFrom(envelope));
        }
        else if (envelope.payloadAsJsonObject().containsKey(LEGAL_ENTITY)) {
            sender.send(envelop(envelope.payloadAsJsonObject())
                    .withName(COMMAND_NAME)
                    .withMetadataFrom(envelope));
        }
    }
}
