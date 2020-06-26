package uk.gov.moj.cpp.sjp.query.view.response;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.decision.PressRestriction;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.persistence.entity.DischargeOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialPenaltyOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.NoSeparatePenaltyOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision;
import uk.gov.moj.cpp.sjp.query.view.util.builders.FinancialPenaltyOffenceDecisionBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.Test;

public class OffenceDecisionViewTest {

    @Test
    public void shouldHaveBackDutyForDischarge() {
        DischargeOffenceDecision dischargeOffenceDecision = mock(DischargeOffenceDecision.class);
        BigDecimal backDuty = new BigDecimal("1.0");
        when(dischargeOffenceDecision.getBackDuty()).thenReturn(backDuty);
        when(dischargeOffenceDecision.getDecisionType()).thenReturn(DecisionType.DISCHARGE);

        OffenceDecisionView underTest = new OffenceDecisionView(dischargeOffenceDecision);

        assertThat("BackDuty should return a BigDecimal value", underTest.getBackDuty(), is(backDuty));
    }

    @Test
    public void shouldHaveBackDutyAndExcisePenaltyForFinancialPenalty() {
        FinancialPenaltyOffenceDecision financialPenaltyOffenceDecision = mock(FinancialPenaltyOffenceDecision.class);
        BigDecimal backDuty = new BigDecimal("1.0");
        BigDecimal excisePenalty = new BigDecimal("2.0");
        when(financialPenaltyOffenceDecision.getBackDuty()).thenReturn(backDuty);
        when(financialPenaltyOffenceDecision.getExcisePenalty()).thenReturn(excisePenalty);
        when(financialPenaltyOffenceDecision.getDecisionType()).thenReturn(DecisionType.FINANCIAL_PENALTY);

        OffenceDecisionView underTest = new OffenceDecisionView(financialPenaltyOffenceDecision);

        assertThat("getBackDuty should return a BigDecimal value", underTest.getBackDuty(), is(backDuty));
        assertThat("getExcisePenalty should return a BigDecimal value", underTest.getExcisePenalty(), is(excisePenalty));
    }

    @Test
    public void shouldConvertNoSeparatePenaltyDecisions() {
        final UUID offenceId = UUID.randomUUID();
        final UUID caseDecisionId = UUID.randomUUID();
        final VerdictType verdict = VerdictType.PROVED_SJP;
        final LocalDate convictionDate = LocalDate.now();
        final boolean guiltyPleaTakenIntoAccount = true;
        final boolean licenceEndorsement = true;
        final OffenceDecision offenceDecision = new NoSeparatePenaltyOffenceDecision(
                offenceId,
                caseDecisionId,
                verdict,
                convictionDate,
                guiltyPleaTakenIntoAccount,
                licenceEndorsement,
                null);

        final OffenceDecisionView result = new OffenceDecisionView(offenceDecision);

        assertThat(result.getOffenceId(), equalTo(offenceId));
        assertThat(result.getDecisionType(), equalTo(DecisionType.NO_SEPARATE_PENALTY));
        assertThat(result.getVerdict(), equalTo(verdict));
        assertThat(result.getConvictionDate(), equalTo(convictionDate));
        assertThat(result.getGuiltyPleaTakenIntoAccount(), equalTo(guiltyPleaTakenIntoAccount));
        assertThat(result.getLicenceEndorsement(), equalTo(licenceEndorsement));
    }

    @Test
    public void shouldHaveLicenceEndorsedAndPenalityPointForFinancialPenalty() {
        FinancialPenaltyOffenceDecision financialPenaltyOffenceDecision = mock(FinancialPenaltyOffenceDecision.class);
        Boolean  licenceEndorsed = true ;
        Integer penaltyPoint = 2;
        when(financialPenaltyOffenceDecision.getLicenceEndorsement()).thenReturn(licenceEndorsed);
        when(financialPenaltyOffenceDecision.getPenaltyPointsImposed()).thenReturn(penaltyPoint);
        when(financialPenaltyOffenceDecision.getDecisionType()).thenReturn(DecisionType.FINANCIAL_PENALTY);

        OffenceDecisionView underTest = new OffenceDecisionView(financialPenaltyOffenceDecision);

        assertThat("getLicenceEndorsed should return a Boolean value", underTest.getLicenceEndorsement(), is(licenceEndorsed));
        assertThat("getPenaltyPoint should return a Integer value", underTest.getPenaltyPointsImposed(), is(penaltyPoint));
    }

    @Test
    public void shouldPopulatePressRestrictionApplied() {
        final FinancialPenaltyOffenceDecision financialPenalty = FinancialPenaltyOffenceDecisionBuilder
                .withDefaults()
                .pressRestrictionApplied("Child's Name")
                .build();

        final OffenceDecisionView actual = new OffenceDecisionView(financialPenalty);

        assertThat(actual.getPressRestriction(), equalTo(PressRestriction.requested("Child's Name")));
    }

    @Test
    public void shouldPopulatePressRestrictionRevoked() {
        final FinancialPenaltyOffenceDecision financialPenalty = FinancialPenaltyOffenceDecisionBuilder
                .withDefaults()
                .pressRestrictionRevoked()
                .build();

        final OffenceDecisionView actual = new OffenceDecisionView(financialPenalty);

        assertThat(actual.getPressRestriction(), equalTo(PressRestriction.revoked()));
    }

    @Test
    public void shouldPopulatePressRestrictionNotApplicable() {
        final FinancialPenaltyOffenceDecision financialPenalty = FinancialPenaltyOffenceDecisionBuilder
                .withDefaults()
                .build();

        final OffenceDecisionView actual = new OffenceDecisionView(financialPenalty);

        assertThat(actual.getPressRestriction(), nullValue());
    }
}
