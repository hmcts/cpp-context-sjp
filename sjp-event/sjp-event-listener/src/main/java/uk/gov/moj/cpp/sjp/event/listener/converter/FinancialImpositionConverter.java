package uk.gov.moj.cpp.sjp.event.listener.converter;

import uk.gov.moj.cpp.sjp.domain.decision.imposition.CostsAndSurcharge;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.FinancialImposition;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Installments;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.LumpSum;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Payment;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentTerms;
import uk.gov.moj.cpp.sjp.persistence.entity.CourtDetails;

public class FinancialImpositionConverter {
    static final FinancialImpositionConverter INSTANCE = new FinancialImpositionConverter();

    private FinancialImpositionConverter() {
    }

    public static uk.gov.moj.cpp.sjp.persistence.entity.FinancialImposition convertToFinancialImposition(FinancialImposition financialImposition) {
        final uk.gov.moj.cpp.sjp.persistence.entity.FinancialImposition financialImpositionEntity = new uk.gov.moj.cpp.sjp.persistence.entity.FinancialImposition();
        financialImpositionEntity.setPayment(INSTANCE.convertToPayment(financialImposition.getPayment()));
        financialImpositionEntity.setCostsAndSurcharge(INSTANCE.convertToCostsAndSurcharge(financialImposition.getCostsAndSurcharge()));
        return financialImpositionEntity;
    }

    private uk.gov.moj.cpp.sjp.persistence.entity.CostsAndSurcharge convertToCostsAndSurcharge(CostsAndSurcharge source) {
        uk.gov.moj.cpp.sjp.persistence.entity.CostsAndSurcharge target = null;
        if (source != null) {
            target = new uk.gov.moj.cpp.sjp.persistence.entity.CostsAndSurcharge();
            target.setCollectionOrderMade(source.isCollectionOrderMade());
            target.setCosts(source.getCosts());
            target.setReasonForNoCosts(source.getReasonForNoCosts());
            target.setVictimSurcharge(source.getVictimSurcharge());
            target.setCollectionOrderMade(source.isCollectionOrderMade());
            target.setReasonForNoVictimSurcharge(source.getReasonForNoVictimSurcharge());
            target.setReasonForReducedVictimSurcharge(source.getReasonForReducedVictimSurcharge());
        }
        return target;
    }

    private uk.gov.moj.cpp.sjp.persistence.entity.Payment convertToPayment(final Payment source) {
        uk.gov.moj.cpp.sjp.persistence.entity.Payment target = null;
        if (source != null) {
            target = new uk.gov.moj.cpp.sjp.persistence.entity.Payment();
            target.setPaymentType(source.getPaymentType());
            target.setPaymentTerms(convertToPaymentTerms(source.getPaymentTerms()));
            target.setReasonWhyNotAttachedOrDeducted(source.getReasonWhyNotAttachedOrDeducted());
            target.setTotalSum(source.getTotalSum());
            target.setReasonForDeductingFromBenefits(source.getReasonForDeductingFromBenefits());
            uk.gov.moj.cpp.sjp.domain.decision.CourtDetails sourceCourtDetails = source.getFineTransferredTo();
            if (sourceCourtDetails != null) {
                target.setFineTransferredTo(new CourtDetails(
                        sourceCourtDetails.getNationalCourtCode(),
                        sourceCourtDetails.getNationalCourtName())
                );
            }
        }
        return target;
    }

    private uk.gov.moj.cpp.sjp.persistence.entity.PaymentTerms convertToPaymentTerms(final PaymentTerms source) {
        final uk.gov.moj.cpp.sjp.persistence.entity.PaymentTerms target = new uk.gov.moj.cpp.sjp.persistence.entity.PaymentTerms();
        target.setLumpSum(convertToPaymentTerms(source.getLumpSum()));
        target.setInstallments(convertToInstalmentsValue(source.getInstallments()));
        target.setReserveTerms(source.isReserveTerms());
        return target;
    }

    private uk.gov.moj.cpp.sjp.persistence.entity.LumpSum convertToPaymentTerms(final LumpSum source) {
        if (source == null) {
            return null;
        }
        final uk.gov.moj.cpp.sjp.persistence.entity.LumpSum target = new uk.gov.moj.cpp.sjp.persistence.entity.LumpSum();
        target.setAmount(source.getAmount());
        target.setPayByDate(source.getPayByDate());
        target.setWithinDays(source.getWithinDays());
        return target;
    }

    private uk.gov.moj.cpp.sjp.persistence.entity.Installments convertToInstalmentsValue(final Installments source) {
        if (source == null) {
            return null;
        }
        final uk.gov.moj.cpp.sjp.persistence.entity.Installments target = new uk.gov.moj.cpp.sjp.persistence.entity.Installments();
        target.setAmount(source.getAmount());
        target.setStartDate(source.getStartDate());
        target.setPeriod(source.getPeriod());
        return target;
    }
}
