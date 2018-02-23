package uk.gov.moj.cpp.sjp.query.view.converter;

import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialMeans;

public class FinancialMeansConverter {

    public uk.gov.moj.cpp.sjp.domain.FinancialMeans convertToFinancialMeans(final FinancialMeans entity) {

        final Income income = new Income(entity.getIncomePaymentFrequency(), entity.getIncomePaymentAmount());
        final Benefits benefit = new Benefits(entity.getBenefitsClaimed(), entity.getBenefitsType(), null);

        return new uk.gov.moj.cpp.sjp.domain.FinancialMeans(entity.getDefendantId(), income, benefit, entity.getEmploymentStatus());
    }
}
