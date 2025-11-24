package uk.gov.moj.cpp.sjp.domain.verdict;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

public class VerdictServiceTest {
    private VerdictService verdictService = new VerdictService();

    static Stream<Arguments> testData() {
        return Stream.of(
                Arguments.of(PleaType.GUILTY, DecisionType.ADJOURN, ConvictionType.PRE, VerdictType.NO_VERDICT),
                Arguments.of(PleaType.NOT_GUILTY, DecisionType.WITHDRAW, ConvictionType.PRE, VerdictType.NO_VERDICT),
                Arguments.of(PleaType.GUILTY, DecisionType.REFER_FOR_COURT_HEARING, ConvictionType.PRE, VerdictType.FOUND_GUILTY),
                Arguments.of(PleaType.GUILTY_REQUEST_HEARING, DecisionType.REFER_FOR_COURT_HEARING, ConvictionType.PRE, VerdictType.FOUND_GUILTY),
                Arguments.of(null, DecisionType.REFER_FOR_COURT_HEARING, ConvictionType.PRE, VerdictType.PROVED_SJP),
                Arguments.of(PleaType.GUILTY, DecisionType.DISCHARGE, ConvictionType.POST, VerdictType.FOUND_GUILTY),
                Arguments.of(PleaType.NOT_GUILTY, DecisionType.DISCHARGE, ConvictionType.PRE, VerdictType.PROVED_SJP),
                Arguments.of(PleaType.NOT_GUILTY, DecisionType.DISMISS, ConvictionType.PRE, VerdictType.FOUND_NOT_GUILTY),
                Arguments.of(PleaType.NOT_GUILTY, DecisionType.DISMISS, ConvictionType.PRE, VerdictType.FOUND_NOT_GUILTY),
                // NO_SEPARATE_PENALTY
                Arguments.of(PleaType.GUILTY, DecisionType.NO_SEPARATE_PENALTY, ConvictionType.POST, VerdictType.FOUND_GUILTY),
                Arguments.of(PleaType.NOT_GUILTY, DecisionType.NO_SEPARATE_PENALTY, ConvictionType.POST, VerdictType.PROVED_SJP),
                Arguments.of(null, DecisionType.NO_SEPARATE_PENALTY, ConvictionType.POST, VerdictType.PROVED_SJP),
                // FINANCIAL_PENALTY
                Arguments.of(PleaType.GUILTY, DecisionType.FINANCIAL_PENALTY, ConvictionType.POST, VerdictType.FOUND_GUILTY),
                Arguments.of(PleaType.NOT_GUILTY, DecisionType.FINANCIAL_PENALTY, ConvictionType.POST, VerdictType.PROVED_SJP),
                Arguments.of(null, DecisionType.FINANCIAL_PENALTY, ConvictionType.POST, VerdictType.PROVED_SJP),
                Arguments.of(null, DecisionType.SET_ASIDE, null, VerdictType.NO_VERDICT)
        );
    }

    @ParameterizedTest
    @MethodSource("testData")
    public void testVerdicts(PleaType pleaType, DecisionType decisionType, ConvictionType convictionType, VerdictType expectedVerdict) {

        assertEquals(expectedVerdict, verdictService.calculateVerdict(pleaType, decisionType, convictionType));
    }
}
