package uk.gov.moj.cpp.sjp.query.api.decorator;

import static javax.json.Json.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.service.OffenceFineLevels;
import uk.gov.moj.cpp.sjp.query.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.query.service.WithdrawalReasons;

import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;


public class OffenceDecorator {

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private OffenceHelper offenceHelper;

    public JsonObject decorateAllOffences(final JsonObject caseView, final JsonEnvelope envelope, final WithdrawalReasons withdrawalReasons, final OffenceFineLevels offenceFineLevels) {
        return createObjectBuilder(caseView)
                .add("defendant", decorateDefendantOffences(caseView.getJsonObject("defendant"), caseView.getJsonArray("caseDecisions"), envelope, withdrawalReasons, offenceFineLevels))
                .build();
    }

    private JsonObjectBuilder decorateDefendantOffences(final JsonObject defendantView, final JsonArray caseDecisions, final JsonEnvelope envelope, final WithdrawalReasons withdrawalReasons, final OffenceFineLevels offenceFineLevels) {
        return createObjectBuilder(defendantView)
                .add("offences", defendantView.getJsonArray("offences")
                        .getValuesAs(JsonObject.class)
                        .stream()
                        .map(offence -> decorateOffence(offence, caseDecisions, envelope, withdrawalReasons, offenceFineLevels))
                        .reduce(createArrayBuilder(), JsonArrayBuilder::add, JsonArrayBuilder::add));
    }

    private JsonObjectBuilder decorateOffence(final JsonObject offenceInstance, final JsonArray caseDecisions, final JsonEnvelope envelope, final WithdrawalReasons withdrawalReasons, final OffenceFineLevels offenceFineLevels) {
        final String offenceCode = offenceInstance.getString("offenceCode");
        final String offenceDate = offenceInstance.getString("startDate");

        final JsonObject offenceDefinition = referenceDataService.getOffenceDefinition(offenceCode, offenceDate, envelope);

        final JsonObjectBuilder decoratedOffence = createObjectBuilder(offenceInstance)
                .add("title", offenceHelper.getEnglishTitle(offenceDefinition))
                .add("legislation", offenceHelper.getEnglishLegislation(offenceDefinition))
                .add("outOfTime", offenceHelper.isOffenceOutOfTime(offenceInstance, offenceDefinition))
                .add("notInEffect", offenceHelper.isOffenceNotInEffect(offenceInstance, offenceDefinition))
                .add("imprisonable", offenceHelper.isOffenceImprisonable(offenceDefinition))
                .add("maxFineLevel", offenceHelper.getMaxFineLevel(offenceDefinition))
                .add("hasFinalDecision", offenceHelper.hasFinalDecision(offenceInstance, caseDecisions))
                .add("pendingWithdrawal", hasPendingWithdrawal(offenceInstance, caseDecisions))
                .add("backDutyOffence", offenceHelper.isBackDuty(offenceDefinition))
                .add("penaltyType", offenceHelper.getPenaltyType(offenceDefinition))
                .add("sentencing", offenceHelper.getSentencing(offenceDefinition));

        offenceHelper.getWelshTitle(offenceDefinition).ifPresent(welshTitle -> decoratedOffence.add("titleWelsh", welshTitle));
        offenceHelper.getWelshLegislation(offenceDefinition).ifPresent(welshLegislation -> decoratedOffence.add("legislationWelsh", welshLegislation));
        offenceHelper.getWithdrawalRequestReason(offenceInstance, withdrawalReasons).ifPresent(withdrawalReason -> decoratedOffence.add("withdrawalRequestReason", withdrawalReason));
        offenceHelper.getMaxFineValue(offenceDefinition, offenceFineLevels).ifPresent(maxFineValue -> decoratedOffence.add("maxFineValue", maxFineValue));

        return decoratedOffence;
    }

    private Boolean hasPendingWithdrawal(final JsonObject offenceInstance, final JsonArray caseDecisions){
        if(Optional.ofNullable(offenceInstance.getString("withdrawalRequestReasonId", null)).isPresent()
                && !offenceHelper.hasFinalDecision(offenceInstance, caseDecisions)){
            return true;
        }
        return false;
    }

}
