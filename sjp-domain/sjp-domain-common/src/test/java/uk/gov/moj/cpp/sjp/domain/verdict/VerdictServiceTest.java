package uk.gov.moj.cpp.sjp.domain.verdict;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.ADJOURN;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISCHARGE;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISMISS;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.REFER_FOR_COURT_HEARING;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.WITHDRAW;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;

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

    private VerdictService verdictService = new VerdictService();

    @Parameter(0)
    public PleaType pleaType;

    @Parameter(1)
    public DecisionType decisionType;

    @Parameter(2)
    public VerdictType expectedVerdict;

    @Test
    public void testVerdicts(){
        assertEquals(expectedVerdict, verdictService.calculateVerdict(pleaType, decisionType));
    }

    @Parameters(name = "verdict test decisions-pleas {0} returns verdicts {1}")
    public static Collection<Object[]> testData() {
        return asList(new Object[][]{
                { GUILTY, ADJOURN, NO_VERDICT },
                { NOT_GUILTY, WITHDRAW, NO_VERDICT },
                { GUILTY, REFER_FOR_COURT_HEARING, FOUND_GUILTY },
                { GUILTY_REQUEST_HEARING, REFER_FOR_COURT_HEARING, FOUND_GUILTY },
                { GUILTY, DISCHARGE, FOUND_GUILTY },
                { NOT_GUILTY, DISCHARGE, PROVED_SJP },
                { NOT_GUILTY, DISMISS, FOUND_NOT_GUILTY },
                { null, REFER_FOR_COURT_HEARING, PROVED_SJP}
        });
    }
}
