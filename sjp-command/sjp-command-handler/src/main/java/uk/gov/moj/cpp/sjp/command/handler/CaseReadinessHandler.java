package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@ServiceComponent(Component.COMMAND_HANDLER)
public class CaseReadinessHandler extends CaseCommandHandler {

    @Handles("sjp.command.mark-case-ready-for-decision")
    public void markCaseReadyForDecision(final JsonEnvelope command) throws EventStreamException {
        final CaseReadinessReason readinessReason = CaseReadinessReason.valueOf(command.payloadAsJsonObject().getString("reason"));
        final ZonedDateTime markedAt = ZonedDateTime.parse(command.payloadAsJsonObject().getString("markedAt"));
        applyToCaseAggregate(command, aCase -> aCase.markCaseReadyForDecision(readinessReason, markedAt));
    }

    @Handles("sjp.command.unmark-case-ready-for-decision")
    public void unmarkCaseReadyForDecision(final JsonEnvelope command) throws EventStreamException {
        final LocalDate expectedDateReady = LocalDates.from(command.payloadAsJsonObject().getString("expectedDateReady"));
        applyToCaseAggregate(command, aCase -> aCase.unmarkCaseReadyForDecision(expectedDateReady));
    }
}
