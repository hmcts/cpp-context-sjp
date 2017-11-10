package uk.gov.moj.cpp.sjp.event.listener.converter;

import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialMeans;

public class FinancialMeansConverter {

    public FinancialMeans convertToFinancialMeansEntity(final FinancialMeansUpdated financialMeansUpdated) {
        final Income income = financialMeansUpdated.getIncome();
        final Benefits benefit = financialMeansUpdated.getBenefits();

        return new FinancialMeans(financialMeansUpdated.getDefendantId(), income.getFrequency(), income.getAmount(),
                benefit.getClaimed(), benefit.getType(), financialMeansUpdated.getEmploymentStatus());
    }

}
