package uk.gov.moj.cpp.sjp.persistence.repository;

import static java.util.Arrays.*;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.CONDITIONAL;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.PeriodUnit.*;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.*;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.InstallmentPeriod;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CostsAndSurcharge;
import uk.gov.moj.cpp.sjp.persistence.entity.DischargeOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.DischargePeriod;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialImposition;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialPenaltyOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.Installments;
import uk.gov.moj.cpp.sjp.persistence.entity.LumpSum;
import uk.gov.moj.cpp.sjp.persistence.entity.Payment;
import uk.gov.moj.cpp.sjp.persistence.entity.PaymentTerms;
import uk.gov.moj.cpp.sjp.persistence.entity.Session;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class CaseDecisionRepositoryTest extends BaseTransactionalTest {

    @Inject
    private EntityManager entityManager;

    @Inject
    private CaseDecisionRepository caseDecisionRepository;


    @Override
    public void setUpBefore() { }

    @After
    public void tearDownAfterTemporary() { }

    @Test
    public void shouldSaveDischargeOffenceDecision() {

        //given
        final CaseDecision caseDecision = new CaseDecision();
        UUID caseDecisionId = randomUUID();
        caseDecision.setId(caseDecisionId);
        caseDecision.setCaseId(UUID.randomUUID());
        caseDecision.setSavedAt(ZonedDateTime.now());
        caseDecision.setSession(entityManager.getReference(Session.class,randomUUID()));

        DischargeOffenceDecision dischargeOffenceDecision = new DischargeOffenceDecision();
        dischargeOffenceDecision.setCompensation(BigDecimal.valueOf(20.3));
        dischargeOffenceDecision.setDischargePeriod(new DischargePeriod(WEEK,10));
        dischargeOffenceDecision.setGuiltyPleaTakenIntoAccount(true);
        dischargeOffenceDecision.setPleaAtDecisionTime(GUILTY);
        dischargeOffenceDecision.setPleaDate(ZonedDateTime.now());
        dischargeOffenceDecision.setDischargeType(CONDITIONAL);

        caseDecision.setOffenceDecisions(asList(dischargeOffenceDecision));
        caseDecisionRepository.save(caseDecision);

        //when
        final CaseDecision actualCaseDecision = caseDecisionRepository.findBy(caseDecisionId);

        //then
        assertEquals(GUILTY, actualCaseDecision.getOffenceDecisions().get(0).getPleaAtDecisionTime());
        assertEquals(CONDITIONAL, ((DischargeOffenceDecision)actualCaseDecision.getOffenceDecisions().get(0)).getDischargeType());
        assertEquals(10, ((DischargeOffenceDecision)actualCaseDecision.getOffenceDecisions().get(0)).getDischargePeriod().getValue());
    }

    @Test
    public void shouldSaveFinancialPenaltyOffenceDecision() {

        //given
        final CaseDecision caseDecision = new CaseDecision();
        UUID caseDecisionId = randomUUID();
        caseDecision.setId(caseDecisionId);
        caseDecision.setCaseId(UUID.randomUUID());
        caseDecision.setSavedAt(ZonedDateTime.now());
        caseDecision.setSession(entityManager.getReference(Session.class,randomUUID()));

        FinancialPenaltyOffenceDecision financialPenaltyOffenceDecision = new FinancialPenaltyOffenceDecision();
        financialPenaltyOffenceDecision.setCompensation(BigDecimal.valueOf(20.3));
        financialPenaltyOffenceDecision.setGuiltyPleaTakenIntoAccount(true);
        financialPenaltyOffenceDecision.setPleaAtDecisionTime(GUILTY);
        financialPenaltyOffenceDecision.setPleaDate(ZonedDateTime.now());
        financialPenaltyOffenceDecision.setFine(BigDecimal.valueOf(20.3));

        caseDecision.setOffenceDecisions(asList(financialPenaltyOffenceDecision));
        caseDecisionRepository.save(caseDecision);

        //when
        final CaseDecision actualCaseDecision = caseDecisionRepository.findBy(caseDecisionId);

        //then
        assertEquals(GUILTY, actualCaseDecision.getOffenceDecisions().get(0).getPleaAtDecisionTime());
        assertEquals(BigDecimal.valueOf(20.3), ((FinancialPenaltyOffenceDecision)actualCaseDecision.getOffenceDecisions().get(0)).getFine());
    }

    @Test
    public void shouldSaveFinancialImposition() {

        //given
        final CaseDecision caseDecision = new CaseDecision();
        UUID caseDecisionId = randomUUID();
        caseDecision.setId(caseDecisionId);
        caseDecision.setCaseId(UUID.randomUUID());
        caseDecision.setSavedAt(ZonedDateTime.now());
        caseDecision.setSession(entityManager.getReference(Session.class,randomUUID()));

        FinancialImposition financialImposition = new FinancialImposition();
        financialImposition.setCaseDecision(caseDecision);
        financialImposition.setCostsAndSurcharge(buildCostsAndSurcharge());
        financialImposition.setPayment(buildPayment());
        caseDecision.setFinancialImposition(financialImposition);
        caseDecisionRepository.save(caseDecision);

        //when
        final CaseDecision actualCaseDecision = caseDecisionRepository.findBy(caseDecisionId);

        //then
        assertEquals(BigDecimal.valueOf(10.3), caseDecision.getFinancialImposition().getPayment().getTotalSum());
        assertEquals(BigDecimal.valueOf(20.45), caseDecision.getFinancialImposition().getCostsAndSurcharge().getVictimSurcharge());
        assertEquals("reduced victim surcharge reason", caseDecision.getFinancialImposition().getCostsAndSurcharge().getReasonForReducedVictimSurcharge());
    }

    private Payment buildPayment() {
        Payment payment = new Payment();
        payment.setTotalSum(BigDecimal.valueOf(10.3));
        payment.setReasonWhyNotAttachedOrDeducted("some reason");
        payment.setPaymentTerms(buildPaymentTerms());
        payment.setPaymentType(PaymentType.DEDUCT_FROM_BENEFITS);
        return payment;
    }

    private PaymentTerms buildPaymentTerms() {
        PaymentTerms paymentTerms = new PaymentTerms();
        paymentTerms.setInstallments(buildInstalments());
        paymentTerms.setLumpSum(buildLumSum());
        paymentTerms.setReserveTerms(true);
        return  paymentTerms;
    }

    private LumpSum buildLumSum() {
        LumpSum lumpSum = new LumpSum();
        lumpSum.setWithinDays(10);
        lumpSum.setAmount(BigDecimal.valueOf(23.34));
        lumpSum.setPayByDate(LocalDate.now().plusDays(4));
        return lumpSum;
    }

    private Installments buildInstalments() {
        Installments installments = new Installments();
        installments.setPeriod(InstallmentPeriod.WEEKLY);
        installments.setStartDate(LocalDate.now());
        installments.setAmount(BigDecimal.valueOf(20.5));
        return installments;
    }

    private CostsAndSurcharge buildCostsAndSurcharge() {
        CostsAndSurcharge costsAndSurcharge = new CostsAndSurcharge();
        costsAndSurcharge.setCollectionOrderMade(true);
        costsAndSurcharge.setVictimSurcharge(BigDecimal.valueOf(20.45));
        costsAndSurcharge.setReasonForNoCosts("some reason");
        costsAndSurcharge.setCosts(BigDecimal.valueOf(34.56));
        costsAndSurcharge.setReasonForNoVictimSurcharge("no costs");
        costsAndSurcharge.setReasonForReducedVictimSurcharge("reduced victim surcharge reason");
        return costsAndSurcharge;
    }
}
