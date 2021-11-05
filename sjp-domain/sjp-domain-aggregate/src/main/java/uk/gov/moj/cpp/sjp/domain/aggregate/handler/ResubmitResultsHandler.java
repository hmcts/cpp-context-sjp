package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.FinancialImposition;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Installments;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.LumpSum;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Payment;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentTerms;
import uk.gov.moj.cpp.sjp.domain.decision.resubmit.PaymentTermsInfo;
import uk.gov.moj.cpp.sjp.event.PaymentTermsChanged;
import uk.gov.moj.cpp.sjp.event.decision.DecisionResubmitted;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import javax.json.JsonObject;

@SuppressWarnings({"squid:S1698","squid:S2259"})
public class ResubmitResultsHandler {

    public static final ResubmitResultsHandler INSTANCE = new ResubmitResultsHandler();
    public static final String PAYMENT_TERMS_INFO = "paymentTermsInfo";
    public static final String NUMBER_OF_DAYS_TO_POSTPONE_BY = "numberOfDaysToPostponeBy";

    private ResubmitResultsHandler() {
    }

    public Stream<Object> resubmitResults(final JsonObject  payload,
                                          final CaseAggregateState state) {

        final Integer numberOfDaysToPostponeBy = getDaysToPostponeBy(payload);
        PaymentTermsInfo paymentTermsInfo = null;

        // get the latest decision
        final DecisionSaved decisionSaved = state.getDecisionSavedWithFinancialImposition();
        final Stream.Builder<Object> streamBuilder = Stream.builder();

        if (paymentPresent(decisionSaved)
                && isNull(state.getDefendantFinancialImpositionExportDetails(state.getDefendantId()))) {

            final FinancialImposition financialImposition = decisionSaved.getFinancialImposition();
            Payment newPayment = financialImposition.getPayment();

            if (numberOfDaysToPostponeBy != null) {
                newPayment = extendLumpSumPayByDate(numberOfDaysToPostponeBy,
                        extendInstallmentStartDate(numberOfDaysToPostponeBy, financialImposition.getPayment()));
                paymentTermsInfo = new PaymentTermsInfo(numberOfDaysToPostponeBy);
            }

            streamBuilder.add(new DecisionResubmitted(new DecisionSaved(randomUUID(),
                    decisionSaved.getSessionId(),
                    decisionSaved.getCaseId(),
                    decisionSaved.getSavedAt(),
                    decisionSaved.getOffenceDecisions(),
                    new FinancialImposition(financialImposition.getCostsAndSurcharge(), newPayment)
            ), ZonedDateTime.now(), paymentTermsInfo));

            if (newPayment != financialImposition.getPayment()) {
                streamBuilder.add(new PaymentTermsChanged(financialImposition.getPayment().getPaymentTerms(), newPayment.getPaymentTerms()));
            }

        }

        return streamBuilder.build();
    }

    private Payment extendInstallmentStartDate(final Integer numberOfDaysToPostponeBy,
                                               final Payment payment) {

        if (nonNull(payment)
                && nonNull(numberOfDaysToPostponeBy)
                && nonNull(payment.getPaymentTerms())
                && nonNull(payment.getPaymentTerms().getInstallments())
                && nonNull(payment.getPaymentTerms().getInstallments().getStartDate())) {
            final LocalDate newStartDate =
                    payment
                            .getPaymentTerms()
                            .getInstallments()
                            .getStartDate()
                            .plusDays(numberOfDaysToPostponeBy);

            final PaymentTerms paymentTerms = payment.getPaymentTerms();
            final Installments installments = payment.getPaymentTerms().getInstallments();

            return new Payment(
                    payment.getTotalSum(),
                    payment.getPaymentType(),
                    payment.getReasonWhyNotAttachedOrDeducted(),
                    payment.getReasonForDeductingFromBenefits(),
                    new PaymentTerms(
                            paymentTerms.isReserveTerms(),
                            paymentTerms.getLumpSum(),
                            new Installments(
                                    installments.getAmount(),
                                    installments.getPeriod(),
                                    newStartDate)), payment.getFineTransferredTo());
        }

        return payment;
    }

    private Payment extendLumpSumPayByDate(final Integer numberOfDaysToPostponeBy, final Payment payment) {

        if (nonNull(payment)
                && nonNull(numberOfDaysToPostponeBy)
                && nonNull(payment.getPaymentTerms())
                && nonNull(payment.getPaymentTerms().getLumpSum())
                && nonNull(payment.getPaymentTerms().getLumpSum().getPayByDate())) { // on checking the production we don't have the payByDate populated on any event

            final LocalDate newPayByDate =
                    ofNullable(payment.getPaymentTerms().getLumpSum().getPayByDate())
                            .map(e -> e.plusDays((numberOfDaysToPostponeBy)))
                            .orElse(null);

            final PaymentTerms paymentTerms = payment.getPaymentTerms();
            final LumpSum lumpSum = payment.getPaymentTerms().getLumpSum();

            return new Payment(
                    payment.getTotalSum(),
                    payment.getPaymentType(),
                    payment.getReasonWhyNotAttachedOrDeducted(),
                    payment.getReasonForDeductingFromBenefits(),
                    new PaymentTerms(
                            paymentTerms.isReserveTerms(),
                            new LumpSum(lumpSum.getAmount(), payment.getPaymentTerms().getLumpSum().getWithinDays(), newPayByDate),
                            paymentTerms.getInstallments()), payment.getFineTransferredTo());

        }

        return payment;
    }

    private boolean paymentPresent(final DecisionSaved decisionSaved) {
        return nonNull(decisionSaved)
                && nonNull(decisionSaved.getFinancialImposition())
                && nonNull(decisionSaved.getFinancialImposition().getPayment());
    }

    private Integer getDaysToPostponeBy(final JsonObject payload) {
        return payload.getJsonObject(PAYMENT_TERMS_INFO) != null
                && payload.getJsonObject(PAYMENT_TERMS_INFO).containsKey(NUMBER_OF_DAYS_TO_POSTPONE_BY)
                ? payload.getJsonObject(PAYMENT_TERMS_INFO).getInt(NUMBER_OF_DAYS_TO_POSTPONE_BY) : null;
    }

}
