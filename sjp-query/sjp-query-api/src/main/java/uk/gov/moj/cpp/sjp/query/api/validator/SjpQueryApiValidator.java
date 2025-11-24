package uk.gov.moj.cpp.sjp.query.api.validator;

import javax.json.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Collections.*;

public class SjpQueryApiValidator {

    private static final Map<String, List<String>> CASE_ADJOURNED_POST_CONVICTION = singletonMap(
            "CaseAdjournedPostConviction",
            singletonList("Your case has already been reviewed - Contact the Contact Centre if you need to discuss it"));

    public Map<String, List<String>> validateCasePostConviction(final JsonObject caseDetail) {

        if (checkCaseAdjournedTo(caseDetail) ||
                offenceWithPostConviction(caseDetail)) {
            return CASE_ADJOURNED_POST_CONVICTION;
        }

        return emptyMap();
    }

    private Boolean offenceWithPostConviction(final JsonObject caseDetail) {
        return getOffences(caseDetail)
                .anyMatch(offence -> (offence.getString("conviction", null) != null) || (offence.getString("convictionDate", null) != null));
    }

    private Boolean checkCaseAdjournedTo(final JsonObject caseDetail) {
        return Optional.ofNullable(caseDetail.getString("adjournedTo", null))
                .isPresent();
    }

    private Stream<JsonObject> getOffences(final JsonObject caseDetail) {
        return caseDetail.getJsonObject("defendant")
                .getJsonArray("offences")
                .getValuesAs(JsonObject.class)
                .stream();
    }

}
