package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.CaseReopenDetails;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;

import javax.json.JsonObject;
import java.time.LocalDate;
import java.util.stream.Stream;

@ServiceComponent(Component.COMMAND_HANDLER)
public class CaseReopenedHandler extends CaseCommandHandler {

    @FunctionalInterface
    private interface CaseReopenedAction {
        Stream<Object> apply(CaseReopenDetails caseReopenDetails);
    }

    @FunctionalInterface
    private interface CaseAggregateHandler {
        CaseReopenedAction apply(CaseAggregate aCase);
    }

    @Handles("sjp.command.mark-case-reopened-in-libra")
    public void markCaseReopenedInLibra(final JsonEnvelope command) throws EventStreamException {

        handleCaseReopened(command, aCase -> aCase::markCaseReopened);

    }

    @Handles("sjp.command.update-case-reopened-in-libra")
    public void updateCaseReopenedInLibra(final JsonEnvelope command) throws EventStreamException {

        handleCaseReopened(command, aCase -> aCase::updateCaseReopened);

    }

    @Handles("sjp.command.undo-case-reopened-in-libra")
    public void undoCaseReopenedInLibra(final JsonEnvelope command) throws EventStreamException {

        applyToCaseAggregate(command, aCase -> aCase.undoCaseReopened(
                command.payloadAsJsonObject().getString(CASE_ID)
        ));

    }

    private void handleCaseReopened(final JsonEnvelope command,
                                    final CaseAggregateHandler caseAggregateHandler) throws EventStreamException {

        final JsonObject payload = command.payloadAsJsonObject();
        final String caseId = command.payloadAsJsonObject().getString(CASE_ID);
        final LocalDate reopenedDate = LocalDate.parse(payload.getString("reopenedDate"));
        final String libraCaseNumber = payload.getString("libraCaseNumber");
        final String reason = payload.getString("reason");

        applyToCaseAggregate(command, aCase ->
                caseAggregateHandler.apply(aCase).apply(
                        new CaseReopenDetails(caseId, reopenedDate, libraCaseNumber, reason)));
    }
}
