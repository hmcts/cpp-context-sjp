package uk.gov.moj.cpp.sjp.domain.aggregate.casestatus;

import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.tuple.Pair.of;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PIA;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.SET_ASIDE;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.WITHDRAWAL_REQUESTED;
import static uk.gov.moj.cpp.sjp.domain.aggregate.casestatus.OffenceInformation.createOffenceInformation;
import static uk.gov.moj.cpp.sjp.domain.common.CaseState.INVALID_CASE_STATE;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.COMPLETED;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.NO_PLEA_RECEIVED;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.PLEA_RECEIVED_NOT_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.REFERRED_FOR_COURT_HEARING;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.REOPENED_IN_LIBRA;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.SET_ASIDE_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.UNKNOWN;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.WITHDRAWAL_REQUEST_READY_FOR_DECISION;

import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.common.CaseState;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaseStatusResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseStatusResolver.class);

    private CaseStatusResolver() {
        // contains only static methods
    }

    public static CaseState resolve(final CaseAggregateState caseAggregateState) {
        final boolean isValid = validate(caseAggregateState);
        if (!isValid) {
            return INVALID_CASE_STATE;
        }

        final CaseState resultState = isCaseInCompletedStatus(
                caseAggregateState.isCaseReferredForCourtHearing(),
                caseAggregateState.getCaseReopenedDate(),
                caseAggregateState.isCaseCompleted()) ?
                handleCompletedCaseRules(caseAggregateState.isCaseReferredForCourtHearing(),
                        caseAggregateState.getCaseReopenedDate(),
                        caseAggregateState.isCaseCompleted())
                :
                handleNotCompletedCases(buildOffenceInformation(caseAggregateState),
                        caseAggregateState.isDefendantsResponseTimerExpired(),
                        caseAggregateState.getDatesToAvoid(),
                        caseAggregateState.isAdjourned(),
                        caseAggregateState.isDatesToAvoidTimerExpired(),
                        caseAggregateState.isPostConviction(),
                        caseAggregateState.isSetAside());

        if (resultState.getCaseStatus() == UNKNOWN) {
            LOGGER.warn("Case status is not covered! offenceInformation={}, referredToCourt={}, reopenedDate={}, completed={}, defendantsResponseTimerElapsed={}, datesToAvoid={}, adjourned={}",
                    buildOffenceInformation(caseAggregateState),
                    caseAggregateState.isCaseReferredForCourtHearing(),
                    caseAggregateState.getCaseReopenedDate(),
                    caseAggregateState.isCaseCompleted(),
                    caseAggregateState.isDefendantsResponseTimerExpired(),
                    caseAggregateState.getDatesToAvoid(),
                    caseAggregateState.isAdjourned());
        }

        return resultState;
    }

    private static List<OffenceInformation> buildOffenceInformation(final CaseAggregateState caseAggregateState) {
        final List<OffenceInformation> offenceInformationList = new ArrayList<>();
        return Optional.ofNullable(caseAggregateState.getOffences())
                .map(offences ->
                        offences.stream()
                                .map(offenceId ->
                                        of(offenceId, caseAggregateState.getPleaTypeForOffenceId(offenceId)))
                                .map(f -> Triple.of(f.getLeft(), f.getRight(), caseAggregateState.getOffencePleaDates().get(f.getLeft())))
                                .map(f -> createOffenceInformation(
                                        f.getLeft(),
                                        f.getMiddle(),
                                        f.getRight(),
                                        caseAggregateState.getWithdrawalRequests()
                                                .stream().anyMatch(g -> g.getOffenceId().equals(f.getLeft())),
                                        ofNullable(caseAggregateState.getOffenceDecision(f.getLeft())).map(OffenceDecision::getType).orElse(null)
                                        ))
                                .filter(offenceInformation -> !offenceInformation.hasFinalDecision())
                                .collect(Collectors.toList())).orElse(offenceInformationList);
    }

    private static boolean validate(final CaseAggregateState caseAggregateState) {
        final List<OffenceInformation> offenceInformation = buildOffenceInformation(caseAggregateState);
        if (!isNotEmpty(offenceInformation) && !caseAggregateState.isCaseCompleted()) {
            LOGGER.info("OffenceInformation list should not be empty");
            return false;
        }

        if (offenceInformation.stream().anyMatch(Objects::isNull)) {
            LOGGER.info("None of the offenceInformation elements may be null");
            return false;
        }

        return true;
    }

    private static boolean isCaseInCompletedStatus(final boolean referredToCourt, final LocalDate reopenedDate, final boolean completed) {
        return referredToCourt || nonNull(reopenedDate) || completed;
    }

    private static CaseState handleCompletedCaseRules(final boolean referredToCourt, final LocalDate reopenedDate, final boolean completed) {
        final Set<Optional<CaseStatus>> rules = new LinkedHashSet<>();
        rules.add(returnStatusIf(referredToCourt, REFERRED_FOR_COURT_HEARING));
        rules.add(returnStatusIf(nonNull(reopenedDate), REOPENED_IN_LIBRA));
        rules.add(returnStatusIf(completed, COMPLETED));
        return new CaseState(rules.stream().filter(Optional::isPresent).map(Optional::get).findFirst().orElse(CaseStatus.DEFAULT_STATUS));
    }

    private static class Scenario {
        private final String name;
        private final CaseReadinessReason readinessReason;
        private final CaseStateChecker.CaseStateCheckerBuilder caseStatusChecker;
        private final CaseStatus caseStatus;

        public Scenario(final String name, final CaseReadinessReason readinessReason, final CaseStateChecker.CaseStateCheckerBuilder caseStatusChecker, final CaseStatus caseStatus) {
            this.name = name;
            this.readinessReason = readinessReason;
            this.caseStatusChecker = caseStatusChecker;
            this.caseStatus = caseStatus;
        }

        public String getName() {
            return name;
        }

        public CaseReadinessReason getReadinessReason() {
            return readinessReason;
        }

        public CaseStateChecker.CaseStateCheckerBuilder getCaseStatusChecker() {
            return caseStatusChecker;
        }

        public CaseStatus getCaseStatus() {
            return caseStatus;
        }
    }

    private static class CaseStateCheckerBuilderFactory {

        private final boolean defendantsResponseTimerElapsed;
        private final List<OffenceInformation> offenceInformation;
        private final String datesToAvoid;
        private final boolean datesToAvoidTimerElapsed;
        private final boolean adjourned;
        private final boolean postConviction;
        private final boolean setAside;

        public CaseStateCheckerBuilderFactory(final List<OffenceInformation> offenceInformation,
                                              final boolean defendantsResponseTimerElapsed,
                                              final String datesToAvoid,
                                              final boolean datesToAvoidTimerElapsed,
                                              final boolean adjourned,
                                              final boolean postConviction,
                                              final boolean setAside) {
            this.offenceInformation = offenceInformation;
            this.defendantsResponseTimerElapsed = defendantsResponseTimerElapsed;
            this.datesToAvoid = datesToAvoid;
            this.datesToAvoidTimerElapsed = datesToAvoidTimerElapsed;
            this.adjourned = adjourned;
            this.postConviction = postConviction;
            this.setAside = setAside;
        }

        public CaseStateChecker.CaseStateCheckerBuilder getCaseStatusChecker() {
            return CaseStateChecker.CaseStateCheckerBuilder.caseStateCheckerFor(offenceInformation, defendantsResponseTimerElapsed, datesToAvoid, datesToAvoidTimerElapsed, adjourned, postConviction, setAside);
        }
    }

    // TODO: Double check the case readiness!
    private static CaseState handleNotCompletedCases(final List<OffenceInformation> offenceInformation,
                                                     final boolean defendantResponseTimerExpired,
                                                     final String datesToAvoid,
                                                     final boolean adjourned,
                                                     final boolean datesToAvoidTimerExpired,
                                                     final boolean postConviction,
                                                     final boolean setAside) {
        final Set<Scenario> cases = new LinkedHashSet<>();

        final CaseStateCheckerBuilderFactory factory =
                new CaseStateCheckerBuilderFactory(offenceInformation, defendantResponseTimerExpired, datesToAvoid, datesToAvoidTimerExpired, adjourned, postConviction, setAside);

        // The rules below have to be in importance order, so that's why it's implemented on Linked hash set implementation (could be list as well)
        // The first rule which is found will be returned to the outside and treated as the status so this is why they are not exclusive
        cases.add(new Scenario("Case is set aside currently",
                SET_ASIDE,
                factory.getCaseStatusChecker()
                        .setAside()
                , SET_ASIDE_READY_FOR_DECISION)
        );

        cases.add(new Scenario("All withdrawn all no pleas adjourned post conviction",
                null,
                factory.getCaseStatusChecker()
                        .hasWithdrawalRequestedOnAllOffences()
                        .allNoPlea()
                        .adjourned()
                        .postConviction()
                , NO_PLEA_RECEIVED)
        );

        cases.add(new Scenario("All withdrawn some pleas adjourned post conviction",
                null,
                factory.getCaseStatusChecker()
                        .hasWithdrawalRequestedOnAllOffences()
                        .somePleas()
                        .adjourned()
                        .postConviction()
                , PLEA_RECEIVED_NOT_READY_FOR_DECISION)
        );

        cases.add(new Scenario("All withdrawn all no pleas adjourned post conviction",
                WITHDRAWAL_REQUESTED,
                factory.getCaseStatusChecker()
                        .hasWithdrawalRequestedOnAllOffences()
                        .allNoPlea()
                        .postConviction()
                , NO_PLEA_RECEIVED_READY_FOR_DECISION)
        );

        cases.add(new Scenario("All withdrawn some pleas adjourned post conviction",
                WITHDRAWAL_REQUESTED,
                factory.getCaseStatusChecker()
                        .hasWithdrawalRequestedOnAllOffences()
                        .somePleas()
                        .postConviction()
                , PLEA_RECEIVED_READY_FOR_DECISION)
        );

        cases.add(new Scenario("All offences requested to be withdrawn",
                WITHDRAWAL_REQUESTED,
                factory.getCaseStatusChecker()
                        .hasWithdrawalRequestedOnAllOffences(),
                WITHDRAWAL_REQUEST_READY_FOR_DECISION)
        );

        cases.add(new Scenario("At least one not guilty plea with dates to avoid provided",
                PLEADED_NOT_GUILTY,
                factory.getCaseStatusChecker()
                        .atLeastOnePleaNotGuilty()
                        .datesToAvoidProvided()
                , PLEA_RECEIVED_READY_FOR_DECISION)
        );
        cases.add(new Scenario("At least one not guilty plea with 10 days passed from plea date",
                PLEADED_NOT_GUILTY,
                factory.getCaseStatusChecker()
                        .atLeastOneNotGuiltyPleaAndDatesToAvoidTimerHasExpired()
                , PLEA_RECEIVED_READY_FOR_DECISION)
        );

        cases.add(new Scenario("All offences covered by guilty pleas or withdrawals",
                PLEADED_GUILTY,
                factory.getCaseStatusChecker()
                        .allPleasGuiltyOrRequestedToBeWithdrawn()
                , PLEA_RECEIVED_READY_FOR_DECISION)
        );

        cases.add(new Scenario("At least one plea guilty go to court",
                PLEADED_GUILTY_REQUEST_HEARING,
                factory.getCaseStatusChecker()
                        .atLeastOnePleaGuiltyRequestHearing()
                , PLEA_RECEIVED_READY_FOR_DECISION)
        );

        cases.add(new Scenario("At least one not guilty plea, waiting for dates to avoid",
                null,
                factory.getCaseStatusChecker()
                        .atLeastOnePleaNotGuilty()
                , PLEA_RECEIVED_NOT_READY_FOR_DECISION)
        );


        cases.add(new Scenario("At least one guilty and posting day was before 28 days",
                null,
                factory.getCaseStatusChecker()
                        .atLeastOnePleaGuilty()
                        .defendantResponseTimerNotExpired()
                , PLEA_RECEIVED_NOT_READY_FOR_DECISION)
        );

        cases.add(new Scenario("At least one guilty and posting day was at least 28 days ago",
                PIA,
                factory.getCaseStatusChecker()
                        .atLeastOnePleaGuilty()
                , PLEA_RECEIVED_READY_FOR_DECISION)
        );

        cases.add(new Scenario("No pleas, case posted no more than 28 days",
                null,
                factory.getCaseStatusChecker()
                        .defendantResponseTimerNotExpired()
                , NO_PLEA_RECEIVED)
        );

        cases.add(new Scenario("No pleas or anything, case posted more than 28 days ago",
                PIA,
                factory.getCaseStatusChecker().isTrue()
                , NO_PLEA_RECEIVED_READY_FOR_DECISION)
        );

        final Pair<CaseStatus, CaseReadinessReason> caseState = cases.stream()
                .filter(scenario -> scenario.getCaseStatusChecker().build().allRulesValid())
                .findFirst()
                .map(e -> Pair.of(e.getCaseStatus(), e.getReadinessReason()))
                .orElse(Pair.of(UNKNOWN, CaseReadinessReason.DEFAULT_STATUS));

        return new CaseState(modifyCaseStatusBasedOnAdjournmentInformation(adjourned, caseState.getLeft()), caseState.getRight());
    }

    private static CaseStatus modifyCaseStatusBasedOnAdjournmentInformation(final boolean adjourned, final CaseStatus caseStatus) {
        if (adjourned) {
            if (caseStatus == PLEA_RECEIVED_READY_FOR_DECISION) {
                return PLEA_RECEIVED_NOT_READY_FOR_DECISION;
            } else if (caseStatus == NO_PLEA_RECEIVED_READY_FOR_DECISION) {
                return NO_PLEA_RECEIVED;
            } else {
                return caseStatus;
            }
        } else {
            return caseStatus;
        }
    }

    private static Optional<CaseStatus> returnStatusIf(final boolean condition, final CaseStatus caseStatus) {
        return condition ? Optional.of(caseStatus) : empty();
    }

}
