package uk.gov.moj.cpp.sjp.event.listener.converter;

import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialMeans;

import java.math.BigDecimal;

public class FinancialMeansConverter {

    private static final BigDecimal MAX_VALUE = BigDecimal.valueOf(999999999.99);

    public FinancialMeans convertToFinancialMeansEntity(final FinancialMeansUpdated financialMeansUpdated) {
        final Income income = getRevisedIncome(financialMeansUpdated);
        final Benefits benefit = financialMeansUpdated.getBenefits();

        return new FinancialMeans(financialMeansUpdated.getDefendantId(), income.getFrequency(), income.getAmount(),
                benefit.getClaimed(), benefit.getType(), financialMeansUpdated.getEmploymentStatus());
    }

    private static Income getRevisedIncome(final FinancialMeansUpdated financialMeansUpdated) {
        if (financialMeansUpdated.getIncome() != null
                && financialMeansUpdated.getIncome().getAmount() != null
                && financialMeansUpdated.getIncome().getAmount().compareTo(MAX_VALUE) > 0) {
            final BigDecimal incomeAmount = BigDecimal.valueOf(0);
            return new Income(financialMeansUpdated.getIncome().getFrequency(), incomeAmount);
        }
        return financialMeansUpdated.getIncome();
    }

}
