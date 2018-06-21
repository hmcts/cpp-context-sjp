package uk.gov.moj.cpp.sjp.event.processor.activiti;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ReadyCaseCalculatorTest {

    @Parameterized.Parameter(0)
    public boolean provedInAbsence;

    @Parameterized.Parameter(1)
    public boolean pleaReceived;

    @Parameterized.Parameter(2)
    public boolean withdrawalRequested;

    @Parameterized.Parameter(3)
    public boolean isReady;

    @Parameterized.Parameters(name = "proved in absence={0} plea received={1} withdrawal requested={2} is ready={3}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {false, false, false, false},
                {false, false, true, true},
                {false, true, false, true},
                {false, true, true, true},
                {true, false, false, true},
                {true, false, true, true},
                {true, true, false, true},
                {true, true, true, true},
        });
    }

    @Test
    public void shouldCalculateCaseReadiness() {
        assertThat(ReadyCaseCalculator.isCaseReady(provedInAbsence, pleaReceived, withdrawalRequested), equalTo(isReady));
    }

}
