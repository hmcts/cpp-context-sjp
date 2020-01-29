package uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType;

import javax.json.JsonObject;

import static uk.gov.moj.cpp.sjp.command.utils.NullSafeJsonObjectHelper.notNull;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.CONDITIONAL;

public class DischargeDecisionValidator {
    private DischargeDecisionValidator() {
    }

    public static void validateDischargeDecision(final JsonObject discharge) {
        if (isConditionalDischarge(discharge) && !hasDischargedFor(discharge)) {
            throw new BadRequestException("dischargedFor is required for conditional discharge");
        }
    }

    private static boolean hasDischargedFor(JsonObject discharge) {
        return notNull("dischargedFor", discharge);
    }

    static boolean isConditionalDischarge(JsonObject discharge) {
        if (notNull("dischargeType", discharge)) {
            return DischargeType.valueOf(discharge.getString("dischargeType")) == CONDITIONAL;
        }
        return false;
    }
}
