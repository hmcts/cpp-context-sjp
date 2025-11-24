package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.json.schemas.domains.sjp.User.user;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.WithdrawalRequestsStatus;
import uk.gov.moj.cpp.json.schemas.domains.sjp.event.OffencesWithdrawalRequestsStatusSet;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;
import uk.gov.moj.cpp.sjp.event.OffenceWithdrawalRequestCancelled;
import uk.gov.moj.cpp.sjp.event.OffenceWithdrawalRequestReasonChanged;
import uk.gov.moj.cpp.sjp.event.OffenceWithdrawalRequested;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OffenceWithdrawalHandlerTest {

    private final UUID withdrawalRequestReasonId = randomUUID();
    private final OffenceWithdrawalHandler offenceWithdrawalHandler = OffenceWithdrawalHandler.INSTANCE;

    private final UUID caseID = randomUUID();

    private final User user = user().withUserId(randomUUID()).withFirstName("Donald").withLastName("Trump").build();

    private CaseAggregateState caseAggregateState;

    @BeforeEach
    public void onceBeforeEachTest() {
        caseAggregateState = new CaseAggregateState();
        caseAggregateState.setCaseId(caseID);
        caseAggregateState.setProsecutingAuthority("ALL");
    }

    @Test
    public void requestForOffenceWithdrawal() {
        final List<WithdrawalRequestsStatus> offenceWithdrawalDetails = new ArrayList<>();
        final UUID offenceId = randomUUID();
        offenceWithdrawalDetails.add(new WithdrawalRequestsStatus(offenceId, withdrawalRequestReasonId));
        final ZonedDateTime now = ZonedDateTime.now();
        final List<Object> eventList = offenceWithdrawalHandler.requestOffenceWithdrawal(caseID, user.getUserId(), now, offenceWithdrawalDetails, caseAggregateState, "ALL").collect(toList());
        final List<uk.gov.justice.json.schemas.fragments.sjp.WithdrawalRequestsStatus> offenceWithdrawalList = convertWtihdrawalRequestsStatus(offenceWithdrawalDetails);
        assertThat(eventList, containsInAnyOrder(new OffencesWithdrawalRequestsStatusSet(caseID, now, user.getUserId(), offenceWithdrawalList),
                new OffenceWithdrawalRequested(caseID, offenceId, withdrawalRequestReasonId, user.getUserId(), now)));
    }

    @Test
    public void requestForOffenceWithdrawalOnACompletedCaseShouldBeRejected(){
        caseAggregateState.markCaseCompleted();
        final UUID offenceId = randomUUID();
        final List<WithdrawalRequestsStatus> offenceWithdrawalDetails = newArrayList(new WithdrawalRequestsStatus(offenceId, withdrawalRequestReasonId));
        final ZonedDateTime now = ZonedDateTime.now();
        final List<Object> eventList = offenceWithdrawalHandler.requestOffenceWithdrawal(caseID, user.getUserId(), now, offenceWithdrawalDetails, caseAggregateState, "ALL").collect(toList());
        assertThat(eventList, containsInAnyOrder(
                new CaseUpdateRejected(caseID, CaseUpdateRejected.RejectReason.CASE_COMPLETED)
        ));
    }

    @Test
    public void requestForOffenceWithdrawalOnAnAssignedCaseShouldBeRejected(){
        caseAggregateState.setAssigneeId(randomUUID());
        final UUID offenceId = randomUUID();
        final List<WithdrawalRequestsStatus> offenceWithdrawalDetails = newArrayList(new WithdrawalRequestsStatus(offenceId, withdrawalRequestReasonId));
        final ZonedDateTime now = ZonedDateTime.now();
        final List<Object> eventList = offenceWithdrawalHandler.requestOffenceWithdrawal(caseID, user.getUserId(), now, offenceWithdrawalDetails, caseAggregateState, "ALL").collect(toList());
        assertThat(eventList, containsInAnyOrder(
                new CaseUpdateRejected(caseID, CaseUpdateRejected.RejectReason.CASE_ASSIGNED)
        ));
    }

    @Test
    public void requestForMultipleOffenceWithdrawal() {
        final List<WithdrawalRequestsStatus> offenceWithdrawalDetails = new ArrayList<>();
        final UUID offenceId = randomUUID();
        offenceWithdrawalDetails.add(new WithdrawalRequestsStatus(offenceId, withdrawalRequestReasonId));
        final UUID offenceId_1 = randomUUID();
        offenceWithdrawalDetails.add(new WithdrawalRequestsStatus(offenceId_1, withdrawalRequestReasonId));
        final UUID offenceId_2 = randomUUID();
        offenceWithdrawalDetails.add(new WithdrawalRequestsStatus(offenceId_2, withdrawalRequestReasonId));
        final ZonedDateTime now = ZonedDateTime.now();
        final List<Object> eventList = offenceWithdrawalHandler.requestOffenceWithdrawal(caseID, user.getUserId(), now, offenceWithdrawalDetails, caseAggregateState, "ALL").collect(toList());
        final List<uk.gov.justice.json.schemas.fragments.sjp.WithdrawalRequestsStatus> offenceWithdrawalList = convertWtihdrawalRequestsStatus(offenceWithdrawalDetails);

        assertThat(eventList, containsInAnyOrder(new OffencesWithdrawalRequestsStatusSet(caseID, now, user.getUserId(), offenceWithdrawalList),
                new OffenceWithdrawalRequested(caseID, offenceId, withdrawalRequestReasonId, user.getUserId(), now),
                new OffenceWithdrawalRequested(caseID, offenceId_1, withdrawalRequestReasonId, user.getUserId(), now),
                new OffenceWithdrawalRequested(caseID, offenceId_2, withdrawalRequestReasonId, user.getUserId(), now)));
    }

    @Test
    public void cancelOffenceWithdrawalRequest() {
        final UUID offenceId = randomUUID();
        caseAggregateState.addWithdrawnOffences(new WithdrawalRequestsStatus(offenceId, withdrawalRequestReasonId));

        final List<WithdrawalRequestsStatus> offenceWithdrawalDetails = new ArrayList<>();
        final ZonedDateTime after2Days = ZonedDateTime.now().plusDays(2);
        final List<Object> cancelEventList = offenceWithdrawalHandler.requestOffenceWithdrawal(caseID, user.getUserId(), after2Days, offenceWithdrawalDetails, caseAggregateState, "ALL").collect(toList());

        final List<uk.gov.justice.json.schemas.fragments.sjp.WithdrawalRequestsStatus> offenceWithdrawalList = convertWtihdrawalRequestsStatus(offenceWithdrawalDetails);

        assertThat(cancelEventList, containsInAnyOrder(new OffencesWithdrawalRequestsStatusSet(caseID, after2Days, user.getUserId(), offenceWithdrawalList),
                new OffenceWithdrawalRequestCancelled(caseID, offenceId, user.getUserId(), after2Days)));
    }

    @Test
    public void cancelMultipleOffenceWithdrawalRequests() {
        final UUID offenceId = randomUUID();
        caseAggregateState.addWithdrawnOffences(new WithdrawalRequestsStatus(offenceId, withdrawalRequestReasonId));
        final UUID offenceId_1 = randomUUID();
        caseAggregateState.addWithdrawnOffences(new WithdrawalRequestsStatus(offenceId_1, withdrawalRequestReasonId));
        final UUID offenceId_2 = randomUUID();
        caseAggregateState.addWithdrawnOffences(new WithdrawalRequestsStatus(offenceId_2, withdrawalRequestReasonId));
        final UUID offenceId_3 = randomUUID();
        caseAggregateState.addWithdrawnOffences(new WithdrawalRequestsStatus(offenceId_3, withdrawalRequestReasonId));

        final List<WithdrawalRequestsStatus> offenceWithdrawalDetails = new ArrayList<>();
        offenceWithdrawalDetails.add(new WithdrawalRequestsStatus(offenceId, withdrawalRequestReasonId));
        offenceWithdrawalDetails.add(new WithdrawalRequestsStatus(offenceId_1, withdrawalRequestReasonId));
        final ZonedDateTime after2Days = ZonedDateTime.now().plusDays(2);
        final List<Object> cancelEventList = offenceWithdrawalHandler.requestOffenceWithdrawal(caseID, user.getUserId(), after2Days, offenceWithdrawalDetails, caseAggregateState, "ALL").collect(toList());
        final List<uk.gov.justice.json.schemas.fragments.sjp.WithdrawalRequestsStatus> offenceWithdrawalList = convertWtihdrawalRequestsStatus(offenceWithdrawalDetails);

        assertThat(cancelEventList, containsInAnyOrder(new OffencesWithdrawalRequestsStatusSet(caseID, after2Days, user.getUserId(), offenceWithdrawalList),
                new OffenceWithdrawalRequestCancelled(caseID, offenceId_2, user.getUserId(), after2Days),
                new OffenceWithdrawalRequestCancelled(caseID, offenceId_3, user.getUserId(), after2Days)));
    }

    @Test
    public void requestForOffenceWithdrawalWithDifferentReason() {
        final UUID offenceId = randomUUID();
        caseAggregateState.addWithdrawnOffences(new WithdrawalRequestsStatus(offenceId, withdrawalRequestReasonId));

        final UUID offenceNewWithdrawalRequestReasonId = randomUUID();
        final List<WithdrawalRequestsStatus> offenceWithdrawalDetails = new ArrayList<>();
        offenceWithdrawalDetails.add(new WithdrawalRequestsStatus(offenceId, offenceNewWithdrawalRequestReasonId));
        final ZonedDateTime after2Days = ZonedDateTime.now().plusDays(2);
         final List<Object> reasonChangedEventList = offenceWithdrawalHandler.requestOffenceWithdrawal(caseID, user.getUserId(), after2Days, offenceWithdrawalDetails, caseAggregateState, "ALL").collect(toList());
        final List<uk.gov.justice.json.schemas.fragments.sjp.WithdrawalRequestsStatus> offenceWithdrawalList = convertWtihdrawalRequestsStatus(offenceWithdrawalDetails);

        assertThat(reasonChangedEventList, containsInAnyOrder(new OffencesWithdrawalRequestsStatusSet(caseID, after2Days, user.getUserId(), offenceWithdrawalList),
                new OffenceWithdrawalRequestReasonChanged(caseID, offenceId, user.getUserId(), after2Days, offenceNewWithdrawalRequestReasonId, withdrawalRequestReasonId)));
    }

    @Test
    public void requestForMultipleOffenceWithdrawalWithDifferentReason() {
        final UUID offenceId = randomUUID();
        caseAggregateState.addWithdrawnOffences(new WithdrawalRequestsStatus(offenceId, withdrawalRequestReasonId));
        final UUID offenceId_1 = randomUUID();
        caseAggregateState.addWithdrawnOffences(new WithdrawalRequestsStatus(offenceId_1, withdrawalRequestReasonId));

        final UUID offenceNewWithdrawalRequestReasonId = randomUUID();
        final List<WithdrawalRequestsStatus> offenceWithdrawalDetails = new ArrayList<>();
        offenceWithdrawalDetails.add(new WithdrawalRequestsStatus(offenceId, offenceNewWithdrawalRequestReasonId));
        offenceWithdrawalDetails.add(new WithdrawalRequestsStatus(offenceId_1, offenceNewWithdrawalRequestReasonId));
        final ZonedDateTime after2Days = ZonedDateTime.now().plusDays(2);
        final List<Object> reasonChangedEventList = offenceWithdrawalHandler.requestOffenceWithdrawal(caseID, user.getUserId(), after2Days, offenceWithdrawalDetails, caseAggregateState, "ALL").collect(toList());
        final List<uk.gov.justice.json.schemas.fragments.sjp.WithdrawalRequestsStatus> offenceWithdrawalList = convertWtihdrawalRequestsStatus(offenceWithdrawalDetails);

        assertThat(reasonChangedEventList, containsInAnyOrder(new OffencesWithdrawalRequestsStatusSet(caseID, after2Days, user.getUserId(), offenceWithdrawalList),
                new OffenceWithdrawalRequestReasonChanged(caseID, offenceId, user.getUserId(), after2Days, offenceNewWithdrawalRequestReasonId, withdrawalRequestReasonId),
                new OffenceWithdrawalRequestReasonChanged(caseID, offenceId_1, user.getUserId(), after2Days, offenceNewWithdrawalRequestReasonId, withdrawalRequestReasonId)));
    }

    @Test
    public void requestCancelAndReasonChangedForMultipleOffences() {
        final UUID offenceId = randomUUID();
        caseAggregateState.addWithdrawnOffences(new WithdrawalRequestsStatus(offenceId, withdrawalRequestReasonId));
        final UUID offenceId_1 = randomUUID();
        caseAggregateState.addWithdrawnOffences(new WithdrawalRequestsStatus(offenceId_1, withdrawalRequestReasonId));
        final UUID offenceId_2 = randomUUID();
        caseAggregateState.addWithdrawnOffences(new WithdrawalRequestsStatus(offenceId_2, withdrawalRequestReasonId));
        final UUID offenceId_3 = randomUUID();
        caseAggregateState.addWithdrawnOffences(new WithdrawalRequestsStatus(offenceId_3, withdrawalRequestReasonId));

        final UUID offenceNewWithdrawalRequestReasonId = randomUUID();
        List<WithdrawalRequestsStatus> offenceWithdrawalDetails = new ArrayList<>();
        offenceWithdrawalDetails.add(new WithdrawalRequestsStatus(offenceId, offenceNewWithdrawalRequestReasonId));
        offenceWithdrawalDetails.add(new WithdrawalRequestsStatus(offenceId_1, offenceNewWithdrawalRequestReasonId));
        final ZonedDateTime after2Days = ZonedDateTime.now().plusDays(2);

        final List<Object> eventList = offenceWithdrawalHandler.requestOffenceWithdrawal(caseID, user.getUserId(), after2Days, offenceWithdrawalDetails, caseAggregateState, "ALL").collect(toList());
        final List<uk.gov.justice.json.schemas.fragments.sjp.WithdrawalRequestsStatus> offenceWithdrawalList = convertWtihdrawalRequestsStatus(offenceWithdrawalDetails);

        assertThat(eventList, containsInAnyOrder(new OffencesWithdrawalRequestsStatusSet(caseID, after2Days, user.getUserId(), offenceWithdrawalList),
                new OffenceWithdrawalRequestReasonChanged(caseID, offenceId, user.getUserId(), after2Days, offenceNewWithdrawalRequestReasonId, withdrawalRequestReasonId),
                new OffenceWithdrawalRequestReasonChanged(caseID, offenceId_1, user.getUserId(), after2Days, offenceNewWithdrawalRequestReasonId, withdrawalRequestReasonId),
                new OffenceWithdrawalRequestCancelled(caseID, offenceId_2, user.getUserId(), after2Days),
                new OffenceWithdrawalRequestCancelled(caseID, offenceId_3, user.getUserId(), after2Days)));
    }

    private List<uk.gov.justice.json.schemas.fragments.sjp.WithdrawalRequestsStatus> convertWtihdrawalRequestsStatus(List<WithdrawalRequestsStatus> withdrawalRequestsStatuses){

        return withdrawalRequestsStatuses.stream()
                        .map(requestStatus -> new uk.gov.justice.json.schemas.fragments.sjp.WithdrawalRequestsStatus.Builder()
                                .withOffenceId(requestStatus.getOffenceId())
                                .withWithdrawalRequestReasonId(requestStatus.getWithdrawalRequestReasonId())
                                .build()
                        )
                        .collect(Collectors.toList());
    }

}
