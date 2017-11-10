package uk.gov.moj.cpp.sjp.command.controller;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(Component.COMMAND_CONTROLLER)
public class CreateCaseController {

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Handles("sjp.command.create-sjp-case")
    public void createSjpCase(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

}
