package uk.gov.moj.cpp.sjp.query.view.converter;

import static java.util.Objects.nonNull;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.*;

import uk.gov.moj.cpp.sjp.persistence.entity.CostsAndSurcharge;
import uk.gov.moj.cpp.sjp.persistence.entity.CourtDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.Installments;
import uk.gov.moj.cpp.sjp.persistence.entity.LumpSum;
import uk.gov.moj.cpp.sjp.persistence.entity.Payment;
import uk.gov.moj.cpp.sjp.persistence.entity.PaymentTerms;
import uk.gov.moj.cpp.sjp.query.view.response.FinancialImpositionView;
import uk.gov.moj.cpp.sjp.query.view.response.OffenceDecisionView;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class DecisionSavedOffenceConverter {

    public JsonObject convertOffenceDecision(OffenceDecisionView offenceDecisionView) {
        final JsonObjectBuilder offenceDecisionBuilder = createObjectBuilder()
                .add(TYPE, offenceDecisionView.getDecisionType().toString());

        if(nonNull(offenceDecisionView.getWithdrawalReasonId())) {
            offenceDecisionBuilder.add(WITHDRAW_REASON_ID, offenceDecisionView.getWithdrawalReasonId().toString());
        }
        if(nonNull(offenceDecisionView.getAdjournedTo())) {
            offenceDecisionBuilder.add(ADJOURN_TO, offenceDecisionView.getAdjournedTo().toString());
        }
        if(nonNull(offenceDecisionView.getDischargeType())) {
            offenceDecisionBuilder.add(DISCHARGE_TYPE, offenceDecisionView.getDischargeType().toString());
        }
        if(nonNull(offenceDecisionView.getDischargedFor())) {
            offenceDecisionBuilder.add(DISCHARGE_FOR, createObjectBuilder()
                    .add("value", offenceDecisionView.getDischargedFor().getValue())
                    .add("unit", offenceDecisionView.getDischargedFor().getUnit().toString()));
        }
        if(nonNull(offenceDecisionView.getCompensation())) {
            offenceDecisionBuilder.add(COMPENSATION, offenceDecisionView.getCompensation());
        }
        checkForFineAndCompensation(offenceDecisionView, offenceDecisionBuilder);
        checkForBackDutyAndExcisePenalty(offenceDecisionView, offenceDecisionBuilder);
        if(nonNull(offenceDecisionView.getOffenceId()) || nonNull(offenceDecisionView.getVerdict())) {
            offenceDecisionBuilder.add(OFFENCE_DECISION_INFORMATION, createArrayBuilder()
                    .add(covertOffenceDecisionInformation(offenceDecisionView)));
        }
        return offenceDecisionBuilder.build();
    }

    private void checkForBackDutyAndExcisePenalty(OffenceDecisionView offenceDecisionView, JsonObjectBuilder offenceDecisionBuilder) {
        if(nonNull(offenceDecisionView.getBackDuty())) {
            offenceDecisionBuilder.add(BACK_DUTY, offenceDecisionView.getBackDuty());
        }
        if(nonNull(offenceDecisionView.getExcisePenalty())) {
            offenceDecisionBuilder.add(EXCISE_PENALTY, offenceDecisionView.getExcisePenalty());
        }
    }

    private void checkForFineAndCompensation(OffenceDecisionView offenceDecisionView, JsonObjectBuilder offenceDecisionBuilder) {
        if(nonNull( offenceDecisionView.getNoCompensationReason())) {
            offenceDecisionBuilder.add(NO_COMPENSATION_REASON, offenceDecisionView.getNoCompensationReason());
        }
        if(nonNull(offenceDecisionView.getFine())) {
            offenceDecisionBuilder.add(FINE, offenceDecisionView.getFine());
        }
    }

    public JsonObject convertFinancialImposition(FinancialImpositionView financialImpositionView) {
        final JsonObjectBuilder financialImpositionBuilder = createObjectBuilder();

        if(nonNull(financialImpositionView.getCostsAndSurcharge())) {
            financialImpositionBuilder.add(COSTS_AND_SURCHARGE, convertCostsAndSurcharge(financialImpositionView.getCostsAndSurcharge()));
        }

        if(nonNull(financialImpositionView.getPayment())) {
            financialImpositionBuilder.add(PAYMENT, convertPayment(financialImpositionView.getPayment()));
        }

        return financialImpositionBuilder.build();
    }

    private JsonObjectBuilder covertOffenceDecisionInformation(OffenceDecisionView offenceDecisionView) {
        final JsonObjectBuilder offenceDecisionInformation = createObjectBuilder();
        if(nonNull(offenceDecisionView.getOffenceId())) {
            offenceDecisionInformation.add(OFFENCE_ID, offenceDecisionView.getOffenceId().toString());
        }
        if(nonNull(offenceDecisionView.getVerdict())) {
            offenceDecisionInformation.add(VERDICT, offenceDecisionView.getVerdict().toString());
        }
        return offenceDecisionInformation;
    }

    private JsonObjectBuilder convertPayment(Payment payment) {
        final JsonObjectBuilder paymentBuilder = createObjectBuilder();

        if(nonNull(payment.getTotalSum())) {
            paymentBuilder.add(TOTAL_SUM, payment.getTotalSum());
        }
        if(nonNull(payment.getPaymentType())) {
            paymentBuilder.add(PAYMENT_TYPE, payment.getPaymentType().toString());
        }
        if(nonNull(payment.getReasonWhyNotAttachedOrDeducted())) {
            paymentBuilder.add(REASON_WHY_NOT_ATTACHED_OR_DEDUCTED, payment.getReasonWhyNotAttachedOrDeducted());
        }
        if(nonNull(payment.getReasonForDeductingFromBenefits())) {
            paymentBuilder.add(REASON_FOR_DEDUCTING_FROM_BENEFITS, payment.getReasonForDeductingFromBenefits().toString());
        }

        if(nonNull(payment.getPaymentTerms())) {
            paymentBuilder.add(PAYMENT_TERMS, convertPaymentTerms(payment.getPaymentTerms()));
        }

        if(nonNull(payment.getFineTransferredTo())) {
            paymentBuilder.add(FINE_TRANSFERRED_TO, convertFineTransferredTo(payment.getFineTransferredTo()));
        }
        return paymentBuilder;
    }

    private JsonObjectBuilder convertFineTransferredTo(CourtDetails courtDetails) {
        final JsonObjectBuilder fineTransferredToBuilder = createObjectBuilder();

        if(nonNull(courtDetails.getNationalCourtCode())) {
            fineTransferredToBuilder.add(NATIONAL_COURT_CODE, courtDetails.getNationalCourtCode());
        }
        if(nonNull(courtDetails.getNationalCourtName())) {
            fineTransferredToBuilder.add(NATIONAL_COURT_NAME, courtDetails.getNationalCourtName());
        }
        return fineTransferredToBuilder;
    }

    private JsonObjectBuilder convertCostsAndSurcharge(CostsAndSurcharge costsAndSurcharge) {
        final JsonObjectBuilder costsAndSurchargeBuilder = createObjectBuilder();

        if(nonNull(costsAndSurcharge.getCosts())) {
            costsAndSurchargeBuilder.add(COSTS, costsAndSurcharge.getCosts());
        }
        if(nonNull(costsAndSurcharge.getReasonForNoCosts())) {
            costsAndSurchargeBuilder.add(REASON_FOR_NO_COSTS, costsAndSurcharge.getReasonForNoCosts());
        }
        costsAndSurchargeBuilder.add(COLLECTION_ORDER_MADE, costsAndSurcharge.isCollectionOrderMade());
        if(nonNull(costsAndSurcharge.getVictimSurcharge())) {
            costsAndSurchargeBuilder.add(VICTIM_SURCHARGE, costsAndSurcharge.getVictimSurcharge());
        }
        if(nonNull(costsAndSurcharge.getReasonForNoVictimSurcharge())) {
            costsAndSurchargeBuilder.add(REASON_FOR_NO_VICTIM_SURCHARGE, costsAndSurcharge.getReasonForNoVictimSurcharge());
        }
        return costsAndSurchargeBuilder;
    }

    private JsonObjectBuilder convertPaymentTerms(PaymentTerms paymentTerms) {
        final JsonObjectBuilder paymentTermsBuilder = createObjectBuilder();

        paymentTermsBuilder.add(RESERVE_TERMS, paymentTerms.isReserveTerms());

        if(nonNull(paymentTerms.getLumpSum())) {
            paymentTermsBuilder.add(LUMP_SUM, convertLumpSum(paymentTerms.getLumpSum()));
        }
        if(nonNull(paymentTerms.getInstallments())) {
            paymentTermsBuilder.add(INSTALLMENTS, createInstallments(paymentTerms.getInstallments()));
        }
        return paymentTermsBuilder;
    }

    private JsonObjectBuilder createInstallments(Installments installments) {
        final JsonObjectBuilder installmentsBuilder = createObjectBuilder();

        if(nonNull(installments.getAmount())) {
            installmentsBuilder.add(AMOUNT, installments.getAmount());
        }
        if(nonNull(installments.getPeriod())) {
            installmentsBuilder.add(PERIOD, installments.getPeriod().toString());
        }
        if(nonNull(installments.getStartDate())) {
            installmentsBuilder.add(START_DATE, installments.getStartDate().toString());
        }
        return installmentsBuilder;
    }

    private JsonObjectBuilder convertLumpSum(LumpSum lumpSum) {
        final JsonObjectBuilder lumpSumBuilder = createObjectBuilder();

        if(nonNull(lumpSum.getAmount())) {
            lumpSumBuilder.add(AMOUNT, lumpSum.getAmount());
        }
        if(nonNull(lumpSum.getWithinDays())) {
            lumpSumBuilder.add(WITHIN_DAYS, lumpSum.getWithinDays());
        }
        if(nonNull(lumpSum.getPayByDate())) {
            lumpSumBuilder.add(PAY_BY_DATE, lumpSum.getPayByDate().toString());
        }
        return lumpSumBuilder;
    }
}
