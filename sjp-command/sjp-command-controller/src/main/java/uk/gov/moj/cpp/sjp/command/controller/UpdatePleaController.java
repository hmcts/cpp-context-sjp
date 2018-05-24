package uk.gov.moj.cpp.sjp.command.controller;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(Component.COMMAND_CONTROLLER)
public class UpdatePleaController {

    @Inject
    private Sender sender;

    @Handles("sjp.command.update-plea")
    public void updatePlea(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("sjp.command.cancel-plea")
    public void cancelPlea(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

}
