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
    public CaseReadinessReason expectedDecision;

    private ReadyCaseCalculator readyCaseCalculator = new ReadyCaseCalculator();

    @Parameters(name = "proved in absence={0}, withdrawal requested={1}, plea ready={2}, plea type={3}, will have decision={4}")
    public static Collection<Object[]> data() {
        return asList(new Object[][]{
                {false, false, false, null, NOT_READY},
                {false, true, false, null, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {true, false, false, null, CaseReadinessReason.PIA},
                {true, true, false, null, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {false, false, true, null, NOT_READY},
                {false, true, true, null, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {true, false, true, null, CaseReadinessReason.PIA},
                {true, true, true, null, CaseReadinessReason.WITHDRAWAL_REQUESTED},

                {false, false, false, PleaType.GUILTY, NOT_READY},
                {false, true, false, PleaType.GUILTY, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {true, false, false, PleaType.GUILTY, CaseReadinessReason.PIA},
                {true, true, false, PleaType.GUILTY, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {false, false, true, PleaType.GUILTY, CaseReadinessReason.PLEADED_GUILTY},
                {false, true, true, PleaType.GUILTY, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {true, false, true, PleaType.GUILTY, CaseReadinessReason.PLEADED_GUILTY},
                {true, true, true, PleaType.GUILTY, CaseReadinessReason.WITHDRAWAL_REQUESTED},

                {false, false, false, PleaType.NOT_GUILTY, NOT_READY},
                {false, true, false, PleaType.NOT_GUILTY, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {true, false, false, PleaType.NOT_GUILTY, NOT_READY},
                {true, true, false, PleaType.NOT_GUILTY, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {false, false, true, PleaType.NOT_GUILTY, CaseReadinessReason.PLEADED_NOT_GUILTY},
                {false, true, true, PleaType.NOT_GUILTY, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {true, false, true, PleaType.NOT_GUILTY, CaseReadinessReason.PLEADED_NOT_GUILTY},
                {true, true, true, PleaType.NOT_GUILTY, CaseReadinessReason.WITHDRAWAL_REQUESTED},

                {false, false, false, PleaType.GUILTY_REQUEST_HEARING, NOT_READY},
                {false, true, false, PleaType.GUILTY_REQUEST_HEARING, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {true, false, false, PleaType.GUILTY_REQUEST_HEARING, CaseReadinessReason.PIA},
                {true, true, false, PleaType.GUILTY_REQUEST_HEARING, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {false, false, true, PleaType.GUILTY_REQUEST_HEARING, CaseReadinessReason.PLEADED_GUILTY_REQUEST_HEARING},
                {false, true, true, PleaType.GUILTY_REQUEST_HEARING, CaseReadinessReason.WITHDRAWAL_REQUESTED},
                {true, false, true, PleaType.GUILTY_REQUEST_HEARING, CaseReadinessReason.PLEADED_GUILTY_REQUEST_HEARING},
                {true, true, true, PleaType.GUILTY_REQUEST_HEARING, CaseReadinessReason.WITHDRAWAL_REQUESTED},
        });
    }

    @Test
    public void shouldCalculateCaseReadiness() {
        assertThat(readyCaseCalculator.getReasonIfReady(provedInAbsence, withdrawalRequested, pleaReady, pleaType), equalTo(Optional.ofNullable(expectedDecision)));
    }

}
