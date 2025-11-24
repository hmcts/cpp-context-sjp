package uk.gov.moj.cpp.sjp.query.api.decorator;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.service.WithdrawalReasons;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

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