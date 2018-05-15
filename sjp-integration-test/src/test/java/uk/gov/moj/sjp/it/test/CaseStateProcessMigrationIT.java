package uk.gov.moj.sjp.it.test;

import static java.time.ZoneOffset.UTC;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PIA;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.WITHDRAWAL_REQUESTED;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.moj.sjp.it.Constants.EVENT_CASE_MARKED_READY_FOR_DECISION;
import static uk.gov.moj.sjp.it.Constants.EVENT_SELECTOR_CASE_COMPLETED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SELECTOR_CASE_CREATED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SELECTOR_PLEA_UPDATED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubAddAssignmentCommand;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetEmptyAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubRemoveAssignmentCommand;
import static uk.gov.moj.sjp.it.stub.ResultingStub.stubGetCaseDecisionsWithNoDecision;
import static uk.gov.moj.sjp.it.util.ActivitiHelper.signalProcessesInstanceId;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CancelPleaHelper;
import uk.gov.moj.sjp.it.helper.EventedListener;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestCancelHelper;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestHelper;
import uk.gov.moj.sjp.it.helper.UpdatePleaHelper;
import uk.gov.moj.sjp.it.producer.CompleteCaseProducer;
import uk.gov.moj.sjp.it.util.ActivitiHelper;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

//TODO ATCM-3133 - remove
public class CaseStateProcessMigrationIT extends BaseIntegrationTest {

    private final static String PROCESS_NAME = "caseState";
    private final static String PROCESS_MIGRATION_VARIABLE = "processMigration";
    private final UUID caseId = UUID.randomUUID();
    private final UUID offenceId = UUID.randomUUID();
    private final EventedListener eventListener = new EventedListener().withMaxWaiTime(5000);

    @Before
    public void setUp() {
        stubGetCaseDecisionsWithNoDecision(caseId);
        stubGetEmptyAssignmentsByDomainObjectId(caseId);
        stubAddAssignmentCommand();
        stubRemoveAssignmentCommand();

        final CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder
                .withDefaults().withId(caseId).withOffenceId(offenceId).withPostingDate(LocalDate.now());
        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);

        //delete case state process so that it can be recreated by migration
        final String processInstanceId = ActivitiHelper.pollUntilProcessExists(PROCESS_NAME, caseId.toString());
        ActivitiHelper.deleteProcessInstance(processInstanceId);
    }

    @Test
    public void shouldCreateExactCaseStateProcessUsingActivitiRestApiSoThatCaseCanBeProgressedTillCompletion() {

        final LocalDate postingDate = LocalDate.now().minusDays(30);

        final Map<String, Object> processParameters = new HashMap<>();
        processParameters.put("postingDate", postingDate.toString());
        processParameters.put("noticeEndedDate", postingDate.plusDays(28).toString());
        processParameters.put("metadata", metadataBuilder().withName("migration").withId(UUID.randomUUID()).build().asJsonObject().toString());
        processParameters.put("processMigration", true);


        final String processInstanceId = createProcessUsingActivitiApi(processParameters);

        final Optional<JsonEnvelope> publicCaseCreatedEvent = eventListener.popEvent(PUBLIC_EVENT_SELECTOR_CASE_CREATED);
        final Optional<JsonEnvelope> caseMarkedReadyForDecision = eventListener.popEvent(EVENT_CASE_MARKED_READY_FOR_DECISION);

        assertEventAbsent(publicCaseCreatedEvent);
        assertEventPresent(caseMarkedReadyForDecision);

        final ZonedDateTime actualMarkedReadyTimestamp = ZonedDateTime.parse(caseMarkedReadyForDecision.get().payloadAsJsonObject().getString("markedAt"));
        final ZonedDateTime expectedMarkedReadyTimestamp = ZonedDateTime.of(postingDate.plusDays(28).atStartOfDay(), UTC);
        assertThat(actualMarkedReadyTimestamp, equalTo(expectedMarkedReadyTimestamp));

        updatePleaUsingActivitiApi(processInstanceId, GUILTY);
        assertEventAbsent(eventListener.popEvent(PUBLIC_EVENT_SELECTOR_PLEA_UPDATED));
        assertMarkedReady(eventListener.popEvent(EVENT_CASE_MARKED_READY_FOR_DECISION), PLEADED_GUILTY);

        requestAllOffencesWithdrawalUsingActivitiApi(processInstanceId);
        assertEventAbsent(eventListener.popEvent(PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED));
        assertMarkedReady(eventListener.popEvent(EVENT_CASE_MARKED_READY_FOR_DECISION), WITHDRAWAL_REQUESTED);

        cancelAllOffencesWithdrawalRequestUsingActivitiApi(processInstanceId);
        assertEventAbsent(eventListener.popEvent(PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED));
        assertMarkedReady(eventListener.popEvent(EVENT_CASE_MARKED_READY_FOR_DECISION), PLEADED_GUILTY);

        cancelPleaUsingActivitiApi(processInstanceId);
        assertEventAbsent(eventListener.popEvent(PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED));
        assertMarkedReady(eventListener.popEvent(EVENT_CASE_MARKED_READY_FOR_DECISION), PIA);

        updatePleaUsingCommand(GUILTY_REQUEST_HEARING);
        assertEventPresent(eventListener.popEvent(PUBLIC_EVENT_SELECTOR_PLEA_UPDATED));
        assertMarkedReady(eventListener.popEvent(EVENT_CASE_MARKED_READY_FOR_DECISION), PLEADED_GUILTY_REQUEST_HEARING);

        requestAllOffencesWithdrawalUsingCommand();
        assertEventPresent(eventListener.popEvent(PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED));
        assertMarkedReady(eventListener.popEvent(EVENT_CASE_MARKED_READY_FOR_DECISION), WITHDRAWAL_REQUESTED);

        cancelAllOffencesWithdrawalRequestUsingCommand();
        assertEventPresent(eventListener.popEvent(PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED));
        assertMarkedReady(eventListener.popEvent(EVENT_CASE_MARKED_READY_FOR_DECISION), PLEADED_GUILTY_REQUEST_HEARING);

        cancelPleaUsingCommand();
        assertEventPresent(eventListener.popEvent(PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED));
        assertMarkedReady(eventListener.popEvent(EVENT_CASE_MARKED_READY_FOR_DECISION), PIA);

        saveDecisionUsingCommand();
        assertEventPresent(eventListener.popEvent(EVENT_SELECTOR_CASE_COMPLETED));

        ActivitiHelper.pollUntilProcessDoesNotExist(PROCESS_NAME, caseId.toString());
    }

    private static void assertMarkedReady(final Optional<JsonEnvelope> markedForDecisionEvent, final CaseReadinessReason expectedReadinessReason) {
        assertEventPresent(markedForDecisionEvent);
        assertThat(markedForDecisionEvent.get().payloadAsJsonObject().getString("reason"), equalTo(expectedReadinessReason.name()));
    }

    private static void assertEventPresent(final Optional<JsonEnvelope> event) {
        assertThat(event.isPresent(), equalTo(true));
    }

    private static void assertEventAbsent(final Optional<JsonEnvelope> event) {
        assertThat(event.isPresent(), equalTo(false));
    }

    private String createProcessUsingActivitiApi(final Map<String, Object> processParameters) {
        eventListener
                .subscribe(PUBLIC_EVENT_SELECTOR_CASE_CREATED)
                .subscribe(EVENT_CASE_MARKED_READY_FOR_DECISION)
                .run(() -> ActivitiHelper.createProcessInstance(PROCESS_NAME, caseId.toString(), processParameters));
        return ActivitiHelper.pollUntilProcessExists(PROCESS_NAME, caseId.toString());
    }

    private void updatePleaUsingActivitiApi(final String processInstanceId, final PleaType pleaType) {
        eventListener
                .reset()
                .subscribe(PUBLIC_EVENT_SELECTOR_PLEA_UPDATED)
                .subscribe(EVENT_CASE_MARKED_READY_FOR_DECISION)
                .run(() -> signalProcessesInstanceId(processInstanceId, "pleaUpdated", ImmutableMap.of(PROCESS_MIGRATION_VARIABLE, true, "pleaType", pleaType.name())));

    }

    private void cancelPleaUsingActivitiApi(final String processInstanceId) {
        eventListener
                .reset()
                .subscribe(PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED)
                .subscribe(EVENT_CASE_MARKED_READY_FOR_DECISION)
                .run(() -> signalProcessesInstanceId(processInstanceId, "pleaCancelled", singletonMap(PROCESS_MIGRATION_VARIABLE, true)));
    }

    private void requestAllOffencesWithdrawalUsingActivitiApi(final String processInstanceId) {
        eventListener
                .reset()
                .subscribe(PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED)
                .subscribe(EVENT_CASE_MARKED_READY_FOR_DECISION)
                .run(() -> signalProcessesInstanceId(processInstanceId, "withdrawalRequested", singletonMap(PROCESS_MIGRATION_VARIABLE, true)));
    }

    private void cancelAllOffencesWithdrawalRequestUsingActivitiApi(final String processInstanceId) {
        eventListener
                .reset()
                .subscribe(PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED)
                .subscribe(EVENT_CASE_MARKED_READY_FOR_DECISION)
                .run(() -> signalProcessesInstanceId(processInstanceId, "withdrawalRequestCancelled", singletonMap(PROCESS_MIGRATION_VARIABLE, true)));
    }

    private void updatePleaUsingCommand(final PleaType pleaType) {
        eventListener
                .reset()
                .subscribe(PUBLIC_EVENT_SELECTOR_PLEA_UPDATED)
                .subscribe(EVENT_CASE_MARKED_READY_FOR_DECISION)
                .run(() -> new UpdatePleaHelper().updatePlea(caseId, offenceId, pleaType));
    }

    private void cancelPleaUsingCommand() {
        eventListener
                .reset()
                .subscribe(PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED)
                .subscribe(EVENT_CASE_MARKED_READY_FOR_DECISION)
                .run(() -> new CancelPleaHelper(caseId, offenceId).cancelPlea());
    }


    private void requestAllOffencesWithdrawalUsingCommand() {
        eventListener
                .reset()
                .subscribe(PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED)
                .subscribe(EVENT_CASE_MARKED_READY_FOR_DECISION)
                .run(() -> new OffencesWithdrawalRequestHelper(caseId).requestWithdrawalForAllOffences(USER_ID));
    }

    private void cancelAllOffencesWithdrawalRequestUsingCommand() {
        eventListener
                .reset()
                .subscribe(PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED)
                .subscribe(EVENT_CASE_MARKED_READY_FOR_DECISION)
                .run(() -> new OffencesWithdrawalRequestCancelHelper(caseId).cancelRequestWithdrawalForAllOffences(USER_ID));
    }

    private void saveDecisionUsingCommand() {
        eventListener
                .reset()
                .subscribe(EVENT_SELECTOR_CASE_COMPLETED)
                .run(() -> new CompleteCaseProducer(caseId).completeCase());
    }

}
