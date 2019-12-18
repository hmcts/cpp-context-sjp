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

/**
 * @deprecated The other CaseReadinessHandler is raising the markCaseReadyForDecision when the state is transitioned into ready state from a non ready state
 */
@ServiceComponent(Component.COMMAND_HANDLER)
@Deprecated
public class CaseReadinessHandler extends CaseCommandHandler {

    /**
     * @deprecated The other CaseReadinessHandler is raising the markCaseReadyForDecision when the state is transitioned into ready state from a non ready state
     */
    @Handles("sjp.command.mark-case-ready-for-decision")
    @Deprecated
    public void markCaseReadyForDecision(final JsonEnvelope command) throws EventStreamException {
        final CaseReadinessReason readinessReason = CaseReadinessReason.valueOf(command.payloadAsJsonObject().getString("reason"));
        final ZonedDateTime markedAt = ZonedDateTime.parse(command.payloadAsJsonObject().getString("markedAt"));
        applyToCaseAggregate(command, aCase -> aCase.markCaseReadyForDecision(readinessReason, markedAt));
    }

    /**
     * @deprecated The other CaseReadinessHandler is raising the unmarkCaseReadyForDecision when the state is transitioned into non-ready state from a ready state
     */
    @Handles("sjp.command.unmark-case-ready-for-decision")
    @Deprecated
    public void unmarkCaseReadyForDecision(final JsonEnvelope command) throws EventStreamException {
        final LocalDate expectedDateReady = LocalDates.from(command.payloadAsJsonObject().getString("expectedDateReady"));
        applyToCaseAggregate(command, aCase -> aCase.unmarkCaseReadyForDecision(expectedDateReady));
    }
}
