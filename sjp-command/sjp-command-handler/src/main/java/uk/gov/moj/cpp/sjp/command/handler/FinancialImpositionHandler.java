package uk.gov.moj.cpp.sjp.command.handler;

import static java.util.UUID.fromString;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class FinancialImpositionHandler extends CaseCommandHandler {

    private static final String CORRELATION_ID = "correlationId";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String ACCOUNT_NUMBER = "accountNumber";

    @Handles("sjp.command.add-financial-imposition-correlation-id")
    public void addFinancialImpositionCorrelationId(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID defendantId = fromString(payload.getString(DEFENDANT_ID));
        final UUID correlationId = fromString(payload.getString(CORRELATION_ID));
        applyToCaseAggregate(command, caseAggregate -> caseAggregate.addFinancialImpositionCorrelationId(defendantId, correlationId));
    }

    @Handles("sjp.command.add-financial-imposition-account-number")
    public void addFinancialImpositionAccountNumber(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID correlationId = fromString(payload.getString(CORRELATION_ID));
        final String accountNumber = payload.getString(ACCOUNT_NUMBER);
        applyToCaseAggregate(command, caseAggregate -> caseAggregate.addFinancialImpositionAccountNumber(correlationId, accountNumber));
    }

    @Handles("sjp.command.add-financial-imposition-account-number-bdf")
    public void addFinancialImpositionAccountNumberBdf(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID defendantId = fromString(payload.getString(DEFENDANT_ID));
        final UUID correlationId = fromString(payload.getString(CORRELATION_ID));
        final String accountNumber = payload.getString(ACCOUNT_NUMBER);
        applyToCaseAggregate(command, caseAggregate -> caseAggregate.addFinancialImpositionAccountNumberBdf(defendantId, correlationId, accountNumber));
    }

}
