package uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISCHARGE;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.FINANCIAL_PENALTY;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.CONDITIONAL;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType.DEDUCT_FROM_BENEFITS;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType.PAY_TO_COURT;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType;

import java.math.BigDecimal;

import static java.util.Arrays.asList;

import java.util.List;
import java.util.Optional;

import javax.json.JsonObject;

public class FinancialImpositionValidator {
    private static final String PAYMENT = "payment";
    private static final String PAYMENT_TERMS = "paymentTerms";

    private FinancialImpositionValidator() {
    }

    public static void validateFinancialImposition(final JsonObject decision) {
        new FinancialImpositionValidator().innerValidateFinancialImposition(decision);
    }

    private void innerValidateFinancialImposition(final JsonObject decision) {
        if (!financialImpositionRequired(decision))
        {
            return;
        }

        final JsonObject financialImposition = ofNullable(decision.getJsonObject("financialImposition"))
                .orElseThrow(() -> new BadRequestException("Financial imposition is required for discharge or financial penalty decisions"));
        final List<JsonObject> offenceDecisions = decision.getJsonArray("offenceDecisions").getValuesAs(JsonObject.class);
        final boolean thereExistsConditionalDischarge = offenceDecisions
                .stream()
                .filter(offenceDecision -> DISCHARGE == DecisionType.valueOf(offenceDecision.getString("type")))
                .anyMatch(offenceDecision -> DischargeType.valueOf(offenceDecision.getString("dischargeType")) == CONDITIONAL);
        validateCostsAndSurcharge(financialImposition.getJsonObject("costsAndSurcharge"), thereExistsConditionalDischarge);
        validatePayment(financialImposition.getJsonObject(PAYMENT));
    }

    private void validatePayment(final JsonObject payment) {
        validatePaymentTypeReasons(payment);
        validatePaymentTerms(payment);
    }

    private void validateCostsAndSurcharge(final JsonObject payment, final boolean thereExistsConditionalDischarge) {
        validateCosts(payment);
        validateVictimSurcharge(payment, thereExistsConditionalDischarge);
    }

    private void validateVictimSurcharge(final JsonObject payment, final boolean thereExistsConditionalDischarge) {
        if (thereExistsConditionalDischarge && payment.getJsonNumber("victimSurcharge").bigDecimalValue().equals(BigDecimal.ZERO) &&
                isBlank(payment.getString("reasonForNoVictimSurcharge", null))) {
            throw new BadRequestException("reasonForNoVictimSurcharge is required");
        }
    }

    private void validateCosts(final JsonObject payment) {
        if (payment.getJsonNumber("costs").bigDecimalValue().equals(BigDecimal.ZERO) &&
                isBlank(payment.getString("reasonForNoCosts", null))) {
            throw new BadRequestException("reasonForNoCosts is required");
        }
    }

    private void validatePaymentTypeReasons(final JsonObject payment) {
        final PaymentType paymentType = PaymentType.valueOf(payment.getString("paymentType"));
        if (paymentType == PAY_TO_COURT && isBlank(payment.getString("reasonWhyNotAttachedOrDeducted", null))) {
            throw new BadRequestException("reasonWhyNotAttachedOrDeducted is required if paymentType is pay to court");
        } else if (paymentType == DEDUCT_FROM_BENEFITS && isBlank(payment.getString("reasonForDeductingFromBenefits", null))) {
            throw new BadRequestException("reasonForDeductingFromBenefits is required if paymentType is deduct from benefits");
        }
    }

    private void validatePaymentTerms(final JsonObject payment) {
        validateReserveTerms(payment);
        final Optional<JsonObject> lumpSum = ofNullable(payment.getJsonObject(PAYMENT_TERMS).getJsonObject("lumpSum"));
        final Optional<JsonObject> installments = ofNullable(payment.getJsonObject(PAYMENT_TERMS).getJsonObject("installments"));
        if (!lumpSum.isPresent() && !installments.isPresent()) {
            throw new BadRequestException("Either lump sum or installments is required");
        }
        if (lumpSum.isPresent() && !installments.isPresent()) {
            this.validateLumpSum(lumpSum.get());
        }
    }

    private void validateLumpSum(final JsonObject lumpSum) {
        if (!lumpSum.containsKey("withinDays") && !lumpSum.containsKey("payByDate")) {
            throw new BadRequestException("Either withinDays or payByDate is required");
        }
    }

    private void validateReserveTerms(final JsonObject payment) {
        final PaymentType paymentType = PaymentType.valueOf(payment.getString("paymentType"));
        if (paymentType == PAY_TO_COURT && payment.getJsonObject(PAYMENT_TERMS).getBoolean("reserveTerms")) {
            throw new BadRequestException("reserveTerms must be false if paymentType is pay to court");
        } else if (paymentType == DEDUCT_FROM_BENEFITS && !payment.getJsonObject(PAYMENT_TERMS).getBoolean("reserveTerms")) {
            throw new BadRequestException("reserveTerms must be true if paymentType is deduct from benefits");
        }
    }

    private boolean financialImpositionRequired(final JsonObject decision) {
        return decision.getJsonArray("offenceDecisions").getValuesAs(JsonObject.class)
                .stream()
                .anyMatch(offenceDecision -> asList(DISCHARGE, FINANCIAL_PENALTY)
                        .contains(DecisionType.valueOf(offenceDecision.getString("type"))));
    }
}
