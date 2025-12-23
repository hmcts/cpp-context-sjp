package uk.gov.moj.cpp.sjp.query.api.decorator;

import static java.util.UUID.fromString;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.query.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.query.service.ReferralReasons;
import uk.gov.moj.cpp.sjp.query.service.WithdrawalReasons;

import java.util.UUID;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.apache.commons.collections.CollectionUtils;

public class OffenceDecisionDecorator {

    @Inject
    private ReferenceDataService referenceDataService;

    public JsonObject decorate(final JsonObject caseView, final JsonEnvelope envelope, final WithdrawalReasons withdrawalReasons) {
        final JsonArray originalDecisions = caseView.getJsonArray("caseDecisions");

        if (CollectionUtils.isEmpty(originalDecisions)) {
            return caseView;
        }

        final JsonArray caseDecisions = originalDecisions.getValuesAs(JsonObject.class).stream()
                .map(decision -> decorateDecision(decision, envelope, withdrawalReasons))
                .reduce(JsonObjects.createArrayBuilder(), JsonArrayBuilder::add, JsonArrayBuilder::add)
                .build();

        return JsonObjects.createObjectBuilder(caseView).add("caseDecisions", caseDecisions).build();
    }

    private JsonObject decorateDecision(final JsonObject decision, final JsonEnvelope envelope, final WithdrawalReasons withdrawalReasons) {
        final ReferralReasons referralReasons = new ReferralReasons(referenceDataService, envelope);

        final JsonArray offenceDecisions = decision.getJsonArray("offenceDecisions").getValuesAs(JsonObject.class).stream()
                .map(offenceDecision -> decorateOffenceDecision(offenceDecision, referralReasons, withdrawalReasons))
                .reduce(JsonObjects.createArrayBuilder(), JsonArrayBuilder::add, JsonArrayBuilder::add)
                .build();

        return JsonObjects.createObjectBuilder(decision).add("offenceDecisions", offenceDecisions).build();
    }

    private static JsonObject decorateOffenceDecision(final JsonObject offenceDecision,
                                                      final ReferralReasons referralReasons,
                                                      final WithdrawalReasons withdrawalReasons) {
        final DecisionType decisionType = DecisionType.valueOf(offenceDecision.getString("decisionType"));
        switch (decisionType) {
            case REFER_FOR_COURT_HEARING:
                return decorateReferForCourtHearing(offenceDecision, referralReasons);
            case WITHDRAW:
                return decorateWithdrawal(offenceDecision, withdrawalReasons);
            default:
                return offenceDecision;
        }
    }

    private static JsonObject decorateReferForCourtHearing(final JsonObject referForCourtHearing, final ReferralReasons referralReasons) {
        final UUID referralReasonId = fromString(referForCourtHearing.getString("referralReasonId"));
        return referralReasons.getReferralReason(referralReasonId)
                .map(OffenceDecisionDecorator::buildReferralReason)
                .map(reason -> JsonObjects.createObjectBuilder(referForCourtHearing).add("referralReason", reason).build())
                .orElse(referForCourtHearing);
    }

    private static String buildReferralReason(final JsonObject referralReason) {
        if (referralReason.containsKey("subReason")) {
            return String.format("%s (%s)", referralReason.getString("reason"), referralReason.getString("subReason"));
        } else {
            return referralReason.getString("reason");
        }
    }

    private static JsonObject decorateWithdrawal(final JsonObject withdrawal, final WithdrawalReasons withdrawalReasons) {
        final UUID withdrawalReasonId = fromString(withdrawal.getString("withdrawalReasonId"));
        return withdrawalReasons.getWithdrawalReason(withdrawalReasonId)
                .map(reason -> JsonObjects.createObjectBuilder(withdrawal).add("withdrawalReason", reason).build())
                .orElse(withdrawal);
    }
}
