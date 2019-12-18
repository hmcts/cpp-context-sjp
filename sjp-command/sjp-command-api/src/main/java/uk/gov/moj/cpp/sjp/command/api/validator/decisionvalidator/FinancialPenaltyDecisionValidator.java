package uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator.CommonDecisionValidator.isNumericValueProvided;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;

import javax.json.JsonObject;

/**
 * Validation Rules:
 *  given compensation is not provided
 *  given both fine and compensation reason are not provided
 *  then should not validate
 *
 */
public class FinancialPenaltyDecisionValidator {
    private FinancialPenaltyDecisionValidator(){}

    public static void validateFinancialPenaltyDecision(JsonObject financialPenalty){
        if(!isNumericValueProvided(financialPenalty,"compensation") && !bothFineAndCompensationReasonProvided(financialPenalty)) {
            throw new BadRequestException("Either compensation or, compensation reason and fine is required");
        }
    }

    private static boolean bothFineAndCompensationReasonProvided(final JsonObject value) {
        return isNumericValueProvided(value,"fine")
                &&
                isNotBlank(value.getString("noCompensationReason", null));
    }
}
