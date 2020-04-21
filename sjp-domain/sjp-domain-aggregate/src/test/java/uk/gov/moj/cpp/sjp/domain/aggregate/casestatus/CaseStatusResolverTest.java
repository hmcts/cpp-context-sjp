package uk.gov.moj.cpp.sjp.domain.aggregate.casestatus;

import static com.google.common.collect.Lists.newArrayList;
import static java.time.LocalDate.now;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.aggregate.casestatus.CaseStatusResolver.resolve;
import static uk.gov.moj.cpp.sjp.domain.aggregate.casestatus.OffenceInformation.createOffenceInformation;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.COMPLETED;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.NO_PLEA_RECEIVED;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.PLEA_RECEIVED_NOT_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.REFERRED_FOR_COURT_HEARING;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.REOPENED_IN_LIBRA;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.SET_ASIDE_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.WITHDRAWAL_REQUEST_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.ADJOURN;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISMISS;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.SET_ASIDE;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.WITHDRAW;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;

import uk.gov.justice.json.schemas.fragments.sjp.WithdrawalRequestsStatus;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.common.CaseState;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.domain.decision.Adjourn;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.decision.Discharge;
import uk.gov.moj.cpp.sjp.domain.decision.Dismiss;
import uk.gov.moj.cpp.sjp.domain.decision.FinancialPenalty;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.ReferForCourtHearing;
import uk.gov.moj.cpp.sjp.domain.decision.SetAside;
import uk.gov.moj.cpp.sjp.domain.decision.Withdraw;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class CaseStatusResolverTest {

    private static final LocalDate NOT_RELEVANT = null;

    private static final String DATES_TO_AVOID = "not today";
    private static final String NO_DATES_TO_AVOID = null;
    private static final boolean WITHDRAWAL_TRUE = true;
    private static final boolean WITHDRAWAL_FALSE = false;

    private static final boolean ADJOURNED = true;
    private static final boolean NOT_ADJOURNED = false;

    private static final boolean POST_CONVICTION = true;
    private static final boolean NOT_POST_CONVICTION = false;

    private static final boolean DEFENDANTS_RESPONSES_TIMER_ELAPSED = true;
    private static final boolean DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED = false;

    private static final boolean DATES_TO_AVOID_TIMER_ELAPSED = true;
    private static final boolean DATES_TO_AVOID_TIMER_NOT_ELAPSED = false;

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public List<OffenceInformation> offenceInformation;

    @Parameterized.Parameter(2)
    public boolean adjournedTo;

    @Parameterized.Parameter(3)
    public boolean defendantsResponseTimerElapsed;

    @Parameterized.Parameter(4)
    public boolean datesToAvoidTimerElapsed;

    @Parameterized.Parameter(5)
    public String datesToAvoid;

    @Parameterized.Parameter(6)
    public boolean postConviction;

    @Parameterized.Parameter(7)
    public CaseStatus expectedCaseStatus;

    @Parameterized.Parameters(name = "{index}: testName={0} offenceInfo={1}, adjourned={2}, defendantsResponseTimerElapsed={3}, expireDatesToAvoidTimer={4}, dates to avoid={5}, postConviction={6}, expected status={7}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{

                // testName parameter starting with SO is the ones covering single offence cases created before multi offences
                // testName parameter starting with MO is the ones with multi offences
                // Multi offence test cases created based on https://tools.hmcts.net/confluence/display/PLAT/ATCM+Case+Statuses
                // MO case 16 is covered by MO case 2, so there is no test case for case 16

                //Cases below are testing GUILTY plea + request withdrawal, with the timer elapsed, the expected status is Withdrawal requested, adjourned
                {"SO 1_1", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 1_2", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 1_3", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 1_4", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                //Cases below are testing GUILTY request hearing plea + request withdrawal, with the timer elapsed, the expected status is Withdrawal requested, adjourned
                {"SO 2_1", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 2_2", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 2_3", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 2_4", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                //Cases below are testing NOT GUILTY + request withdrawal, with the timer elapsed, the expected status is Withdrawal requested, adjourned
                {"SO 3_1", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 3_2", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 3_3", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 3_4", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                //Cases below are request withdrawal, with the timer elapsed, the expected status is Withdrawal requested, adjourned
                {"SO 4_1", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_TRUE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 4_2", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_TRUE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                //Cases below are testing GUILTY plea + request withdrawal, timer not elapsed, the expected status is Withdrawal requested, adjourned
                {"SO 5_1", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 5_2", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 5_3", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 5_4", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                //Cases below are testing GUILTY request hearing plea + request withdrawal, timer not elapsed, the expected status is Withdrawal requested, adjourned
                {"SO 6_1", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 6_2", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 6_3", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 6_4", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},


                //Cases below are testing NOT GUILTY + request withdrawal, timer not elapsed, the expected status is Withdrawal requested, adjourned
                {"SO 7_1", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 7_2", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 7_3", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 7_4", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                //Cases below are request withdrawal, timer not elapsed, the expected status is Withdrawal requested, adjourned
                {"SO 8_1", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_TRUE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 8_2", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_TRUE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                //Cases below are testing GUILTY plea + request withdrawal, with the timer elapsed, the expected status is Withdrawal requested, not adjourned
                {"SO 9_1", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 9_2", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 9_3", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 9_4", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                //Cases below are testing GUILTY request hearing plea + request withdrawal, with the timer elapsed, the expected status is Withdrawal requested, not adjourned
                {"SO 10_1", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 10_2", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 10_3", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 10_4", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                //Cases below are testing NOT GUILTY + request withdrawal, with the timer elapsed, the expected status is Withdrawal requested, not adjourned
                {"SO 11_1", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 11_2", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 11_3", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 11_4", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                //Cases below are request withdrawal, with the timer elapsed, the expected status is Withdrawal requested, not adjourned
                {"SO 12_1", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 12_2", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                //Cases below are testing GUILTY plea + request withdrawal, timer not elapsed, the expected status is Withdrawal requested, not adjourned
                {"SO 13_1", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 13_2", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 13_3", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 13_4", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                //Cases below are testing GUILTY request hearing plea + request withdrawal, timer not elapsed, the expected status is Withdrawal requested, not adjourned
                {"SO 14_1", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 14_2", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 14_3", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 14_4", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                //Cases below are testing NOT GUILTY + request withdrawal, timer not elapsed, the expected status is Withdrawal requested, not adjourned
                {"SO 15_1", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 15_2", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 15_3", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 15_4", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                //Cases below are request withdrawal, timer not elapsed, the expected status is Withdrawal requested, not adjourned
                {"SO 16_1", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"SO 16_2", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                //Cases below are testing GUILTY plea, with the timer elapsed, adjourned, the expected status is Plea received not ready for decision
                {"SO 17_1", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 17_2", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 17_3", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 17_4", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                //Cases below are testing GUILTY request hearing plea, with the timer elapsed, adjourned, the expected status is Plea received not ready for decision
                {"SO 18_1", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 18_2", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 18_3", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 18_4", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                //Cases below are testing NOT GUILTY, with the timer elapsed,a adjourned, the expected status is Plea received not ready for decision
                {"SO 19_1", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 19_2", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 19_3", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 19_4", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                // The timer elapsed, adjourned, the expected status is NO PLEA RECEIVED
                {"SO 20_1", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_FALSE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, NO_PLEA_RECEIVED},
                {"SO 20_2", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_FALSE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, NO_PLEA_RECEIVED},

                //Cases below are testing GUILTY plea, timer not elapsed, adjourned, the expected status is Plea received not ready for decision
                {"SO 21_1", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 21_2", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 21_3", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 21_4", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                //Cases below are testing GUILTY request hearing plea, timer not elapsed, adjourned, the expected status is Plea received not ready for decision
                {"SO 22_1", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 22_2", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 22_3", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 22_4", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                //Cases below are testing NOT GUILTY, with the timer elapsed, not adjourned, the expected status is Plea received not ready for decision
                {"SO 23_1", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 23_2", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 23_3", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 23_4", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                // Timer not elapsed, adjourned, the expected status is no plea received
                {"SO 24_1", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_FALSE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, NO_PLEA_RECEIVED},
                {"SO 24_2", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_FALSE)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, NO_PLEA_RECEIVED},

                //Cases below are testing GUILTY plea, with the timer elapsed, not adjourned, the expected status is Plea received ready for decision
                {"SO 25_1", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 25_2", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 25_3", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 25_4", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},

                //Cases below are testing GUILTY request hearing plea, with the timer elapsed, not adjourned, the expected status is Plea received not ready for decision
                {"SO 26_1", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 26_2", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 26_3", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 26_4", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},


                //Cases below are testing NOT GUILTY, with the timer elapsed, not adjourned, the expected status is Plea received  ready for decision
                {"SO 27_1", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 27_2", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 27_3", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 27_4", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},


                // Timer not elapsed, not adjourned, the expected status is No lea received not ready for decision
                {"SO 28_1", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, NO_PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 28_2", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, NO_PLEA_RECEIVED_READY_FOR_DECISION},


                //Cases below are testing GUILTY plea, timer not elapsed, not adjourned, the expected status is Plea received ready for decision
                {"SO 29_1", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 29_2", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 29_3", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 29_4", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},

                //Cases below are testing GUILTY request hearing plea, timer not elapsed, not adjourned, the expected status is Plea received ready for decision
                {"SO 30_1", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 30_2", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 30_3", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 30_4", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},

                //Cases below are testing NOT GUILTY, timer not elapsed, not adjourned, the expected status is Plea received ready for decision
                {"SO 31_1", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 31_2", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 31_3", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                // no dates to avoid so status is Plea received not ready for decision
                {"SO 31_4", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                //Timer not elapsed, not adjourned, the expected status is no plea received
                {"SO 32_1", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, NO_PLEA_RECEIVED},
                {"SO 32_2", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, NO_PLEA_RECEIVED},

                //1 Only not adjourned, only posted less than 28 days, can have dates to avoid or not, no withdrawal
                {"MO case 1_1", allNoPleas(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, NO_PLEA_RECEIVED},
                {"MO case 1_2", allNoPleas(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, NO_PLEA_RECEIVED},

                //2 Only not adjourned, only posted greater than 28 days, can have dates to avoid or not, no withdrawal
                {"MO case 2_1", allNoPleas(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, NO_PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 2_1", allNoPleas(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, NO_PLEA_RECEIVED_READY_FOR_DECISION},

                //3 Only not adjourned, posted any time, can have dates to avoid or not, withdrawal on all
                {"MO case 3_1", somePleasWithdrawalOnAll(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 3_2", somePleasWithdrawalOnAll(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 3_3", somePleasWithdrawalOnAll(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 3_4", somePleasWithdrawalOnAll(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                //4 Only not adjourned, posted any time, can have dates to avoid or not, no withdrawal
                {"MO case 4_1", allGuiltyPleas(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 4_2", allGuiltyPleas(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 4_3", allGuiltyPleas(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 4_4", allGuiltyPleas(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},

                //5 Only not adjourned, posted any time, no dates to avoid, no withdrawal
                {"MO case 5_1", allNotGuiltyPleasPleadedOn(NOT_RELEVANT), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 5_2", allNotGuiltyPleasPleadedOn(NOT_RELEVANT), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},

                //6 Only not adjourned, posted any time, no dates to avoid, no withdrawal
                {"MO case 6_1", allNotGuiltyPleasPleadedOn(NOT_RELEVANT), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                //7 only not adjourned, only posted less than 28 days, can have dates to avoid or not, withdrawal requested on some
                {"MO case 7_1", withdrawalRequestedOnSomeWithPleaType(null, null), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, NO_PLEA_RECEIVED},
                {"MO case 7_2", withdrawalRequestedOnSomeWithPleaType(null, null), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, NO_PLEA_RECEIVED},

                //8 only not adjourned, only posted less than 28 days, can have dates to avoid or not, withdrawal requested on some + some no pleas (excluding the withdrawal request)
                //!!! the plea is not guilty in that lot when dates to avoid the case is actually ready for decision...
                {"MO case 8_1", somePleasWithdrawalRequestedOnSome(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 8_2", somePleasWithdrawalRequestedOnSome(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                //9 only not adjourned, only posted greater than 28 days, can have dates to avoid or not, Withdrawal requested on some + some no pleas (excluding the withdrawal request)
                {"MO case 9_1", somePleasWithdrawalRequestedOnSomePleadedWith(GUILTY), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 9_2", somePleasWithdrawalRequestedOnSomePleadedWith(GUILTY), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},

                //10 only not adjourned, only posted less than 28 days, no dates to avoid, no withdrawal
                {"MO case 10_1", somePleasWithAllPleaded(NOT_GUILTY), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                //11 only not adjourned, only posted greater than 28 days, can have dates to avoid or not, no withdrawal
                {"MO case 11_1", somePleasWithAllPleaded(GUILTY), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 11_2", somePleasWithAllPleaded(GUILTY), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},

                //12 only not adjourned, only posted less than 28 days, has dates to avoid to or pleaded more than 10 days, no withdrawal
                {"MO case 12_1", somePleasWithAllPleaded(NOT_GUILTY), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION}, // has dates to avoid
                {"MO case 12_2", someNotGuiltyPleasPleadedOn(NOT_RELEVANT), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION}, // pleaded 10 days ago
                {"MO case 12_3", someNotGuiltyPleasPleadedOn(NOT_RELEVANT), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION}, // has dates to avoid && pleaded 10 days ago

                //13 only not adjourned, only posted less than 28 days, no dates to avoid, pleaded less than 10 days, no withdrawal
                {"MO case 13_1", someNotGuiltyPleasPleadedOn(NOT_RELEVANT), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                //14 only not adjourned, posted any time, can have dates to avoid or not, no withdrawal
                {"MO case 14_1", allGuiltyCourtHearingPleas(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 14_2", allGuiltyCourtHearingPleas(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 14_3", allGuiltyCourtHearingPleas(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 14_4", allGuiltyCourtHearingPleas(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},

                //15 only not adjourned, posted any time, can have dates to avoid or not, no withdrawal
                {"MO case 15_1", somePleasWithAllPleaded(GUILTY_REQUEST_HEARING, 1), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 15_2", somePleasWithAllPleaded(GUILTY_REQUEST_HEARING, 1), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 15_3", somePleasWithAllPleaded(GUILTY_REQUEST_HEARING, 1), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 15_4", somePleasWithAllPleaded(GUILTY_REQUEST_HEARING, 1), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 15_5", somePleasWithAllPleaded(GUILTY_REQUEST_HEARING, 2), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 15_6", somePleasWithAllPleaded(GUILTY_REQUEST_HEARING, 2), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 15_7", somePleasWithAllPleaded(GUILTY_REQUEST_HEARING, 2), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 15_8", somePleasWithAllPleaded(GUILTY_REQUEST_HEARING, 2), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},

                //17 adjournment date is future, only posted greater than 28 days, can have dates to avoid or not, no withdrawal
                {"MO case 17_1", allNoPleas(), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, NO_PLEA_RECEIVED},
                {"MO case 17_2", allNoPleas(), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, NO_PLEA_RECEIVED},
                {"MO case 17_3", allNoPleas(), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, NO_PLEA_RECEIVED},
                {"MO case 17_4", allNoPleas(), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, NO_PLEA_RECEIVED},

                //18 adjournment date is today or past -means not adjourned-, only posted greater than 28 days, can have dates to avoid or not, no withdrawal
                {"MO case 18_1", allGuiltyPleas(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 18_2", allGuiltyPleas(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 18_3", allGuiltyPleas(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 18_4", allGuiltyPleas(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 18_5", allGuiltyPleas(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 18_6", allGuiltyPleas(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 18_7", allGuiltyPleas(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 18_8", allGuiltyPleas(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 18_9", somePleasWithAllPleaded(GUILTY), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 18_10", somePleasWithAllPleaded(GUILTY), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 18_11", somePleasWithAllPleaded(GUILTY), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 18_12", somePleasWithAllPleaded(GUILTY), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},

                //19 adjournment date is future, posted any time, can have dates to avoid or not, withdrawal on all
                {"MO case 19_1", somePleasWithdrawalOnAll(), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 19_2", somePleasWithdrawalOnAll(), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 19_3", allPleasWithdrawalOnAll(), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 19_4", allPleasWithdrawalOnAll(), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 19_5", somePleasWithdrawalOnAll(), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 19_6", somePleasWithdrawalOnAll(), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 19_7", allPleasWithdrawalOnAll(), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 19_8", allPleasWithdrawalOnAll(), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                //20 adjournment date is future, posted any time, can have dates to avoid or not, withdrawal on some
                {"MO case 20_1", somePleasWithdrawalRequestedOnSomeIncludingNoPleas(), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 20_2", somePleasWithdrawalRequestedOnSomeIncludingNoPleas(), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 20_3", allPleasWithdrawalOnSome(), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 20_4", allPleasWithdrawalOnSome(), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 20_5", somePleasWithdrawalRequestedOnSomeIncludingNoPleas(), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 20_6", somePleasWithdrawalRequestedOnSomeIncludingNoPleas(), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 20_7", allPleasWithdrawalOnSome(), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 20_8", allPleasWithdrawalOnSome(), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                //21 adjournment date is future, posted any time, can have dates to avoid or not, withdrawal on all
                {"MO case 21_1", allSamePleas(null, null, WITHDRAWAL_TRUE, 2), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 21_2", allSamePleas(null, null, WITHDRAWAL_TRUE, 2), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 21_3", allSamePleas(null, null, WITHDRAWAL_TRUE, 2), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 21_4", allSamePleas(null, null, WITHDRAWAL_TRUE, 2), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                //22 adjournment date is today or past -means not adjourned, only posted greater than 28 days, can have dates to avoid or not, withdrawal on some
                {"MO case 22_1", allNoPleasWithdrawalOnSome(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, NO_PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 22_2", allNoPleasWithdrawalOnSome(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, NO_PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 22_3", allNoPleasWithdrawalOnSome(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, NO_PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 22_4", allNoPleasWithdrawalOnSome(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, NO_PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 22_5", someRequestedWithdrawnSomeFinalDecision(null,null,2,3, WITHDRAW), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 22_6", someRequestedWithdrawnSomeFinalDecision(null,null,2,3, DISMISS), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 22_7", someRequestedWithdrawnSomeFinalDecision(null,null,2,3, DISMISS), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                //23 adjournment date is future, posted any time, can have dates to avoid or not, withdrawal on some
                {"MO case 23_1", allNoPleasWithdrawalOnSome(), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, NO_PLEA_RECEIVED},
                {"MO case 23_2", allNoPleasWithdrawalOnSome(), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, NO_PLEA_RECEIVED},
                {"MO case 23_3", allNoPleasWithdrawalOnSome(), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, NO_PLEA_RECEIVED},
                {"MO case 23_4", allNoPleasWithdrawalOnSome(), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, NO_PLEA_RECEIVED},

                //24 adjournment date is today or past -means not adjourned-, only posted greater than 28 days, can have dates to avoid or not, no withdrawal
                {"MO case 24_1", somePleasWithAllPleaded(NOT_GUILTY), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 24_2", somePleasWithAllPleaded(NOT_GUILTY), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                //25 only not adjourned, posted any time, can have dates to avoid or not, withdrawal on some
                {"MO case 25_1", withdrawalOnSomeAllGuiltyAndPleadedWith(GUILTY_REQUEST_HEARING), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 25_2", withdrawalOnSomeAllGuiltyAndPleadedWith(GUILTY_REQUEST_HEARING), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 25_3", withdrawalOnSomeAllGuiltyAndPleadedWith(GUILTY_REQUEST_HEARING), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 25_4", withdrawalOnSomeAllGuiltyAndPleadedWith(GUILTY_REQUEST_HEARING), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 25_5", withdrawalOnSomeAllGuiltyAndPleadedWith(NOT_GUILTY), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                // !!! again no dates to avoid this is waiting
                {"MO case 25_6", withdrawalOnSomeAllGuiltyAndPleadedWith(NOT_GUILTY), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 25_7", withdrawalOnSomeAllGuiltyAndPleadedWith(NOT_GUILTY), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                // !!! not ready dates to avoid
                {"MO case 25_8", withdrawalOnSomeAllGuiltyAndPleadedWith(NOT_GUILTY), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 25_9", allPleasWithdrawalOnSome(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                // !!! not guilty pleas waiting for dta
                {"MO case 25_10", allPleasWithdrawalOnSome(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 25_11", allPleasWithdrawalOnSome(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                // !!! waiting for dta
                {"MO case 25_12", allPleasWithdrawalOnSome(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                //26 only not adjourned, posted any time, can have dates to avoid or not, withdrawal on some
                {"MO case 26_1", withdrawalRequestedOnSomeWithPleaType(GUILTY, NOT_RELEVANT), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 26_2", withdrawalRequestedOnSomeWithPleaType(GUILTY, NOT_RELEVANT), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 26_3", withdrawalRequestedOnSomeWithPleaType(GUILTY, NOT_RELEVANT), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 26_4", withdrawalRequestedOnSomeWithPleaType(GUILTY, NOT_RELEVANT), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},

                //27 only not adjourned, only posted greater than 28 days, has dates to avoid or pleaded greater than ten days, withdrawal on some only with pleas
                {
                        "MO case 27_1", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_FALSE),
                        createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE),
                        createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE),
                        createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE),
                        createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION}, // has DTA

                {
                        "MO case 27_2", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_FALSE),
                        createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE),
                        createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE),
                        createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE),
                        createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION}, // pleaDate > 10 days

                {
                        "MO case 27_3", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_FALSE),
                        createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE),
                        createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE),
                        createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE),
                        createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION}, // has DTA and pleaDate > 10 days

                // not adjourned, all same, posted any time, can have dates to avoid or not, withdrawal requested on all
                {"MO case 28_1", withdrawalRequestedOnAllWithPleaType(null, null), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 28_2", withdrawalRequestedOnAllWithPleaType(null, null), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 28_3", withdrawalRequestedOnAllWithPleaType(null, null), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 28_4", withdrawalRequestedOnAllWithPleaType(null, null), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 28_5", withdrawalRequestedOnAllWithPleaType(GUILTY, now()), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 28_6", withdrawalRequestedOnAllWithPleaType(GUILTY, now()), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 28_7", withdrawalRequestedOnAllWithPleaType(GUILTY, now()), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 28_8", withdrawalRequestedOnAllWithPleaType(GUILTY, now()), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 28_5", withdrawalRequestedOnAllWithPleaType(NOT_GUILTY, now()), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 28_6", withdrawalRequestedOnAllWithPleaType(NOT_GUILTY, now()), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 28_7", withdrawalRequestedOnAllWithPleaType(NOT_GUILTY, now()), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 28_8", withdrawalRequestedOnAllWithPleaType(NOT_GUILTY, now()), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 28_5", withdrawalRequestedOnAllWithPleaType(GUILTY_REQUEST_HEARING, now()), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 28_6", withdrawalRequestedOnAllWithPleaType(GUILTY_REQUEST_HEARING, now()), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 28_7", withdrawalRequestedOnAllWithPleaType(GUILTY_REQUEST_HEARING, now()), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {"MO case 28_8", withdrawalRequestedOnAllWithPleaType(GUILTY_REQUEST_HEARING, now()), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                //adjourned, all pleas, posted any time, can have dates to avoid or not, no withdrawal
                {"MO case 29_1", allPleas(), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 29_2", allPleas(), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 29_3", allPleas(), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 29_4", allPleas(), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                // all not guilty, not adjourned, posted any time, has dates to avoid
                {"MO case 30_1", allNotGuiltyPleas(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 30_2", allNotGuiltyPleas(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},

                // All guilty request court hearing,  not adjourned, posted any time, can have dates to avoid or not, no withdrawal
                {"MO case 31_1", allGuiltyCourtHearingPleas(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 31_2", allGuiltyCourtHearingPleas(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 31_3", allGuiltyCourtHearingPleas(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 31_4", allGuiltyCourtHearingPleas(), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},

                // Some guilty SJP, not adjourned, posted before 28 days, can have dates to avoid or not, no withdrawal
                {"MO case 32_1", somePleasWithAllPleaded(GUILTY), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 32_2", somePleasWithAllPleaded(GUILTY), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 32_3", somePleasWithAllPleaded(GUILTY), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 32_4", somePleasWithAllPleaded(GUILTY), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                // Some guilty SJP, request withdrawals on remainding, not adjourned, posted before 28 days, can have dates to avoid or not
                {"MO case 33_1", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE), createOffenceInformation(null, null, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 33_2", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE), createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE), createOffenceInformation(null, null, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 33_3", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE), createOffenceInformation(null, null, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 33_4", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE), createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE), createOffenceInformation(null, null, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 33_5", newArrayList(
                        createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE),
                        createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE),
                        createOffenceInformation(NOT_GUILTY, null, WITHDRAWAL_TRUE)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, NOT_POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},

                // not adjourned, post conviction
                // some pleas post conviction
                {"SO 36_1", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 36_2", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 36_3", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 36_4", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 36_5", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 36_6", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 36_7", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 36_8", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 36_9", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 36_10", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 36_11", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 36_12", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 36_13", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 36_14", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 36_15", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 36_16", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 36_17", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 36_18", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 36_19", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 36_20", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 36_21", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 36_22", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 36_23", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 36_24", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 36_25", allGuiltyPleas(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_26", allGuiltyPleas(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_27", allGuiltyPleas(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_28", allGuiltyPleas(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_29", allNotGuiltyPleasPleadedOn(NOT_RELEVANT, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_30", allNotGuiltyPleasPleadedOn(NOT_RELEVANT, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_31", allNotGuiltyPleasPleadedOn(NOT_RELEVANT, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 36_32", somePleasWithAllPleaded(NOT_GUILTY, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 36_33", somePleasWithAllPleaded(GUILTY, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_34", somePleasWithAllPleaded(GUILTY, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_35", somePleasWithAllPleaded(NOT_GUILTY, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION}, // has dates to avoid
                {"MO case 36_36", someNotGuiltyPleasPleadedOn(NOT_RELEVANT, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION}, // pleaded 10 days ago
                {"MO case 36_37", someNotGuiltyPleasPleadedOn(NOT_RELEVANT, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION}, // has dates to avoid && pleaded 10 days ago
                {"MO case 36_38", someNotGuiltyPleasPleadedOn(NOT_RELEVANT, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 36_39", allGuiltyCourtHearingPleas(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_40", allGuiltyCourtHearingPleas(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_41", allGuiltyCourtHearingPleas(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_42", allGuiltyCourtHearingPleas(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_43", somePleasWithAllPleaded(GUILTY_REQUEST_HEARING, ADJOURN, 1), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_44", somePleasWithAllPleaded(GUILTY_REQUEST_HEARING, ADJOURN, 1), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_45", somePleasWithAllPleaded(GUILTY_REQUEST_HEARING, ADJOURN, 1), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_46", somePleasWithAllPleaded(GUILTY_REQUEST_HEARING, ADJOURN, 1), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_47", somePleasWithAllPleaded(GUILTY_REQUEST_HEARING, ADJOURN, 2), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_48", somePleasWithAllPleaded(GUILTY_REQUEST_HEARING, ADJOURN, 2), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_49", somePleasWithAllPleaded(GUILTY_REQUEST_HEARING, ADJOURN, 2), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_50", somePleasWithAllPleaded(GUILTY_REQUEST_HEARING, ADJOURN, 2), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_51", allGuiltyPleas(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_52", allGuiltyPleas(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_53", allGuiltyPleas(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_54", allGuiltyPleas(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_55", allGuiltyPleas(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_56", allGuiltyPleas(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_57", allGuiltyPleas(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_58", allGuiltyPleas(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_59", somePleasWithAllPleaded(GUILTY, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_60", somePleasWithAllPleaded(GUILTY, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_61", somePleasWithAllPleaded(GUILTY, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_62", somePleasWithAllPleaded(GUILTY, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_63", somePleasWithAllPleaded(NOT_GUILTY, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_64", somePleasWithAllPleaded(NOT_GUILTY, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 36_65", allNotGuiltyPleas(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_66", allNotGuiltyPleas(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_67", allGuiltyCourtHearingPleas(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_68", allGuiltyCourtHearingPleas(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_69", allGuiltyCourtHearingPleas(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_70", allGuiltyCourtHearingPleas(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 36_71", somePleasWithAllPleaded(GUILTY, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 36_72", somePleasWithAllPleaded(GUILTY, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 36_73", somePleasWithAllPleaded(GUILTY, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 36_74", somePleasWithAllPleaded(GUILTY, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                // all no pleas post conviction
                {"SO 37_1", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 37_2", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 37_3", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED},
                {"SO 37_4", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED},
                {"MO case 37_5", allNoPleas(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED},
                {"MO case 37_6", allNoPleas(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED},
                {"MO case 37_7", allNoPleas(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 37_8", allNoPleas(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED_READY_FOR_DECISION},

                // some pleas Withdrawal requested on some
                {"MO case 42_1", somePleasWithdrawalRequestedOnSome(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 42_2", somePleasWithdrawalRequestedOnSome(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 42_3", somePleasWithdrawalRequestedOnSomePleadedWith(GUILTY, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 42_4", somePleasWithdrawalRequestedOnSomePleadedWith(GUILTY, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 42_5", withdrawalOnSomeAllGuiltyAndPleadedWith(GUILTY_REQUEST_HEARING, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 42_6", withdrawalOnSomeAllGuiltyAndPleadedWith(GUILTY_REQUEST_HEARING, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 42_7", withdrawalOnSomeAllGuiltyAndPleadedWith(GUILTY_REQUEST_HEARING, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 42_8", withdrawalOnSomeAllGuiltyAndPleadedWith(GUILTY_REQUEST_HEARING, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 42_9", withdrawalOnSomeAllGuiltyAndPleadedWith(NOT_GUILTY, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                // !!! again no dates to avoid this is waiting
                {"MO case 42_10", withdrawalOnSomeAllGuiltyAndPleadedWith(NOT_GUILTY, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 42_11", withdrawalOnSomeAllGuiltyAndPleadedWith(NOT_GUILTY, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                // !!! not ready dates to avoid
                {"MO case 42_12", withdrawalOnSomeAllGuiltyAndPleadedWith(NOT_GUILTY, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 42_13", allPleasWithdrawalOnSome(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                // !!! not guilty pleas waiting for dta
                {"MO case 42_14", allPleasWithdrawalOnSome(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 42_15", allPleasWithdrawalOnSome(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                // !!! waiting for dta
                {"MO case 42_16", allPleasWithdrawalOnSome(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 42_17", withdrawalRequestedOnSomeWithPleaType(GUILTY, NOT_RELEVANT, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 42_18", withdrawalRequestedOnSomeWithPleaType(GUILTY, NOT_RELEVANT, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 42_19", withdrawalRequestedOnSomeWithPleaType(GUILTY, NOT_RELEVANT, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 42_20", withdrawalRequestedOnSomeWithPleaType(GUILTY, NOT_RELEVANT, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {
                        "MO case 42_21", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_FALSE, ADJOURN),
                        createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN),
                        createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN),
                        createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN),
                        createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION}, // has DTA

                {
                        "MO case 42_22", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_FALSE, ADJOURN),
                        createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN),
                        createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN),
                        createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN),
                        createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION}, // pleaDate > 10 days

                {
                        "MO case 42_23", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_FALSE, ADJOURN),
                        createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN),
                        createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN),
                        createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN),
                        createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION}, // has DTA and pleaDate > 10 days
                {"MO case 42_24", newArrayList(
                        createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN),
                        createOffenceInformation(null, null, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 42_25", newArrayList(
                        createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN),
                        createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN),
                        createOffenceInformation(null, null, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 42_26", newArrayList(
                        createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN),
                        createOffenceInformation(null, null, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 42_27", newArrayList(
                        createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN),
                        createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN),
                        createOffenceInformation(null, null, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 42_28", newArrayList(
                        createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN),
                        createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN),
                        createOffenceInformation(NOT_GUILTY, null, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},

                // some pleas withdrawal requested on all
                {"SO 43_1", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 43_2", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 43_3", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 43_4", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 43_5", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 43_6", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 43_7", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 43_8", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 43_9", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 43_10", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 43_11", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 43_12", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 43_13", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 43_14", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 43_15", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 43_16", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 43_17", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 43_18", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 43_19", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 43_20", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 43_21", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 43_22", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 43_23", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 43_24", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 43_25", somePleasWithdrawalOnAll(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 43_26", somePleasWithdrawalOnAll(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 43_27", somePleasWithdrawalOnAll(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 43_28", somePleasWithdrawalOnAll(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 43_29", withdrawalRequestedOnAllWithPleaType(GUILTY, now(), ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 43_30", withdrawalRequestedOnAllWithPleaType(GUILTY, now(), ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 43_31", withdrawalRequestedOnAllWithPleaType(GUILTY, now(), ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 43_32", withdrawalRequestedOnAllWithPleaType(GUILTY, now(), ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 43_33", withdrawalRequestedOnAllWithPleaType(NOT_GUILTY, now(), ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 43_34", withdrawalRequestedOnAllWithPleaType(NOT_GUILTY, now(), ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 43_35", withdrawalRequestedOnAllWithPleaType(NOT_GUILTY, now(), ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 43_36", withdrawalRequestedOnAllWithPleaType(NOT_GUILTY, now(), ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 43_37", withdrawalRequestedOnAllWithPleaType(GUILTY_REQUEST_HEARING, now(), ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 43_38", withdrawalRequestedOnAllWithPleaType(GUILTY_REQUEST_HEARING, now(), ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 43_39", withdrawalRequestedOnAllWithPleaType(GUILTY_REQUEST_HEARING, now(), ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 43_40", withdrawalRequestedOnAllWithPleaType(GUILTY_REQUEST_HEARING, now(), ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_READY_FOR_DECISION},

                // all no plea withdrawal requested on some
                {"MO case 44_1", withdrawalRequestedOnSomeWithPleaType(null, null, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED},
                {"MO case 44_2", withdrawalRequestedOnSomeWithPleaType(null, null, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED},
                {"MO case 44_3", allNoPleasWithdrawalOnSome(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 44_4", allNoPleasWithdrawalOnSome(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 44_5", allNoPleasWithdrawalOnSome(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 44_6", allNoPleasWithdrawalOnSome(ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED_READY_FOR_DECISION},

                // all no plea withdrawal requested on all
                {"SO 45_1", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 45_2", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 45_3", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED_READY_FOR_DECISION},
                {"SO 45_4", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_TRUE, ADJOURN)), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 45_5", someRequestedWithdrawnSomeFinalDecision(null,null,2,3, ADJOURN, WITHDRAW), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 45_6", someRequestedWithdrawnSomeFinalDecision(null,null,2,3, ADJOURN, DISMISS), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 45_7", someRequestedWithdrawnSomeFinalDecision(null,null,2,3, ADJOURN, DISMISS), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 45_8", withdrawalRequestedOnAllWithPleaType(null, null, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 45_9", withdrawalRequestedOnAllWithPleaType(null, null, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 45_10", withdrawalRequestedOnAllWithPleaType(null, null, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED_READY_FOR_DECISION},
                {"MO case 45_11", withdrawalRequestedOnAllWithPleaType(null, null, ADJOURN), NOT_ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED_READY_FOR_DECISION},

                // adjourn post-conviction
                // some pleas no withdrawal
                {"SO 34_1", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 34_2", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 34_3", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 34_4", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 34_5", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 34_6", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 34_7", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 34_8", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 34_9", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 34_10", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 34_11", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 34_12", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 34_13", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 34_14", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 34_15", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 34_16", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 34_17", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 34_18", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 34_19", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 34_20", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 34_21", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 34_22", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 34_23", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 34_24", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                // all no pleas no withdrawal
                {"SO 35_1", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_FALSE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED},
                {"SO 35_2", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_FALSE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED},
                {"SO 35_3", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_FALSE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED},
                {"SO 35_4", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_FALSE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED},

                // some pleas withdrawal on all
                {"SO 39_1", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 39_2", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 39_3", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 39_4", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 39_5", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 39_6", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 39_7", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 39_8", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 39_9", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 39_10", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 39_10", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 39_10", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 39_11", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 39_12", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 39_13", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 39_14", newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 39_15", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 39_16", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 39_17", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 39_18", newArrayList(createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 39_19", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 39_20", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 39_21", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"SO 39_22", newArrayList(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                // all no pleas withdrawal on all
                {"SO 41_1", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_TRUE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED},
                {"SO 41_2", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_TRUE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED},
                {"SO 41_3", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_TRUE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED},
                {"SO 41_4", newArrayList(createOffenceInformation(null, null, WITHDRAWAL_TRUE, ADJOURN)), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED},

                // some pleas no withdrawal
                {"MO case 34_1", allPleas(ADJOURN), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 34_2", allPleas(ADJOURN), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 34_3", allPleas(ADJOURN), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 34_4", allPleas(ADJOURN), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                // all no pleas no withdrawal
                {"MO case 35_1", allNoPleas(ADJOURN), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED},
                {"MO case 35_2", allNoPleas(ADJOURN), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED},
                {"MO case 35_3", allNoPleas(ADJOURN), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED},
                {"MO case 35_4", allNoPleas(ADJOURN), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED},

                {"MO case 38_1", somePleasWithdrawalRequestedOnSomeIncludingNoPleas(ADJOURN), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 38_2", somePleasWithdrawalRequestedOnSomeIncludingNoPleas(ADJOURN), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 38_3", somePleasWithdrawalRequestedOnSomeIncludingNoPleas(ADJOURN), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 38_4", somePleasWithdrawalRequestedOnSomeIncludingNoPleas(ADJOURN), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 38_5", allPleasWithdrawalOnSome(ADJOURN), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 38_6", allPleasWithdrawalOnSome(ADJOURN), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 38_7", allPleasWithdrawalOnSome(ADJOURN), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 38_8", allPleasWithdrawalOnSome(ADJOURN), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                // some pleas withdrawal on all
                {"MO case 39_1", somePleasWithdrawalOnAll(ADJOURN), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 39_2", somePleasWithdrawalOnAll(ADJOURN), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 39_3", somePleasWithdrawalOnAll(ADJOURN), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 39_4", somePleasWithdrawalOnAll(ADJOURN), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 39_5", allPleasWithdrawalOnAll(ADJOURN), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 39_6", allPleasWithdrawalOnAll(ADJOURN), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 39_7", allPleasWithdrawalOnAll(ADJOURN), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {"MO case 39_8", allPleasWithdrawalOnAll(ADJOURN), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                // all no pleas withdrawal on some
                {"MO case 40_1", allNoPleasWithdrawalOnSome(ADJOURN), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED},
                {"MO case 40_2", allNoPleasWithdrawalOnSome(ADJOURN), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED},
                {"MO case 40_3", allNoPleasWithdrawalOnSome(ADJOURN), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED},
                {"MO case 40_4", allNoPleasWithdrawalOnSome(ADJOURN), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED},
                {"MO case 40_5", someRequestedWithdrawnSomeFinalDecision(null,null,2,3, ADJOURN, DISMISS), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED},

                // all no pleas, withdrawal on all
                {"MO case 41_1", allSamePleas(null, null, WITHDRAWAL_TRUE, ADJOURN, 2), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED},
                {"MO case 41_2", allSamePleas(null, null, WITHDRAWAL_TRUE, ADJOURN, 2), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED},
                {"MO case 41_3", allSamePleas(null, null, WITHDRAWAL_TRUE, ADJOURN, 2), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED},
                {"MO case 41_4", allSamePleas(null, null, WITHDRAWAL_TRUE, ADJOURN, 2), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_NOT_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, NO_PLEA_RECEIVED},
                {"MO case 41_5", allNoPleas(SET_ASIDE), ADJOURNED, DEFENDANTS_RESPONSES_TIMER_ELAPSED, DATES_TO_AVOID_TIMER_NOT_ELAPSED, NO_DATES_TO_AVOID, POST_CONVICTION, SET_ASIDE_READY_FOR_DECISION},
        });
    }

    private static List<OffenceInformation> allPleas() {
        return allPleas(null);
    }
    private static List<OffenceInformation> allPleas(final DecisionType decisionType) {
        final List<OffenceInformation> list = newArrayList();
        list.addAll(allNotGuiltyPleas(decisionType));
        list.addAll(allGuiltyPleas(decisionType));
        list.addAll(allGuiltyCourtHearingPleas(decisionType));
        return list;
    }

    private static List<OffenceInformation> allSamePleas(final PleaType pleaType, final LocalDate pleaDate, final Boolean pendingWithdrawal, final int pleaCount) {
        return allSamePleas(pleaType, pleaDate, pendingWithdrawal, null, pleaCount);
    }

    private static List<OffenceInformation> allSamePleas(final PleaType pleaType, final LocalDate pleaDate, final Boolean pendingWithdrawal, final DecisionType decisionType, final int pleaCount) {
        final List<OffenceInformation> list = newArrayList();
        IntStream.range(0, pleaCount).forEach(index -> list.add(createOffenceInformation(pleaType, pleaDate, pendingWithdrawal, decisionType)));
        return list;
    }

    private static List<OffenceInformation> someRequestedWithdrawnSomeFinalDecision(final PleaType pleaType, final LocalDate pleaDate, int numWithdrawn, int numFinalDecision, DecisionType finalDecision) {
        return someRequestedWithdrawnSomeFinalDecision(pleaType, pleaDate, numWithdrawn, numFinalDecision, null, finalDecision);
    }

    private static List<OffenceInformation> someRequestedWithdrawnSomeFinalDecision(final PleaType pleaType, final LocalDate pleaDate, int numWithdrawn, int numFinalDecision, final DecisionType otherDecision, DecisionType finalDecision) {
        final List<OffenceInformation> withdrawn = newArrayList();
        IntStream.range(0, numWithdrawn).forEach(index -> withdrawn.add(createOffenceInformation(pleaType, pleaDate, true, otherDecision)));
        final List<OffenceInformation> withFinalDecision = new ArrayList<>();
        if(finalDecision.isFinal()){
            IntStream.range(0, numFinalDecision).forEach(index -> withFinalDecision.add(createOffenceInformation(randomUUID(), pleaType, pleaDate, false, finalDecision)));
        }
        withdrawn.addAll(withFinalDecision);
        return withdrawn;
    }

    private static List<OffenceInformation> allNoPleas() {
        return allNoPleas(null);
    }

    private static List<OffenceInformation> allNoPleas(final DecisionType decisionType) {
        return allSamePleas(null, null, WITHDRAWAL_FALSE, decisionType, 2);
    }

    private static List<OffenceInformation> allGuiltyPleas() {
        return allGuiltyPleas(null);
    }

    private static List<OffenceInformation> allGuiltyPleas(final DecisionType decisionType) {
        return allSamePleas(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, decisionType, 2);
    }

    private static List<OffenceInformation> allNotGuiltyPleas() {
        return allNotGuiltyPleas(null);
    }

    private static List<OffenceInformation> allNotGuiltyPleas(final DecisionType decisionType) {
        return allSamePleas(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, decisionType, 2);
    }

    private static List<OffenceInformation> allGuiltyCourtHearingPleas() {
        return allGuiltyCourtHearingPleas(null);
    }

    private static List<OffenceInformation> allGuiltyCourtHearingPleas(final DecisionType decisionType) {
        return allSamePleas(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_FALSE, decisionType, 2);
    }

    private static List<OffenceInformation> allNotGuiltyPleasPleadedOn(final LocalDate pleaDate, final DecisionType decisionType) {
        return allSamePleas(NOT_GUILTY, pleaDate, WITHDRAWAL_FALSE, 2);
    }

    private static List<OffenceInformation> allNotGuiltyPleasPleadedOn(final LocalDate pleaDate) {
        return allNotGuiltyPleasPleadedOn(pleaDate, null);
    }

    private static List<OffenceInformation> allPleasWithdrawalOnAll() {
        return allPleasWithdrawalOnAll(null);
    }

    private static List<OffenceInformation> allPleasWithdrawalOnAll(final DecisionType decisionType) {
        final List<OffenceInformation> list = new ArrayList<>();
        Arrays.stream(PleaType.values()).forEach(pleaType -> list.add(createOffenceInformation(pleaType, NOT_RELEVANT, WITHDRAWAL_TRUE, decisionType)));
        return list;
    }

    private static List<OffenceInformation> allPleasWithdrawalOnSome() {
        return allPleasWithdrawalOnSome(null);
    }

    private static List<OffenceInformation> allPleasWithdrawalOnSome(final DecisionType decisionType) {
        final List<OffenceInformation> list = new ArrayList<>();
        list.add(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, decisionType));
        list.add(createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, decisionType));
        list.add(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, decisionType));
        list.add(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, decisionType));
        return list;
    }

    private static List<OffenceInformation> allNoPleasWithdrawalOnSome() {
        return allNoPleasWithdrawalOnSome(null);
    }

    private static List<OffenceInformation> allNoPleasWithdrawalOnSome(final DecisionType decisionType) {
        return newArrayList(createOffenceInformation(null, null, WITHDRAWAL_TRUE, decisionType),
                createOffenceInformation(null, null, WITHDRAWAL_FALSE, decisionType));
    }

    private static List<OffenceInformation> somePleasWithdrawalOnAll() {
        return somePleasWithdrawalOnAll(null);
    }
    private static List<OffenceInformation> somePleasWithdrawalOnAll(final DecisionType decisionType) {
        return newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, decisionType),
                createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, decisionType),
                createOffenceInformation(GUILTY_REQUEST_HEARING, NOT_RELEVANT, WITHDRAWAL_TRUE, decisionType),
                createOffenceInformation(null, null, WITHDRAWAL_TRUE, decisionType));
    }

    private static List<OffenceInformation> someNotGuiltyPleasPleadedOn(final LocalDate pleaDate, final DecisionType decisionType) {
        final List<OffenceInformation> list = allSamePleas(NOT_GUILTY, pleaDate, WITHDRAWAL_FALSE, decisionType, 2);
        list.add(createOffenceInformation(null, null, WITHDRAWAL_FALSE, decisionType));
        return list;
    }

    private static List<OffenceInformation> someNotGuiltyPleasPleadedOn(final LocalDate pleaDate) {
        return someNotGuiltyPleasPleadedOn(pleaDate, null);
    }

    private static List<OffenceInformation> somePleasWithdrawalRequestedOnSomePleadedWith(final PleaType pleaType, final DecisionType decisionType) {
        final List<OffenceInformation> list = new ArrayList<>();
        list.add(createOffenceInformation(pleaType, NOT_RELEVANT, WITHDRAWAL_TRUE, decisionType));
        list.add(createOffenceInformation(pleaType, NOT_RELEVANT, WITHDRAWAL_FALSE, decisionType));
        list.add(createOffenceInformation(null, null, WITHDRAWAL_FALSE, decisionType));
        return list;
    }

    private static List<OffenceInformation> somePleasWithdrawalRequestedOnSomePleadedWith(final PleaType pleaType) {
        return somePleasWithdrawalRequestedOnSomePleadedWith(pleaType, null);
    }

    private static List<OffenceInformation> somePleasWithAllPleaded(final PleaType pleaType, final int pleaCount) {
        return somePleasWithAllPleaded(pleaType, null, pleaCount);
    }

    private static List<OffenceInformation> somePleasWithAllPleaded(final PleaType pleaType, final DecisionType decisionType, final int pleaCount) {
        final List<OffenceInformation> list = allSamePleas(pleaType, NOT_RELEVANT, WITHDRAWAL_FALSE, decisionType, pleaCount);
        list.add(createOffenceInformation(null, null, WITHDRAWAL_FALSE, decisionType));
        return list;
    }

    private static List<OffenceInformation> somePleasWithAllPleaded(final PleaType pleaType, final DecisionType decisionType) {
        return somePleasWithAllPleaded(pleaType, decisionType, 2);
    }

    private static List<OffenceInformation> somePleasWithAllPleaded(final PleaType pleaType) {
        return somePleasWithAllPleaded(pleaType, null, 2);
    }

    private static List<OffenceInformation> somePleasWithdrawalRequestedOnSome(final DecisionType decisionType) {
        return newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, decisionType),
                createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, decisionType),
                createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, decisionType),
                createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, decisionType),
                createOffenceInformation(null, null, WITHDRAWAL_FALSE, decisionType));
    }

    private static List<OffenceInformation> somePleasWithdrawalRequestedOnSome() {
        return somePleasWithdrawalRequestedOnSome(null);
    }

    private static List<OffenceInformation> somePleasWithdrawalRequestedOnSomeIncludingNoPleas() {
        return somePleasWithdrawalRequestedOnSomeIncludingNoPleas(null);
    }
    private static List<OffenceInformation> somePleasWithdrawalRequestedOnSomeIncludingNoPleas(final DecisionType decisionType) {
        return newArrayList(createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, decisionType),
                createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, decisionType),
                createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, decisionType),
                createOffenceInformation(NOT_GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, decisionType),
                createOffenceInformation(null, null, WITHDRAWAL_TRUE, decisionType),
                createOffenceInformation(null, null, WITHDRAWAL_FALSE, decisionType));
    }

    private static List<OffenceInformation> withdrawalOnSomeAllGuiltyAndPleadedWith(final PleaType pleaType, final DecisionType decisionType) {
        return newArrayList(createOffenceInformation(pleaType, NOT_RELEVANT, WITHDRAWAL_TRUE, decisionType),
                createOffenceInformation(pleaType, NOT_RELEVANT, WITHDRAWAL_FALSE, decisionType),
                createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_TRUE, decisionType),
                createOffenceInformation(GUILTY, NOT_RELEVANT, WITHDRAWAL_FALSE, decisionType));
    }

    private static List<OffenceInformation> withdrawalOnSomeAllGuiltyAndPleadedWith(final PleaType pleaType) {
        return withdrawalOnSomeAllGuiltyAndPleadedWith(pleaType, null);
    }

    private static List<OffenceInformation> withdrawalRequestedOnSomeWithPleaType(final PleaType pleaType, final LocalDate pleaDate, final DecisionType decisionType) {
        return newArrayList(createOffenceInformation(pleaType, pleaDate, WITHDRAWAL_FALSE, decisionType),
                createOffenceInformation(pleaType, pleaDate, WITHDRAWAL_TRUE, decisionType));
    }

    private static List<OffenceInformation> withdrawalRequestedOnSomeWithPleaType(final PleaType pleaType, final LocalDate pleaDate) {
        return withdrawalRequestedOnSomeWithPleaType(pleaType, pleaDate, null);
    }

    private static List<OffenceInformation> withdrawalRequestedOnAllWithPleaType(final PleaType pleaType, final LocalDate pleaDate, final DecisionType decisionType) {
        return allSamePleas(pleaType, pleaDate, WITHDRAWAL_TRUE, decisionType, 2);
    }

    private static List<OffenceInformation> withdrawalRequestedOnAllWithPleaType(final PleaType pleaType, final LocalDate pleaDate) {
        return withdrawalRequestedOnAllWithPleaType(pleaType, pleaDate, null);
    }

    @Test
    public void shouldReturnStatusReopenedInLibraIfCaseCompletedAndReopened() {
        CaseAggregateState caseAggregateState = populateCaseAggregateState();
        caseAggregateState.setCaseReopenedDate(now());
        caseAggregateState.markCaseCompleted();

        final CaseState caseState = resolve(caseAggregateState);

        // TODO: Assert against readiness
        assertThat(caseState.getCaseStatus(), is(REOPENED_IN_LIBRA));
    }

    @Test
    public void shouldSetStatusReferredToCourtWhenCaseCompletedAndReferredToCourt() {

        CaseAggregateState caseAggregateState = populateCaseAggregateState();
        caseAggregateState.markCaseReferredForCourtHearing();
        caseAggregateState.setCaseReopenedDate(null);
        caseAggregateState.markCaseCompleted();

        final CaseState caseState = resolve(caseAggregateState);

        // TODO: Assert against readiness
        assertThat(caseState.getCaseStatus(), is(REFERRED_FOR_COURT_HEARING));
    }

    @Test
    public void shouldSetStatusCompletedIfCaseCompletedButNotReferredNorReopened() {

        CaseAggregateState caseAggregateState = populateCaseAggregateState();
        caseAggregateState.setCaseReopenedDate(null);
        caseAggregateState.markCaseCompleted();

        final CaseState caseState = resolve(caseAggregateState);

        // TODO: Assert against readiness
        assertThat(caseState.getCaseStatus(), is(COMPLETED));
    }

    @Test
    public void shouldSetStatusForNotCompletedCases() {
        CaseAggregateState caseAggregateState = populateCaseAggregateState();
        caseAggregateState.setCaseReopenedDate(null);

        final CaseState caseState = resolve(caseAggregateState);

        // TODO: Assert against readiness
        assertThat(caseState.getCaseStatus(), is(expectedCaseStatus));
    }

    public OffenceDecision offenceDecision(final UUID offenceId, final DecisionType decisionType) {
        switch (decisionType) {
            case ADJOURN:
                return this.postConviction
                ? new Adjourn(randomUUID(), singletonList(createOffenceDecisionInformation(offenceId, FOUND_GUILTY)), "reason", LocalDate.now().plusDays(14))
                : new Adjourn(randomUUID(), singletonList(createOffenceDecisionInformation(offenceId, NO_VERDICT)), "reason", LocalDate.now().plusDays(14));
            case DISMISS:
                return new Dismiss(randomUUID(), createOffenceDecisionInformation(offenceId, FOUND_NOT_GUILTY));
            case WITHDRAW:
                return new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId, NO_VERDICT), randomUUID());
            case REFER_FOR_COURT_HEARING:
                return new ReferForCourtHearing(randomUUID(), singletonList(createOffenceDecisionInformation(offenceId,NO_VERDICT)), randomUUID(), null, null, null);
            case DISCHARGE:
                return new Discharge(randomUUID(), createOffenceDecisionInformation(offenceId, FOUND_GUILTY), DischargeType.ABSOLUTE, null,new BigDecimal(10), null, true,null,
                        null,null, null,null,null,null,null,null);
            case FINANCIAL_PENALTY:
                return new FinancialPenalty(randomUUID(), createOffenceDecisionInformation(offenceId, FOUND_GUILTY), new BigDecimal(10), new BigDecimal(20), null, true, null, null,
                        null, null, null, null, null, null, null, null);
            case SET_ASIDE:
                return new SetAside(randomUUID(), singletonList(createOffenceDecisionInformation(offenceId, null)));

            default:
                throw new IllegalArgumentException("unknown decision type");
        }
    }

    private CaseAggregateState populateCaseAggregateState() {
        final UUID defendantId = randomUUID();

        final CaseAggregateState caseAggregateState = new CaseAggregateState();
        caseAggregateState.setDefendantId(defendantId);
        final Set<UUID> offenceIDs = new HashSet<>();

        offenceInformation
                .forEach((e) -> {

                    UUID offenceId = randomUUID();
                    offenceIDs.add(offenceId);

                    ofNullable(e.getPendingWithdrawal())
                            .filter(Boolean.TRUE::equals)
                            .ifPresent((a) -> caseAggregateState.addWithdrawnOffences(WithdrawalRequestsStatus.withdrawalRequestsStatus().withOffenceId(offenceId).build()));

                    ofNullable(e.getPleaDate())
                            .ifPresent((pleaDate) -> caseAggregateState.putOffencePleaDate(offenceId, pleaDate));

                    ofNullable(e.getPleaType())
                            .ifPresent((pleaType) -> caseAggregateState.getPleas().add((new Plea(defendantId, offenceId, pleaType))));

                    ofNullable(e.getDecision())
                            .ifPresent((decisionType) -> {
                                final List<OffenceDecision> offenceDecisions = singletonList(offenceDecision(offenceId, decisionType));
                                caseAggregateState.updateOffenceDecisions(singletonList(offenceDecision(offenceId, decisionType)), randomUUID());
                                caseAggregateState.updateOffenceConvictionDates(ZonedDateTime.now(), offenceDecisions);
                            });

                    ofNullable(e.getDecision())
                            .ifPresent((decisionType) -> {
                                if (decisionType.equals(SET_ASIDE))
                                    caseAggregateState.setSetAside(true);
                            });

                });

        caseAggregateState.addOffenceIdsForDefendant(caseAggregateState.getDefendantId(), offenceIDs);
        caseAggregateState.setDatesToAvoid(datesToAvoid);
        if (adjournedTo) {
            caseAggregateState.setAdjournedTo(now());
        } else {
            caseAggregateState.makeNonAdjourned();
        }
        if (datesToAvoidTimerElapsed) {
            caseAggregateState.datesToAvoidTimerExpired();
        } else {
            caseAggregateState.setDatesToAvoidExpirationDate(now().plusDays(1));
        }

        if (this.defendantsResponseTimerElapsed) {
            caseAggregateState.setDefendantsResponseTimerExpired();
        }

        return caseAggregateState;
    }

}
