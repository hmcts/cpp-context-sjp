package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;

import static java.time.LocalTime.MIDNIGHT;
import static java.time.ZoneOffset.UTC;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PIA;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.WITHDRAWAL_REQUESTED;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.MARKED_AT;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.REASON;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.CREATE_BY_PROCESS_MIGRATION_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.IS_READY_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.MARKED_READY_TIMESTAMP_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_TYPE_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.POSTING_DATE_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PROVED_IN_ABSENCE_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.WITHDRAWAL_REQUESTED_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.ReadyCaseCalculator.isCaseReady;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonObject;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Named
public class ReadyCaseDelegate extends AbstractCaseDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReadyCaseDelegate.class);
    private static final Map<PleaType, CaseReadinessReason> READINESS_REASON_BY_PLEA_TYPE = new EnumMap<>(PleaType.class);

    static {
        READINESS_REASON_BY_PLEA_TYPE.put(GUILTY, PLEADED_GUILTY);
        READINESS_REASON_BY_PLEA_TYPE.put(NOT_GUILTY, PLEADED_NOT_GUILTY);
        READINESS_REASON_BY_PLEA_TYPE.put(GUILTY_REQUEST_HEARING, PLEADED_GUILTY_REQUEST_HEARING);
    }

    @Inject
    private Clock clock;

    @Override
    public void execute(final UUID caseId, final Metadata metadata, final DelegateExecution execution, boolean processMigration) {
        final boolean withdrawalRequested = isTrue(execution.getVariable(WITHDRAWAL_REQUESTED_VARIABLE, Boolean.class));
        final boolean provedInAbsence = isTrue(execution.getVariable(PROVED_IN_ABSENCE_VARIABLE, Boolean.class));
        final boolean pleaReceived = execution.hasVariable(PLEA_TYPE_VARIABLE);

        final boolean isReady = isCaseReady(provedInAbsence, pleaReceived, withdrawalRequested);

        if (isReady) {
            final CaseReadinessReason caseReadinessReason;

            if (withdrawalRequested) {
                caseReadinessReason = WITHDRAWAL_REQUESTED;
            } else if (pleaReceived) {
                final PleaType pleaType = PleaType.valueOf(execution.getVariable(PLEA_TYPE_VARIABLE, String.class));
                caseReadinessReason = READINESS_REASON_BY_PLEA_TYPE.get(pleaType);
            } else {
                caseReadinessReason = PIA;

                //TODO ATCM-3133 - remove
                if (execution.hasVariable(CREATE_BY_PROCESS_MIGRATION_VARIABLE) && !getMarkedReadyTimestamp(execution).isPresent()) {
                    LOGGER.warn("Migrated process. Marked ready timestamp for proved in absence case {} calculated based on posting date", caseId);

                    final LocalDate postingDate = LocalDate.parse(execution.getVariable(POSTING_DATE_VARIABLE, String.class));
                    final ZonedDateTime inferredMarkedReadyTimestamp = ZonedDateTime.of(postingDate.plusDays(28), MIDNIGHT, UTC);
                    updateAndGetMarkedReadyTimestamp(execution, inferredMarkedReadyTimestamp);
                }
            }

            final ZonedDateTime markedReadyTimestamp = getMarkedReadyTimestamp(execution)
                    .orElseGet(() -> updateAndGetMarkedReadyTimestamp(execution));

            sendMarkCaseReadyForDecisionCommand(caseId, caseReadinessReason, markedReadyTimestamp, metadata);
        } else {
            removeMarkedReadyTimestamp(execution);
            sendUnmarkCaseReadyForDecisionCommand(caseId, metadata);
        }

        execution.setVariable(IS_READY_VARIABLE, isReady);
    }

    private void sendUnmarkCaseReadyForDecisionCommand(final UUID caseId, final Metadata metadata) {
        final JsonObject commandPayload = Json.createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .build();
        final Metadata commandMetadata = metadataFrom(metadata)
                .withName("sjp.command.unmark-case-ready-for-decision")
                .build();

        sender.sendAsAdmin(envelopeFrom(commandMetadata, commandPayload));
    }

    private void sendMarkCaseReadyForDecisionCommand(
            final UUID caseId,
            final CaseReadinessReason caseReadinessReason,
            final ZonedDateTime markedReadyAt,
            final Metadata metadata) {

        final JsonObject commandPayload = Json.createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .add(REASON, caseReadinessReason.name())
                .add(MARKED_AT, markedReadyAt.toString())
                .build();
        final Metadata commandMetadata = metadataFrom(metadata)
                .withName("sjp.command.mark-case-ready-for-decision")
                .build();

        sender.sendAsAdmin(envelopeFrom(commandMetadata, commandPayload));
    }

    private ZonedDateTime updateAndGetMarkedReadyTimestamp(final DelegateExecution delegateExecution) {
        final ZonedDateTime newMarkedReadyTimestamp = clock.now();
        return updateAndGetMarkedReadyTimestamp(delegateExecution, newMarkedReadyTimestamp);
    }

    private ZonedDateTime updateAndGetMarkedReadyTimestamp(final DelegateExecution delegateExecution, final ZonedDateTime timestamp) {
        delegateExecution.setVariable(MARKED_READY_TIMESTAMP_VARIABLE, timestamp.toString());
        return timestamp;
    }

    private Optional<ZonedDateTime> getMarkedReadyTimestamp(final DelegateExecution delegateExecution) {
        return Optional.ofNullable(delegateExecution.getVariable(MARKED_READY_TIMESTAMP_VARIABLE, String.class)).map(ZonedDateTime::parse);
    }

    private void removeMarkedReadyTimestamp(final DelegateExecution delegateExecution) {
        delegateExecution.removeVariable(MARKED_READY_TIMESTAMP_VARIABLE);
    }

}