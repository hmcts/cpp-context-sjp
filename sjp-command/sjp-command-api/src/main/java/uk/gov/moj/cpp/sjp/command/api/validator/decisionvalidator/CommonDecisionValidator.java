package uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;

import java.math.BigDecimal;

import javax.json.JsonObject;

public class CommonDecisionValidator {
    private CommonDecisionValidator(){}

    public static void validateCompensation(final JsonObject value) {
        if (!hasEitherCompensationOrNoCompensationReason(value)) {
            throw new BadRequestException("Either compensation or noCompensationReason is required");
        }
    }

    public static boolean isNumericValueProvided(final JsonObject value,String path){
        return value.containsKey(path)
                        &&
                        value.getJsonNumber(path).bigDecimalValue().compareTo(BigDecimal.ZERO) > 0;

    }
    public static boolean hasEitherCompensationOrNoCompensationReason(final JsonObject value) {
        return isNumericValueProvided(value,"compensation")
                ||
                isNotBlank(value.getString("noCompensationReason", null));
    }
}