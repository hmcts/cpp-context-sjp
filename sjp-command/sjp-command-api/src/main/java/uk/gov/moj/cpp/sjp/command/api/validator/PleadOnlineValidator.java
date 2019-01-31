package uk.gov.moj.cpp.sjp.command.api.validator;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.COMPLETED;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.REFERRED_FOR_COURT_HEARING;

import uk.gov.justice.json.schemas.domains.sjp.command.FinancialMeans;
import uk.gov.justice.json.schemas.domains.sjp.command.Offence;
import uk.gov.justice.json.schemas.domains.sjp.command.Plea;
import uk.gov.justice.json.schemas.domains.sjp.command.PleadOnline;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.json.JsonObject;

public class PleadOnlineValidator {

    private static final Map<String, List<String>> PLEA_GUILTY_WITHOUT_FINANCIAL_MEANS = singletonMap(
            "FinancialMeansRequiredWhenPleadingGuilty",
            singletonList("Financial Means are required when you are pleading GUILTY"));
    private static final Map<String, List<String>> CASE_HAS_BEEN_REVIEWED = singletonMap(
            "CaseAlreadyReviewed",
            singletonList("Your case has already been reviewed - Contact the Contact Centre if you need to discuss it"));
    private static final Map<String, List<String>> PLEA_ALREADY_SUBMITTED = singletonMap(
            "PleaAlreadySubmitted",
            singletonList("Plea already submitted - Contact the Contact Centre if you need to change or discuss it"));
    private static final Set PROHIBITED_CASE_STATES = new HashSet<>(Arrays.asList(COMPLETED.name(),
            REFERRED_FOR_COURT_HEARING.name()));


    /**
     * Rules: - Financial Means are mandatory when Plea is GUILTY
     */
    @SuppressWarnings("squid:S4274")
    public Map<String, List<String>> validate(final PleadOnline pleadOnline) {
        assert isEmpty(pleadOnline.getOffences()) || pleadOnline.getOffences().size() == 1 : "supports just single offence";

        final boolean anyGuiltyPlea = pleadOnline.getOffences().stream()
                .map(Offence::getPlea)
                .anyMatch(Plea.GUILTY::equals);

        if (anyGuiltyPlea && hasEmptyFinancialMeans(pleadOnline.getFinancialMeans())) {
            return PLEA_GUILTY_WITHOUT_FINANCIAL_MEANS;
        }

        return emptyMap();
    }

    private static boolean hasEmptyFinancialMeans(final FinancialMeans financialMeans) {
        return financialMeans == null ||
                financialMeans.getBenefits() == null ||
                financialMeans.getIncome() == null ||
                isEmpty(financialMeans.getEmploymentStatus());
    }

    public Map<String, List<String>> validate(final JsonObject caseDetail) {
        if (caseStatusProhibited(caseDetail) ||
                !checkCaseDetailField(caseDetail, "completed", FALSE) ||
                !checkCaseDetailField(caseDetail, "assigned", FALSE) ||
                offenceHasPendingWithdrawal(caseDetail)) {
            return CASE_HAS_BEEN_REVIEWED;
        }

        if (caseAlreadyPleaded(caseDetail)) {
            return PLEA_ALREADY_SUBMITTED;
        }
        return emptyMap();
    }

    private Boolean caseStatusProhibited(final JsonObject caseDetail) {
        return Optional.ofNullable(caseDetail.getString("status", null))
                .filter(PROHIBITED_CASE_STATES::contains)
                .isPresent();
    }

    private Boolean checkCaseDetailField(final JsonObject caseDetail, final String fieldName, final Boolean fieldValue) {
        return Optional.of(caseDetail.getBoolean(fieldName, FALSE))
                .filter(fieldValue::equals)
                .isPresent();
    }

    private Boolean offenceHasPendingWithdrawal(final JsonObject caseDetail) {
        return getOffences(caseDetail)
                .map(offence -> offence.getBoolean("pendingWithdrawal", FALSE))
                .anyMatch(TRUE::equals);
    }

    private Boolean caseAlreadyPleaded(final JsonObject caseDetail) {
        return getOffences(caseDetail)
                .anyMatch(offence -> offence.getString("plea", null) != null);
    }

    private Stream<JsonObject> getOffences(final JsonObject caseDetail) {
        return caseDetail.getJsonObject("defendant")
                .getJsonArray("offences")
                .getValuesAs(JsonObject.class)
                .stream();
    }
}
