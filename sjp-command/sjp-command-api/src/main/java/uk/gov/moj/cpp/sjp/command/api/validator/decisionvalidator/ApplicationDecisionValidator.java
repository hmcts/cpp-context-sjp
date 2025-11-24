package uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;

import javax.json.JsonObject;

public class ApplicationDecisionValidator {

    private static final String OUT_OF_TIME_REASON = "outOfTimeReason";
    private static final String OUT_OF_TIME = "outOfTime";
    private static final String REJECTION_REASON = "rejectionReason";
    private static final String GRANTED = "granted";

    private ApplicationDecisionValidator() {
    }

    public static void validateApplicationDecision(final JsonObject applicationDecision) {
        final String outOfTimeReason = applicationDecision.getString(OUT_OF_TIME_REASON, "").trim();
        if(applicationDecision.containsKey(OUT_OF_TIME) && applicationDecision.getBoolean(OUT_OF_TIME)
                && isEmpty(outOfTimeReason)) {
            throw new BadRequestException("Out of time reason must be provided if application is out of time");
        }

        final String rejectionReason = applicationDecision.getString(REJECTION_REASON,"").trim();
        if(!applicationDecision.getBoolean(GRANTED) && isEmpty(rejectionReason)) {
            throw new BadRequestException("Rejection reason must be provided if application is rejected");
        }
    }
}
