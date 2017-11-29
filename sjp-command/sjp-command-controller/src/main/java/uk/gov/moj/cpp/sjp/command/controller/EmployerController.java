package uk.gov.moj.cpp.sjp.command.controller;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.command.service.CaseUpdateHelper;

import java.util.Optional;

import javax.inject.Inject;

@ServiceComponent(COMMAND_CONTROLLER)
public class EmployerController {

    @Inject
    private Sender sender;

    @Inject
    private CaseUpdateHelper caseUpdateHelper;

    @Handles("sjp.command.update-employer")
    public void updateEmployer(final JsonEnvelope envelope) {
        final Optional<JsonEnvelope> rejectCommandEnvelope = caseUpdateHelper.checkForCaseUpdateRejectReasons(envelope);
        sender.send(rejectCommandEnvelope.orElse(envelope));
    }

    @Handles("sjp.command.delete-employer")
    public void deleteEmployer(final JsonEnvelope envelope) {
        final Optional<JsonEnvelope> rejectCommandEnvelope = caseUpdateHelper.checkForCaseUpdateRejectReasons(envelope);
        sender.send(rejectCommandEnvelope.orElse(envelope));
    }

}