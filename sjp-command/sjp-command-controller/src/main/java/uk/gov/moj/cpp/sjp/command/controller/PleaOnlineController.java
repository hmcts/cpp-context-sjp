package uk.gov.moj.cpp.sjp.command.controller;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.command.service.CaseUpdateHelper;

import java.util.Optional;

import javax.inject.Inject;

@ServiceComponent(Component.COMMAND_CONTROLLER)
public class PleaOnlineController {

    @Inject
    private Sender sender;

    @Inject
    private CaseUpdateHelper caseUpdateHelper;

    @Inject
    private Enveloper enveloper;


    @Handles("sjp.command.plea-online")
    public void onlinePlea(final JsonEnvelope envelope) {
        final Optional<JsonEnvelope> rejectCommandEnvelope = caseUpdateHelper.checkForCaseUpdateRejectReasons(envelope);
        sender.send(rejectCommandEnvelope.orElse(envelope));
    }

}
