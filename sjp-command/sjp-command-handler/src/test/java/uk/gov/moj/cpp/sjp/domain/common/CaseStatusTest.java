package uk.gov.moj.cpp.sjp.domain.common;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.NO_PLEA_RECEIVED;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.PLEA_RECEIVED_NOT_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.WITHDRAWAL_REQUEST_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.calculateStatus;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;

import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class CaseStatusTest {

    private final LocalDate FIVE_DAYS_AGO = LocalDate.now().minusDays(5);
    private final LocalDate TWENTY_EIGHT_DAYS_AGO = LocalDate.now().minusDays(28);
    private final LocalDate TEN_DAYS_AGO = LocalDate.now().minusDays(10);

    private boolean withdrawalRequested;

    private boolean adjourned;

    private LocalDate postingDate;

    private PleaInformation pleaInformation;

    private String datesToAvoid;

    private CaseStatus expectedCaseStatus;

    public CaseStatusTest(boolean withdrawalRequested, boolean adjourned, boolean pia, PleaType pleaType, boolean pleadedMoreThan10DaysAgo, boolean datesToAvoidProvided, CaseStatus expectedCaseStatus) {
        this.withdrawalRequested = withdrawalRequested;
        this.adjourned = adjourned;
        this.postingDate = pia ? TWENTY_EIGHT_DAYS_AGO : FIVE_DAYS_AGO;
        this.pleaInformation = new PleaInformation(pleaType, pleadedMoreThan10DaysAgo ? TEN_DAYS_AGO : FIVE_DAYS_AGO);
        this.datesToAvoid = datesToAvoidProvided ? "not today" : null;
        this.expectedCaseStatus = expectedCaseStatus;
    }

    @Parameterized.Parameters(name = "{index}: withdrawal={0}, adjourned={1}, pia={2}, plea={3}, plea older than 10 days={4}, dates to avoid={5}, status={6}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {true, true, true, GUILTY, true, true, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, true, true, GUILTY, true, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, true, true, GUILTY, false, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, true, true, GUILTY, false, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                {true, true, true, GUILTY_REQUEST_HEARING, true, true, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, true, true, GUILTY_REQUEST_HEARING, true, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, true, true, GUILTY_REQUEST_HEARING, false, true, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, true, true, GUILTY_REQUEST_HEARING, true, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                {true, true, true, NOT_GUILTY, true, true, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, true, true, NOT_GUILTY, true, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, true, true, NOT_GUILTY, false, true, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, true, true, NOT_GUILTY, false, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                {true, true, true, null, false, true, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, true, true, null, false, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},


                {true, true, false, GUILTY, true, true, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, true, false, GUILTY, true, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, true, false, GUILTY, false, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, true, false, GUILTY, false, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                {true, true, false, GUILTY_REQUEST_HEARING, true, true, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, true, false, GUILTY_REQUEST_HEARING, true, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, true, false, GUILTY_REQUEST_HEARING, false, true, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, true, false, GUILTY_REQUEST_HEARING, true, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                {true, true, false, NOT_GUILTY, true, true, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, true, false, NOT_GUILTY, true, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, true, false, NOT_GUILTY, false, true, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, true, false, NOT_GUILTY, false, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                {true, true, false, null, false, true, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, true, false, null, false, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},


                {true, false, true, GUILTY, true, true, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, false, true, GUILTY, true, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, false, true, GUILTY, false, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, false, true, GUILTY, false, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                {true, false, true, GUILTY_REQUEST_HEARING, true, true, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, false, true, GUILTY_REQUEST_HEARING, true, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, false, true, GUILTY_REQUEST_HEARING, false, true, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, false, true, GUILTY_REQUEST_HEARING, true, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                {true, false, true, NOT_GUILTY, true, true, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, false, true, NOT_GUILTY, true, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, false, true, NOT_GUILTY, false, true, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, false, true, NOT_GUILTY, false, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                {true, false, true, null, false, true, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, false, true, null, false, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},


                {true, false, false, GUILTY, true, true, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, false, false, GUILTY, true, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, false, false, GUILTY, false, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, false, false, GUILTY, false, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                {true, false, false, GUILTY_REQUEST_HEARING, true, true, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, false, false, GUILTY_REQUEST_HEARING, true, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, false, false, GUILTY_REQUEST_HEARING, false, true, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, false, false, GUILTY_REQUEST_HEARING, true, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                {true, false, false, NOT_GUILTY, true, true, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, false, false, NOT_GUILTY, true, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, false, false, NOT_GUILTY, false, true, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, false, false, NOT_GUILTY, false, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},

                {true, false, false, null, false, true, WITHDRAWAL_REQUEST_READY_FOR_DECISION},
                {true, false, false, null, false, false, WITHDRAWAL_REQUEST_READY_FOR_DECISION},


                {false, true, true, GUILTY, true, true, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {false, true, true, GUILTY, true, false, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {false, true, true, GUILTY, false, false, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {false, true, true, GUILTY, false, false, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                {false, true, true, GUILTY_REQUEST_HEARING, true, true, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {false, true, true, GUILTY_REQUEST_HEARING, true, false, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {false, true, true, GUILTY_REQUEST_HEARING, false, true, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {false, true, true, GUILTY_REQUEST_HEARING, true, false, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                {false, true, true, NOT_GUILTY, true, true, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {false, true, true, NOT_GUILTY, true, false, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {false, true, true, NOT_GUILTY, false, true, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {false, true, true, NOT_GUILTY, false, false, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                {false, true, true, null, false, true, NO_PLEA_RECEIVED},
                {false, true, true, null, false, false, NO_PLEA_RECEIVED},


                {false, true, false, GUILTY, true, true, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {false, true, false, GUILTY, true, false, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {false, true, false, GUILTY, false, false, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {false, true, false, GUILTY, false, false, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                {false, true, false, GUILTY_REQUEST_HEARING, true, true, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {false, true, false, GUILTY_REQUEST_HEARING, true, false, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {false, true, false, GUILTY_REQUEST_HEARING, false, true, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {false, true, false, GUILTY_REQUEST_HEARING, true, false, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                {false, true, false, NOT_GUILTY, true, true, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {false, true, false, NOT_GUILTY, true, false, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {false, true, false, NOT_GUILTY, false, true, PLEA_RECEIVED_NOT_READY_FOR_DECISION},
                {false, true, false, NOT_GUILTY, false, false, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                {false, true, false, null, false, true, NO_PLEA_RECEIVED},
                {false, true, false, null, false, false, NO_PLEA_RECEIVED},


                {false, false, true, GUILTY, true, true, PLEA_RECEIVED_READY_FOR_DECISION},
                {false, false, true, GUILTY, true, false, PLEA_RECEIVED_READY_FOR_DECISION},
                {false, false, true, GUILTY, false, false, PLEA_RECEIVED_READY_FOR_DECISION},
                {false, false, true, GUILTY, false, false, PLEA_RECEIVED_READY_FOR_DECISION},

                {false, false, true, GUILTY_REQUEST_HEARING, true, true, PLEA_RECEIVED_READY_FOR_DECISION},
                {false, false, true, GUILTY_REQUEST_HEARING, true, false, PLEA_RECEIVED_READY_FOR_DECISION},
                {false, false, true, GUILTY_REQUEST_HEARING, false, true, PLEA_RECEIVED_READY_FOR_DECISION},
                {false, false, true, GUILTY_REQUEST_HEARING, true, false, PLEA_RECEIVED_READY_FOR_DECISION},

                {false, false, true, NOT_GUILTY, true, true, PLEA_RECEIVED_READY_FOR_DECISION},
                {false, false, true, NOT_GUILTY, true, false, PLEA_RECEIVED_READY_FOR_DECISION},
                {false, false, true, NOT_GUILTY, false, true, PLEA_RECEIVED_READY_FOR_DECISION},
                {false, false, true, NOT_GUILTY, false, false, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                {false, false, true, null, false, true, NO_PLEA_RECEIVED_READY_FOR_DECISION},
                {false, false, true, null, false, false, NO_PLEA_RECEIVED_READY_FOR_DECISION},


                {false, false, false, GUILTY, true, true, PLEA_RECEIVED_READY_FOR_DECISION},
                {false, false, false, GUILTY, true, false, PLEA_RECEIVED_READY_FOR_DECISION},
                {false, false, false, GUILTY, false, false, PLEA_RECEIVED_READY_FOR_DECISION},
                {false, false, false, GUILTY, false, false, PLEA_RECEIVED_READY_FOR_DECISION},

                {false, false, false, GUILTY_REQUEST_HEARING, true, true, PLEA_RECEIVED_READY_FOR_DECISION},
                {false, false, false, GUILTY_REQUEST_HEARING, true, false, PLEA_RECEIVED_READY_FOR_DECISION},
                {false, false, false, GUILTY_REQUEST_HEARING, false, true, PLEA_RECEIVED_READY_FOR_DECISION},
                {false, false, false, GUILTY_REQUEST_HEARING, true, false, PLEA_RECEIVED_READY_FOR_DECISION},

                {false, false, false, NOT_GUILTY, true, true, PLEA_RECEIVED_READY_FOR_DECISION},
                {false, false, false, NOT_GUILTY, true, false, PLEA_RECEIVED_READY_FOR_DECISION},
                {false, false, false, NOT_GUILTY, false, true, PLEA_RECEIVED_READY_FOR_DECISION},
                {false, false, false, NOT_GUILTY, false, false, PLEA_RECEIVED_NOT_READY_FOR_DECISION},

                {false, false, false, null, false, true, NO_PLEA_RECEIVED},
                {false, false, false, null, false, false, NO_PLEA_RECEIVED},
        });
    }


    @Test
    public void shouldReturnStatusReopenedInLibraIfCaseCompletedAndReopened() {
        final CaseStatus caseStatus = calculateStatus(postingDate, withdrawalRequested, pleaInformation, datesToAvoid, true, false, LocalDate.now(), adjourned);

        assertThat(caseStatus, is(CaseStatus.REOPENED_IN_LIBRA));
    }

    @Test
    public void shouldSetStatusReferredToCourtWhenCaseCompletedAndReferredToCourt() {
        final CaseStatus caseStatus = calculateStatus(postingDate, withdrawalRequested, pleaInformation, datesToAvoid, true, true, null, adjourned);

        assertThat(caseStatus, is(CaseStatus.REFERRED_FOR_COURT_HEARING));
    }

    @Test
    public void shouldSetStatusCompletedIfCaseCompletedButNotReferredNorReopened() {
        final CaseStatus caseStatus = calculateStatus(postingDate, withdrawalRequested, pleaInformation, datesToAvoid, true, false, null, adjourned);

        assertThat(caseStatus, is(CaseStatus.COMPLETED));
    }

    @Test
    public void shouldSetStatusForNotCompletedCases() {
        final CaseStatus caseStatus = calculateStatus(postingDate, withdrawalRequested, pleaInformation, datesToAvoid, false, false, null, adjourned);

        assertThat(caseStatus, is(expectedCaseStatus));
    }
}