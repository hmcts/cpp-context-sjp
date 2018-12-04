package uk.gov.moj.cpp.sjp.domain.common;

import static java.time.LocalDate.now;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.moj.cpp.sjp.domain.DomainConstants.NUMBER_DAYS_WAITING_FOR_DATES_TO_AVOID;
import static uk.gov.moj.cpp.sjp.domain.DomainConstants.NUMBER_DAYS_WAITING_FOR_PLEA;

import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.time.LocalDate;

public enum CaseStatus {
    NO_PLEA_RECEIVED,
    NO_PLEA_RECEIVED_READY_FOR_DECISION,
    WITHDRAWAL_REQUEST_READY_FOR_DECISION,
    PLEA_RECEIVED_READY_FOR_DECISION,
    PLEA_RECEIVED_NOT_READY_FOR_DECISION,
    REFERRED_FOR_COURT_HEARING,
    COMPLETED,
    REOPENED_IN_LIBRA;

    public static CaseStatus calculateStatus(final LocalDate postingDate,
                                             final boolean isWithdrawalRequested,
                                             final PleaInformation pleaInformation,
                                             final String datesToAvoid,
                                             final boolean completed,
                                             final boolean referredToCourt,
                                             final LocalDate reopenedDate) {
        if (referredToCourt) {
            return REFERRED_FOR_COURT_HEARING;
        }

        if (nonNull(reopenedDate)) {
            return REOPENED_IN_LIBRA;
        }

        if (completed) {
            return COMPLETED;
        }

        if (isWithdrawalRequested) {
            return WITHDRAWAL_REQUEST_READY_FOR_DECISION;
        }

        if (pleaInformation.getPleaType() == PleaType.GUILTY || pleaInformation.getPleaType() == PleaType.GUILTY_REQUEST_HEARING) {
            return PLEA_RECEIVED_READY_FOR_DECISION;
        }

        if (pleaInformation.getPleaType() == PleaType.NOT_GUILTY) {
            return checkStatusForNotGuiltyPlea(pleaInformation.getPleaDate(), datesToAvoid);
        }

        if (now().minusDays(NUMBER_DAYS_WAITING_FOR_PLEA).isBefore(postingDate)) {
            return NO_PLEA_RECEIVED;
        }
        else {
            return NO_PLEA_RECEIVED_READY_FOR_DECISION;
        }
    }

    private static CaseStatus checkStatusForNotGuiltyPlea(final LocalDate pleaDate, final String datesToAvoid) {
        if (now().minusDays(NUMBER_DAYS_WAITING_FOR_DATES_TO_AVOID).isBefore(pleaDate) && isBlank(datesToAvoid)) {
            return PLEA_RECEIVED_NOT_READY_FOR_DECISION;
        }
        else {
            return PLEA_RECEIVED_READY_FOR_DECISION;
        }
    }

}
