package uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator;

import static java.time.LocalDate.now;
import static java.time.LocalDate.parse;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;

import java.time.LocalDate;

import javax.json.JsonObject;

public class AdjournDecisionValidator {

    private AdjournDecisionValidator(){}

    public static void validateAdjournDecision(JsonObject adjourn) {
        final LocalDate adjournTo = parse(adjourn.getString("adjournTo"));
        if (adjournTo.isBefore(now().plusDays(1)) || adjournTo.isAfter(now().plusDays(28))) {
            throw new BadRequestException("The adjournment date must be between 1 and 28 days in the future");
        }
    }
}
