package uk.gov.moj.cpp.sjp.event.processor.activiti;

import static java.time.LocalDate.now;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.CASE_COMPLETED_SIGNAL_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.DATES_TO_AVOID_ADDED_SIGNAL_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.DATES_TO_AVOID_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.METADATA_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.OFFENCE_ID_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_CANCELLED_SIGNAL_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_DEADLINE_ADD_DATES_TO_AVOID_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_READY_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_TYPE_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_UPDATED_SIGNAL_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.POSTING_DATE_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PROVED_IN_ABSENCE_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.WITHDRAWAL_REQUESTED_SIGNAL_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.WITHDRAWAL_REQUESTED_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.WITHDRAWAL_REQUEST_CANCELLED_SIGNAL_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.DelegatesVerifier.Delegate.CASE_COMPLETED;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.DelegatesVerifier.Delegate.CASE_STARTED;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.DelegatesVerifier.Delegate.DATES_TO_AVOID_PROCESSED;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.DelegatesVerifier.Delegate.PLEA_CANCELLED;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.DelegatesVerifier.Delegate.PLEA_UPDATED;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.DelegatesVerifier.Delegate.PROVED_IN_ABSENCE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.DelegatesVerifier.Delegate.READY_CASE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.DelegatesVerifier.Delegate.WITHDRAWAL_REQUESTED;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.DelegatesVerifier.Delegate.WITHDRAWAL_REQUEST_CANCELLED;
import static uk.gov.moj.cpp.sjp.event.processor.matcher.DelegateExecutionProcessBusinessKeyMatcher.withProcessBusinessKey;
import static uk.gov.moj.cpp.sjp.event.processor.matcher.DelegateExecutionVariableMatcher.withProcessVariable;
import static uk.gov.moj.cpp.sjp.event.processor.matcher.DelegateExecutionVariableMatcher.withoutProcessVariable;
import static uk.gov.moj.cpp.sjp.event.processor.utils.MetadataHelper.metadataToString;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.processor.activiti.delegates.MockedDelegate;
import uk.gov.moj.cpp.sjp.event.processor.activiti.delegates.PleaUpdatedDelegate;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.activiti.engine.test.ActivitiRule;
import org.activiti.engine.test.Deployment;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class CaseStateProcessTest {

    private static final String PROCESS_PATH = "processes/case-state.bpmn20.xml";

    @Rule
    public ActivitiRule rule = new ActivitiRule();

    private DelegatesVerifier delegatesVerifier;

    private UUID caseId;
    private Metadata metadata;
    private CaseStateService caseStateService;

    @Before
    public void init() {
        caseId = randomUUID();
        metadata = metadataWithRandomUUIDAndName().build();
        caseStateService = new CaseStateService(new ActivitiService(rule.getRuntimeService()));
        delegatesVerifier = new DelegatesVerifier(rule);

        resetExecutions();
    }

    private void resetExecutions() {
        // reset manipulated parameters - the ones injected from the XML should should be reset as they were
        Stream.of(PLEA_CANCELLED, PLEA_UPDATED)
                .map(delegatesVerifier::getDelegateExecution)
                .forEach(delegate -> delegate.setVariablesOnExecution(emptyMap()));
    }

    @Test
    @Deployment(resources = PROCESS_PATH)
    public void shouldExecuteCaseStartedDelegateWhenProcessStarted() {
        final LocalDate postingDate = now().plusDays(1);

        final String processInstanceId = caseStateService.caseReceived(caseId, postingDate, metadata);

        delegatesVerifier.assertDelegateCalledWith(CASE_STARTED, 0, allOf(
                withProcessBusinessKey(caseId.toString()),
                withProcessVariable(POSTING_DATE_VARIABLE, postingDate.toString()),
                withProcessVariable(METADATA_VARIABLE, metadataToString(metadata))
        ));

        delegatesVerifier.verifyDelegatesInteractionWith(CASE_STARTED);

        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, CASE_COMPLETED_SIGNAL_NAME, is(1));
        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, WITHDRAWAL_REQUESTED_SIGNAL_NAME, is(1));
        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, WITHDRAWAL_REQUEST_CANCELLED_SIGNAL_NAME, is(1));
        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, PLEA_CANCELLED_SIGNAL_NAME, is(1));
        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, PLEA_UPDATED_SIGNAL_NAME, is(1));
        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, DATES_TO_AVOID_ADDED_SIGNAL_NAME, is(1));
        assertThat(delegatesVerifier.isActivitiItemRunning(processInstanceId, "waitForDatesToAvoidGateway"), equalTo(false));

        delegatesVerifier.assertProcessFinished(processInstanceId, false);
    }

    @Test
    @Deployment(resources = PROCESS_PATH)
    public void shouldAllowDatesToAvoidBeforePleaNotGuilty() {
        final LocalDate postingDate = now().minusDays(1);
        final UUID offenceId = randomUUID();
        final PleaType pleaType = PleaType.NOT_GUILTY;
        final String datesToAvoid = "my-dates-to-avoid";

        final String processInstanceId = caseStateService.caseReceived(caseId, postingDate, metadata);

        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, DATES_TO_AVOID_ADDED_SIGNAL_NAME, is(1));

        callAddDatesToAvoid(datesToAvoid);

        assertNotPossibleToReAddDatesToAvoid(processInstanceId);

        callPleaUpdated(offenceId, pleaType);

        assertNotPossibleToReAddDatesToAvoid(processInstanceId);

        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, CASE_COMPLETED_SIGNAL_NAME, is(1));
        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, WITHDRAWAL_REQUESTED_SIGNAL_NAME, is(1));
        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, WITHDRAWAL_REQUEST_CANCELLED_SIGNAL_NAME, is(1));
        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, PLEA_CANCELLED_SIGNAL_NAME, is(1));
        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, PLEA_UPDATED_SIGNAL_NAME, is(1));

        delegatesVerifier.assertDelegateCalledWith(READY_CASE, 1,
                allOf(
                        withoutProcessVariable(WITHDRAWAL_REQUESTED_VARIABLE),
                        withProcessVariable(DATES_TO_AVOID_VARIABLE, datesToAvoid),
                        withProcessVariable(PLEA_TYPE_VARIABLE, pleaType.name()),
                        withProcessVariable(PLEA_READY_VARIABLE, true)));

        delegatesVerifier.verifyNumberOfExecution(DATES_TO_AVOID_PROCESSED, 1);
        delegatesVerifier.verifyNumberOfExecution(READY_CASE, 2);
        delegatesVerifier.verifyDelegatesInteractionWith(CASE_STARTED, READY_CASE, PLEA_UPDATED, DATES_TO_AVOID_PROCESSED);
    }

    @Test
    @Deployment(resources = PROCESS_PATH)
    public void shouldPersistDatesToAvoidAfterWithdrawalRequestCancelled() {
        final LocalDate postingDate = now().minusDays(1);
        final UUID offenceId = randomUUID();
        final PleaType pleaType = PleaType.NOT_GUILTY;
        final String datesToAvoid = "my-dates-to-avoid";

        final String processInstanceId = caseStateService.caseReceived(caseId, postingDate, metadata);

        callPleaUpdated(offenceId, pleaType);

        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, DATES_TO_AVOID_ADDED_SIGNAL_NAME, is(2));
        assertThat(delegatesVerifier.isActivitiItemRunning(processInstanceId, "waitForDatesToAvoidGateway"), equalTo(true));

        delegatesVerifier.assertDelegateCalledWith(READY_CASE, 0,
                allOf(
                        withoutProcessVariable(WITHDRAWAL_REQUESTED_VARIABLE),
                        withoutProcessVariable(DATES_TO_AVOID_VARIABLE),
                        withProcessVariable(PLEA_TYPE_VARIABLE, pleaType.name()),
                        withProcessVariable(PLEA_READY_VARIABLE, false)));

        caseStateService.withdrawalRequested(caseId, metadata);

        delegatesVerifier.assertDelegateCalledWith(READY_CASE, 1,
                allOf(
                        withProcessVariable(WITHDRAWAL_REQUESTED_VARIABLE, true),
                        withoutProcessVariable(DATES_TO_AVOID_VARIABLE),
                        withProcessVariable(PLEA_TYPE_VARIABLE, pleaType.name()),
                        withProcessVariable(PLEA_READY_VARIABLE, false)));

        caseStateService.withdrawalRequestCancelled(caseId, metadata);

        delegatesVerifier.assertDelegateCalledWith(READY_CASE, 2,
                allOf(
                        withoutProcessVariable(WITHDRAWAL_REQUESTED_VARIABLE),
                        withoutProcessVariable(DATES_TO_AVOID_VARIABLE),
                        withProcessVariable(PLEA_TYPE_VARIABLE, pleaType.name()),
                        withProcessVariable(PLEA_READY_VARIABLE, false)));

        callAddDatesToAvoid(datesToAvoid);

        delegatesVerifier.assertDelegateCalledWith(READY_CASE, 3,
                allOf(
                        withoutProcessVariable(WITHDRAWAL_REQUESTED_VARIABLE),
                        withProcessVariable(DATES_TO_AVOID_VARIABLE, datesToAvoid),
                        withProcessVariable(PLEA_TYPE_VARIABLE, pleaType.name()),
                        withProcessVariable(PLEA_READY_VARIABLE, true)));

        assertThat(delegatesVerifier.isActivitiItemRunning(processInstanceId, "waitForDatesToAvoidGateway"), equalTo(false));

        callPleaUpdated(offenceId, pleaType);

        // at this stage the Dates To avoid are considered already added - no possibility to add them again but plea is still ready as plea-non-guilty
        assertThat(delegatesVerifier.isActivitiItemRunning(processInstanceId, "waitForDatesToAvoidGateway"), equalTo(false));

        delegatesVerifier.assertDelegateCalledWith(READY_CASE, 4,
                allOf(
                        withoutProcessVariable(WITHDRAWAL_REQUESTED_VARIABLE),
                        withProcessVariable(DATES_TO_AVOID_VARIABLE, datesToAvoid),
                        withProcessVariable(PLEA_TYPE_VARIABLE, pleaType.name()),
                        withProcessVariable(PLEA_READY_VARIABLE, true)));

        delegatesVerifier.verifyNumberOfExecution(READY_CASE, 5);
        delegatesVerifier.verifyDelegatesInteractionWith(CASE_STARTED, READY_CASE, PLEA_UPDATED, WITHDRAWAL_REQUESTED, WITHDRAWAL_REQUEST_CANCELLED, DATES_TO_AVOID_PROCESSED);
    }

    @Test
    @Deployment(resources = PROCESS_PATH)
    public void shouldExecuteWithdrawalRelatedDelegates() {
        final LocalDate postingDate = now().plusDays(1);

        final String processInstanceId = caseStateService.caseReceived(caseId, postingDate, metadata);

        caseStateService.withdrawalRequested(caseId, metadata);

        delegatesVerifier.verifyDelegatesInteractionWith(CASE_STARTED, WITHDRAWAL_REQUESTED, READY_CASE);
        delegatesVerifier.assertDelegateCalledWith(WITHDRAWAL_REQUESTED, 0, withProcessVariable(METADATA_VARIABLE, metadataToString(metadata)));
        delegatesVerifier.verifyNumberOfExecution(WITHDRAWAL_REQUESTED, 1);
        delegatesVerifier.verifyNumberOfExecution(READY_CASE, 1);

        delegatesVerifier.assertProcessFinished(processInstanceId, false);

        caseStateService.withdrawalRequestCancelled(caseId, metadata);

        delegatesVerifier.verifyDelegatesInteractionWith(CASE_STARTED, WITHDRAWAL_REQUESTED, READY_CASE, WITHDRAWAL_REQUEST_CANCELLED);
        delegatesVerifier.assertDelegateCalledWith(WITHDRAWAL_REQUEST_CANCELLED, 0, withProcessVariable(METADATA_VARIABLE, metadataToString(metadata)));
        delegatesVerifier.verifyNumberOfExecution(READY_CASE, 2);

        delegatesVerifier.assertProcessFinished(processInstanceId, false);

        caseStateService.withdrawalRequested(caseId, metadata);

        delegatesVerifier.assertDelegateCalledWith(WITHDRAWAL_REQUESTED, 1, withProcessVariable(METADATA_VARIABLE, metadataToString(metadata)));
        delegatesVerifier.verifyNumberOfExecution(READY_CASE, 3);
        delegatesVerifier.verifyDelegatesInteractionWith(CASE_STARTED, WITHDRAWAL_REQUESTED, READY_CASE, WITHDRAWAL_REQUEST_CANCELLED);

        delegatesVerifier.assertProcessFinished(processInstanceId, false);

        caseStateService.withdrawalRequestCancelled(caseId, metadata);

        delegatesVerifier.assertDelegateCalledWith(WITHDRAWAL_REQUEST_CANCELLED, 1, withProcessVariable(METADATA_VARIABLE, metadataToString(metadata)));

        delegatesVerifier.verifyNumberOfExecution(READY_CASE, 4);

        delegatesVerifier.verifyDelegatesInteractionWith(CASE_STARTED, WITHDRAWAL_REQUESTED, READY_CASE, WITHDRAWAL_REQUEST_CANCELLED);

        delegatesVerifier.assertProcessFinished(processInstanceId, false);

        caseStateService.caseCompleted(caseId, metadata);

        delegatesVerifier.assertProcessFinished(processInstanceId, true);

        delegatesVerifier.verifyNumberOfExecution(READY_CASE, 4);
        IntStream.range(0, 4).forEach(i -> {
            boolean expectedWithdrawalRequestedVariable = i % 2 == 0;
            delegatesVerifier.assertDelegateCalledWith(READY_CASE, i, allOf(
                    withProcessVariable(METADATA_VARIABLE, metadataToString(metadata)),
                    expectedWithdrawalRequestedVariable
                            ? withProcessVariable(WITHDRAWAL_REQUESTED_VARIABLE, true)
                            : withoutProcessVariable(WITHDRAWAL_REQUESTED_VARIABLE)
            ));
        });
    }

    @Test
    @Deployment(resources = PROCESS_PATH)
    public void shouldReadyCaseDelegateMergeVariablesFromDifferentDelegates() {
        final LocalDate postingDate = now().minusDays(30);

        final String processInstanceId = caseStateService.caseReceived(caseId, postingDate, metadata);
        delegatesVerifier.tryProcessPendingJobs();

        caseStateService.withdrawalRequested(caseId, metadata);

        delegatesVerifier.assertProcessFinished(processInstanceId, false);

        delegatesVerifier.verifyDelegatesInteractionWith(CASE_STARTED, PROVED_IN_ABSENCE, WITHDRAWAL_REQUESTED, READY_CASE);
        delegatesVerifier.verifyNumberOfExecution(READY_CASE, 2);
        delegatesVerifier.assertDelegateCalledWith(READY_CASE, 1, allOf(
                withProcessVariable(METADATA_VARIABLE, metadataToString(metadata)),
                withProcessVariable(PROVED_IN_ABSENCE_VARIABLE, true),
                withProcessVariable(WITHDRAWAL_REQUESTED_VARIABLE, true)));
    }

    @Test
    @Deployment(resources = PROCESS_PATH)
    public void shouldExecutePleaRelatedDelegatesWhenGuilty() {
        final UUID offenceId = randomUUID();
        final LocalDate postingDate = LocalDate.now().plusDays(1);
        final PleaType pleaType = PleaType.GUILTY;

        final String processInstanceId = caseStateService.caseReceived(caseId, postingDate, metadata);

        callPleaUpdated(offenceId, pleaType);

        assertThat(delegatesVerifier.isActivitiItemRunning(processInstanceId, "waitForDatesToAvoidGateway"), equalTo(false));

        delegatesVerifier.assertDelegateCalledWith(PLEA_UPDATED, 0, allOf(
                withProcessVariable(METADATA_VARIABLE, metadataToString(metadata)),
                withProcessVariable(OFFENCE_ID_VARIABLE, offenceId.toString()),
                withProcessVariable(PLEA_TYPE_VARIABLE, pleaType.name()),
                withProcessVariable(PLEA_READY_VARIABLE, true)
        ));

        delegatesVerifier.verifyNumberOfExecution(READY_CASE, 1);
        delegatesVerifier.assertDelegateCalledWith(PLEA_UPDATED, 0, allOf(
                withProcessVariable(PLEA_READY_VARIABLE, true),
                withProcessVariable(PLEA_TYPE_VARIABLE, pleaType.name())));

        caseStateService.pleaCancelled(caseId, offenceId, metadata);

        delegatesVerifier.assertDelegateCalledWith(PLEA_CANCELLED, 0, allOf(
                withProcessVariable(METADATA_VARIABLE, metadataToString(metadata)),
                withProcessVariable(OFFENCE_ID_VARIABLE, offenceId.toString()),
                withoutProcessVariable(PLEA_READY_VARIABLE),
                withoutProcessVariable(PLEA_TYPE_VARIABLE)
        ));

        delegatesVerifier.verifyNumberOfExecution(READY_CASE, 2);
        delegatesVerifier.assertDelegateCalledWith(READY_CASE, 1, allOf(
                withoutProcessVariable(PLEA_READY_VARIABLE),
                withoutProcessVariable(PLEA_TYPE_VARIABLE)));

        delegatesVerifier.verifyDelegatesInteractionWith(CASE_STARTED, READY_CASE, PLEA_UPDATED, PLEA_CANCELLED);

        delegatesVerifier.assertProcessFinished(processInstanceId, false);
    }

    @Test
    @Deployment(resources = PROCESS_PATH)
    public void shouldExecutePleaRelatedDelegatesWhenNotGuiltyAndAddDatesToAvoid() {
        final UUID offenceId = randomUUID();
        final LocalDate postingDate = LocalDate.now().plusDays(1);
        final PleaType pleaType = PleaType.NOT_GUILTY;
        final String datesToAvoid = "my-dates-to-avoid";

        final String processInstanceId = caseStateService.caseReceived(caseId, postingDate, metadata);

        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, DATES_TO_AVOID_ADDED_SIGNAL_NAME, is(1));
        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, PLEA_CANCELLED_SIGNAL_NAME, is(1));
        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, PLEA_UPDATED_SIGNAL_NAME, is(1));
        assertThat(delegatesVerifier.isActivitiItemRunning(processInstanceId, "waitForDatesToAvoidGateway"), equalTo(false));

        callPleaUpdated(offenceId, pleaType);

        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, DATES_TO_AVOID_ADDED_SIGNAL_NAME, is(2));
        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, PLEA_CANCELLED_SIGNAL_NAME, is(2));
        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, PLEA_UPDATED_SIGNAL_NAME, is(2));
        assertThat(delegatesVerifier.isActivitiItemRunning(processInstanceId, "waitForDatesToAvoidGateway"), equalTo(true));

        delegatesVerifier.assertDelegateCalledWith(PLEA_UPDATED, 0, allOf(
                withProcessVariable(METADATA_VARIABLE, metadataToString(metadata)),
                withProcessVariable(OFFENCE_ID_VARIABLE, offenceId.toString()),
                withProcessVariable(PLEA_TYPE_VARIABLE, pleaType.name())));

        delegatesVerifier.verifyNumberOfExecution(READY_CASE, 1);

        callAddDatesToAvoid(datesToAvoid);
        delegatesVerifier.verifyDelegatesInteractionWith(CASE_STARTED, READY_CASE, PLEA_UPDATED, DATES_TO_AVOID_PROCESSED);

        delegatesVerifier.verifyNumberOfExecution(READY_CASE, 2);
        delegatesVerifier.assertDelegateCalledWith(READY_CASE, 1, allOf(
                withProcessVariable(METADATA_VARIABLE, metadataToString(metadata)),
                withProcessVariable(OFFENCE_ID_VARIABLE, offenceId.toString()),
                withProcessVariable(DATES_TO_AVOID_VARIABLE, datesToAvoid)));

        assertNotPossibleToReAddDatesToAvoid(processInstanceId);
        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, PLEA_CANCELLED_SIGNAL_NAME, is(1));
        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, PLEA_UPDATED_SIGNAL_NAME, is(1));

        delegatesVerifier.assertProcessFinished(processInstanceId, false);
    }

    @Test
    @Deployment(resources = PROCESS_PATH)
    public void shouldKillDatesToAvoidSubProcessWhenReceivePleaUpdatedAction() {
        final UUID offenceId = randomUUID();

        final String processInstanceId = caseStateService.caseReceived(caseId, LocalDate.now(), metadata);

        callPleaUpdated(offenceId, PleaType.NOT_GUILTY);

        assertThat(delegatesVerifier.isActivitiItemRunning(processInstanceId, "waitForDatesToAvoidGateway"), equalTo(true));

        callPleaUpdated(offenceId, PleaType.GUILTY_REQUEST_HEARING);

        assertThat(delegatesVerifier.isActivitiItemRunning(processInstanceId, "waitForDatesToAvoidGateway"), equalTo(false));

        delegatesVerifier.verifyNumberOfExecution(READY_CASE, 2);

        delegatesVerifier.assertProcessFinished(processInstanceId, false);
    }

    @Test
    @Deployment(resources = PROCESS_PATH)
    public void shouldKillDatesToAvoidSubProcessWhenReceivePleaCancelAction() {
        final UUID offenceId = randomUUID();
        final LocalDate postingDate = LocalDate.now().plusDays(2);
        final ZonedDateTime pleaDatesToAvoidDays = ZonedDateTime.now().minusDays(1);
        final PleaType pleaType = PleaType.NOT_GUILTY;

        final String processInstanceId = caseStateService.caseReceived(caseId, postingDate, metadata);

        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, PLEA_CANCELLED_SIGNAL_NAME, is(1));
        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, PLEA_UPDATED_SIGNAL_NAME, is(1));
        assertThat(delegatesVerifier.isActivitiItemRunning(processInstanceId, "waitForDatesToAvoidGateway"), equalTo(false));

        callPleaUpdated(offenceId, pleaType, pleaDatesToAvoidDays);

        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, PLEA_CANCELLED_SIGNAL_NAME, is(2));
        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, PLEA_UPDATED_SIGNAL_NAME, is(2));
        assertThat(delegatesVerifier.isActivitiItemRunning(processInstanceId, "waitForDatesToAvoidGateway"), equalTo(true));

        caseStateService.pleaCancelled(caseId, offenceId, metadata);

        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, PLEA_CANCELLED_SIGNAL_NAME, is(1));
        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, PLEA_UPDATED_SIGNAL_NAME, is(1));
        assertThat(delegatesVerifier.isActivitiItemRunning(processInstanceId, "waitForDatesToAvoidGateway"), equalTo(false));

        delegatesVerifier.verifyNumberOfExecution(READY_CASE, 2);

        delegatesVerifier.assertProcessFinished(processInstanceId, false);
    }

    @Test
    @Deployment(resources = PROCESS_PATH)
    public void shouldUpdatePleaWhenNotGuiltyAfter10Days() {
        final UUID offenceId = randomUUID();
        final LocalDate postingDate = LocalDate.now().plusDays(1);
        final ZonedDateTime pleaDatesToAvoidDays = ZonedDateTime.now().minusDays(12);
        final PleaType pleaType = PleaType.NOT_GUILTY;

        final String processInstanceId = caseStateService.caseReceived(caseId, postingDate, metadata);

        callPleaUpdated(offenceId, pleaType, pleaDatesToAvoidDays);
        delegatesVerifier.tryProcessPendingJobs();

        delegatesVerifier.verifyDelegatesInteractionWith(CASE_STARTED, PLEA_UPDATED, DATES_TO_AVOID_PROCESSED, READY_CASE);
        delegatesVerifier.verifyNumberOfExecution(READY_CASE, 2);

        delegatesVerifier.assertDelegateCalledWith(PLEA_UPDATED, 0, allOf(
                withProcessVariable(METADATA_VARIABLE, metadataToString(metadata)),
                withProcessVariable(OFFENCE_ID_VARIABLE, offenceId.toString()),
                withProcessVariable(PLEA_TYPE_VARIABLE, pleaType.name()),
                withProcessVariable(PLEA_DEADLINE_ADD_DATES_TO_AVOID_VARIABLE, pleaDatesToAvoidDays.plusDays(10).format(ISO_LOCAL_DATE_TIME))));

        delegatesVerifier.assertDelegateCalledWith(DATES_TO_AVOID_PROCESSED, 0, allOf(
                withProcessVariable(METADATA_VARIABLE, metadataToString(metadata)),
                withProcessVariable(OFFENCE_ID_VARIABLE, offenceId.toString()),
                withProcessVariable(PLEA_TYPE_VARIABLE, pleaType.name()),
                withProcessVariable(PLEA_DEADLINE_ADD_DATES_TO_AVOID_VARIABLE, pleaDatesToAvoidDays.plusDays(10).format(ISO_LOCAL_DATE_TIME))));

        delegatesVerifier.assertDelegateCalledWith(READY_CASE, 0, allOf(
                withProcessVariable(METADATA_VARIABLE, metadataToString(metadata)),
                withProcessVariable(OFFENCE_ID_VARIABLE, offenceId.toString()),
                withProcessVariable(PLEA_TYPE_VARIABLE, pleaType.name())));

        delegatesVerifier.assertDelegateCalledWith(READY_CASE, 1, allOf(
                withProcessVariable(METADATA_VARIABLE, metadataToString(metadata)),
                withProcessVariable(OFFENCE_ID_VARIABLE, offenceId.toString()),
                withProcessVariable(PLEA_TYPE_VARIABLE, pleaType.name()),
                withProcessVariable(PLEA_DEADLINE_ADD_DATES_TO_AVOID_VARIABLE, pleaDatesToAvoidDays.plusDays(10).format(ISO_LOCAL_DATE_TIME))));

        assertNotPossibleToReAddDatesToAvoid(processInstanceId);
        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, PLEA_CANCELLED_SIGNAL_NAME, is(1));
        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, PLEA_UPDATED_SIGNAL_NAME, is(1));

        delegatesVerifier.assertProcessFinished(processInstanceId, false);
    }

    @Test
    @Deployment(resources = PROCESS_PATH)
    public void shouldExecutePleaCancelled() {
        final UUID offenceId = randomUUID();
        final LocalDate postingDate = LocalDate.now().plusDays(1);

        final String TESTING_KEY = "test-param";
        final String TESTING_VALUE = UUID.randomUUID().toString();
        final Map<String, Object> testingVariable = singletonMap(TESTING_KEY, TESTING_VALUE);
        delegatesVerifier.getDelegateExecution(PLEA_CANCELLED).setVariablesOnExecution(testingVariable);

        final String processInstanceId = caseStateService.caseReceived(caseId, postingDate, metadata);

        caseStateService.pleaCancelled(caseId, offenceId, metadata);

        delegatesVerifier.verifyDelegatesInteractionWith(CASE_STARTED, READY_CASE, PLEA_CANCELLED);

        delegatesVerifier.assertDelegateCalledWith(PLEA_CANCELLED, 0, allOf(
                withProcessVariable(METADATA_VARIABLE, metadataToString(metadata)),
                withProcessVariable(OFFENCE_ID_VARIABLE, offenceId.toString())));

        delegatesVerifier.assertDelegateCalledWith(READY_CASE, 0, allOf(
                withProcessVariable(METADATA_VARIABLE, metadataToString(metadata)),
                withProcessVariable(TESTING_KEY, TESTING_VALUE)));

        delegatesVerifier.assertProcessFinished(processInstanceId, false);
    }

    @Test
    @Deployment(resources = PROCESS_PATH)
    public void shouldExecuteProvedInAbsenceDelegateWhenPostingDateIsOlderThan28Days() {
        final LocalDate postingDate = now().minusDays(29);

        final String processInstanceId = caseStateService.caseReceived(caseId, postingDate, metadata);
        delegatesVerifier.tryProcessPendingJobs();

        delegatesVerifier.verifyDelegatesInteractionWith(CASE_STARTED, PROVED_IN_ABSENCE, READY_CASE);

        delegatesVerifier.verifyNumberOfExecution(PROVED_IN_ABSENCE, 1);
        delegatesVerifier.assertDelegateCalledWith(PROVED_IN_ABSENCE, 0, withProcessVariable(METADATA_VARIABLE, metadataToString(metadata)));

        delegatesVerifier.verifyNumberOfExecution(READY_CASE, 1);
        delegatesVerifier.assertDelegateCalledWith(READY_CASE, 0, allOf(
                withProcessBusinessKey(caseId.toString()),
                withProcessVariable(POSTING_DATE_VARIABLE, postingDate.toString()),
                withProcessVariable(PROVED_IN_ABSENCE_VARIABLE, true),
                withProcessVariable(METADATA_VARIABLE, metadataToString(metadata))
        ));

        delegatesVerifier.assertProcessFinished(processInstanceId, false);
    }

    @Test
    @Deployment(resources = PROCESS_PATH)
    public void shouldNotExecuteProvedInAbsenceDelegateWhenPostingDateIsYoungerThan28Days() {
        final LocalDate postingDate = now().minusDays(27);

        final String processInstanceId = caseStateService.caseReceived(caseId, postingDate, metadata);

        delegatesVerifier.verifyNumberOfExecution(PROVED_IN_ABSENCE, 0);
        delegatesVerifier.verifyDelegatesInteractionWith(CASE_STARTED);

        delegatesVerifier.assertProcessFinished(processInstanceId, false);
    }

    @Test
    @Deployment(resources = {PROCESS_PATH})
    public void shouldCompleteAlwaysBeReachable() {
        final LocalDate postingDate = now().plusDays(1);

        final String processInstanceId = caseStateService.caseReceived(caseId, postingDate, metadata);

        assertThat(delegatesVerifier.isActivitiItemRunning(processInstanceId, "waitForDatesToAvoidGateway"), equalTo(false));
        asList(
                WITHDRAWAL_REQUESTED_SIGNAL_NAME,
                WITHDRAWAL_REQUEST_CANCELLED_SIGNAL_NAME,
                PLEA_CANCELLED_SIGNAL_NAME,
                PLEA_UPDATED_SIGNAL_NAME,
                DATES_TO_AVOID_ADDED_SIGNAL_NAME)
                .forEach(signalName -> delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, signalName, is(1)));


        caseStateService.caseCompleted(caseId, metadata);

        delegatesVerifier.verifyDelegatesInteractionWith(CASE_STARTED, CASE_COMPLETED);
        delegatesVerifier.assertDelegateCalledWith(CASE_COMPLETED, 0, withProcessVariable(METADATA_VARIABLE, metadataToString(metadata)));

        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, CASE_COMPLETED_SIGNAL_NAME, is(0));
        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, WITHDRAWAL_REQUESTED_SIGNAL_NAME, is(0));
        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, WITHDRAWAL_REQUEST_CANCELLED_SIGNAL_NAME, is(0));
        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, PLEA_CANCELLED_SIGNAL_NAME, is(0));
        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, PLEA_UPDATED_SIGNAL_NAME, is(0));
        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, DATES_TO_AVOID_ADDED_SIGNAL_NAME, is(0));

        delegatesVerifier.assertProcessFinished(processInstanceId, true);
    }

    @Test
    @Deployment(resources = {PROCESS_PATH})
    public void shouldProvedInAbsenceVariableRemainPersistent() {
        final UUID offenceId = randomUUID();
        final LocalDate postingDate = now().minusDays(29);
        final PleaType pleaType = PleaType.GUILTY;

        final String processInstanceId = caseStateService.caseReceived(caseId, postingDate, metadata);
        delegatesVerifier.tryProcessPendingJobs();

        delegatesVerifier.verifyDelegatesInteractionWith(CASE_STARTED, PROVED_IN_ABSENCE, READY_CASE);

        caseStateService.withdrawalRequested(caseId, metadata);

        callPleaUpdated(offenceId, pleaType);

        caseStateService.pleaCancelled(caseId, offenceId, metadata);

        caseStateService.withdrawalRequestCancelled(caseId, metadata);

        delegatesVerifier.verifyDelegatesInteractionWith(CASE_STARTED, PROVED_IN_ABSENCE, READY_CASE, WITHDRAWAL_REQUESTED, PLEA_UPDATED, PLEA_CANCELLED, WITHDRAWAL_REQUEST_CANCELLED);
        delegatesVerifier.verifyNumberOfExecution(CASE_STARTED, 1);
        delegatesVerifier.verifyNumberOfExecution(PROVED_IN_ABSENCE, 1);
        delegatesVerifier.verifyNumberOfExecution(WITHDRAWAL_REQUESTED, 1);
        delegatesVerifier.verifyNumberOfExecution(PLEA_UPDATED, 1);
        delegatesVerifier.verifyNumberOfExecution(PLEA_CANCELLED, 1);
        delegatesVerifier.verifyNumberOfExecution(WITHDRAWAL_REQUEST_CANCELLED, 1);
        delegatesVerifier.verifyNumberOfExecution(READY_CASE, 5);

        delegatesVerifier.assertProcessFinished(processInstanceId, false);

        caseStateService.caseCompleted(caseId, metadata);

        delegatesVerifier.assertProcessFinished(processInstanceId, true);

        delegatesVerifier.verifyNumberOfExecution(READY_CASE, 5);
        IntStream.range(0, 5).forEach(i ->
                delegatesVerifier.assertDelegateCalledWith(READY_CASE, i, allOf(
                        withProcessBusinessKey(caseId.toString()),
                        withProcessVariable(POSTING_DATE_VARIABLE, postingDate.toString()),
                        withProcessVariable(PROVED_IN_ABSENCE_VARIABLE, true),
                        withProcessVariable(METADATA_VARIABLE, metadataToString(metadata)))));

        delegatesVerifier.assertDelegateCalledWith(READY_CASE, 4, allOf(
                withoutProcessVariable(PLEA_TYPE_VARIABLE),
                withoutProcessVariable(WITHDRAWAL_REQUESTED_VARIABLE)));
    }

    private void assertNotPossibleToReAddDatesToAvoid(final String processInstanceId) {
        assertThat(delegatesVerifier.isActivitiItemRunning(processInstanceId, "waitForDatesToAvoidGateway"), equalTo(false));
        delegatesVerifier.assertNumberEventSubscriptions(processInstanceId, DATES_TO_AVOID_ADDED_SIGNAL_NAME, is(0));
    }

    private void callAddDatesToAvoid(final String datesToAvoid) {
        final Map<String, Object> variablesOnExecutionDatesToAvoid = delegatesVerifier.getDelegateExecution(DATES_TO_AVOID_PROCESSED).getVariablesOnExecution();
        variablesOnExecutionDatesToAvoid.put(DATES_TO_AVOID_VARIABLE, datesToAvoid);
        caseStateService.datesToAvoidAdded(caseId, datesToAvoid, metadata);
        variablesOnExecutionDatesToAvoid.remove(DATES_TO_AVOID_VARIABLE);
    }

    private void callPleaUpdated(final UUID offenceId, final PleaType pleaType) {
        callPleaUpdated(offenceId, pleaType, ZonedDateTime.now());
    }

    private void callPleaUpdated(final UUID offenceId, final PleaType pleaType, final ZonedDateTime pleaDatesToAvoidDays) {
        final MockedDelegate readyCaseDelegate = delegatesVerifier.getDelegateExecution(READY_CASE);
        final boolean areDatesToAvoidSet = Optional.of(readyCaseDelegate.getExecutionTimes())
                .filter(l -> l > 0)
                .map(l -> readyCaseDelegate.getDelegateExecutions().get(l - 1).hasVariable(DATES_TO_AVOID_VARIABLE))
                .orElse(false);

        final MockedDelegate delegateExecution = delegatesVerifier.getDelegateExecution(PLEA_UPDATED);
        final Map<String, Object> variablesOnExecution = new HashMap<>(delegateExecution.getVariablesOnExecution());
        variablesOnExecution.put(PLEA_TYPE_VARIABLE, pleaType.name());
        variablesOnExecution.put(PLEA_READY_VARIABLE, PleaUpdatedDelegate.isPleaReady(pleaType, areDatesToAvoidSet));
        delegateExecution.setVariablesOnExecution(variablesOnExecution);

        caseStateService.pleaUpdated(caseId, offenceId, pleaType, pleaDatesToAvoidDays, metadata);
    }

}
