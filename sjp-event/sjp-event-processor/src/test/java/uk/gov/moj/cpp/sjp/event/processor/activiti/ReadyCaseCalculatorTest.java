package uk.gov.moj.cpp.sjp.event.processor.activiti;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.util.Optional;

public class ReadyCaseCalculatorTest {

    private static final CaseReadinessReason NOT_READY = null;
    private ReadyCaseCalculator readyCaseCalculator = new ReadyCaseCalculator();

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(false, false, false, null, false, NOT_READY),
                Arguments.of(false, false, false, null, true, NOT_READY),
                Arguments.of(false, true, false, null, false, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(false, true, false, null, true, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(true, false, false, null, false, CaseReadinessReason.PIA),
                Arguments.of(true, false, false, null, true, NOT_READY),
                Arguments.of(true, true, false, null, false, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(true, true, false, null, true, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(false, false, true, null, false, NOT_READY),
                Arguments.of(false, false, true, null, true, NOT_READY),
                Arguments.of(false, true, true, null, false, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(false, true, true, null, true, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(true, false, true, null, false, CaseReadinessReason.PIA),
                Arguments.of(true, false, true, null, true, NOT_READY),
                Arguments.of(true, true, true, null, false, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(true, true, true, null, true, CaseReadinessReason.WITHDRAWAL_REQUESTED),

                Arguments.of(false, false, false, PleaType.GUILTY, false, NOT_READY),
                Arguments.of(false, false, false, PleaType.GUILTY, true, NOT_READY),
                Arguments.of(false, true, false, PleaType.GUILTY, false, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(false, true, false, PleaType.GUILTY, true, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(true, false, false, PleaType.GUILTY, false, CaseReadinessReason.PIA),
                Arguments.of(true, false, false, PleaType.GUILTY, true, NOT_READY),
                Arguments.of(true, true, false, PleaType.GUILTY, false, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(true, true, false, PleaType.GUILTY, true, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(true, true, false, PleaType.GUILTY, false, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(true, true, false, PleaType.GUILTY, true, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(false, false, true, PleaType.GUILTY, false, CaseReadinessReason.PLEADED_GUILTY),
                Arguments.of(false, false, true, PleaType.GUILTY, true, NOT_READY),
                Arguments.of(false, true, true, PleaType.GUILTY, false, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(false, true, true, PleaType.GUILTY, true, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(true, false, true, PleaType.GUILTY, false, CaseReadinessReason.PLEADED_GUILTY),
                Arguments.of(true, false, true, PleaType.GUILTY, true, NOT_READY),
                Arguments.of(true, true, true, PleaType.GUILTY, false, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(true, true, true, PleaType.GUILTY, true, CaseReadinessReason.WITHDRAWAL_REQUESTED),

                Arguments.of(false, false, false, PleaType.NOT_GUILTY, false, NOT_READY),
                Arguments.of(false, false, false, PleaType.NOT_GUILTY, true, NOT_READY),
                Arguments.of(false, true, false, PleaType.NOT_GUILTY, false, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(false, true, false, PleaType.NOT_GUILTY, true, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(true, false, false, PleaType.NOT_GUILTY, false, NOT_READY),
                Arguments.of(true, false, false, PleaType.NOT_GUILTY, true, NOT_READY),
                Arguments.of(true, true, false, PleaType.NOT_GUILTY, false, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(true, true, false, PleaType.NOT_GUILTY, true, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(false, false, true, PleaType.NOT_GUILTY, false, CaseReadinessReason.PLEADED_NOT_GUILTY),
                Arguments.of(false, false, true, PleaType.NOT_GUILTY, true, NOT_READY),
                Arguments.of(false, true, true, PleaType.NOT_GUILTY, false, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(false, true, true, PleaType.NOT_GUILTY, true, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(true, false, true, PleaType.NOT_GUILTY, false, CaseReadinessReason.PLEADED_NOT_GUILTY),
                Arguments.of(true, false, true, PleaType.NOT_GUILTY, true, NOT_READY),
                Arguments.of(true, true, true, PleaType.NOT_GUILTY, false, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(true, true, true, PleaType.NOT_GUILTY, true, CaseReadinessReason.WITHDRAWAL_REQUESTED),

                Arguments.of(false, false, false, PleaType.GUILTY_REQUEST_HEARING, false, NOT_READY),
                Arguments.of(false, false, false, PleaType.GUILTY_REQUEST_HEARING, true, NOT_READY),
                Arguments.of(false, true, false, PleaType.GUILTY_REQUEST_HEARING, false, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(false, true, false, PleaType.GUILTY_REQUEST_HEARING, true, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(true, false, false, PleaType.GUILTY_REQUEST_HEARING, false, CaseReadinessReason.PIA),
                Arguments.of(true, false, false, PleaType.GUILTY_REQUEST_HEARING, true, NOT_READY),
                Arguments.of(true, true, false, PleaType.GUILTY_REQUEST_HEARING, false, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(true, true, false, PleaType.GUILTY_REQUEST_HEARING, true, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(false, false, true, PleaType.GUILTY_REQUEST_HEARING, false, CaseReadinessReason.PLEADED_GUILTY_REQUEST_HEARING),
                Arguments.of(false, false, true, PleaType.GUILTY_REQUEST_HEARING, true, NOT_READY),
                Arguments.of(false, true, true, PleaType.GUILTY_REQUEST_HEARING, false, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(false, true, true, PleaType.GUILTY_REQUEST_HEARING, true, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(true, false, true, PleaType.GUILTY_REQUEST_HEARING, false, CaseReadinessReason.PLEADED_GUILTY_REQUEST_HEARING),
                Arguments.of(true, false, true, PleaType.GUILTY_REQUEST_HEARING, true, NOT_READY),
                Arguments.of(true, true, true, PleaType.GUILTY_REQUEST_HEARING, false, CaseReadinessReason.WITHDRAWAL_REQUESTED),
                Arguments.of(true, true, true, PleaType.GUILTY_REQUEST_HEARING, true, CaseReadinessReason.WITHDRAWAL_REQUESTED)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldCalculateCaseReadiness(boolean provedInAbsence, boolean withdrawalRequested, boolean pleaReady, PleaType pleaType, boolean caseAdjourned, CaseReadinessReason expectedDecision) {
        assertThat(readyCaseCalculator.getReasonIfReady(provedInAbsence, withdrawalRequested, pleaReady, pleaType, caseAdjourned), equalTo(Optional.ofNullable(expectedDecision)));
    }

}
