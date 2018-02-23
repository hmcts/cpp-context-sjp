package uk.gov.moj.cpp.sjp.command.controller;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_CONTROLLER)
public class UpdateDefendantController {

    @Inject
    private Sender sender;

    @Handles("sjp.command.update-defendant-details")
    public void updateDefendantDetails(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("sjp.command.update-defendant-national-insurance-number")
    public void updateNationalInsuranceNumber(final JsonEnvelope envelope) {
        sender.send(envelope);
    }
}
