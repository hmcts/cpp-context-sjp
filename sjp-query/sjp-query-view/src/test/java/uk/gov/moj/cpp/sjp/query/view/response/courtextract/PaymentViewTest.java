package uk.gov.moj.cpp.sjp.query.view.response.courtextract;

import static java.time.ZonedDateTime.now;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.ABSOLUTE;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.CONDITIONAL;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.PeriodUnit.WEEK;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.InstallmentPeriod.WEEKLY;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType.ATTACH_TO_EARNINGS;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType.PAY_TO_COURT;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.ReasonForDeductingFromBenefits.COMPENSATION_ORDERED;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder.aCase;
import static uk.gov.moj.cpp.sjp.persistence.builder.DefendantDetailBuilder.aDefendantDetail;

import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CostsAndSurcharge;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DischargeOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.DischargePeriod;
import uk.gov.moj.cpp.sjp.persistence.entity.DismissOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialImposition;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialPenaltyOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.Installments;
import uk.gov.moj.cpp.sjp.persistence.entity.LumpSum;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.Payment;
import uk.gov.moj.cpp.sjp.persistence.entity.PaymentTerms;
import uk.gov.moj.cpp.sjp.persistence.entity.Session;
import uk.gov.moj.cpp.sjp.persistence.entity.WithdrawOffenceDecision;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.Test;


public class PaymentViewTest {

    private final UUID caseId = randomUUID();
    private final UUID offence1Id = randomUUID();
    private static final String MAGISTRATE_NAME = "Legal adviser name";


    @Test
    public void shouldVerifyWithFinancialImpositionWithFinancialPenaltyAndDischargeAndExcisePenaltyDecisions() {
        final CaseDetail caseDetail = buildCaseDetailWithOneOffence();
        final CaseDecision caseDecision = buildCaseDecisionEntity(caseDetail.getId(), false, now());
        caseDecision.setOffenceDecisions(asList(
                new DischargeOffenceDecision(offence1Id,
                        caseDecision.getId(),
                        FOUND_GUILTY,
                        new DischargePeriod(WEEK, 10),
                        true,
                        BigDecimal.valueOf(1000),
                        "Limited means of defendant",
                        CONDITIONAL, BigDecimal.valueOf(1000), null),
                new FinancialPenaltyOffenceDecision(offence1Id,
                        caseDecision.getId(),
                        FOUND_GUILTY,
                        true,
                        BigDecimal.valueOf(1500),
                        null,
                        BigDecimal.valueOf(2000.600), null, null, null, null),
                new FinancialPenaltyOffenceDecision(offence1Id,
                        caseDecision.getId(),
                        FOUND_GUILTY,
                        true,
                        BigDecimal.valueOf(1000),
                        null,
                        null, BigDecimal.valueOf(523), BigDecimal.valueOf(321), null, null),
                new FinancialPenaltyOffenceDecision(offence1Id,
                        caseDecision.getId(),
                        FOUND_GUILTY,
                        true,
                        BigDecimal.valueOf(1000),
                        null,
                        null, null, BigDecimal.valueOf(319.31), null, null)));

        caseDecision.setFinancialImposition(buildFinancialImposition(PAY_TO_COURT));
        caseDetail.setCaseDecisions(asList(caseDecision));

        PaymentView payment = PaymentView.getPayment(caseDetail);
        assertEquals("£9,164.14", payment.getTotalToPay());
        assertEquals("£2,000.60", payment.getTotalFine());
        assertNull(payment.getNoVictimSurchargeReason());
        assertNull(payment.getReasonForReducedVictimSurcharge());
        assertEquals("Pay directly to court", payment.getPaymentMethod());
        assertEquals("some reason", payment.getPaymentMethodReason());
        assertEquals("£200.23", payment.getProsecutionCosts());
        assertNull(payment.getReasonForNoCosts());
        assertEquals("To be paid as a lump sum in 20 days", payment.getPaymentTerms());
        assertNull(payment.getReserveTerms());
        assertEquals("£300", payment.getVictimSurcharge());
        assertEquals("£4,500", payment.getTotalCompensation());
        assertEquals("£640.31", payment.getTotalExcisePenalty());
        assertEquals("£1,523", payment.getTotalBackDuty());
    }

    @Test
    public void shouldVerifyWithFinancialImpositionWithDischargeDecisionsWithAbsoluteDischarge() {
        final CaseDetail caseDetail = buildCaseDetailWithOneOffence();
        final CaseDecision caseDecision = buildCaseDecisionEntity(caseDetail.getId(), false, now());
        caseDecision.setOffenceDecisions(asList(
                new DischargeOffenceDecision(offence1Id,
                        caseDecision.getId(),
                        FOUND_GUILTY,
                        null,
                        true,
                        BigDecimal.valueOf(1000),
                        null,
                        ABSOLUTE, BigDecimal.valueOf(500), null),
                new DischargeOffenceDecision(offence1Id,
                        caseDecision.getId(),
                        FOUND_GUILTY,
                        null,
                        true,
                        BigDecimal.valueOf(1000),
                        null,
                        ABSOLUTE,BigDecimal.valueOf(700), null)));

        caseDecision.setFinancialImposition(buildFinancialImposition(PAY_TO_COURT));
        caseDetail.setCaseDecisions(asList(caseDecision));
        caseDecision.getFinancialImposition().getCostsAndSurcharge().setVictimSurcharge(BigDecimal.ZERO);

        verifyAllAbsoluteDischargePayment(caseDetail);
    }

    @Test
    public void shouldVerifyWithFinancialImpositionWithFinancialPenaltyAndDischargeDecisionsWithAttachToEarning() {
        final CaseDetail caseDetail = buildCaseDetailWithOneOffence();
        final CaseDecision caseDecision = buildCaseDecisionEntity(caseDetail.getId(), false, now());
        caseDecision.setOffenceDecisions(asList(
                new DischargeOffenceDecision(offence1Id,
                        caseDecision.getId(),
                        FOUND_GUILTY,
                        new DischargePeriod(WEEK, 10),
                        true,
                        BigDecimal.valueOf(1000),
                        "Limited means of defendant",
                        CONDITIONAL,null,null),
                new FinancialPenaltyOffenceDecision(offence1Id,
                        caseDecision.getId(),
                        FOUND_GUILTY,
                        true,
                        BigDecimal.valueOf(1500),
                        null,
                        BigDecimal.valueOf(2000.600), null, null, null, null)));

        caseDecision.setFinancialImposition(buildFinancialImposition(ATTACH_TO_EARNINGS));
        caseDetail.setCaseDecisions(asList(caseDecision));

        PaymentView payment = PaymentView.getPayment(caseDetail);
        assertEquals("£5,000.83", payment.getTotalToPay());
        assertEquals("£2,000.60", payment.getTotalFine());
        assertNull(payment.getNoVictimSurchargeReason());
        assertNull(payment.getReasonForReducedVictimSurcharge());
        assertEquals("Attach to earnings", payment.getPaymentMethod());
        assertEquals("Compensation Ordered", payment.getPaymentMethodReason());
        assertEquals("£200.23", payment.getProsecutionCosts());
        assertNull(payment.getReasonForNoCosts());
        assertNull(payment.getPaymentTerms());
        assertEquals("Outstanding balance to be paid as a Lump sum within 20 days", payment.getReserveTerms());
        assertEquals("£300", payment.getVictimSurcharge());
        assertEquals("£2,500", payment.getTotalCompensation());
    }

    @Test
    public void shouldVerifyWithFinancialImpositionWithInstalments() {
        final CaseDetail caseDetail = buildCaseDetailWithOneOffence();
        final CaseDecision caseDecision = buildCaseDecisionEntity(caseDetail.getId(), false, now());
        caseDecision.setOffenceDecisions(asList(
                new DischargeOffenceDecision(offence1Id,
                        caseDecision.getId(),
                        FOUND_GUILTY,
                        new DischargePeriod(WEEK, 10),
                        true,
                        BigDecimal.valueOf(1000),
                        "Limited means of defendant",
                        CONDITIONAL, null,null),
                new FinancialPenaltyOffenceDecision(offence1Id,
                        caseDecision.getId(),
                        FOUND_GUILTY,
                        true,
                        BigDecimal.valueOf(1500),
                        null,
                        BigDecimal.valueOf(2000.600), null, null, null, null)));

        caseDecision.setFinancialImposition(buildFinancialImposition(ATTACH_TO_EARNINGS, new LumpSum(BigDecimal.valueOf(30.34), 0, LocalDate.now())));
        caseDetail.setCaseDecisions(asList(caseDecision));

        PaymentView payment = PaymentView.getPayment(caseDetail);
        assertEquals("£5,000.83", payment.getTotalToPay());
        assertEquals("£2,000.60", payment.getTotalFine());
        assertNull(payment.getNoVictimSurchargeReason());
        assertNull(payment.getReasonForReducedVictimSurcharge());
        assertEquals("Attach to earnings", payment.getPaymentMethod());
        assertEquals("Compensation Ordered", payment.getPaymentMethodReason());
        assertEquals("£200.23", payment.getProsecutionCosts());
        assertNull(payment.getReasonForNoCosts());
        assertNull(payment.getPaymentTerms());
        assertEquals("A lump sum of £30.34 to be paid, followed by instalments of £40 paid weekly starting on 1 August 2019", payment.getReserveTerms());
        assertEquals("£300", payment.getVictimSurcharge());
        assertTrue(payment.isCollectionOrderMade());
        assertEquals("£2,500", payment.getTotalCompensation());
    }

    @Test
    public void shouldVerifyWithAllAbsoluteAndWithWithdrawalAndWithDischarge() {
        final CaseDetail caseDetail = buildCaseDetailWithOneOffence();
        final CaseDecision caseDecision = buildCaseDecisionEntity(caseDetail.getId(), false, now());
        caseDecision.setOffenceDecisions(asList(
                new DischargeOffenceDecision(offence1Id,
                        caseDecision.getId(),
                        FOUND_GUILTY,
                        null,
                        true,
                        BigDecimal.valueOf(1000),
                        null,
                        ABSOLUTE, BigDecimal.valueOf(500), null),
                new DischargeOffenceDecision(offence1Id,
                        caseDecision.getId(),
                        FOUND_GUILTY,
                        null,
                        true,
                        BigDecimal.valueOf(1000),
                        null,
                        ABSOLUTE, BigDecimal.valueOf(700), null),
                new WithdrawOffenceDecision(),
                new DismissOffenceDecision()));

        caseDecision.setFinancialImposition(buildFinancialImposition(PAY_TO_COURT));
        caseDetail.setCaseDecisions(asList(caseDecision));
        caseDecision.getFinancialImposition().getCostsAndSurcharge().setVictimSurcharge(BigDecimal.ZERO);

        verifyAllAbsoluteDischargePayment(caseDetail);
    }

    @Test
    public void shouldVerifyWithFinancialPenaltyAndWithAbsoluteDischargeAndWithWithdrawalAndWithDismissDecisions() {
        final CaseDetail caseDetail = buildCaseDetailWithOneOffence();
        final CaseDecision caseDecision = buildCaseDecisionEntity(caseDetail.getId(), false, now());
        caseDecision.setOffenceDecisions(asList(
                new DischargeOffenceDecision(offence1Id,
                        caseDecision.getId(),
                        FOUND_GUILTY,
                        null,
                        true,
                        BigDecimal.valueOf(1000),
                        "Limited means of defendant",
                        ABSOLUTE, null,null),
                new DischargeOffenceDecision(offence1Id,
                        caseDecision.getId(),
                        FOUND_GUILTY,
                        null,
                        true,
                        BigDecimal.valueOf(1000),
                        "Limited means of defendant",
                        ABSOLUTE, null, null),
                new FinancialPenaltyOffenceDecision(offence1Id,
                        caseDecision.getId(),
                        FOUND_GUILTY,
                        true,
                        BigDecimal.valueOf(1500),
                        null,
                        BigDecimal.valueOf(2000.600), null, null, null, null),
                new WithdrawOffenceDecision(),
                new DismissOffenceDecision()));

        caseDecision.setFinancialImposition(buildFinancialImposition(PAY_TO_COURT));
        caseDetail.setCaseDecisions(asList(caseDecision));

        PaymentView payment = PaymentView.getPayment(caseDetail);
        assertEquals("£6,000.83", payment.getTotalToPay());
        assertEquals("£2,000.60", payment.getTotalFine());
        assertNull(payment.getNoVictimSurchargeReason());
        assertNull(payment.getReasonForReducedVictimSurcharge());
        assertEquals("Pay directly to court", payment.getPaymentMethod());
        assertEquals("some reason", payment.getPaymentMethodReason());
        assertEquals("£200.23", payment.getProsecutionCosts());
        assertNull(payment.getReasonForNoCosts());
        assertEquals("To be paid as a lump sum in 20 days", payment.getPaymentTerms());
        assertNull(payment.getReserveTerms());
        assertEquals("£300", payment.getVictimSurcharge());
        assertEquals("£3,500", payment.getTotalCompensation());
    }

    @Test
    public void shouldVerifyWithFinancialPenaltyAndWithAbsoluteDischargeAndWithWithdrawalAndWithDismissDecisionsWithNullCompensation() {
        final CaseDetail caseDetail = buildCaseDetailWithOneOffence();
        final CaseDecision caseDecision = buildCaseDecisionEntity(caseDetail.getId(), false, now());
        caseDecision.setOffenceDecisions(asList(
                new DischargeOffenceDecision(offence1Id,
                        caseDecision.getId(),
                        FOUND_GUILTY,
                        null,
                        true,
                        null,
                        "Limited means of defendant",
                        ABSOLUTE, null,null),
                new DischargeOffenceDecision(offence1Id,
                        caseDecision.getId(),
                        FOUND_GUILTY,
                        null,
                        true,
                        null,
                        "Limited means of defendant",
                        ABSOLUTE, null, null),
                new FinancialPenaltyOffenceDecision(offence1Id,
                        caseDecision.getId(),
                        FOUND_GUILTY,
                        true,
                        null,
                        null,
                        BigDecimal.valueOf(2000.600), null, null, null, null),
                new WithdrawOffenceDecision(),
                new DismissOffenceDecision()));

        caseDecision.setFinancialImposition(buildFinancialImposition(PAY_TO_COURT));
        caseDetail.setCaseDecisions(asList(caseDecision));

        PaymentView payment = PaymentView.getPayment(caseDetail);
        assertEquals("£2,500.83", payment.getTotalToPay());
        assertEquals("£2,000.60", payment.getTotalFine());
        assertNull(payment.getNoVictimSurchargeReason());
        assertNull(payment.getReasonForReducedVictimSurcharge());
        assertEquals("Pay directly to court", payment.getPaymentMethod());
        assertEquals("some reason", payment.getPaymentMethodReason());
        assertEquals("£200.23", payment.getProsecutionCosts());
        assertNull(payment.getReasonForNoCosts());
        assertEquals("To be paid as a lump sum in 20 days", payment.getPaymentTerms());
        assertNull(payment.getReserveTerms());
        assertEquals("£300", payment.getVictimSurcharge());
        assertEquals(null, payment.getTotalCompensation());
    }

    @Test
    public void shouldSendTotalToPayNullWhenZero() {
        final CaseDetail caseDetail = buildCaseDetailWithOneOffence();
        final CaseDecision caseDecision = buildCaseDecisionEntity(caseDetail.getId(), false, now());

        caseDecision.setOffenceDecisions(asList(
                new FinancialPenaltyOffenceDecision(offence1Id,
                        caseDecision.getId(),
                        FOUND_GUILTY,
                        true,
                        BigDecimal.ZERO,
                        "Some reason for no compensation",
                        null, BigDecimal.ZERO, BigDecimal.ZERO, null, null)));

        caseDecision.setFinancialImposition(new FinancialImposition(
                new CostsAndSurcharge(BigDecimal.ZERO, null,
                        BigDecimal.ZERO, null,
                        true, null),
                new Payment(BigDecimal.ZERO, PAY_TO_COURT,
                        "some reason", COMPENSATION_ORDERED,
                        new PaymentTerms(false,
                                new LumpSum(BigDecimal.ZERO, 20, LocalDate.now()),
                                new Installments(BigDecimal.valueOf(40), WEEKLY, LocalDate.of(2019, 8, 1))), null)));
        caseDetail.setCaseDecisions(asList(caseDecision));

        PaymentView payment = PaymentView.getPayment(caseDetail);
        assertNull(payment.getTotalToPay());

    }

    private void verifyAllAbsoluteDischargePayment(CaseDetail caseDetail) {
        PaymentView payment = PaymentView.getPayment(caseDetail);
        assertEquals("£3,400.23", payment.getTotalToPay());
        assertNull(payment.getTotalFine());
        assertEquals("Absolute Discharge", payment.getNoVictimSurchargeReason());
        assertNull(payment.getReasonForReducedVictimSurcharge());
        assertEquals("Pay directly to court", payment.getPaymentMethod());
        assertEquals("some reason", payment.getPaymentMethodReason());
        assertEquals("£200.23", payment.getProsecutionCosts());
        assertNull(payment.getReasonForNoCosts());
        assertNull(payment.getReserveTerms());
        assertNull(payment.getVictimSurcharge());
        assertEquals("£2,000", payment.getTotalCompensation());
        assertEquals("£1,200", payment.getTotalBackDuty());
    }

    private CaseDetail buildCaseDetailWithOneOffence() {
        final CaseDetail caseDetail = aCase()
                .withCaseId(caseId)
                .withProsecutingAuthority("TFL")
                .build();

        final DefendantDetail defendantDetail = aDefendantDetail().build();
        final List<OffenceDetail> offenceDetails = asList(buildOffenceDetailEntity(1, offence1Id));
        defendantDetail.setOffences(offenceDetails);
        caseDetail.setDefendant(defendantDetail);
        return caseDetail;
    }

    private OffenceDetail buildOffenceDetailEntity(final int sequenceNumber, final UUID offenceId) {
        final OffenceDetail.OffenceDetailBuilder offenceDetailBuilder = OffenceDetail.builder().
                setId(offenceId).
                setCode("CA03013").
                setWording("offence wording").
                setSequenceNumber(sequenceNumber).
                setPlea(PleaType.values()[nextInt(PleaType.values().length)]).
                setPleaDate(now().minusDays(3));

        return offenceDetailBuilder.build();
    }

    private CaseDecision buildCaseDecisionEntity(UUID caseId, boolean magistrate, ZonedDateTime savedAt) {
        final CaseDecision caseDecision = new CaseDecision();
        caseDecision.setCaseId(caseId);
        caseDecision.setId(randomUUID());
        caseDecision.setSavedAt(savedAt);

        caseDecision.setSession(new Session(randomUUID(), randomUUID(), "ASDF", "Lavender Hill",
                "YUIO", magistrate ? MAGISTRATE_NAME : null, now()));
        return caseDecision;
    }


    private FinancialImposition buildFinancialImposition(PaymentType paymentType) {
        return buildFinancialImposition(paymentType, new LumpSum(BigDecimal.valueOf(30.23), 20, LocalDate.now()));
    }

    private FinancialImposition buildFinancialImposition(PaymentType paymentType, LumpSum lumpSum) {
        FinancialImposition financialImposition = new FinancialImposition(
                new CostsAndSurcharge(BigDecimal.valueOf(200.23), null,
                        BigDecimal.valueOf(300), null,
                        true, null),
                new Payment(BigDecimal.valueOf(300), paymentType,
                        "some reason", COMPENSATION_ORDERED,
                        new PaymentTerms(false,
                                lumpSum,
                                new Installments(BigDecimal.valueOf(40), WEEKLY, LocalDate.of(2019, 8, 1))), null));
        return financialImposition;
    }
}
