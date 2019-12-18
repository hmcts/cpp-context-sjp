package uk.gov.moj.cpp.sjp.event.processor.activiti;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.util.Collection;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;


@RunWith(Parameterized.class)
public class ReadyCaseCalculatorTest {

    private static final CaseReadinessReason NOT_READY = null;

    @Parameter(0)
    public boolean provedInAbsence;

    @Parameter(1)
    public boolean withdrawalRequested;

    @Parameter(2)
    public boolean pleaReady;

    @Parameter(3)
    public PleaType pleaType;

    @Parameter(4)
    public boolean caseAdjourned;

    @Parameter(5)
    public CaseReadinessReason expectedDecision;

    private ReadyCaseCalculator readyCaseCalculator = new ReadyCaseCalculator();

    @Parameters(name = "proved in absence={0}, withdrawal requested={1}, plea ready={2}, plea type={3}, case adjourned={4} will have decision={5}")
    public static Collection<Object[]> data() {
        return asList(new Object[][]{
                {false, false, false, null, false, NOT_READY},
                {false, false, false, null, true, NOT_READY},
                {false, true, false, null, false, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {false, true, false, null, true, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {true, false, false, null, false, CaseReadinessReason.PIA},
                {true, false, false, null, true, NOT_READY},
                {true, true, false, null, false, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {true, true, false, null, true, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {false, false, true, null, false, NOT_READY},
                {false, false, true, null, true, NOT_READY},
                {false, true, true, null, false, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {false, true, true, null, true, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {true, false, true, null, false, CaseReadinessReason.PIA},
                {true, false, true, null, true, NOT_READY},
                {true, true, true, null, false, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {true, true, true, null, true, CaseReadinessReason.WITHDRAWAL_REQUESTED},

                {false, false, false, PleaType.GUILTY, false, NOT_READY},
                {false, false, false, PleaType.GUILTY, true, NOT_READY},
                {false, true, false, PleaType.GUILTY, false, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {false, true, false, PleaType.GUILTY, true, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {true, false, false, PleaType.GUILTY, false, CaseReadinessReason.PIA},
                {true, false, false, PleaType.GUILTY, true, NOT_READY},
                {true, true, false, PleaType.GUILTY, false, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {true, true, false, PleaType.GUILTY, true, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {true, true, false, PleaType.GUILTY, false, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {true, true, false, PleaType.GUILTY, true, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {false, false, true, PleaType.GUILTY, false, CaseReadinessReason.PLEADED_GUILTY},
                {false, false, true, PleaType.GUILTY, true, NOT_READY},
                {false, true, true, PleaType.GUILTY, false, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {false, true, true, PleaType.GUILTY, true, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {true, false, true, PleaType.GUILTY, false, CaseReadinessReason.PLEADED_GUILTY},
                {true, false, true, PleaType.GUILTY, true, NOT_READY},
                {true, true, true, PleaType.GUILTY, false, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {true, true, true, PleaType.GUILTY, true, CaseReadinessReason.WITHDRAWAL_REQUESTED},

                {false, false, false, PleaType.NOT_GUILTY, false, NOT_READY},
                {false, false, false, PleaType.NOT_GUILTY, true, NOT_READY},
                {false, true, false, PleaType.NOT_GUILTY, false, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {false, true, false, PleaType.NOT_GUILTY, true, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {true, false, false, PleaType.NOT_GUILTY, false, NOT_READY},
                {true, false, false, PleaType.NOT_GUILTY, true, NOT_READY},
                {true, true, false, PleaType.NOT_GUILTY, false, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {true, true, false, PleaType.NOT_GUILTY, true, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {false, false, true, PleaType.NOT_GUILTY, false, CaseReadinessReason.PLEADED_NOT_GUILTY},
                {false, false, true, PleaType.NOT_GUILTY, true, NOT_READY},
                {false, true, true, PleaType.NOT_GUILTY, false, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {false, true, true, PleaType.NOT_GUILTY, true, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {true, false, true, PleaType.NOT_GUILTY, false, CaseReadinessReason.PLEADED_NOT_GUILTY},
                {true, false, true, PleaType.NOT_GUILTY, true, NOT_READY},
                {true, true, true, PleaType.NOT_GUILTY, false, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {true, true, true, PleaType.NOT_GUILTY, true, CaseReadinessReason.WITHDRAWAL_REQUESTED},

                {false, false, false, PleaType.GUILTY_REQUEST_HEARING, false, NOT_READY},
                {false, false, false, PleaType.GUILTY_REQUEST_HEARING, true, NOT_READY},
                {false, true, false, PleaType.GUILTY_REQUEST_HEARING, false, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {false, true, false, PleaType.GUILTY_REQUEST_HEARING, true, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {true, false, false, PleaType.GUILTY_REQUEST_HEARING, false, CaseReadinessReason.PIA},
                {true, false, false, PleaType.GUILTY_REQUEST_HEARING, true, NOT_READY},
                {true, true, false, PleaType.GUILTY_REQUEST_HEARING, false, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {true, true, false, PleaType.GUILTY_REQUEST_HEARING, true, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {false, false, true, PleaType.GUILTY_REQUEST_HEARING, false, CaseReadinessReason.PLEADED_GUILTY_REQUEST_HEARING},
                {false, false, true, PleaType.GUILTY_REQUEST_HEARING, true, NOT_READY},
                {false, true, true, PleaType.GUILTY_REQUEST_HEARING, false, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {false, true, true, PleaType.GUILTY_REQUEST_HEARING, true, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {true, false, true, PleaType.GUILTY_REQUEST_HEARING, false, CaseReadinessReason.PLEADED_GUILTY_REQUEST_HEARING},
                {true, false, true, PleaType.GUILTY_REQUEST_HEARING, true, NOT_READY},
                {true, true, true, PleaType.GUILTY_REQUEST_HEARING, false, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {true, true, true, PleaType.GUILTY_REQUEST_HEARING, true, CaseReadinessReason.WITHDRAWAL_REQUESTED},
        });
    }

    @Test
    public void shouldCalculateCaseReadiness() {
        assertThat(readyCaseCalculator.getReasonIfReady(provedInAbsence, withdrawalRequested, pleaReady, pleaType, caseAdjourned), equalTo(Optional.ofNullable(expectedDecision)));
    }

}
