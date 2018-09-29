package uk.gov.moj.cpp.sjp.command.api.validator;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import uk.gov.justice.json.schemas.domains.sjp.command.FinancialMeans;
import uk.gov.justice.json.schemas.domains.sjp.command.Offence;
import uk.gov.justice.json.schemas.domains.sjp.command.Plea;
import uk.gov.justice.json.schemas.domains.sjp.command.PleadOnline;

import java.util.List;
import java.util.Map;

public class PleadOnlineValidator {

    private static final Map<String, List<String>> PLEA_GUILTY_WITHOUT_FINANCIAL_MEANS = singletonMap(
            "financialMeans",
            singletonList("Financial Means are required when you are pleading GUILTY"));

    /**
     * Rules:
     * - Financial Means are mandatory when Plea is GUILTY
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

}
