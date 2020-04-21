package uk.gov.moj.cpp.sjp.query.api.decorator;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.query.service.OffenceFineLevels;
import uk.gov.moj.cpp.sjp.query.service.WithdrawalReasons;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class OffenceHelper {

    private static final List<String> nonImprisonableModeOfTrials = asList("STRAFF", "SNONIMP");
    private static final List<String> unLimitedFineLevels = asList("U", "S");

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

    public Optional<BigDecimal> getMaxFineValue(final JsonObject offenceDefinition, final OffenceFineLevels fineLevels) {
        final String maxFineLevel = getMaxFineLevel(offenceDefinition);
        if(StringUtils.isNotEmpty(maxFineLevel) && !unLimitedFineLevels.contains(maxFineLevel)) {
            return fineLevels.getOffenceMaxFineValue(Integer.valueOf(maxFineLevel));
        }

        return Optional.empty();
    }

    public String getPenaltyType(JsonObject offenceDefinition) {
        return JsonObjects.getString(offenceDefinition, "penaltyType").orElse(EMPTY);
    }

    public String getSentencing(JsonObject offenceDefinition) {
        return JsonObjects.getString(offenceDefinition, "sentencing").orElse(EMPTY);
    }

    public boolean isBackDuty(JsonObject offenceDefinition) {
        return JsonObjects.getBoolean(offenceDefinition, "backDuty")
                .orElse(false);
    }

    public boolean hasFinalDecision(final JsonObject offenceInstance, final JsonArray caseDecisions) {
        final Optional<JsonObject> offenceDecision = caseDecisions.getValuesAs(JsonObject.class)
                .stream()
                .map(e -> Pair.of(e, ZonedDateTime.parse(e.getString("savedAt"))))
                .sorted((e1, e2) -> e2.getRight().compareTo(e1.getRight()))
                .map(e -> e.getLeft())
                .collect(Collectors.toList())
                .stream()
                .flatMap(caseDecision ->
                        caseDecision
                                .getJsonArray("offenceDecisions")
                                .getValuesAs(JsonObject.class)
                                .stream())
                .filter(e -> e.getString("offenceId").equals(offenceInstance.getString("id")))
                .findFirst();

        if (offenceDecision.isPresent()) {
            return DecisionType.valueOf(offenceDecision.get().getString("decisionType")).isFinal();
        }
        return false;
    }
}
