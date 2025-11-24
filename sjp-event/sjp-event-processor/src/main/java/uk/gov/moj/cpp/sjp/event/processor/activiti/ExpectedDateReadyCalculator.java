package uk.gov.moj.cpp.sjp.event.processor.activiti;

import static java.lang.Boolean.FALSE;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.CASE_ADJOURNED_DATE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.CASE_ADJOURNED_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.NOTICE_ENDED_DATE_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_DEADLINE_ADD_DATES_TO_AVOID_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_READY_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_TYPE_VARIABLE;

import java.time.LocalDateTime;
import java.util.Optional;

import org.activiti.engine.delegate.DelegateExecution;

/**
 * @deprecated the expected ready date is calculated in the aggregate
 */
@Deprecated
public class ExpectedDateReadyCalculator {

    public LocalDateTime calculateExpectedDateReady(final DelegateExecution execution) {
        final LocalDateTime defendantResponseTimerExpiration = getDefendantResponseTimerExpiration(execution);
        final Optional<LocalDateTime> adjournmentTimerExpiration = getAdjournmentTimerExpiration(execution);
        final Optional<LocalDateTime> datesToAvoidTimerExpiration = getDatesToAvoidTimerExpiration(execution);

        if (adjournmentTimerExpiration.isPresent() && datesToAvoidTimerExpiration.isPresent()) {
            return laterDate(adjournmentTimerExpiration.get(), datesToAvoidTimerExpiration.get());
        }

        if (adjournmentTimerExpiration.isPresent()) {
            final boolean pleaPresent = nonNull(execution.getVariable(PLEA_TYPE_VARIABLE, String.class));
            final boolean provedInAbsenceOnAdjournmentDate = adjournmentTimerExpiration.get().isAfter(defendantResponseTimerExpiration);
            final boolean readyOnAdjournmentDate = provedInAbsenceOnAdjournmentDate || pleaPresent;
            return readyOnAdjournmentDate ? adjournmentTimerExpiration.get() : defendantResponseTimerExpiration;
        }

        if (datesToAvoidTimerExpiration.isPresent()) {
            return datesToAvoidTimerExpiration.get();
        }

        return defendantResponseTimerExpiration;
    }

    private LocalDateTime getDefendantResponseTimerExpiration(final DelegateExecution delegateExecution) {
        return LocalDateTime.parse(delegateExecution.getVariable(NOTICE_ENDED_DATE_VARIABLE, String.class));
    }

    private Optional<LocalDateTime> getAdjournmentTimerExpiration(final DelegateExecution delegateExecution) {
        final boolean adjourned = isTrue(delegateExecution.getVariable(CASE_ADJOURNED_VARIABLE, Boolean.class));
        if (adjourned) {
            return Optional.ofNullable(delegateExecution.getVariable(CASE_ADJOURNED_DATE, String.class)).map(LocalDateTime::parse);
        } else {
            return Optional.empty();
        }
    }

    private Optional<LocalDateTime> getDatesToAvoidTimerExpiration(final DelegateExecution delegateExecution) {
        final boolean waitingForDatesToAvoid = FALSE.equals(delegateExecution.getVariable(PLEA_READY_VARIABLE, Boolean.class));
        if (waitingForDatesToAvoid) {
            return Optional.ofNullable(delegateExecution.getVariable(PLEA_DEADLINE_ADD_DATES_TO_AVOID_VARIABLE, String.class)).map(LocalDateTime::parse);
        } else {
            return Optional.empty();
        }
    }

    private LocalDateTime laterDate(final LocalDateTime localDateTime1, final LocalDateTime localDateTime2) {
        return localDateTime1.isAfter(localDateTime2) ? localDateTime1 : localDateTime2;
    }
}
