package uk.gov.moj.cpp.sjp.query.api.decorator;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.query.service.WithdrawalReasons;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class OffenceHelper {

    private static final List<String> nonImprisonableModeOfTrials = asList("STRAFF", "SNONIMP");

    public String getEnglishTitle(final JsonObject offenceDefinition) {
        return offenceDefinition.getString("title");
    }

    public Optional<String> getWelshTitle(final JsonObject offenceDefinition) {
        return JsonObjects.getString(offenceDefinition, "details", "document", "welsh", "welshoffencetitle");
    }

    public String getEnglishLegislation(final JsonObject offenceDefinition) {
        return offenceDefinition.getString("legislation");
    }

    public Optional<String> getWelshLegislation(final JsonObject offenceDefinition) {
        return JsonObjects.getString(offenceDefinition, "details", "document", "welsh", "welshlegislation");
    }

    public Optional<String> getWithdrawalRequestReason(final JsonObject offence, final WithdrawalReasons withdrawalReasons) {
        final Optional<String> withdrawalRequestReasonId = ofNullable(offence.getString("withdrawalRequestReasonId", null));
        return withdrawalRequestReasonId.map(UUID::fromString).flatMap(withdrawalReasons::getWithdrawalReason);
    }

    public boolean isOffenceNotInEffect(final JsonObject offenceInstance, final JsonObject offenceDefinition) {
        final LocalDate committedDate = LocalDate.parse(offenceInstance.getString("startDate"));
        final Optional<LocalDate> validFrom = JsonObjects.getString(offenceDefinition, "offenceStartDate").map(LocalDate::parse);
        final Optional<LocalDate> validTo = JsonObjects.getString(offenceDefinition, "offenceEndDate").map(LocalDate::parse);
        return (validFrom.isPresent() && validFrom.get().isAfter(committedDate)) || (validTo.isPresent() && validTo.get().isBefore(committedDate));
    }

    public boolean isOffenceOutOfTime(final JsonObject offenceInstance, final JsonObject offenceDefinition) {
        final LocalDate offenceCommittedDate = LocalDate.parse(offenceInstance.getString("startDate"));
        final LocalDate offenceChargeDate = LocalDate.parse(offenceInstance.getString("chargeDate"));
        final Optional<String> prosecutionTimeLimit = JsonObjects.getString(offenceDefinition, "prosecutionTimeLimit");

        if (prosecutionTimeLimit.isPresent()) {
            return offenceCommittedDate.plusMonths(Integer.valueOf(prosecutionTimeLimit.get())).isBefore(offenceChargeDate);
        }
        return false;
    }

    public boolean isOffenceImprisonable(final JsonObject offenceDefinition) {
        return !nonImprisonableModeOfTrials.contains( ofNullable(offenceDefinition.getString("modeOfTrial")).orElse(EMPTY).toUpperCase());
    }

    public String getMaxFineLevel(JsonObject offenceDefinition) {
        return JsonObjects.getString(offenceDefinition,"details","document","libra","maxfinetypemagct","code").orElse(EMPTY);
    }

    public boolean hasFinalDecision(final JsonObject offenceInstance, final JsonArray caseDecisions) {

        return caseDecisions.getValuesAs(JsonObject.class)
                .stream()
                .anyMatch(caseDecision ->
                     caseDecision
                            .getJsonArray("offenceDecisions")
                            .getValuesAs(JsonObject.class)
                            .stream()
                            .filter(decision -> decision.getString("offenceId").equals(offenceInstance.getString("id")) && DecisionType.valueOf(decision.getString("decisionType")).isFinal())
                            .count() == 1
                );
    }
}
