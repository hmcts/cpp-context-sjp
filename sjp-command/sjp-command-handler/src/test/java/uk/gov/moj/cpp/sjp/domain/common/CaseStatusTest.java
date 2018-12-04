package uk.gov.moj.cpp.sjp.domain.common;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.time.LocalDate;

import org.junit.Test;

public class CaseStatusTest {

    private final LocalDate FIVE_DAYS_AGO = LocalDate.now().minusDays(5);
    private final LocalDate TWENTY_EIGHT_DAYS_AGO = LocalDate.now().minusDays(28);
    private final LocalDate TWENTY_DAYS_AGO = LocalDate.now().minusDays(20);
    private final LocalDate NINE_DAYS_AGO = LocalDate.now().minusDays(9);
    private final LocalDate TEN_DAYS_AGO = LocalDate.now().minusDays(10);

    @Test
    public void noPleaReceivedWhenCaseIsNotPIA() {
        final CaseStatus caseStatus = CaseStatus.calculateStatus(FIVE_DAYS_AGO, false, new PleaInformation(null, TEN_DAYS_AGO), null, false, false, null);

        assertThat(caseStatus, is(CaseStatus.NO_PLEA_RECEIVED));
    }

    @Test
    public void noPleaReceivedWhenCaseIsPIA() {
        final CaseStatus caseStatus = CaseStatus.calculateStatus(TWENTY_EIGHT_DAYS_AGO, false, new PleaInformation(null, TEN_DAYS_AGO), null, false, false, null);

        assertThat(caseStatus, is(CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION));
    }

    @Test
    public void requestedWithdrawalWhenCaseIsPIA() {
        final CaseStatus caseStatus = CaseStatus.calculateStatus(TWENTY_EIGHT_DAYS_AGO, true, new PleaInformation(null, TEN_DAYS_AGO), null, false, false, null);

        assertThat(caseStatus, is(CaseStatus.WITHDRAWAL_REQUEST_READY_FOR_DECISION));

    }

    @Test
    public void requestedWithdrawalWhenCaseIsNotPIA() {
        final CaseStatus caseStatus = CaseStatus.calculateStatus(TWENTY_DAYS_AGO, true, new PleaInformation(null, TEN_DAYS_AGO), null, false, false, null);

        assertThat(caseStatus, is(CaseStatus.WITHDRAWAL_REQUEST_READY_FOR_DECISION));

    }

    @Test
    public void pleaReceivedReadyWhenGuiltyPlea() {
        final CaseStatus caseStatus = CaseStatus.calculateStatus(TWENTY_DAYS_AGO, false, new PleaInformation(PleaType.GUILTY, TEN_DAYS_AGO), null, false, false, null);

        assertThat(caseStatus, is(CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION));

    }

    @Test
    public void pleaReceivedReadyWhenGuiltyRequestHearing() {
        final CaseStatus caseStatus = CaseStatus.calculateStatus(TWENTY_DAYS_AGO, false, new PleaInformation(PleaType.GUILTY_REQUEST_HEARING, TEN_DAYS_AGO), null, false, false, null);

        assertThat(caseStatus, is(CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION));

    }

    @Test
    public void pleaReceivedNotReadyWhenNotGuiltyPlea() {
        final CaseStatus caseStatus = CaseStatus.calculateStatus(TWENTY_DAYS_AGO, false, new PleaInformation(PleaType.NOT_GUILTY, TEN_DAYS_AGO), null, false, false, null);

        assertThat(caseStatus, is(CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION));
    }

    @Test
    public void pleaReceivedReadyWhenNotGuiltyPleaOver10DaysAgo() {
        final CaseStatus caseStatus = CaseStatus.calculateStatus(TWENTY_EIGHT_DAYS_AGO, false, new PleaInformation(PleaType.NOT_GUILTY, NINE_DAYS_AGO), null, false, false, null);

        assertThat(caseStatus, is(CaseStatus.PLEA_RECEIVED_NOT_READY_FOR_DECISION));
    }

    @Test
    public void pleaReceivedReadyWhenNotGuiltyPleaWithDatesToAvoid() {
        final CaseStatus caseStatus = CaseStatus.calculateStatus(TWENTY_EIGHT_DAYS_AGO, false, new PleaInformation(PleaType.NOT_GUILTY, NINE_DAYS_AGO), "All mondays", false, false, null);

        assertThat(caseStatus, is(CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION));
    }

    @Test
    public void completedWhenTheCaseIsCompleted() {
        final CaseStatus caseStatus = CaseStatus.calculateStatus(TWENTY_EIGHT_DAYS_AGO, false, new PleaInformation(PleaType.NOT_GUILTY, NINE_DAYS_AGO), "All mondays", true, false, null);

        assertThat(caseStatus, is(CaseStatus.COMPLETED));
    }

    @Test
    public void reopenedInLibraWhenTheReopenedDateIsProvided() {
        final CaseStatus caseStatus = CaseStatus.calculateStatus(TWENTY_EIGHT_DAYS_AGO, false, new PleaInformation(PleaType.NOT_GUILTY, NINE_DAYS_AGO), "All mondays", true, false, LocalDate.now());

        assertThat(caseStatus, is(CaseStatus.REOPENED_IN_LIBRA));
    }

    @Test
    public void referredToCourtWhenTheCaseIsReferredToCourt() {
        final CaseStatus caseStatus = CaseStatus.calculateStatus(TWENTY_EIGHT_DAYS_AGO, false, new PleaInformation(PleaType.NOT_GUILTY, NINE_DAYS_AGO), "All mondays", true, true, null);

        assertThat(caseStatus, is(CaseStatus.REFERRED_FOR_COURT_HEARING));
    }

}