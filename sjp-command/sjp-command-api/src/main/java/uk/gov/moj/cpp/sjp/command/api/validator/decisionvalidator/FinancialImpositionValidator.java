package uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator;

import static java.math.BigDecimal.ZERO;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator.CommonDecisionValidator.getFinancialPenaltyValue;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISCHARGE;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.FINANCIAL_PENALTY;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType.DEDUCT_FROM_BENEFITS;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType.PAY_TO_COURT;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import javax.json.JsonObject;

public class FinancialImpositionValidator {
    private static final String PAYMENT = "payment";
    private static final String PAYMENT_TERMS = "paymentTerms";

    private FinancialImpositionValidator() {
    }

    public static void validateFinancialImposition(final JsonObject decision, final boolean allOffencesAreExcisePenalty) {
        final List<JsonObject> offenceDecisions = decision.getJsonArray("offenceDecisions").getValuesAs(JsonObject.class);
        if(financialImpositionRequired(offenceDecisions)) {
            final JsonObject financialImposition = ofNullable(decision.getJsonObject("financialImposition"))
                    .orElseThrow(() -> new BadRequestException("Financial imposition is required for discharge or financial penalty decisions"));

            final boolean thereExistsConditionalDischarge = offenceDecisions.stream()
                    .filter(offenceDecision -> DISCHARGE.equals(DecisionType.valueOf(offenceDecision.getString("type"))))
                    .anyMatch(DischargeDecisionValidator::isConditionalDischarge);

            validateCostsAndSurcharge(financialImposition.getJsonObject("costsAndSurcharge"), thereExistsConditionalDischarge, allOffencesAreExcisePenalty);

            validatePayment(financialImposition.getJsonObject(PAYMENT));
        }
    }

    private static void validatePayment(final JsonObject payment) {
        validatePaymentTypeReasons(payment);
        validatePaymentTerms(payment);
    }

    private static void validateCostsAndSurcharge(final JsonObject payment, final boolean thereExistsConditionalDischarge, final boolean allOffencesAreExcisePenalty) {
        if(!allOffencesAreExcisePenalty) {
            validateVictimSurcharge(payment, thereExistsConditionalDischarge);
        }
    }

    private static void validateVictimSurcharge(final JsonObject payment, final boolean thereExistsConditionalDischarge) {
        if (thereExistsConditionalDischarge && payment.getJsonNumber("victimSurcharge").bigDecimalValue().equals(ZERO) &&
                isBlank(payment.getString("reasonForNoVictimSurcharge", null))) {
            throw new BadRequestException("reasonForNoVictimSurcharge is required");
        }
    }

    private static void validatePaymentTypeReasons(final JsonObject payment) {
        final PaymentType paymentType = PaymentType.valueOf(payment.getString("paymentType"));
        if (paymentType == PAY_TO_COURT && isBlank(payment.getString("reasonWhyNotAttachedOrDeducted", null))) {
            throw new BadRequestException("reasonWhyNotAttachedOrDeducted is required if paymentType is pay to court");
        } else if (paymentType == DEDUCT_FROM_BENEFITS && isBlank(payment.getString("reasonForDeductingFromBenefits", null))) {
            throw new BadRequestException("reasonForDeductingFromBenefits is required if paymentType is deduct from benefits");
        }
    }

    private static void validatePaymentTerms(final JsonObject payment) {
        validateReserveTerms(payment);
        final Optional<JsonObject> lumpSum = ofNullable(payment.getJsonObject(PAYMENT_TERMS).getJsonObject("lumpSum"));
        final Optional<JsonObject> installments = ofNullable(payment.getJsonObject(PAYMENT_TERMS).getJsonObject("installments"));
        if (!lumpSum.isPresent() && !installments.isPresent()) {
            throw new BadRequestException("Either lump sum or installments is required");
        }
        if (lumpSum.isPresent() && !installments.isPresent()) {
            validateLumpSum(lumpSum.get());
        }
    }

    private static void validateLumpSum(final JsonObject lumpSum) {
        if (!lumpSum.containsKey("withinDays") && !lumpSum.containsKey("payByDate")) {
            throw new BadRequestException("Either withinDays or payByDate is required");
        }
    }

    private static void validateReserveTerms(final JsonObject payment) {
        final PaymentType paymentType = PaymentType.valueOf(payment.getString("paymentType"));
        if (paymentType == PAY_TO_COURT && payment.getJsonObject(PAYMENT_TERMS).getBoolean("reserveTerms")) {
            throw new BadRequestException("reserveTerms must be false if paymentType is pay to court");
        } else if (paymentType == DEDUCT_FROM_BENEFITS && !payment.getJsonObject(PAYMENT_TERMS).getBoolean("reserveTerms")) {
            throw new BadRequestException("reserveTerms must be true if paymentType is deduct from benefits");
        }
    }

    private static boolean financialImpositionRequired(final List<JsonObject> offenceDecisions) {
        return offenceDecisions.stream().anyMatch(FinancialImpositionValidator::validFinancialImposition);
    }

    private static boolean validFinancialImposition(final JsonObject offenceDecision) {
        final boolean financialOffence = asList(DISCHARGE, FINANCIAL_PENALTY)
                .contains(DecisionType.valueOf(offenceDecision.getString("type")));

        if(financialOffence) {
            final BigDecimal backDuty = getFinancialPenaltyValue(offenceDecision, "backDuty").orElse(ZERO);
            final BigDecimal excisePenalty = getFinancialPenaltyValue(offenceDecision, "excisePenalty").orElse(ZERO);
            final BigDecimal compensation = getFinancialPenaltyValue(offenceDecision, "compensation").orElse(ZERO);
            final BigDecimal fine = getFinancialPenaltyValue(offenceDecision, "fine").orElse(ZERO);

            return backDuty.add(excisePenalty).add(compensation).add(fine).compareTo(ZERO) > 0;
        }
        return false;
    }
}
