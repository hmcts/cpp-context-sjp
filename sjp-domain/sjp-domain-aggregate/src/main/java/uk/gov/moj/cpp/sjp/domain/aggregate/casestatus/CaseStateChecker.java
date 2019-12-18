package uk.gov.moj.cpp.sjp.domain.aggregate.casestatus;

import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.BooleanUtils;

class CaseStateChecker {
    private final List<SingleRuleChecker> checkers;

    private CaseStateChecker(final List<SingleRuleChecker> checkers) {
        this.checkers = checkers;
    }

    boolean allRulesValid() {
        return checkers.stream()
                .map(SingleRuleChecker::check)
                .allMatch((value) -> value.equals(Boolean.TRUE));
    }

    private interface SingleRuleChecker {
        boolean check();
    }

    public static class CaseStateCheckerBuilder {
        private final List<OffenceInformation> offenceInformations;
        private final boolean defendantsResponseTimerExpired;
        private final String datesToAvoid;
        private final boolean datesToAvoidTimerExpired;

        private final List<SingleRuleChecker> checkers;

        private CaseStateCheckerBuilder(final List<OffenceInformation> offenceInformations, final boolean defendantsResponseTimerExpired, final String datesToAvoid, final boolean datesToAvoidTimerExpired) {
            this.offenceInformations = offenceInformations;
            this.defendantsResponseTimerExpired = defendantsResponseTimerExpired;
            this.datesToAvoid = datesToAvoid;
            this.datesToAvoidTimerExpired = datesToAvoidTimerExpired;
            this.checkers = new ArrayList<>();
        }

        static CaseStateCheckerBuilder caseStateCheckerFor(final List<OffenceInformation> offenceInformation,
                                                           final boolean defendantsResponseTimerElapsed,
                                                           final String datesToAvoid, final boolean datesToAvoidTimerElapsed) {
            return new CaseStateCheckerBuilder(offenceInformation, defendantsResponseTimerElapsed, datesToAvoid, datesToAvoidTimerElapsed);
        }

        private static boolean notGuiltyPlea(final OffenceInformation offenceInformation) {
            return offenceInformation.getPleaType() == PleaType.NOT_GUILTY;
        }

        private static boolean requestToBeWithdrawn(final OffenceInformation offenceInformation) {
            return BooleanUtils.isTrue(offenceInformation.getPendingWithdrawal());
        }

        private static boolean guiltyRequestHearing(final OffenceInformation offenceInformation) {
            return offenceInformation.getPleaType() == PleaType.GUILTY_REQUEST_HEARING;
        }

        private static boolean guiltyPlea(final OffenceInformation offenceInformation) {
            return offenceInformation.getPleaType() == PleaType.GUILTY;
        }

        public CaseStateChecker build() {
            return new CaseStateChecker(checkers);
        }

        CaseStateCheckerBuilder hasWithdrawalRequestedOnAllOffences() {
            checkers.add(() -> offenceInformations.stream()
                    .allMatch(CaseStateCheckerBuilder::requestToBeWithdrawn));
            return this;
        }

        CaseStateCheckerBuilder atLeastOnePleaNotGuilty() {
            checkers.add(() -> offenceInformations.stream()
                    .anyMatch(CaseStateCheckerBuilder::notGuiltyPlea)
            );
            return this;
        }

        CaseStateCheckerBuilder datesToAvoidProvided() {
            checkers.add(() -> datesToAvoid != null);
            return this;
        }

        CaseStateCheckerBuilder atLeastOneNotGuiltyPleaAndDatesToAvoidTimerHasExpired() {
            checkers.add(() -> offenceInformations.stream()
                    .anyMatch(CaseStateCheckerBuilder::notGuiltyPlea)
                    && datesToAvoidTimerExpired);
            return this;
        }

        CaseStateCheckerBuilder atLeastOnePleaGuiltyRequestHearing() {
            checkers.add(() -> offenceInformations.stream()
                    .anyMatch(CaseStateCheckerBuilder::guiltyRequestHearing)
            );
            return this;
        }

        CaseStateCheckerBuilder allPleasGuiltyOrRequestedToBeWithdrawn() {
            checkers.add(() -> offenceInformations.stream()
                    .allMatch(offenceInformation -> guiltyPlea(offenceInformation) || requestToBeWithdrawn(offenceInformation))
            );
            return this;
        }

        CaseStateCheckerBuilder atLeastOnePleaGuilty() {
            checkers.add(() -> offenceInformations.stream()
                    .anyMatch(CaseStateCheckerBuilder::guiltyPlea)
            );

            return this;
        }

        CaseStateCheckerBuilder defendantResponseTimerNotExpired() {
            checkers.add(() -> !defendantsResponseTimerExpired);
            return this;
        }

        CaseStateCheckerBuilder allPleasGuilty() {
            checkers.add(() -> offenceInformations.stream()
                    .allMatch(CaseStateCheckerBuilder::guiltyPlea)
            );

            return this;
        }

        CaseStateCheckerBuilder isTrue() {
            checkers.add(() -> true);
            return this;
        }

    }

}
