package uk.gov.moj.cpp.sjp.domain.aggregate.casestatus;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

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
        private final boolean adjourned;
        private final boolean postConviction;
        private final boolean setAside;
        private final boolean applicationGranted;

        private final List<SingleRuleChecker> checkers;

        private CaseStateCheckerBuilder(final List<OffenceInformation> offenceInformations,
                                        final boolean defendantsResponseTimerExpired,
                                        final String datesToAvoid,
                                        final boolean datesToAvoidTimerExpired,
                                        final boolean adjourned,
                                        final boolean postConviction,
                                        final boolean setAside,
                                        final boolean applicationGranted) {
            this.offenceInformations = offenceInformations;
            this.defendantsResponseTimerExpired = defendantsResponseTimerExpired;
            this.datesToAvoid = datesToAvoid;
            this.datesToAvoidTimerExpired = datesToAvoidTimerExpired;
            this.adjourned = adjourned;
            this.postConviction = postConviction;
            this.setAside = setAside;
            this.applicationGranted = applicationGranted;
            this.checkers = new ArrayList<>();
        }

        static CaseStateCheckerBuilder caseStateCheckerFor(final List<OffenceInformation> offenceInformation,
                                                           final boolean defendantsResponseTimerElapsed,
                                                           final String datesToAvoid,
                                                           final boolean datesToAvoidTimerElapsed,
                                                           final boolean adjourned,
                                                           final boolean postConviction,
                                                           final boolean setAside,
                                                           final boolean applicationGranted) {
            return new CaseStateCheckerBuilder(offenceInformation, defendantsResponseTimerElapsed, datesToAvoid, datesToAvoidTimerElapsed, adjourned, postConviction, setAside, applicationGranted);
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

        CaseStateCheckerBuilder allNoPlea() {
            checkers.add(() -> offenceInformations.stream()
                    .allMatch(offenceInformation -> isNull(offenceInformation.getPleaType()))
            );

            return this;
        }

        CaseStateCheckerBuilder somePleas() {
            checkers.add(() -> offenceInformations.stream()
                    .anyMatch(offenceInformation -> nonNull(offenceInformation.getPleaType()))
            );

            return this;
        }

        CaseStateCheckerBuilder adjourned() {
            checkers.add(() -> adjourned);
            return this;
        }

        CaseStateCheckerBuilder postConviction() {
            checkers.add(() -> postConviction);
            return this;
        }

        CaseStateCheckerBuilder setAside() {
            checkers.add(() -> setAside);
            return this;
        }

        CaseStateCheckerBuilder applicationGranted() {
            checkers.add(() -> applicationGranted);
            return this;
        }

        CaseStateCheckerBuilder isTrue() {
            checkers.add(() -> true);
            return this;
        }

    }

}
