package uk.gov.moj.cpp.sjp.event.processor.service.enforcementnotification;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDecision;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;

import javax.inject.Inject;
import javax.json.JsonObject;

public class EnforcementAreaEmailHelper {

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private LastDecisionHelper lastDecisionService;

    @Inject
    private EnforcementAreaService enforcementAreaService;

    @SuppressWarnings({"squid:S1186", "squid:S1602"})
    public String enforcementEmail(final JsonEnvelope jsonEnvelope,
                                   final CaseDetails caseDetails,
                                   final String postcode){
        final String errorMessage = String.format("Unable to find Enforcement area email address in reference data for postcode : %s", postcode);

        return lastDecisionService.getLastDecision(caseDetails)
                .map(caseDecision -> extracted(jsonEnvelope, postcode, caseDecision)).orElseThrow(() ->
                        new IllegalStateException(errorMessage));
    }

    private String extracted(JsonEnvelope jsonEnvelope, String postcode, final CaseDecision lastDecision) {
        final String localJusticeAreaNationalCourtCode = extractlocalJusticeAreaNationalCourtCode(lastDecision);
        final JsonObject enforcementArea = enforcementAreaService.getEnforcementArea(postcode, localJusticeAreaNationalCourtCode, jsonEnvelope);
        return enforcementArea.getString("email");
    }

    private String extractlocalJusticeAreaNationalCourtCode(final CaseDecision lastDecision) {
        return lastDecision.getSession().getLocalJusticeAreaNationalCourtCode();
    }
}
