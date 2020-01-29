package uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator;

import static java.math.BigDecimal.ZERO;
import static java.util.Optional.ofNullable;
import static javax.json.JsonValue.ValueType.NUMBER;
import static uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator.CommonDecisionValidator.getFinancialPenaltyValue;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;

import java.math.BigDecimal;
import java.util.Optional;

import javax.json.JsonNumber;
import javax.json.JsonObject;

public class FinancialPenaltyDecisionValidator {

    private static final BigDecimal MAX_FINE_VALUE = BigDecimal.valueOf(999999999.99);

    private FinancialPenaltyDecisionValidator() {
    }

    public static void validateFinancialPenaltyDecision(final JsonObject  offenceDecision, final Optional<JsonObject> offence) {

        final Optional<BigDecimal> excisePenalty = getFinancialPenaltyValue(offenceDecision, "excisePenalty");
        final BigDecimal compensation = getFinancialPenaltyValue(offenceDecision, "compensation").orElse(ZERO);
        final Optional<BigDecimal> fine = getFinancialPenaltyValue(offenceDecision, "fine");

        excisePenalty.ifPresent(FinancialPenaltyDecisionValidator::validateExcisePenalty);
        fine.ifPresent(fineValue -> validateFine(fineValue, compensation, offence));
    }

    private static void validateExcisePenalty(BigDecimal excisePenalty) {
        if (excisePenalty.compareTo(MAX_FINE_VALUE) > 0) {
            throw new BadRequestException("The maximum excise penalty for this offence is £" + MAX_FINE_VALUE + "");
        }
    }

    private static void validateFine(final BigDecimal fineValue, final BigDecimal compensation, final Optional<JsonObject> offence) {
        if(fineValue.compareTo(ZERO) > 0) {
            offence.ifPresent(offenceJson -> {
                final BigDecimal maxFineValue = ofNullable(offenceJson.get("maxFineValue"))
                        .filter(maxAmount -> NUMBER.equals(maxAmount.getValueType()))
                        .map(maxValue -> ((JsonNumber) maxValue).bigDecimalValue())
                        .orElse(MAX_FINE_VALUE);

                if (fineValue.compareTo(maxFineValue) > 0) {
                    throw new BadRequestException("The maximum fine for this offence is £" + maxFineValue + "");
                }
            });
        } else if(compensation.equals(ZERO)) {
            throw new BadRequestException("Both compensation and fine cannot be empty");
        }
    }
}
