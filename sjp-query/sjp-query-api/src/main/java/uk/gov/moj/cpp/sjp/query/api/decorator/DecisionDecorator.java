package uk.gov.moj.cpp.sjp.query.api.decorator;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.service.WithdrawalReasons;

import jakarta.inject.Inject;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

public class DecisionDecorator {

    @Inject
    private DecisionSessionDecorator decisionSessionDecorator;

    @Inject
    private OffenceDecisionDecorator offenceDecisionDecorator;

    public JsonObject decorate(final JsonObject caseView, final JsonEnvelope envelope, final WithdrawalReasons withdrawalReasons) {
        final JsonArray caseDecisions = caseView.getJsonArray("caseDecisions");

        if (caseDecisions == null) {
            return caseView;
        } else {
            return decisionSessionDecorator.decorateWithLegalAdviserName(offenceDecisionDecorator.decorate(caseView, envelope, withdrawalReasons), envelope);
        }
    }

}