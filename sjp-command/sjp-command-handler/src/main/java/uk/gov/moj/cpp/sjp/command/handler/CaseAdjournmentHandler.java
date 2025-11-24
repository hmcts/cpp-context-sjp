package uk.gov.moj.cpp.sjp.command.handler;

import static java.time.LocalDate.parse;
import static java.util.UUID.fromString;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.LocalDate;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class CaseAdjournmentHandler extends CaseCommandHandler {

    @Inject
    private Clock clock;

    @Handles("sjp.command.record-case-adjourned-to-later-sjp-hearing")
    public void recordCaseAdjournedToLaterSjpHearing(final JsonEnvelope command) throws EventStreamException {
        final JsonObject recordAdjournmentPayload = command.payloadAsJsonObject();

        final UUID caseId = fromString(recordAdjournmentPayload.getString("caseId"));
        final UUID sessionId = fromString(recordAdjournmentPayload.getString("sjpSessionId"));
        final LocalDate adjournedTo = parse(recordAdjournmentPayload.getString("adjournedTo"));

        applyToCaseAggregate(command, caseAggregate -> caseAggregate.recordCaseAdjournedToLaterSjpHearing(caseId, sessionId, adjournedTo));
    }

    @Handles("sjp.command.record-case-adjournment-to-later-sjp-hearing-elapsed")
    public void recordCaseAdjournmentToLaterSjpHearingElapsed(final JsonEnvelope command) throws EventStreamException {
        final JsonObject recordAdjournmentPayload = command.payloadAsJsonObject();

        final UUID caseId = fromString(recordAdjournmentPayload.getString("caseId"));

        applyToCaseAggregate(command, caseAggregate -> caseAggregate.recordCaseAdjournmentToLaterSjpHearingElapsed(caseId, clock.now()));
    }
}
