package uk.gov.moj.cpp.sjp.event.processor.activiti;


import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ISO_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static uk.gov.moj.cpp.sjp.event.processor.utils.MetadataHelper.metadataToString;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CaseStateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseStateService.class);

    public static final int NOTICE_PERIOD = 28;
    public static final int PLEA_DATES_TO_AVOID_NOTICE_DAYS = 10;

    public static final String PROCESS_NAME = "caseState";

    public static final String PLEA_UPDATED_SIGNAL_NAME = "pleaUpdated";
    public static final String PLEA_CANCELLED_SIGNAL_NAME = "pleaCancelled";
    public static final String WITHDRAWAL_REQUESTED_SIGNAL_NAME = "withdrawalRequested";
    public static final String WITHDRAWAL_REQUEST_CANCELLED_SIGNAL_NAME = "withdrawalRequestCancelled";
    public static final String DATES_TO_AVOID_ADDED_SIGNAL_NAME = "datesToAvoidAdded";
    public static final String CASE_COMPLETED_SIGNAL_NAME = "caseCompleted";

    public static final String NOTICE_ENDED_DATE_VARIABLE = "noticeEndedDate";
    public static final String DATES_TO_AVOID_VARIABLE = "datesToAvoid";
    public static final String POSTING_DATE_VARIABLE = "postingDate";
    public static final String PLEA_DEADLINE_ADD_DATES_TO_AVOID_VARIABLE = "pleaDeadlineAddDatesToAvoid";
    public static final String PLEA_TYPE_VARIABLE = "pleaType";
    public static final String PLEA_READY_VARIABLE = "pleaReady"; // if NOT_GUILTY -> need to wait for dates to avoid
    public static final String WITHDRAWAL_REQUESTED_VARIABLE = "withdrawalRequested";
    public static final String PROVED_IN_ABSENCE_VARIABLE = "provedInAbsence";
    public static final String OFFENCE_ID_VARIABLE = "offenceId";
    public static final String METADATA_VARIABLE = "metadata";
    public static final String PROCESS_MIGRATION_VARIABLE = "processMigration";
    public static final String CREATE_BY_PROCESS_MIGRATION_VARIABLE = "createdByProcessMigration";

    private ActivitiService activitiService;

    @Inject
    public CaseStateService(final ActivitiService activitiService) {
        this.activitiService = activitiService;
    }

    public String caseReceived(final UUID caseId, final LocalDate postingDate, final Metadata metadata) {
        final Map<String, Object> params = getCommonParams(metadata);
        params.put(POSTING_DATE_VARIABLE, postingDate.format(ISO_DATE));
        params.put(NOTICE_ENDED_DATE_VARIABLE, postingDate.plusDays(NOTICE_PERIOD).atStartOfDay().format(ISO_LOCAL_DATE_TIME));

        LOGGER.debug("CaseReceived with id={}, noticePeriod={}", caseId, NOTICE_PERIOD);

        return activitiService.startProcess(PROCESS_NAME, caseId.toString(), params);
    }

    public void withdrawalRequested(final UUID caseId, final Metadata metadata) {
        signalProcess(caseId, WITHDRAWAL_REQUESTED_SIGNAL_NAME, getCommonParams(metadata));
    }

    public void withdrawalRequestCancelled(final UUID caseId, final Metadata metadata) {
        signalProcess(caseId, WITHDRAWAL_REQUEST_CANCELLED_SIGNAL_NAME, getCommonParams(metadata));
    }

    @SuppressWarnings("squid:S4274")
    public void pleaUpdated(final UUID caseId, final UUID offenceId, final PleaType pleaType, final ZonedDateTime updatedDate, final Metadata metadata) {
        final Map<String, Object> params = getCommonParams(metadata);
        params.put(PLEA_TYPE_VARIABLE, pleaType.name());
        params.put(OFFENCE_ID_VARIABLE, offenceId.toString());

        if (PleaType.NOT_GUILTY.equals(pleaType)) {
            assert updatedDate != null : "Please specify a plea update date when the type is NOT_GUILTY";
            params.put(PLEA_DEADLINE_ADD_DATES_TO_AVOID_VARIABLE, updatedDate.plusDays(PLEA_DATES_TO_AVOID_NOTICE_DAYS).format(ISO_LOCAL_DATE_TIME));
        }

        signalProcess(caseId, PLEA_UPDATED_SIGNAL_NAME, params);
    }

    public void pleaCancelled(final UUID caseId, final UUID offenceId, final Metadata metadata) {
        final Map<String, Object> params = getCommonParams(metadata);
        params.put(OFFENCE_ID_VARIABLE, offenceId.toString());

        signalProcess(caseId, PLEA_CANCELLED_SIGNAL_NAME, params);
    }

    public void datesToAvoidAdded(final UUID caseId, final String datesToAvoid, final Metadata metadata) {
        final Map<String, Object> params = getCommonParams(metadata);
        params.put(DATES_TO_AVOID_VARIABLE, datesToAvoid);

        signalProcess(caseId, DATES_TO_AVOID_ADDED_SIGNAL_NAME, params);
    }

    public void caseCompleted(final UUID caseId, final Metadata metadata) {
        signalProcess(caseId, CASE_COMPLETED_SIGNAL_NAME, getCommonParams(metadata));
    }

    private void signalProcess(final UUID caseId, final String signalName, final Map<String, Object> params) {
        final String processInstanceId = Optional.ofNullable(caseId)
                .flatMap(cId -> activitiService.getProcessInstanceId(PROCESS_NAME, caseId.toString()))
                .orElseThrow(() -> new IllegalArgumentException(format("%s process instance does not exist for caseId %s", PROCESS_NAME, caseId)));

        activitiService.signalProcess(processInstanceId, signalName, params);
    }

    private static Map<String, Object> getCommonParams(final Metadata metadata) {
        final Map<String, Object> params = new HashMap<>();
        params.put(METADATA_VARIABLE, metadataToString(metadata));

        return params;
    }
}