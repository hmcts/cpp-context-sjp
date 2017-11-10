package uk.gov.moj.cpp.sjp.command.controller;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;

@ServiceComponent(COMMAND_CONTROLLER)
public class UpdateDefendantController {

    @Inject
    private Sender sender;

    @Handles("sjp.command.update-defendant-details")
    public void updateDefendantDetails(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

}
