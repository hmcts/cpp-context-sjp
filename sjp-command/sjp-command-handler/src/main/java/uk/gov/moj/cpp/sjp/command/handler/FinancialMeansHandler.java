package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;

import java.util.UUID;

import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class FinancialMeansHandler extends CaseCommandHandler {

    @Handles("sjp.command.update-financial-means")
    public void updateFinancialMeans(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final FinancialMeans financialMeans = converter.convert(payload, FinancialMeans.class);
        applyToCaseAggregate(command, aggregate -> aggregate.updateFinancialMeans(
                getUserId(command), financialMeans));
    }

    @Handles("sjp.command.delete-defendant-financial-means-information")
    public void deleteFinancialMeans(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payloadAsJsonObject = command.payloadAsJsonObject();
        final String defendantId = payloadAsJsonObject.getString("defendantId");
        applyToCaseAggregate(command,
                aggregate -> aggregate.deleteFinancialMeans(UUID.fromString(defendantId)));

    }
}
