package uk.gov.moj.cpp.sjp.event.processor.activiti;


import static java.time.format.DateTimeFormatter.ISO_DATE;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.util.Collections.singletonMap;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.processor.utils.MetadataHelper;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;

public class CaseStateService {

    public static final int NOTICE_PERIOD = 28;

    public static final String PROCESS_NAME = "caseState";

    public static final String PLEA_UPDATED_SIGNAL_NAME = "pleaUpdated";
    public static final String PLEA_CANCELLED_SIGNAL_NAME = "pleaCancelled";
    public static final String WITHDRAWAL_REQUESTED_SIGNAL_NAME = "withdrawalRequested";
    public static final String WITHDRAWAL_REQUEST_CANCELLED_SIGNAL_NAME = "withdrawalRequestCancelled";
    public static final String CASE_COMPLETED_SIGNAL_NAME = "caseCompleted";

    public static final String NOTICE_ENDED_DATE_VARIABLE = "noticeEndedDate";
    public static final String POSTING_DATE_VARIABLE = "postingDate";
    public static final String PLEA_TYPE_VARIABLE = "pleaType";
    public static final String WITHDRAWAL_REQUESTED_VARIABLE = "withdrawalRequested";
    public static final String PROVED_IN_ABSENCE_VARIABLE = "provedInAbsence";
    public static final String MARKED_READY_TIMESTAMP_VARIABLE = "markedReadyTimestamp";
    public static final String IS_READY_VARIABLE = "isReady";
    public static final String OFFENCE_ID_VARIABLE = "offenceId";
    public static final String METADATA_VARIABLE = "metadata";
    public static final String PROCESS_MIGRATION_VARIABLE = "processMigration";
    public static final String CREATE_BY_PROCESS_MIGRATION_VARIABLE = "createdByProcessMigration";

    @Inject
    private ActivitiService activitiService;

    @Inject
    private MetadataHelper metadataHelper;

    public String caseReceived(final UUID caseId, final LocalDate postingDate, final Metadata metadata) {
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put(POSTING_DATE_VARIABLE, postingDate.format(ISO_DATE));
        parameters.put(NOTICE_ENDED_DATE_VARIABLE, postingDate.plusDays(NOTICE_PERIOD).atStartOfDay().format(ISO_DATE_TIME));
        parameters.put(METADATA_VARIABLE, metadataHelper.metadataToString(metadata));

        return activitiService.startProcess(PROCESS_NAME, caseId.toString(), parameters);
    }

    public void withdrawalRequested(final UUID caseId, final Metadata metadata) {
        final String processInstanceId = activitiService.getProcessInstanceId(PROCESS_NAME, caseId.toString()).get();

        final Map<String, Object> params = singletonMap(METADATA_VARIABLE, metadataHelper.metadataToString(metadata));

        activitiService.signalProcess(processInstanceId, WITHDRAWAL_REQUESTED_SIGNAL_NAME, params);
    }

    public void withdrawalRequestCancelled(final UUID caseId, final Metadata metadata) {
        final String processInstanceId = activitiService.getProcessInstanceId(PROCESS_NAME, caseId.toString()).get();

        final Map<String, Object> params = singletonMap(METADATA_VARIABLE, metadataHelper.metadataToString(metadata));

        activitiService.signalProcess(processInstanceId, WITHDRAWAL_REQUEST_CANCELLED_SIGNAL_NAME, params);
    }

    public void pleaUpdated(final UUID caseId, final UUID offenceId, final PleaType pleaType, final Metadata metadata) {
        final String processInstanceId = activitiService.getProcessInstanceId(PROCESS_NAME, caseId.toString()).get();

        final Map<String, Object> params = new HashMap<>();
        params.put(PLEA_TYPE_VARIABLE, pleaType.name());
        params.put(OFFENCE_ID_VARIABLE, offenceId.toString());
        params.put(METADATA_VARIABLE, metadataHelper.metadataToString(metadata));

        activitiService.signalProcess(processInstanceId, PLEA_UPDATED_SIGNAL_NAME, params);
    }

    public void pleaCancelled(final UUID caseId, final UUID offenceId, final Metadata metadata) {
        final String processInstanceId = activitiService.getProcessInstanceId(PROCESS_NAME, caseId.toString()).get();

        final Map<String, Object> params = new HashMap<>();
        params.put(OFFENCE_ID_VARIABLE, offenceId.toString());
        params.put(METADATA_VARIABLE, metadataHelper.metadataToString(metadata));

        activitiService.signalProcess(processInstanceId, PLEA_CANCELLED_SIGNAL_NAME, params);
    }

    public void caseCompleted(final UUID caseId, final Metadata metadata) {
        final String processInstanceId = activitiService.getProcessInstanceId(PROCESS_NAME, caseId.toString()).get();

        final Map<String, Object> params = ImmutableMap.of(METADATA_VARIABLE, metadataHelper.metadataToString(metadata));

        activitiService.signalProcess(processInstanceId, CASE_COMPLETED_SIGNAL_NAME, params);
    }
}