package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.MARKED_AT;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.REASON;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.CASE_ADJOURNED_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_READY_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_TYPE_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PROVED_IN_ABSENCE_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.WITHDRAWAL_REQUESTED_VARIABLE;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;
import uk.gov.moj.cpp.sjp.event.processor.activiti.ExpectedDateReadyCalculator;
import uk.gov.moj.cpp.sjp.event.processor.activiti.ReadyCaseCalculator;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.activiti.engine.delegate.DelegateExecution;
import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

@Named
public class ReadyCaseDelegate extends AbstractCaseDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReadyCaseDelegate.class);

    //TODO remove as part of ATCM-4169
    public static final String IS_READY_VARIABLE = "isReady";

    @Inject
    private ReadyCaseCalculator readyCaseCalculator;

    @Inject
    private ExpectedDateReadyCalculator expectedDateReadyCalculator;

    @Override
    public void execute(final UUID caseId, final Metadata metadata, final DelegateExecution execution, boolean processMigration) {
        final boolean provedInAbsence = isTrue(execution.getVariable(PROVED_IN_ABSENCE_VARIABLE, Boolean.class));
        final boolean withdrawalRequested = isTrue(execution.getVariable(WITHDRAWAL_REQUESTED_VARIABLE, Boolean.class));
        final PleaType pleaType = EnumUtils.getEnum(PleaType.class, execution.getVariable(PLEA_TYPE_VARIABLE, String.class));
        final boolean pleaReady = isPleaReady(pleaType, execution.getVariable(PLEA_READY_VARIABLE, Boolean.class));
        final boolean caseAdjourned = isTrue(execution.getVariable(CASE_ADJOURNED_VARIABLE, Boolean.class));

        final Optional<CaseReadinessReason> readyReason = readyCaseCalculator.getReasonIfReady(provedInAbsence, withdrawalRequested, pleaReady, pleaType, caseAdjourned);

        LOGGER.debug("{} called with {} and responded with a reason of {}", ReadyCaseDelegate.class.getSimpleName(), execution.getVariables(), readyReason);

        if (readyReason.isPresent()) {
            //TODO remove as part of ATCM-4169
            execution.setVariable(IS_READY_VARIABLE, true);
            sendMarkCaseReadyForDecisionCommand(caseId, readyReason.get(), metadata);
        } else {
            //TODO remove as part of ATCM-4169
            execution.setVariable(IS_READY_VARIABLE, false);

            final LocalDate expectedDateReady = expectedDateReadyCalculator.calculateExpectedDateReady(execution).toLocalDate();
            sendUnmarkCaseReadyForDecisionCommand(caseId, expectedDateReady, metadata);
        }
    }

    @Required
    public void setReadyCaseCalculator(final ReadyCaseCalculator readyCaseCalculator) {
        this.readyCaseCalculator = readyCaseCalculator;
    }

    /**
     * done for Backward compatibility old cases does't have {@link CaseStateService#PLEA_READY_VARIABLE}
     * but can have {@link CaseStateService#PLEA_TYPE_VARIABLE}
     */
    private boolean isPleaReady(final PleaType pleaType, final Boolean pleaReady) {
        return pleaReady == null && pleaType != null || isTrue(pleaReady);
    }

    private void sendUnmarkCaseReadyForDecisionCommand(final UUID caseId, final LocalDate expectedDateReady, final Metadata metadata) {
        sendAsAdmin(metadata, "sjp.command.unmark-case-ready-for-decision",
                createObjectBuilder()
                        .add(CASE_ID, caseId.toString())
                        .add("expectedDateReady", expectedDateReady.toString())
                        .build());
    }

    private void sendMarkCaseReadyForDecisionCommand(final UUID caseId, final CaseReadinessReason caseReadinessReason, final Metadata metadata) {
        sendAsAdmin(metadata, "sjp.command.mark-case-ready-for-decision",
                createObjectBuilder()
                        .add(CASE_ID, caseId.toString())
                        .add(REASON, caseReadinessReason.name())
                        .add(MARKED_AT, clock.now().toString())
                        .build());
    }

}