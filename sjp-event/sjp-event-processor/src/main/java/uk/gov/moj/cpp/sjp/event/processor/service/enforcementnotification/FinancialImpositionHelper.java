package uk.gov.moj.cpp.sjp.event.processor.service.enforcementnotification;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;

import javax.inject.Inject;

public class FinancialImpositionHelper {

    @Inject
    private LastDecisionHelper lastDecisionHelper;

    @SuppressWarnings({"squid:S1186", "squid:S1602"})
    public boolean financialImposition(final CaseDetails caseDetails) {
        return lastDecisionHelper.getLastDecision(caseDetails)
                .map(lastDecision -> (lastDecision.getFinancialImposition() != null))
                .orElse(false);
    }
}
