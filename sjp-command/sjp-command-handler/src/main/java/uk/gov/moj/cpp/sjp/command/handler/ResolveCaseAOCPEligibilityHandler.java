package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import static java.util.UUID.fromString;

import javax.json.JsonObject;

import java.util.UUID;

@ServiceComponent(Component.COMMAND_HANDLER)
public class ResolveCaseAOCPEligibilityHandler extends CaseCommandHandler {

    @Handles("sjp.command.resolve-case-aocp-eligibility")
    public void handleResolveCaseAOCPEligibility(final JsonEnvelope command) throws EventStreamException {

        final JsonObject payload = command.payloadAsJsonObject();
        final UUID caseId = fromString(payload.getString("caseId"));
        final boolean isProsecutorAOCPApproved = payload.getBoolean("isProsecutorAOCPApproved");

        applyToCaseAggregate(command, aCase -> aCase.resolveCaseAOCPEligibility(caseId, isProsecutorAOCPApproved));
    }
}
