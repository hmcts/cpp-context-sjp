package uk.gov.moj.cpp.sjp.query.view.response.courtextract;

import static com.google.common.collect.ImmutableSortedMap.of;
import static java.lang.String.*;
import static java.math.BigDecimal.*;
import static java.text.NumberFormat.*;
import static java.time.format.DateTimeFormatter.*;
import static java.util.Objects.*;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.ABSOLUTE;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType.*;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.ReasonForDeductingFromBenefits.*;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.ReasonForDeductingFromBenefits;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CostsAndSurcharge;
import uk.gov.moj.cpp.sjp.persistence.entity.DischargeOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialImposition;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialPenaltyOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.Installments;
import uk.gov.moj.cpp.sjp.persistence.entity.LumpSum;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.Payment;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

public class PaymentView {
    private String totalFine;
    private String totalToPay;
    private String paymentTerms;
    private String reserveTerms;
    private String prosecutionCosts;
    private String victimSurcharge;
    private String noVictimSurchargeReason;
    private final String reasonForNoCosts;
    private final boolean collectionOrderMade;
    private final String paymentMethod;
    private String paymentMethodReason;
    private final String reasonForReducedVictimSurcharge;
    private String totalCompensation;
    private String totalBackDuty;
    private String totalExcisePenalty;

    private final CaseDetail caseDetail;

    private static Predicate<BigDecimal> isGreaterThanZero = number -> nonNull(number) && number.compareTo(ZERO) > 0;

    private static final Map<PaymentType, String> PAYMENT_NAMES = of(
            PAY_TO_COURT, "Pay directly to court",
            ATTACH_TO_EARNINGS, "Attach to earnings",
            DEDUCT_FROM_BENEFITS, "Deduct from benefits"
    );

    private static final Map<ReasonForDeductingFromBenefits, String> REASON_FOR_DEDUCTING_FROM_BENEFITS_NAMES = of(
            COMPENSATION_ORDERED, "Compensation Ordered",
            DEFENDANT_KNOWN_DEFAULTER, "Defendant is a known defaulter",
            DEFENDANT_REQUESTED, "Defendant requested this"
    );

    private PaymentView(final CaseDetail caseDetail) {
        this.caseDetail = caseDetail;
        setTotalFine();
        setTotalCompensation();
        setTotalBackDuty();
        setTotalExcisePenalty();
        setTotalToPay();

        final FinancialImposition financialImposition = extractFinancialImposition();
        final CostsAndSurcharge costsAndSurcharge = financialImposition.getCostsAndSurcharge();
        setVictimSurcharge(costsAndSurcharge);
        setProsecutionCosts(costsAndSurcharge);
        reasonForNoCosts = costsAndSurcharge.getReasonForNoCosts();
        collectionOrderMade = costsAndSurcharge.isCollectionOrderMade();
        reasonForReducedVictimSurcharge = costsAndSurcharge.getReasonForReducedVictimSurcharge();

        final Payment payment = financialImposition.getPayment();
        paymentMethod = PAYMENT_NAMES.get(payment.getPaymentType());
        setPaymentMethodReason(payment);
        setTerms(payment);
    }

    private void setTotalToPay() {
        final BigDecimal nTotalToPay = calcTotalToPay();
        if (isGreaterThanZero.test(nTotalToPay)) {
            this.totalToPay = formatCurrency(nTotalToPay);
        }
    }

    private void setProsecutionCosts(CostsAndSurcharge costsAndSurcharge) {
        if (isGreaterThanZero.test(costsAndSurcharge.getCosts())) {
            prosecutionCosts = formatCurrency(costsAndSurcharge.getCosts());
        }
    }

    private void setVictimSurcharge(final CostsAndSurcharge costsAndSurcharge) {
        final BigDecimal nVictimSurcharge = calcVictimSurcharge();
        if (isGreaterThanZero.test(nVictimSurcharge)) {
            victimSurcharge = formatCurrency(nVictimSurcharge);
        } else {
            noVictimSurchargeReason = getNoVictimSurchargeReason(costsAndSurcharge);
        }
    }

    private void setPaymentMethodReason(Payment payment) {
        paymentMethodReason = payment.getPaymentType() == PAY_TO_COURT ? payment.getReasonWhyNotAttachedOrDeducted() :
                REASON_FOR_DEDUCTING_FROM_BENEFITS_NAMES.get(payment.getReasonForDeductingFromBenefits());
    }

    private void setTerms(Payment payment) {
        if (payment.getPaymentType() == PAY_TO_COURT) {
            paymentTerms = buildPaymentTerms(payment);
            reserveTerms = null;
        } else {
            paymentTerms = null;
            reserveTerms = buildPaymentTerms(payment);
        }
    }

    private void setTotalExcisePenalty() {
        final BigDecimal nTotalExcisePenalty = calcTotalExcisePenalty();
        if (isGreaterThanZero.test(nTotalExcisePenalty)) {
            totalExcisePenalty = formatCurrency(nTotalExcisePenalty);
        }
    }

    private void setTotalBackDuty() {
        final BigDecimal nTotalBackDuty = calcTotalBackDuty();
        if (isGreaterThanZero.test(nTotalBackDuty)) {
            totalBackDuty = formatCurrency(nTotalBackDuty);
        }
    }

    private void setTotalCompensation() {
        final BigDecimal nTotalCompensation = calcTotalCompensation();
        if (isGreaterThanZero.test(nTotalCompensation)) {
            totalCompensation = formatCurrency(nTotalCompensation);
        }
    }

    private void setTotalFine() {
        final BigDecimal nTotalFine = calcTotalFine();
        if (isGreaterThanZero.test(nTotalFine)) {
            totalFine = formatCurrency(nTotalFine);
        }
    }

    private String getNoVictimSurchargeReason(final CostsAndSurcharge costsAndSurcharge) {
        return allAbsoluteDischarge() ? "Absolute Discharge" : costsAndSurcharge.getReasonForNoVictimSurcharge();
    }

    private String buildPaymentTerms(Payment payment) {
        final LumpSum lumpSum = payment.getPaymentTerms().getLumpSum();
        final Installments instalments = payment.getPaymentTerms().getInstallments();

        if (nonNull(lumpSum) && nonNull(lumpSum.getWithinDays()) && lumpSum.getWithinDays() > 0) {
            if (payment.getPaymentType() == PAY_TO_COURT) {
                return format("To be paid as a lump sum in %d days", lumpSum.getWithinDays());
            } else {
                return format("Outstanding balance to be paid as a Lump sum within %d days", lumpSum.getWithinDays());
            }
        } else {
            if (nonNull(lumpSum) && lumpSum.getAmount().compareTo(ZERO) > 0) {
                return format("A lump sum of %s to be paid, followed by instalments of %s paid %s starting on %s",
                        formatCurrency(lumpSum.getAmount()),
                        formatCurrency(instalments.getAmount()),
                        instalments.getPeriod().name().toLowerCase(),
                        ofPattern("d MMMM yyyy").format(instalments.getStartDate()));
            } else {
                return format("Instalments of %s to be paid %s starting on %s",
                        formatCurrency(instalments.getAmount()),
                        instalments.getPeriod().name().toLowerCase(),
                        ofPattern("d MMMM yyyy").format(instalments.getStartDate()));
            }
        }
    }

    public static PaymentView getPayment(final CaseDetail caseDetail) {
        final boolean containsFinancialImposition = caseDetail
                .getCaseDecisions()
                .stream()
                .anyMatch(a -> nonNull(a.getFinancialImposition()));

        if (containsFinancialImposition) {
            return new PaymentView(caseDetail);
        } else {
            return null;
        }
    }

    private BigDecimal calcTotalToPay() {
        final BigDecimal costs = caseDetail
                .getCaseDecisions()
                .stream()
                .filter(a -> nonNull(a.getFinancialImposition()))
                .map(a -> a.getFinancialImposition().getCostsAndSurcharge().getCosts())
                .reduce(ZERO, BigDecimal::add);

        final BigDecimal compensationSum = caseDetail
                .getCaseDecisions()
                .stream()
                .flatMap(a -> a.getOffenceDecisions().stream())
                .map(this::getCompensation)
                .reduce(ZERO, BigDecimal::add);

        return calcTotalFine()
                .add(calcTotalBackDuty())
                .add(calcTotalExcisePenalty())
                .add(compensationSum)
                .add(costs)
                .add(calcVictimSurcharge());
    }

    private BigDecimal getCompensation(OffenceDecision a) {
        if (a instanceof DischargeOffenceDecision) {
            return ((DischargeOffenceDecision) a).getCompensation();
        }
        if (a instanceof FinancialPenaltyOffenceDecision) {
            return ((FinancialPenaltyOffenceDecision) a).getCompensation();
        }
        return ZERO;
    }

    private BigDecimal calcTotalFine() {
        return caseDetail
                .getCaseDecisions()
                .stream()
                .flatMap(a -> a.getOffenceDecisions().stream())
                .filter(a -> a instanceof FinancialPenaltyOffenceDecision)
                .map(a -> (FinancialPenaltyOffenceDecision) a)
                .filter(decision -> nonNull(decision.getFine()))
                .map(FinancialPenaltyOffenceDecision::getFine)
                .reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal calcTotalExcisePenalty() {
        return caseDetail
                .getCaseDecisions()
                .stream()
                .flatMap(a -> a.getOffenceDecisions().stream())
                .filter(a -> a instanceof FinancialPenaltyOffenceDecision)
                .map(a -> (FinancialPenaltyOffenceDecision) a)
                .filter(decision -> nonNull(decision.getExcisePenalty()))
                .map(FinancialPenaltyOffenceDecision::getExcisePenalty)
                .reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal calcTotalBackDutyForFinancialPenalty() {
        return caseDetail
                .getCaseDecisions()
                .stream()
                .flatMap(a -> a.getOffenceDecisions().stream())
                .filter(a -> a instanceof FinancialPenaltyOffenceDecision)
                .map(a -> (FinancialPenaltyOffenceDecision) a)
                .filter(decision -> nonNull(decision.getBackDuty()))
                .map(FinancialPenaltyOffenceDecision::getBackDuty)
                .reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal calcTotalBackDutyForDischarge() {
        return caseDetail
                .getCaseDecisions()
                .stream()
                .flatMap(a -> a.getOffenceDecisions().stream())
                .filter(a -> a instanceof DischargeOffenceDecision)
                .map(a -> (DischargeOffenceDecision) a)
                .filter(decision -> nonNull(decision.getBackDuty()))
                .map(DischargeOffenceDecision::getBackDuty)
                .reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal calcVictimSurcharge() {
        return caseDetail
                .getCaseDecisions()
                .stream()
                .filter(a -> nonNull(a.getFinancialImposition()))
                .filter(a -> nonNull(a.getFinancialImposition().getCostsAndSurcharge().getVictimSurcharge()))
                .map(a -> a.getFinancialImposition().getCostsAndSurcharge().getVictimSurcharge())
                .reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal calcTotalBackDuty() {
        return calcTotalBackDutyForFinancialPenalty().add(calcTotalBackDutyForDischarge());
    }

    private BigDecimal calcTotalCompensation() {
        return calcFinancialPenaltyTotalCompensation().add(calcDischargeTotalCompensation());
    }

    private BigDecimal calcFinancialPenaltyTotalCompensation() {
        return caseDetail
                .getCaseDecisions()
                .stream()
                .flatMap(a -> a.getOffenceDecisions().stream())
                .filter(a -> a instanceof FinancialPenaltyOffenceDecision)
                .map(a -> (FinancialPenaltyOffenceDecision) a)
                .map(FinancialPenaltyOffenceDecision::getCompensation)
                .reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal calcDischargeTotalCompensation() {
        return caseDetail
                .getCaseDecisions()
                .stream()
                .flatMap(a -> a.getOffenceDecisions().stream())
                .filter(a -> a instanceof DischargeOffenceDecision)
                .map(a -> (DischargeOffenceDecision) a)
                .map(DischargeOffenceDecision::getCompensation)
                .reduce(ZERO, BigDecimal::add);
    }

    private FinancialImposition extractFinancialImposition() {
        return caseDetail
                .getCaseDecisions()
                .stream()
                .map(CaseDecision::getFinancialImposition)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("FinancialImposition cannot be null"));
    }

    private boolean allAbsoluteDischarge() {
        if (hasFinancialPenalty()) {
            return false;
        }
        return caseDetail
                .getCaseDecisions()
                .stream()
                .flatMap(a -> a.getOffenceDecisions().stream())
                .filter(a -> (a instanceof DischargeOffenceDecision))
                .allMatch(a -> ((DischargeOffenceDecision) a).getDischargeType() == ABSOLUTE);
    }
    private boolean hasFinancialPenalty() {
        return caseDetail
                .getCaseDecisions()
                .stream()
                .flatMap(a -> a.getOffenceDecisions().stream())
                .anyMatch(a -> a instanceof FinancialPenaltyOffenceDecision);
    }

    private String formatCurrency(final BigDecimal value) {
        return getCurrencyInstance(Locale.UK).format(value).replace(".00", "");
    }

    public String getTotalFine() {
        return totalFine;
    }

    public String getTotalToPay() {
        return totalToPay;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public String getReserveTerms() {
        return reserveTerms;
    }

    public String getProsecutionCosts() {
        return prosecutionCosts;
    }

    public String getVictimSurcharge() {
        return victimSurcharge;
    }

    public String getNoVictimSurchargeReason() {
        return noVictimSurchargeReason;
    }

    public String getReasonForNoCosts() {
        return reasonForNoCosts;
    }

    public boolean isCollectionOrderMade() {
        return collectionOrderMade;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getPaymentMethodReason() {
        return paymentMethodReason;
    }

    public  String getReasonForReducedVictimSurcharge() {
        return reasonForReducedVictimSurcharge;
    }

    public String getTotalCompensation() {
        return totalCompensation;
    }

    public String getTotalBackDuty() {
        return totalBackDuty;
    }

    public String getTotalExcisePenalty() {
        return totalExcisePenalty;
    }

    public static Map<PaymentType, String> getPaymentNames() {
        return PAYMENT_NAMES;
    }
}
