package uk.gov.moj.cpp.sjp.command.controller;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

/**
 * Command api for marking a case as reopened.
 */
@ServiceComponent(COMMAND_CONTROLLER)
public class CaseReopenedController {

    @Inject
    private Sender sender;

    @Handles("sjp.command.mark-case-reopened-in-libra")
    public void markCaseReopenedInLibra(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("sjp.command.update-case-reopened-in-libra")
    public void updateCaseReopenedInLibra(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("sjp.command.undo-case-reopened-in-libra")
    public void undoCaseReopenedInLibra(final JsonEnvelope envelope) {
        sender.send(envelope);
    }
}
