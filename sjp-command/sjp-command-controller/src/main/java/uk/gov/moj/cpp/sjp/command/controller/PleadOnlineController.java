package uk.gov.moj.cpp.sjp.command.controller;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(Component.COMMAND_CONTROLLER)
public class PleadOnlineController {

    @Inject
    private Sender sender;

    @Handles("sjp.command.plead-online")
    public void pleadOnline(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

}
