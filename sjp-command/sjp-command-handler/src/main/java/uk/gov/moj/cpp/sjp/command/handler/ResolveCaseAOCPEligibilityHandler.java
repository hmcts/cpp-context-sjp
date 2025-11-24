package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.fromString;

import javax.json.JsonObject;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@ServiceComponent(Component.COMMAND_HANDLER)
public class ResolveCaseAOCPEligibilityHandler extends CaseCommandHandler {

    public static final String SURCHARGE_AMOUNT = "surchargeAmount";
    public static final String SURCHARGE_AMOUNT_MIN = "surchargeAmountMin";
    public static final String SURCHARGE_AMOUNT_MAX = "surchargeAmountMax";
    public static final String SURCHARGE_FINE_PERCENTAGE = "surchargeFinePercentage";

    @Handles("sjp.command.resolve-case-aocp-eligibility")
    public void handleResolveCaseAOCPEligibility(final JsonEnvelope command) throws EventStreamException {

        final JsonObject payload = command.payloadAsJsonObject();
        final UUID caseId = fromString(payload.getString("caseId"));
        final boolean isProsecutorAOCPApproved = payload.getBoolean("isProsecutorAOCPApproved");

        final Optional<BigDecimal> surchargeAmount = extractSurchargeVictimParams(payload, SURCHARGE_AMOUNT);
        final Optional<BigDecimal> surchargeAmountMin = extractSurchargeVictimParams(payload, SURCHARGE_AMOUNT_MIN);
        final Optional<BigDecimal> surchargeAmountMax = extractSurchargeVictimParams(payload, SURCHARGE_AMOUNT_MAX);
        final Optional<BigDecimal> surchargeFinePercentage = extractSurchargeVictimParams(payload, SURCHARGE_FINE_PERCENTAGE);

        applyToCaseAggregate(command, aCase ->
                aCase.resolveCaseAOCPEligibility(caseId, isProsecutorAOCPApproved, surchargeAmountMin,
                        surchargeAmountMax, surchargeFinePercentage, surchargeAmount));
    }

    private Optional<BigDecimal> extractSurchargeVictimParams(final JsonObject payload, final String fieldName) {
        if (!payload.containsKey(fieldName) || payload.isNull(fieldName)) {
            return empty();
        }
        return of(payload.getJsonNumber(fieldName).bigDecimalValue());
    }
}
