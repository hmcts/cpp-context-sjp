package uk.gov.moj.cpp.sjp.event.processor.activiti;


import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.CASE_ADJOURNED_DATE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.CASE_ADJOURNED_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.CASE_COMPLETED_SIGNAL_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.METADATA_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.NOTICE_ENDED_DATE_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.NOTICE_PERIOD;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.OFFENCE_ID_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_CANCELLED_SIGNAL_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_TYPE_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_UPDATED_SIGNAL_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.POSTING_DATE_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PROCESS_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.WITHDRAWAL_REQUESTED_SIGNAL_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.WITHDRAWAL_REQUEST_CANCELLED_SIGNAL_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.utils.MetadataHelper.metadataToString;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseStateServiceTest {

    @Mock
    private ActivitiService activitiService;

    @InjectMocks
    private CaseStateService caseStateService;

    private UUID caseId;
    private Metadata metadata;
    private String processId;
    private String metadataAsString;

    @BeforeEach
    public void init() {
        caseId = randomUUID();
        processId = randomAlphanumeric(10);
        metadata = metadataWithRandomUUIDAndName().build();
        metadataAsString = metadataToString(metadata);
    }

    @Test
    public void shouldStartProcessInstance() {

        final LocalDate postingDate = LocalDate.now();

        caseStateService.caseReceived(caseId, postingDate, metadata);

        final Matcher<Map<String, Object>> paramsMatcher = allOf(
                hasEntry(POSTING_DATE_VARIABLE, postingDate.toString()),
                hasEntry(NOTICE_ENDED_DATE_VARIABLE, postingDate.plusDays(NOTICE_PERIOD).atStartOfDay().format(ISO_DATE_TIME)),
                hasEntry(METADATA_VARIABLE, metadataAsString));

        verify(activitiService).startProcess(
                eq(PROCESS_NAME),
                eq(caseId.toString()),
                argThat(paramsMatcher));
    }

    @Test
    public void shouldSendWithdrawalRequestedSignal() {

        when(activitiService.getProcessInstanceId(PROCESS_NAME, caseId.toString())).thenReturn(Optional.of(processId));

        caseStateService.withdrawalRequested(caseId, metadata);

        verify(activitiService).signalProcess(
                eq(processId),
                eq(WITHDRAWAL_REQUESTED_SIGNAL_NAME),
                (Map) argThat(hasEntry(METADATA_VARIABLE, metadataAsString)));
    }

    @Test
    public void shouldSendWithdrawalRequestCancelledSignal() {

        when(activitiService.getProcessInstanceId(PROCESS_NAME, caseId.toString())).thenReturn(Optional.of(processId));

        caseStateService.withdrawalRequestCancelled(caseId, metadata);

        verify(activitiService).signalProcess(
                eq(processId),
                eq(WITHDRAWAL_REQUEST_CANCELLED_SIGNAL_NAME),
                (Map) argThat(hasEntry(METADATA_VARIABLE, metadataAsString)));
    }

    @Test
    public void shouldSendPleaUpdatedSignal() {
        final UUID offenceId = randomUUID();
        final PleaType pleaType = GUILTY;

        when(activitiService.getProcessInstanceId(PROCESS_NAME, caseId.toString())).thenReturn(Optional.of(processId));

        caseStateService.pleaUpdated(caseId, offenceId, pleaType, ZonedDateTime.now(), metadata);

        final Matcher<Map<String, Object>> paramsMatcher = allOf(
                hasEntry(OFFENCE_ID_VARIABLE, offenceId.toString()),
                hasEntry(PLEA_TYPE_VARIABLE, pleaType.name()),
                hasEntry(METADATA_VARIABLE, metadataAsString));

        verify(activitiService).signalProcess(
                eq(processId),
                eq(PLEA_UPDATED_SIGNAL_NAME),
                argThat(paramsMatcher));
    }

    @Test
    public void shouldSendPleaCancelledSignal() {
        final UUID offenceId = randomUUID();

        when(activitiService.getProcessInstanceId(PROCESS_NAME, caseId.toString())).thenReturn(Optional.of(processId));

        caseStateService.pleaCancelled(caseId, offenceId, metadata);

        final Matcher<Map<String, Object>> paramsMatcher = allOf(
                hasEntry(OFFENCE_ID_VARIABLE, offenceId.toString()),
                hasEntry(METADATA_VARIABLE, metadataAsString)
        );

        verify(activitiService).signalProcess(
                eq(processId),
                eq(PLEA_CANCELLED_SIGNAL_NAME),
                argThat(paramsMatcher));
    }

    @Test
    public void shouldSendCaseCompletedSignal() {

        when(activitiService.getProcessInstanceId(PROCESS_NAME, caseId.toString())).thenReturn(Optional.of(processId));

        caseStateService.caseCompleted(caseId, metadata);

        verify(activitiService).signalProcess(
                eq(processId),
                eq(CASE_COMPLETED_SIGNAL_NAME),
                (Map) argThat(hasEntry(METADATA_VARIABLE, metadataAsString)));
    }

    @Test
    public void shouldSendCaseAdjournedForLaterHearing() {
        when(activitiService.getProcessInstanceId(PROCESS_NAME, caseId.toString())).thenReturn(Optional.of(processId));

        final LocalDateTime adjournedTo = LocalDateTime.now();
        caseStateService.caseAdjournedForLaterHearing(caseId, adjournedTo, metadata);

        final Matcher<Map<String, Object>> paramsMatcher = allOf(
                hasEntry(CASE_ADJOURNED_DATE, adjournedTo.format(ISO_DATE_TIME)));

        verify(activitiService).signalProcess(
                eq(processId),
                eq(CASE_ADJOURNED_VARIABLE),
                argThat(paramsMatcher));
    }
}
