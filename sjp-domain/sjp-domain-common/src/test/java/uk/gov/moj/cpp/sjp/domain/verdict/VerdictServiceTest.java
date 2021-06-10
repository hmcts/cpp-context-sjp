package uk.gov.moj.cpp.sjp.domain.verdict;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class VerdictServiceTest {

    @Parameter(0)
    public PleaType pleaType;
    @Parameter(1)
    public DecisionType decisionType;
    @Parameter(2)
    public ConvictionType convictionType;
    @Parameter(3)
    public VerdictType expectedVerdict;

    private VerdictService verdictService = new VerdictService();

    @Parameters(name = "verdict test decisions-pleas {0} returns verdicts {1}")
    public static Collection<Object[]> testData() {
        return asList(new Object[][]{
                {PleaType.GUILTY, DecisionType.ADJOURN, ConvictionType.PRE, VerdictType.NO_VERDICT},
                {PleaType.NOT_GUILTY, DecisionType.WITHDRAW, ConvictionType.PRE, VerdictType.NO_VERDICT},
                {PleaType.GUILTY, DecisionType.REFER_FOR_COURT_HEARING, ConvictionType.PRE, VerdictType.FOUND_GUILTY},
                {PleaType.GUILTY_REQUEST_HEARING, DecisionType.REFER_FOR_COURT_HEARING, ConvictionType.PRE, VerdictType.FOUND_GUILTY},
                {null, DecisionType.REFER_FOR_COURT_HEARING, ConvictionType.PRE, VerdictType.PROVED_SJP},
                {PleaType.GUILTY, DecisionType.DISCHARGE, ConvictionType.POST, VerdictType.FOUND_GUILTY},
                {PleaType.NOT_GUILTY, DecisionType.DISCHARGE, ConvictionType.PRE, VerdictType.PROVED_SJP},
                {PleaType.NOT_GUILTY, DecisionType.DISMISS, ConvictionType.PRE, VerdictType.FOUND_NOT_GUILTY},
                {PleaType.NOT_GUILTY, DecisionType.DISMISS, ConvictionType.PRE, VerdictType.FOUND_NOT_GUILTY},
                // NO_SEPARATE_PENALTY
                {PleaType.GUILTY, DecisionType.NO_SEPARATE_PENALTY, ConvictionType.POST, VerdictType.FOUND_GUILTY},
                {PleaType.NOT_GUILTY, DecisionType.NO_SEPARATE_PENALTY, ConvictionType.POST, VerdictType.PROVED_SJP},
                {null, DecisionType.NO_SEPARATE_PENALTY, ConvictionType.POST, VerdictType.PROVED_SJP},
                // FINANCIAL_PENALTY
                {PleaType.GUILTY, DecisionType.FINANCIAL_PENALTY, ConvictionType.POST, VerdictType.FOUND_GUILTY},
                {PleaType.NOT_GUILTY, DecisionType.FINANCIAL_PENALTY, ConvictionType.POST, VerdictType.PROVED_SJP},
                {null, DecisionType.FINANCIAL_PENALTY, ConvictionType.POST, VerdictType.PROVED_SJP},
                {null, DecisionType.SET_ASIDE, null, VerdictType.NO_VERDICT}
        });
    }

    @Test
    public void testVerdicts() {
        assertEquals(expectedVerdict, verdictService.calculateVerdict(pleaType, decisionType, convictionType));
    }
}
