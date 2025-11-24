package uk.gov.moj.cpp.sjp.domain.aggregate.casestatus;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.moj.cpp.sjp.domain.DomainConstants.NUMBER_DAYS_WAITING_FOR_PLEA;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;

import java.time.LocalDate;

public class ExpectedDateReadyCalculator {

    public LocalDate calculateExpectedDateReady(final CaseAggregateState caseAggregateState) {
        final LocalDate defendantResponseTimerExpiration = caseAggregateState.getPostingDate().plusDays(NUMBER_DAYS_WAITING_FOR_PLEA);
        if (caseAggregateState.isAdjourned() && awaitingDatesToAvoid(caseAggregateState)) {
            return laterDate(caseAggregateState.getAdjournedTo(), caseAggregateState.getDatesToAvoidExpirationDate());
        }

        if (caseAggregateState.isAdjourned()) {
            final boolean provedInAbsenceOnAdjournmentDate = caseAggregateState.getAdjournedTo().isAfter(defendantResponseTimerExpiration);
            final boolean readyOnAdjournmentDate = provedInAbsenceOnAdjournmentDate || caseAggregateState.isPleaPresent();
            return readyOnAdjournmentDate ? caseAggregateState.getAdjournedTo() : defendantResponseTimerExpiration;
        }

        if (awaitingDatesToAvoid(caseAggregateState)) {
            return caseAggregateState.getDatesToAvoidExpirationDate();
        }

        return defendantResponseTimerExpiration;
    }


    private LocalDate laterDate(final LocalDate localDate1, final LocalDate localDate2) {
        return localDate1.isAfter(localDate2) ? localDate1 : localDate2;
    }

    private boolean awaitingDatesToAvoid(final CaseAggregateState caseAggregateState) {
        return caseAggregateState.isDatesToAvoidTimerPreviouslyStarted()
                && isEmpty(caseAggregateState.getDatesToAvoid())
                && nonNull(caseAggregateState.getDatesToAvoidExpirationDate())
                && notGuiltyProvided(caseAggregateState);
    }

    private boolean notGuiltyProvided(final CaseAggregateState caseAggregateState){
        return nonNull(caseAggregateState.getPleas())
                && caseAggregateState.getPleas().stream().anyMatch(e -> NOT_GUILTY.equals(e.getPleaType()));
    }
}
