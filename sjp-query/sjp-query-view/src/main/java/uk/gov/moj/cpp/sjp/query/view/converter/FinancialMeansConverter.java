package uk.gov.moj.cpp.sjp.query.view.converter;

import static java.util.Objects.nonNull;

import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialMeans;
import uk.gov.moj.cpp.sjp.persistence.entity.LegalEntityFinancialMeans;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;

public class FinancialMeansConverter {

    public uk.gov.moj.cpp.sjp.domain.FinancialMeans convertToFinancialMeans(final FinancialMeans entity, final OnlinePlea onlinePlea) {

        final LegalEntityFinancialMeans legalEntityFinancialMeans = (nonNull(onlinePlea) && nonNull(onlinePlea.getLegalEntityDetails())) ? onlinePlea.getLegalEntityDetails().getLegalEntityFinancialMeans(): null;
        if (nonNull(legalEntityFinancialMeans)) {
            return new uk.gov.moj.cpp.sjp.domain.FinancialMeans(entity.getDefendantId(), null, null, entity.getEmploymentStatus(), legalEntityFinancialMeans.getGrossTurnover(), legalEntityFinancialMeans.getNetTurnover(), legalEntityFinancialMeans.getNumberOfEmployees(), legalEntityFinancialMeans.getTradingMoreThan12Months());
        }
        else {
            final Income income;
            if (nonNull(entity.getIncomePaymentFrequency()) && nonNull(entity.getIncomePaymentAmount())) {
                income = new Income(entity.getIncomePaymentFrequency(), entity.getIncomePaymentAmount());
            }
            else {
                income = null;
            }
            final Benefits benefit = new Benefits(entity.getBenefitsClaimed(), entity.getBenefitsType(), null);

            return new uk.gov.moj.cpp.sjp.domain.FinancialMeans(entity.getDefendantId(), income, benefit, entity.getEmploymentStatus(), null, null, null, null);
        }

    }
}
