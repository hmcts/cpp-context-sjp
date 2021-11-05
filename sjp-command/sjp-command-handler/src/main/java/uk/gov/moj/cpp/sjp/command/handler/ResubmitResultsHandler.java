package uk.gov.moj.cpp.sjp.command.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(COMMAND_HANDLER)
public class ResubmitResultsHandler extends CaseCommandHandler {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("sjp.command.resubmit-results")
    public void resubmitResults(final JsonEnvelope command) throws EventStreamException {

        final UUID caseId = UUID.fromString(command.payloadAsJsonObject().getString("caseId"));


        applyToCaseAggregate(caseId, command, caseAggregate -> caseAggregate.resubmitResults(command.payloadAsJsonObject()));
    }
}
