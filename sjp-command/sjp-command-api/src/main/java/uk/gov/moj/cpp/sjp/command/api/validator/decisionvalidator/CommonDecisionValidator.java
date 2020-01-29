package uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator;

import static java.math.BigDecimal.ZERO;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static uk.gov.moj.cpp.sjp.command.utils.NullSafeJsonObjectHelper.notNull;

import java.math.BigDecimal;
import java.util.Optional;

import javax.json.JsonObject;

public class CommonDecisionValidator {
    private CommonDecisionValidator() {
    }

    public static Optional<BigDecimal> getFinancialPenaltyValue(final JsonObject financialPenalty, final String key) {
        if(notNull(key, financialPenalty)) {
            final BigDecimal penaltyValue = financialPenalty.getJsonNumber(key).bigDecimalValue();
            if(penaltyValue.compareTo(ZERO) >= 0) {
                return of(penaltyValue);
            }
        }
        return empty();
    }
}
