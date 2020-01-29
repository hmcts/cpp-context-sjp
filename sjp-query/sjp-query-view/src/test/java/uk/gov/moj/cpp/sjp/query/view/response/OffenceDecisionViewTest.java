package uk.gov.moj.cpp.sjp.query.view.response;

import org.junit.Test;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.persistence.entity.DischargeOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialPenaltyOffenceDecision;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
}
