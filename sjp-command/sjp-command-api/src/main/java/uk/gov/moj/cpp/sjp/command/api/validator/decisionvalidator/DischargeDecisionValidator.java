package uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator;

import static java.util.Optional.ofNullable;
import static uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator.CommonDecisionValidator.validateCompensation;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.CONDITIONAL;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType;

import javax.json.JsonObject;

public class DischargeDecisionValidator {
    private DischargeDecisionValidator(){}

    public static void validateDischargeDecision(final JsonObject discharge) {
        validateCompensation(discharge);
        validateDischargedFor(discharge);
    }


    public static void validateDischargedFor(final JsonObject discharge) {
        if (DischargeType.valueOf(discharge.getString("dischargeType")) == CONDITIONAL &&
                !ofNullable(discharge.getJsonObject("dischargedFor")).isPresent()) {
            throw new BadRequestException("dischargedFor is required for conditional discharge");
        }
    }
}
