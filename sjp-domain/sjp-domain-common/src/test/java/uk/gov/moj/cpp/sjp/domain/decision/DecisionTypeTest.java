package uk.gov.moj.cpp.sjp.domain.decision;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class DecisionTypeTest {

    @ParameterizedTest
    @CsvSource({
            "WITHDRAW, true, true",
            "DISMISS, true, true",
            "ADJOURN, false, false",
            "REFER_FOR_COURT_HEARING, true, false",
            "DISCHARGE, true, true",
            "FINANCIAL_PENALTY, true, true",
            "NO_SEPARATE_PENALTY, true, true",
            "SET_ASIDE, false, false",
            "REFERRED_TO_OPEN_COURT, true, false",
            "REFERRED_FOR_FUTURE_SJP_SESSION, true, false",
            "OATS, true, true"
    })
    public void shouldDeclareWhetherDecisionIsFinalAndConcluded(final DecisionType decisionType, final boolean isFinal, final boolean isConcluded) {
        assertThat(decisionType.isFinal(), is(isFinal));
        assertThat(decisionType.isConcluded(), is(isConcluded));
    }
}
