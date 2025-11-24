package uk.gov.moj.cpp.sjp.event.processor.service.enforcementnotification;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDecision;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;

import javax.inject.Inject;
import javax.json.JsonObject;

public class DivisionCodeHelper {

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private LastDecisionHelper lastDecisionService;

    @Inject
    private EnforcementAreaService enforcementAreaService;

    @SuppressWarnings({"squid:S1186", "squid:S1602"})
    public int divisionCode(final JsonEnvelope jsonEnvelope,
                            final CaseDetails caseDetails,
                            final String postcode) {
        final String errorMessage = String.format("Unable to find Enforcement area division code in reference data for postcode : %s", postcode);
        return lastDecisionService.getLastDecision(caseDetails)
                .map(caseDecision -> extracted(jsonEnvelope, postcode, caseDecision)).orElseThrow(() ->
                        new IllegalStateException(errorMessage));
    }

    private Integer extracted(JsonEnvelope jsonEnvelope, String postcode, final CaseDecision lastDecision) {
        final String localJusticeAreaNationalCourtCode =  lastDecision.getSession().getLocalJusticeAreaNationalCourtCode();
        final JsonObject enforcementArea = enforcementAreaService.getEnforcementArea(postcode, localJusticeAreaNationalCourtCode, jsonEnvelope);
        final int accountDivisionCode = enforcementArea.getInt("accountDivisionCode");
        if (accountDivisionCode == 0){
            final String errorMessage = String.format("Unable to find Enforcement area division code in reference data for postcode : %s", postcode);
            throw new IllegalStateException(errorMessage);
        }
        return accountDivisionCode;
    }
}
