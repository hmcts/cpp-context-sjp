package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.math.BigDecimal.ROUND_DOWN;
import static java.math.BigDecimal.ZERO;
import static java.time.ZonedDateTime.now;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.json.schemas.domains.sjp.Note.note;
import static uk.gov.justice.json.schemas.domains.sjp.NoteType.ADJOURNMENT;
import static uk.gov.justice.json.schemas.domains.sjp.NoteType.DECISION;
import static uk.gov.justice.json.schemas.domains.sjp.events.CaseNoteAdded.caseNoteAdded;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.ADJOURN;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISCHARGE;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISMISS;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.FINANCIAL_PENALTY;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.REFERRED_FOR_FUTURE_SJP_SESSION;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.REFERRED_TO_OPEN_COURT;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.REFER_FOR_COURT_HEARING;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.SET_ASIDE;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.WITHDRAW;
import static uk.gov.moj.cpp.sjp.domain.decision.FinancialPenalty.createFinancialPenalty;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;
import static uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearingV2.caseReferredForCourtHearingV2;

import uk.gov.justice.json.schemas.domains.sjp.Note;
import uk.gov.justice.json.schemas.domains.sjp.NoteType;
import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.json.schemas.domains.sjp.events.CaseNoteAdded;
import uk.gov.moj.cpp.sjp.domain.AOCPCost;
import uk.gov.moj.cpp.sjp.domain.AOCPCostDefendant;
import uk.gov.moj.cpp.sjp.domain.AOCPCostOffence;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtInterpreter;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.aggregate.Session;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.decision.Adjourn;
import uk.gov.moj.cpp.sjp.domain.decision.AocpDecision;
import uk.gov.moj.cpp.sjp.domain.decision.ConvictingInformation;
import uk.gov.moj.cpp.sjp.domain.decision.CourtDetails;
import uk.gov.moj.cpp.sjp.domain.decision.Decision;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.decision.Defendant;
import uk.gov.moj.cpp.sjp.domain.decision.Discharge;
import uk.gov.moj.cpp.sjp.domain.decision.Dismiss;
import uk.gov.moj.cpp.sjp.domain.decision.FinancialPenalty;
import uk.gov.moj.cpp.sjp.domain.decision.NoSeparatePenalty;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionVisitor;
import uk.gov.moj.cpp.sjp.domain.decision.ReferForCourtHearing;
import uk.gov.moj.cpp.sjp.domain.decision.ReferredForFutureSJPSession;
import uk.gov.moj.cpp.sjp.domain.decision.ReferredToOpenCourt;
import uk.gov.moj.cpp.sjp.domain.decision.SessionCourt;
import uk.gov.moj.cpp.sjp.domain.decision.SetAside;
import uk.gov.moj.cpp.sjp.domain.decision.Withdraw;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.CostsAndSurcharge;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.FinancialImposition;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.LumpSum;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Payment;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentTerms;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.AocpPleasSet;
import uk.gov.moj.cpp.sjp.event.CaseAdjournedToLaterSjpHearingRecorded;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.DefendantAocpResponseTimerExpired;
import uk.gov.moj.cpp.sjp.event.decision.DecisionRejected;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSetAside;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSetAsideReset;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CaseDecisionHandler {

    public static final CaseDecisionHandler INSTANCE = new CaseDecisionHandler();

    private static final List<DecisionType> NO_PREVIOUS_CONVICTION_DECISIONS = asList(DISMISS, WITHDRAW);
    private static final Set<DecisionType> MULTIPLE_OFFENCE_DECISION_TYPES = newHashSet(ADJOURN,
            REFER_FOR_COURT_HEARING, REFERRED_FOR_FUTURE_SJP_SESSION, REFERRED_TO_OPEN_COURT, SET_ASIDE);

    private CaseDecisionHandler() {
    }

    private static void addDecisionSavedEvent(final Session session, Decision decision, final CaseAggregateState state, final Stream.Builder<Object> streamBuilder, final Boolean resultedThroughAocp ) {

        final Optional<SessionCourt> convictingCourt =
                ofNullable(session.getCourtHouseCode()).map(chc ->
                                ofNullable(session.getLocalJusticeAreaNationalCourtCode()).map(lja ->
                                        new SessionCourt(chc, lja)))
                        .orElse(Optional.empty());


        if (decisionHasFinancialImposition(decision) && sessionCourtAndDefendantCourtAreDifferent(session, decision.getDefendant())) {
            decision.getFinancialImposition().getPayment().setFineTransferredTo(decision.getDefendant().getCourt());
        }

        addOffencesConvictionDetails(decision, state, convictingCourt.orElse(null));
        addOffencesPressRestrictable(decision, state);

        final String defendantFirstName = state.getDefendantFirstName();
        final String defendantLastName = state.getDefendantLastName();
        final String defendantName = defendantFirstName + " " + defendantLastName;
        final UUID defendantId = state.getDefendantId();

        streamBuilder.add(new DecisionSaved(
                decision.getDecisionId(),
                decision.getSessionId(),
                decision.getCaseId(),
                state.getUrn(),
                decision.getSavedAt(),
                decision.getOffenceDecisions(),
                decision.getFinancialImposition(),
                defendantId,
                defendantName,
                resultedThroughAocp));
    }
    private static void addOffencesPressRestrictable(final Decision decision, final CaseAggregateState state) {
        final PressRestrictableOffenceDecisionVistor pressRestrictableOffenceDecisionVistor = new PressRestrictableOffenceDecisionVistor(state);
        decision.getOffenceDecisions().forEach(offenceDecision -> offenceDecision.accept(pressRestrictableOffenceDecisionVistor));
    }

    private static void addOffencesConvictionDetails(final Decision decision, final CaseAggregateState state, final SessionCourt sessionCourt) {
        final ConvictionDateOffenceVisitor convictionDateOffenceVisitor = new ConvictionDateOffenceVisitor(state, decision.getSavedAt(), sessionCourt);
        decision.getOffenceDecisions()
                .forEach(offenceDecision -> offenceDecision.accept(convictionDateOffenceVisitor));
    }

    private static boolean sessionCourtAndDefendantCourtAreDifferent(final Session session, final Defendant defendant) {
        return defendant != null && defendant.getCourt() != null &&
                !session.getLocalJusticeAreaNationalCourtCode().equals(defendant.getCourt().getNationalCourtCode());
    }

    private static boolean decisionHasFinancialImposition(final Decision decision) {
        return decision.getFinancialImposition() != null && decision.getOffenceDecisions().stream()
                .map(OffenceDecision::getType)
                .anyMatch(decisionType -> decisionType.equals(DISCHARGE) || decisionType.equals(FINANCIAL_PENALTY));
    }

    private static void addCaseNoteAddedEventIfNoteIsPresent(final Decision decision, final Stream.Builder<Object> streamBuilder) {
        if (isNotBlank(decision.getNote())) {
            streamBuilder.add(new CaseNoteAdded(decision.getSavedBy(), decision.getCaseId(), decision.getDecisionId(), new Note(decision.getSavedAt(), randomUUID(), decision.getNote(), DECISION)));
        }
    }

    private static void addCaseUnassignedEvent(final Decision decision, final Stream.Builder<Object> streamBuilder) {
        final boolean setAsideDecision = decision
                .getOffenceDecisions()
                .stream()
                .allMatch(e -> SET_ASIDE.equals(e.getType()));

        if (!setAsideDecision) {
            streamBuilder.add(new CaseUnassigned(decision.getCaseId()));
        }
    }

    private static void handleSetAside(final Decision decision,
                                       final Stream.Builder<Object> streamBuilder,
                                       final CaseAggregateState caseAggregateState) {
        final boolean setAsideDecision = decision
                .getOffenceDecisions()
                .stream()
                .allMatch(e -> SET_ASIDE.equals(e.getType()));

        // if previous one is set aside and the current one is not
        if (caseAggregateState.isSetAside() && !setAsideDecision) {
            streamBuilder.add(new DecisionSetAsideReset(decision.getDecisionId(), decision.getCaseId()));
        } else if (setAsideDecision) { // if the current one is set aside
            streamBuilder.add(new DecisionSetAside(decision.getDecisionId(), decision.getCaseId()));
        }
    }

    private static void addCaseCompletedEventIfAllOffencesHasFinalDecision(final Decision decision, final Stream.Builder<Object> streamBuilder, final CaseAggregateState state) {
        final Set<UUID> offencesWithFinalDecision = getOffenceIds(state.getOffenceDecisions(), OffenceDecision::isFinalDecision);
        final Set<UUID> incomingOffencesWithFinalDecision = getOffenceIds(decision.getOffenceDecisions(), OffenceDecision::isFinalDecision);

        offencesWithFinalDecision.addAll(incomingOffencesWithFinalDecision);

        if (offencesWithFinalDecision.equals(state.getOffences())) {
            streamBuilder.add(new CaseCompleted(decision.getCaseId(), state.getSessionIds()));
        }

    }

    private static void handleAdjournDecision(final Decision decision, final Stream.Builder<Object> streamBuilder) {
        getDecisions(decision, Adjourn.class)
                .findFirst()
                .ifPresent(adjourn -> {
                            streamBuilder.add(new CaseAdjournedToLaterSjpHearingRecorded(adjourn.getAdjournTo(), decision.getCaseId(), decision.getSessionId()));
                            streamBuilder.add(new CaseNoteAdded(decision.getSavedBy(), decision.getCaseId(), decision.getDecisionId(), new Note(decision.getSavedAt(), randomUUID(), adjourn.getReason(), ADJOURNMENT)));
                        }
                );
    }

    private static void handleReferToCriminalCourtDecision(final Decision decision, final CaseAggregateState state, final Stream.Builder<Object> streamBuilder) {
        final Optional<ReferForCourtHearing> referForCourtHearingDecision = getDecisions(decision, ReferForCourtHearing.class).findFirst();
        if (referForCourtHearingDecision.isPresent()) {
            final ReferForCourtHearing referForCourtHearing = referForCourtHearingDecision.get();
            final List<OffenceDecisionInformation> offenceDecisionInformationList = referForCourtHearing.getOffenceDecisionInformation();

            streamBuilder.add(caseReferredForCourtHearingV2()
                    .withCaseId(decision.getCaseId())
                    .withDecisionId(decision.getDecisionId())
                    .withReferredAt(decision.getSavedAt())
                    .withReferralReasonId(referForCourtHearing.getReferralReasonId())
                    .withReferralReason(referForCourtHearing.getReferralReason())
                    .withEstimatedHearingDuration(referForCourtHearing.getEstimatedHearingDuration())
                    .withListingNotes(referForCourtHearing.getListingNotes())
                    .withReferredOffences(offenceDecisionInformationList)
                    .withDefendantCourtOptions(referForCourtHearing.getDefendantCourtOptions())
                    .withConvictionDate(referForCourtHearing.getConvictionDate())
                    .withConvictingCourt(referForCourtHearing.getConvictingCourt())
                    .withNextHearing(referForCourtHearing.getNextHearing())
                    .build());

            if (isNotEmpty(referForCourtHearing.getListingNotes())) {
                streamBuilder.add(caseNoteAdded()
                        .withCaseId(decision.getCaseId())
                        .withDecisionId(decision.getDecisionId())
                        .withAuthor(decision.getSavedBy())
                        .withNote(note()
                                .withId(randomUUID())
                                .withType(NoteType.LISTING)
                                .withText(referForCourtHearing.getListingNotes())
                                .withAddedAt(decision.getSavedAt())
                                .build()
                        ).build());
            }
            ofNullable(referForCourtHearing.getDefendantCourtOptions()).ifPresent(courtOptions ->
                    updateHearingRequirements(decision.getSavedBy(), courtOptions, state).forEach(streamBuilder::add)
            );
        }
    }

    private static void validateOffencesBelongToTheCase(final Decision decision, final CaseAggregateState state, final List<String> rejectionReason) {
        final Set<UUID> offenceIds = state.getOffences();

        getOffenceIds(decision.getOffenceDecisions())
                .filter(offenceId -> !offenceIds.contains(offenceId))
                .map(offenceId -> format("Offence with id %s does not belong to this case", offenceId))
                .forEach(rejectionReason::add);
    }

    private static void validateCaseIsNotCompleted(final CaseAggregateState state, final List<String> rejectionReason) {
        if (state.isCaseCompleted()) {
            rejectionReason.add("The case is already completed");
        }
    }

    private static void validateCaseIsAssignedToTheCaller(final Decision decision, final CaseAggregateState state, final List<String> rejectionReason) {
        if (!decision.getSavedBy().getUserId().equals(state.getAssigneeId())) {
            rejectionReason.add("The case must be assigned to the caller");
        }
    }

    private static void validateOffencesHasOnlyOneDecision(final Decision decision, final List<String> rejectionReason) {
        final Map<UUID, Long> offenceIdWithCount = getOffenceIds(decision.getOffenceDecisions()).collect(groupingBy(Function.identity(), counting()));
        offenceIdWithCount.forEach((offenceId, count) -> {
            if (count > 1) {
                rejectionReason.add(format("Offence with id %s has more than 1 decision", offenceId));
            }
        });
    }

    private static void validateOffencesDoNotHavePreviousFinalDecision(final Decision decision, final CaseAggregateState state, final List<String> rejectionReasons) {
        if (decision
                .getOffenceDecisions()
                .stream()
                .allMatch(e -> SET_ASIDE.equals(e.getType()))) {
            return;
        }

        if (state.isSetAside()) {
            return;
        }

        final Set<UUID> offenceIdsWithFinalDecision = getOffenceIds(state.getOffenceDecisions(), OffenceDecision::isFinalDecision);

        decision.getOffenceIds().stream()
                .filter(offenceIdsWithFinalDecision::contains)
                .map(offenceId -> String.format("Offence %s already has a final decision", offenceId))
                .forEach(rejectionReasons::add);
    }

    private static void validateDecisionsCombinations(final Decision decision, final List<String> rejectionReasons) {
        final Map<DecisionType, Long> decisionsCountByType = decision.getOffenceDecisions().stream()
                .map(OffenceDecision::getType)
                .collect(groupingBy(identity(), counting()));

        if (decisionsCountByType.containsKey(REFER_FOR_COURT_HEARING)) {
            final Set<DecisionType> incompatibleDecisionTypes = decisionsCountByType.keySet().stream()
                    .filter(decisionType -> !WITHDRAW.equals(decisionType))
                    .filter(decisionType -> !REFER_FOR_COURT_HEARING.equals(decisionType))
                    .filter(decisionType -> !DISMISS.equals(decisionType))
                    .collect(toSet());

            if (!incompatibleDecisionTypes.isEmpty()) {
                rejectionReasons.add(format("%s decision can not be saved with decision(s) %s", REFER_FOR_COURT_HEARING, incompatibleDecisionTypes.stream().map(DecisionType::name).collect(joining(","))));
            }
        } else if (decisionsCountByType.containsKey(ADJOURN)) {
            final Set<DecisionType> incompatibleDecisionTypes = decisionsCountByType.keySet().stream()
                    .filter(decisionType -> !WITHDRAW.equals(decisionType))
                    .filter(decisionType -> !ADJOURN.equals(decisionType))
                    .filter(decisionType -> !DISMISS.equals(decisionType))
                    .collect(toSet());

            if (!incompatibleDecisionTypes.isEmpty()) {
                rejectionReasons.add(format("%s decision can not be saved with decision(s) %s", ADJOURN, incompatibleDecisionTypes.stream().map(DecisionType::name).collect(joining(","))));
            }

            final Set<VerdictType> verdicts = decision.getOffenceDecisions().stream()
                    .filter(offenceDecision -> offenceDecision.getType() == ADJOURN)
                    .map(OffenceDecision::offenceDecisionInformationAsList)
                    .flatMap(List::stream)
                    .map(OffenceDecisionInformation::getVerdict)
                    .collect(toSet());

            if (verdicts.size() > 1 && verdicts.contains(NO_VERDICT)) {
                rejectionReasons.add("ADJOURN decisions with pre and post convictions can not be combined");
            }
        } else if (decisionsCountByType.containsKey(SET_ASIDE)) {

            if (decisionsCountByType.get(SET_ASIDE) != 1) {
                rejectionReasons.add("Only one set-aside decision can be made");
            }

            if (decisionsCountByType.size() != 1) {
                rejectionReasons.add("Along with set-aside not other decisions are allowed");
            }
        }
    }

    private static Stream<Object> updateHearingRequirements(final User user, final DefendantCourtOptions courtOptions, final CaseAggregateState state) {
        final String interpreterLanguage = ofNullable(courtOptions.getInterpreter())
                .map(DefendantCourtInterpreter::getLanguage)
                .orElse(null);

        return CaseLanguageHandler.INSTANCE.updateHearingRequirements(user.getUserId(), state.getDefendantId(), interpreterLanguage, courtOptions.getWelshHearing(), state, PleaMethod.POSTAL, null);
    }

    private static Stream<UUID> getOffenceIds(final Collection<OffenceDecision> offencesDecisions) {
        return offencesDecisions.stream()
                .flatMap(offencesDecision -> offencesDecision.getOffenceIds().stream());
    }

    private static Set<UUID> getOffenceIds(Collection<? extends OffenceDecision> offenceDecisions, Predicate<OffenceDecision> offencesDecisionPredicate) {
        return offenceDecisions.stream()
                .filter(offencesDecisionPredicate)
                .flatMap(offencesDecision -> offencesDecision.getOffenceIds().stream())
                .collect(toSet());
    }

    private static <T extends OffenceDecision> Stream<T> getDecisions(final Decision decision, final Class<T> domainClass) {
        return decision.getOffenceDecisions().stream()
                .filter(offenceDecision -> Objects.equals(offenceDecision.getClass(), domainClass))
                .map(domainClass::cast);
    }

    public Stream saveDecision(final Decision decision, final CaseAggregateState state, final Session session) {
        final List<String> validationErrors = validateDecision(decision, state, session.getSessionType());
        if (!validationErrors.isEmpty()) {
            return Stream.of(new DecisionRejected(decision, validationErrors));
        }

        final Stream.Builder<Object> streamBuilder = Stream.builder();
        addDecisionSavedEvent(session, decision, state, streamBuilder, null);
        addCaseNoteAddedEventIfNoteIsPresent(decision, streamBuilder);
        addCaseUnassignedEvent(decision, streamBuilder);

        handleSetAside(decision, streamBuilder, state);
        handleAdjournDecision(decision, streamBuilder);
        handleReferToCriminalCourtDecision(decision, state, streamBuilder);
        if(isNull(decision.isApplicationFlow()) || !decision.isApplicationFlow()) {
            addCaseCompletedEventIfAllOffencesHasFinalDecision(decision, streamBuilder, state);
        }else{
            addCaseCompletedEventIfAnyOffencesHasFinalDecision(decision, streamBuilder, state);
        }

        return streamBuilder.build();
    }

    private void addCaseCompletedEventIfAnyOffencesHasFinalDecision(final Decision decision, final Stream.Builder<Object> streamBuilder, final CaseAggregateState state) {
        final Set<UUID> incomingOffencesWithFinalDecision = getOffenceIds(decision.getOffenceDecisions(), OffenceDecision::isFinalDecision);
        if (nonNull(state.getOffences()) && !state.getOffences().isEmpty() &&
                !incomingOffencesWithFinalDecision.isEmpty() && state.getOffences().containsAll(incomingOffencesWithFinalDecision)) {
            streamBuilder.add(new CaseCompleted(decision.getCaseId(), state.getSessionIds()));
        }
    }


    private List<String> validateDecision(final Decision decision, final CaseAggregateState state, final SessionType sessionType) {
        final List<String> rejectionReason = new ArrayList<>();

        validateCaseIsNotCompleted(state, rejectionReason);
        validateCaseIsAssignedToTheCaller(decision, state, rejectionReason);
        validateOffencesBelongToTheCase(decision, state, rejectionReason);
        validateOffencesHasOnlyOneDecision(decision, rejectionReason);
        validateDecisionsCombinations(decision, rejectionReason);
        validateOffencesDoNotHavePreviousFinalDecision(decision, state, rejectionReason);
        validatePressRestrictableOffences(decision, state, rejectionReason);
        validateDecisionTypesAndVerdict(decision, rejectionReason);
        validateDecisionsWithoutVerdict(decision, state, rejectionReason);
        validateEmployerDetailsAppliedWhenPaymentTypeIsAttachedToEarnings(decision, state, rejectionReason);
        validateFinancialCosts(decision, state, rejectionReason);
        validateDecisionTypesAndPreviousConvictions(decision, state, rejectionReason);

        validateDelegatedPowerCanNotSubmitVerdictForReferToCourtHearing(decision, sessionType, rejectionReason);
        validateDelegatedPowerCanNotSubmitVerdictForAdjourn(decision, sessionType, rejectionReason);
        validateForMagistrateSessionNotGuiltyCanNotBeWithReferToCourtHearing(decision, state, sessionType, rejectionReason);
        validateNoPleaShouldHaveProvedSJPVerdictForReferToCourtHearing(decision, state, rejectionReason);
        validateIfPleaExistsThenVerdictCanNotBeProvedSJPVerdict(decision, state, rejectionReason);
        validateNotGuiltyPleaShouldHaveNoVerdictForReferToCourtHearing(decision, state, rejectionReason);
        validateGuiltyPleaShouldHaveFoundGuiltyVerdictForReferToCourtHearing(decision, state, rejectionReason);
        return rejectionReason;
    }

    private void validatePressRestrictableOffences(final Decision decision, final CaseAggregateState state, final List<String> rejectionReasons) {
        decision.getOffenceDecisions()
                .stream()
                .filter(OffenceDecision::hasPressRestriction)
                .filter(offenceDecision -> !MULTIPLE_OFFENCE_DECISION_TYPES.contains(offenceDecision.getType()))
                .flatMap(offenceDecision -> offenceDecision.getOffenceIds().stream())
                .filter(offenceId -> !state.isPressRestrictable(offenceId))
                .forEach(offenceId -> rejectionReasons.add(format("Press restriction cannot be applied to non-press-restrictable offence: %s", offenceId.toString())));

        decision.getOffenceDecisions()
                .stream()
                .filter(OffenceDecision::hasPressRestriction)
                .filter(offenceDecision -> offenceDecision.getPressRestriction().isRevoked())
                .flatMap(offenceDecision -> offenceDecision.getOffenceIds().stream())
                .filter(state::isPressRestrictable)
                .filter(offenceId -> !state.hasPreviousPressRestriction(offenceId))
                .forEach(offenceId -> rejectionReasons.add(format("Press restriction cannot be revoked on offence that has no previous press restriction requested. Failed offenceId: %s", offenceId.toString())));

        decision.getOffenceDecisions()
                .stream()
                .filter(offenceDecision -> !offenceDecision.hasPressRestriction())
                .flatMap(offenceDecision -> offenceDecision.getOffenceIds().stream())
                .filter(state::hasPreviousPressRestriction)
                .forEach(offenceId -> rejectionReasons.add(format("Expected to find press restriction for offence %s but found none", offenceId.toString())));
    }

    private void validateDecisionTypesAndPreviousConvictions(final Decision decision, final CaseAggregateState state, final List<String> rejectionReasons) {
        decision.getOffenceDecisions()
                .stream()
                .filter(offenceDecision -> NO_PREVIOUS_CONVICTION_DECISIONS.contains(offenceDecision.getType()))
                .flatMap(offenceDecision -> offenceDecision.getOffenceIds().stream())
                .filter(state::offenceHasPreviousConviction)
                .forEach(offenceId -> rejectionReasons.add(format("offence %s : WITHDRAW or DISMISS can't be used on an offence decision with a previous conviction", offenceId.toString())));
    }

    private void validateDecisionTypesAndVerdict(final Decision decision, final List<String> rejectionReasons) {

        final Map<DecisionType, List<OffenceDecisionInformation>> collect =
                decision.getOffenceDecisions()
                        .stream()
                        .collect(groupingBy(OffenceDecision::getType,
                                mapping(OffenceDecision::offenceDecisionInformationAsList, toList()))).entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, decisionTypeListEntry -> decisionTypeListEntry.getValue().stream().flatMap(Collection::stream).collect(toList())));

        final BiFunction<Predicate<DecisionType>, Predicate<VerdictType>, Long> unmatchedDecisionTypeAndVerdicts = (decisionType, matchedVerdicts) -> collect.entrySet().stream()
                .filter(a -> decisionType.test(a.getKey()))
                .flatMap(a -> a.getValue().stream())
                .filter(offenceDecisionInformation -> !matchedVerdicts.test(offenceDecisionInformation.getVerdict()))
                .count();

        final String message = "Decisions of type %s's verdict type can only be %s";

        if (unmatchedDecisionTypeAndVerdicts.apply(a -> a == FINANCIAL_PENALTY || a == DISCHARGE,
                verdict -> verdict == FOUND_GUILTY || verdict == PROVED_SJP || verdict == null) > 0) {
            rejectionReasons.add(format(message, "Financial Penalty and Discharge", "either FOUND_GUILTY or PROVED_SJP"));
        }

        if (unmatchedDecisionTypeAndVerdicts.apply(a -> a == WITHDRAW, verdict -> verdict == NO_VERDICT) > 0) {
            rejectionReasons.add(format(message, "Withdraw", "NO_VERDICT"));
        }

        if (unmatchedDecisionTypeAndVerdicts.apply(a -> a == DISMISS, verdict -> verdict == FOUND_NOT_GUILTY) > 0) {
            rejectionReasons.add(format(message, "Dismiss", "FOUND_NOT_GUILTY"));
        }

        if (unmatchedDecisionTypeAndVerdicts.apply(a -> a == SET_ASIDE, Objects::isNull) > 0) {
            rejectionReasons.add(format(message, "SetAside", "null"));
        }
    }

    private static void validateDecisionsWithoutVerdict(final Decision decision, final CaseAggregateState state, final List<String> rejectionReason) {
        if (decision.getOffenceDecisions()
                .stream()
                .allMatch(e -> SET_ASIDE.equals(e.getType()))) {
            return;
        }

        final Stream<OffenceDecisionInformation> offenceDecisionsWithoutVerdict = decision
                .getOffenceDecisions().stream()
                .flatMap(offenceDecision -> offenceDecision.offenceDecisionInformationAsList().stream())
                .filter(offenceDecisionInformation -> Objects.isNull(offenceDecisionInformation.getVerdict()));

        offenceDecisionsWithoutVerdict
                .map(OffenceDecisionInformation::getOffenceId)
                .filter(offenceId -> !state.offenceHasPreviousConviction(offenceId))
                .forEach(offenceId ->
                        rejectionReason.add(format("offence %s : can't have an offence without verdict if it wasn't previously convicted", offenceId.toString())));
    }

    private static void validateEmployerDetailsAppliedWhenPaymentTypeIsAttachedToEarnings(final Decision decision, final CaseAggregateState state, final List<String> rejectionReason) {
        if (decision.getFinancialImposition() != null && decision.getFinancialImposition().getPayment() != null && decision.getFinancialImposition().getPayment().getPaymentType() == PaymentType.ATTACH_TO_EARNINGS && !state.hasEmployerDetailsUpdated()) {
            rejectionReason.add("Decision with payment type attach to earnings requires employer details");
        }
    }

    private static void validateFinancialCosts(final Decision decision, final CaseAggregateState state, final List<String> rejectionReason) {
        ofNullable(decision.getFinancialImposition())
                .map(FinancialImposition::getCostsAndSurcharge)
                .ifPresent(costsAndSurcharge -> {
                    final BigDecimal impositionCosts = costsAndSurcharge.getCosts();
                    final String impositionReasonForNoCosts = ofNullable(costsAndSurcharge.getReasonForNoCosts()).map(String::trim).orElse(null);
                    if (state.getCosts() != null && state.getCosts().compareTo(ZERO) > 0 && impositionCosts.compareTo(ZERO) <= 0 && isEmpty(impositionReasonForNoCosts)) {
                        rejectionReason.add("Reason for no costs is required when costs is zero");
                    }
                });
    }

    private void validateIfPleaExistsThenVerdictCanNotBeProvedSJPVerdict(Decision decision, CaseAggregateState state, List<String> rejectionReasons) {
        decision.getOffenceDecisions().stream()
                .map(offenceDecision ->
                        offenceDecision.offenceDecisionInformationAsList().stream().filter(offenceDecisionInformation -> {
                            final PleaType pleaType = state.getPleaTypeForOffenceId(offenceDecisionInformation.getOffenceId());
                            return !isNull(pleaType) && PROVED_SJP == offenceDecisionInformation.getVerdict();
                        }).collect(toList())
                )
                .flatMap(List::stream)
                .map(offenceDecisionInformation -> format("Offence with Plea can not have verdict as PROVED_SJP, %s decision can not be saved for offence %s", REFER_FOR_COURT_HEARING, offenceDecisionInformation.getOffenceId()))
                .forEach(rejectionReasons::add);
    }


    private void validateVerdictForDecisionReferToCourtIsCorrect(final Decision decision, final CaseAggregateState state, final List<String> rejectionReasons, final BiPredicate<PleaType, VerdictType> predicate, final String errorMessage) {
        decision.getOffenceDecisions().stream()
                .filter(offenceDecision -> DecisionType.REFER_FOR_COURT_HEARING == offenceDecision.getType())
                .map(offenceDecision ->
                        offenceDecision.offenceDecisionInformationAsList().stream().filter(offenceDecisionInformation -> {
                            final PleaType pleaType = state.getPleaTypeForOffenceId(offenceDecisionInformation.getOffenceId());
                            return predicate.test(pleaType, offenceDecisionInformation.getVerdict());
                        }).collect(toList())
                )
                .flatMap(List::stream)
                .map(offenceDecisionInformation -> format("%s, %s decision can not be saved for offence %s", errorMessage, REFER_FOR_COURT_HEARING, offenceDecisionInformation.getOffenceId()))
                .forEach(rejectionReasons::add);
    }

    private void validateGuiltyPleaShouldHaveFoundGuiltyVerdictForReferToCourtHearing(Decision decision, CaseAggregateState state, List<String> rejectionReasons) {
        validateVerdictForDecisionReferToCourtIsCorrect(decision, state, rejectionReasons, (pleaType, verdictType) ->
                        PleaType.GUILTY.equals(pleaType) && !asList(FOUND_GUILTY, NO_VERDICT, null).contains(verdictType),
                "Guilty plea should have Found Guilty Verdict");
    }

    private void validateNotGuiltyPleaShouldHaveNoVerdictForReferToCourtHearing(Decision decision, CaseAggregateState state, List<String> rejectionReasons) {
        validateVerdictForDecisionReferToCourtIsCorrect(decision, state, rejectionReasons, (pleaType, verdictType) ->
                        PleaType.NOT_GUILTY.equals(pleaType) && VerdictType.NO_VERDICT != verdictType,
                "Not Guilty plea should have no verdict");
    }

    private void validateNoPleaShouldHaveProvedSJPVerdictForReferToCourtHearing(Decision decision, CaseAggregateState state, List<String> rejectionReasons) {

        validateVerdictForDecisionReferToCourtIsCorrect(decision, state, rejectionReasons, (pleaType, verdictType) ->
                        isNull(pleaType) && VerdictType.NO_VERDICT != verdictType && PROVED_SJP != verdictType && verdictType != null,
                "Offence with No Plea should have verdict as either NO_VERDICT or PROVED_SJP");
    }

    private void validateForMagistrateSessionNotGuiltyCanNotBeWithReferToCourtHearing(Decision decision, final CaseAggregateState state, final SessionType sessionType, List<String> rejectionReasons) {

        validateVerdictForDecisionReferToCourtIsCorrect(decision, state, rejectionReasons,
                (pleaType, verdictType) -> SessionType.MAGISTRATE == sessionType && PleaType.NOT_GUILTY == pleaType && (verdictType == null || verdictType == PROVED_SJP),
                "For Magistrate Session, NOT GUILTY cannot be with refer to court hearing post conviction");
    }

    private void validateDelegatedPowerCanNotSubmitVerdictForReferToCourtHearing(final Decision decision, final SessionType sessionType, final List<String> rejectionReasons) {

        if (SessionType.DELEGATED_POWERS == sessionType) {
            decision.getOffenceDecisions().stream()
                    .filter(offenceDecision -> DecisionType.REFER_FOR_COURT_HEARING == offenceDecision.getType())
                    .map(offenceDecision ->
                            offenceDecision.offenceDecisionInformationAsList().stream().filter(offenceDecisionInformation ->
                                    VerdictType.NO_VERDICT != offenceDecisionInformation.getVerdict()
                            ).collect(toList())
                    )
                    .flatMap(List::stream)
                    .map(offenceDecisionInformation -> format("For Delegated Power session only NO_VERDICT is allowed, %s decision can not be saved for offence %s", REFER_FOR_COURT_HEARING, offenceDecisionInformation.getOffenceId()))
                    .forEach(rejectionReasons::add);
        }
    }

    private void validateDelegatedPowerCanNotSubmitVerdictForAdjourn(final Decision decision, final SessionType sessionType, final List<String> rejectionReasons) {

        if (SessionType.DELEGATED_POWERS == sessionType) {
            decision.getOffenceDecisions().stream()
                    .filter(offenceDecision -> ADJOURN == offenceDecision.getType())
                    .map(offenceDecision ->
                            offenceDecision.offenceDecisionInformationAsList().stream().filter(offenceDecisionInformation ->
                                    VerdictType.NO_VERDICT != offenceDecisionInformation.getVerdict()
                            ).collect(toList())
                    )
                    .flatMap(List::stream)
                    .map(offenceDecisionInformation -> "For Delegated Power session only NO_VERDICT is allowed, ADJOURN decision can not be saved for offence " + offenceDecisionInformation.getOffenceId())
                    .forEach(rejectionReasons::add);
        }
    }

    @SuppressWarnings("PMD.BeanMembersShouldSerialize")
    private static class ConvictionDateOffenceVisitor implements OffenceDecisionVisitor {

        private final CaseAggregateState state;
        private final ZonedDateTime decisionSavedAt;


        private final SessionCourt sessionCourt;

        public ConvictionDateOffenceVisitor(final CaseAggregateState state, final ZonedDateTime decisionSavedAt, final SessionCourt sessionCourt) {
            this.state = state;
            this.decisionSavedAt = decisionSavedAt;
            this.sessionCourt = sessionCourt;
        }

        @Override
        public void visit(final Dismiss dismiss) {
            //No conviction possible for dismiss decisions
        }

        @Override
        public void visit(final Withdraw withdraw) {
            //No conviction possible for withdraw decisions
        }


        private boolean offenceHasPreviousConviction(final OffenceDecision offenceDecision) {
            return offenceDecision.getOffenceIds()
                    .stream()
                    .anyMatch(state::offenceHasPreviousConviction);
        }

        @Override
        public void visit(final Adjourn adjourn) {
            if (offenceHasPreviousConviction(adjourn)) {
                adjourn.getOffenceIds().stream()
                        .map(state::getOffenceConvictionInfo)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .ifPresent(convictingInfo -> {
                            adjourn.setConvictionDate(convictingInfo.getConvictionDate().toLocalDate());
                            adjourn.setConvictionCourt(convictingInfo.getConvictingCourt());

                        });
            } else {
                final LocalDate convictionDate = adjourn
                        .getOffenceDecisionInformation()
                        .stream()
                        .filter(OffenceDecisionInformation::isConviction)
                        .findFirst()
                        .map(e -> decisionSavedAt.toLocalDate()).orElse(null);
                if (nonNull(convictionDate)) {
                    adjourn.setConvictionDate(convictionDate);
                    adjourn.setConvictionCourt(sessionCourt);
                }
            }
        }

        @Override
        public void visit(final ReferForCourtHearing referForCourtHearing) {
            if (offenceHasPreviousConviction(referForCourtHearing)) {
                referForCourtHearing.getOffenceIds().stream()
                        .map(state::getOffenceConvictionInfo)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .ifPresent(convictingInfo -> {
                            referForCourtHearing.setConvictionDate(convictingInfo.getConvictionDate().toLocalDate());
                            referForCourtHearing.setConvictingCourt(convictingInfo.getConvictingCourt());
                        });
            } else {
                final LocalDate convictionDate = referForCourtHearing
                        .getOffenceDecisionInformation()
                        .stream()
                        .filter(OffenceDecisionInformation::isConviction)
                        .findFirst()
                        .map(e -> decisionSavedAt.toLocalDate()).orElse(null);
                if (convictionDate != null) {
                    referForCourtHearing.setConvictionDate(convictionDate);
                    referForCourtHearing.setConvictingCourt(sessionCourt);
                }
            }

        }

        @Override
        public void visit(final Discharge discharge) {
            if (offenceHasPreviousConviction(discharge)) {
                final UUID offenceId = discharge.getOffenceDecisionInformation().getOffenceId();
                final ConvictingInformation convictingInfo = state.getOffenceConvictionInfo(offenceId);
                discharge.setConvictionDate(convictingInfo.getConvictionDate().toLocalDate());
                discharge.setConvictingCourt(convictingInfo.getConvictingCourt());
            } else {
                discharge.setConvictionDate(decisionSavedAt.toLocalDate());
                discharge.setConvictingCourt(sessionCourt);
            }

        }

        @Override
        public void visit(final FinancialPenalty financialPenalty) {
            if (offenceHasPreviousConviction(financialPenalty)) {
                final UUID offenceId = financialPenalty.getOffenceDecisionInformation().getOffenceId();
                final ConvictingInformation convictingInformation = state.getOffenceConvictionInfo(offenceId);
                financialPenalty.setConvictionDate(convictingInformation.getConvictionDate().toLocalDate());
                financialPenalty.setConvictingCourt(convictingInformation.getConvictingCourt());
            } else {
                financialPenalty.setConvictionDate(decisionSavedAt.toLocalDate());
                financialPenalty.setConvictingCourt(sessionCourt);
            }
        }

        @Override
        public void visit(final ReferredToOpenCourt referredToOpenCourt) {
            //No conviction for legacy decision types
        }

        @Override
        public void visit(final ReferredForFutureSJPSession referredForFutureSJPSession) {
            //No conviction for legacy decision types
        }

        @Override
        public void visit(final NoSeparatePenalty noSeparatePenalty) {
            if (offenceHasPreviousConviction(noSeparatePenalty)) {
                final UUID offenceId = noSeparatePenalty.getOffenceDecisionInformation().getOffenceId();
                final ConvictingInformation convictingInformation = state.getOffenceConvictionInfo(offenceId);
                noSeparatePenalty.setConvictionDate(convictingInformation.getConvictionDate().toLocalDate());
                noSeparatePenalty.setConvictingCourt(convictingInformation.getConvictingCourt());
            } else {
                noSeparatePenalty.setConvictionDate(decisionSavedAt.toLocalDate());
                noSeparatePenalty.setConvictingCourt(sessionCourt);
            }
        }

        @Override
        public void visit(final SetAside setAside) {
            //No conviction as the decision should be discarded
        }

    }

    @SuppressWarnings("PMD.BeanMembersShouldSerialize")
    private static class PressRestrictableOffenceDecisionVistor implements OffenceDecisionVisitor {

        private final CaseAggregateState state;

        public PressRestrictableOffenceDecisionVistor(final CaseAggregateState state) {
            this.state = state;
        }

        @Override
        public void visit(final Dismiss dismiss) {
            visitOffenceDecision(dismiss);
        }

        @Override
        public void visit(final Withdraw withdraw) {
            visitOffenceDecision(withdraw);
        }

        @Override
        public void visit(final Adjourn adjourn) {
            visitOffenceDecision(adjourn);
        }

        @Override
        public void visit(final ReferForCourtHearing referForCourtHearing) {
            visitOffenceDecision(referForCourtHearing);
        }

        @Override
        public void visit(final Discharge discharge) {
            visitOffenceDecision(discharge);
        }

        @Override
        public void visit(final FinancialPenalty financialPenalty) {
            visitOffenceDecision(financialPenalty);
        }

        @Override
        public void visit(final ReferredToOpenCourt referredToOpenCourt) {
            visitOffenceDecision(referredToOpenCourt);
        }

        @Override
        public void visit(final ReferredForFutureSJPSession referredForFutureSJPSession) {
            visitOffenceDecision(referredForFutureSJPSession);
        }

        @Override
        public void visit(final NoSeparatePenalty noSeparatePenalty) {
            visitOffenceDecision(noSeparatePenalty);
        }

        @Override
        public void visit(final SetAside setAside) {
            visitOffenceDecision(setAside);
        }

        private void visitOffenceDecision(final OffenceDecision offenceDecision) {
            offenceDecision.offenceDecisionInformationAsList().forEach(offDecInfo -> {
                final UUID offenceId = offDecInfo.getOffenceId();
                offDecInfo.setPressRestrictable(state.isPressRestrictable(offenceId));
            });
        }
    }

    public Stream<Object> expireAocpResponseTimerAndSaveDecision( final AocpDecision aocpDecision, final CaseAggregateState state, final Session session) {

        final Stream.Builder<Object> streamBuilder = Stream.builder();
        final ZonedDateTime savedAt = now();

        addAocpAcceptanceResponseTimerExpiredEvent(state.getCaseId(), streamBuilder);

        // add the defence login here just in case

        final List<OffenceDecision> offenceDecisions = new ArrayList<>();
        final List<Plea> pleas = new ArrayList<>();
        final AOCPCost aocpCost = state.getAOCPCost().get(state.getCaseId());
        final AOCPCostDefendant defendant= aocpCost.getDefendant();
        final List<AOCPCostOffence> aocpCostOffences = defendant.getOffences();

        final BigDecimal aocpTotalCost =  state.getAocpTotalCost().setScale(2, ROUND_DOWN);

        final LumpSum lumpSum = new LumpSum(aocpTotalCost, 28, null );
        final PaymentTerms paymentTerms = new PaymentTerms(false, lumpSum, null);
        final CourtDetails courtDetails = new CourtDetails(aocpDecision.getDefendant().getCourt().getNationalCourtCode(), aocpDecision.getDefendant().getCourt().getNationalCourtName());
        final Payment payment = new Payment(aocpTotalCost, PaymentType.PAY_TO_COURT, "No information from defendant", null, paymentTerms, courtDetails);
        final CostsAndSurcharge costsAndSurcharge = new CostsAndSurcharge(aocpCost.getCosts(), null, state.getAocpVictimSurcharge(), null, null, true);

        final FinancialImposition financialImposition = new FinancialImposition(costsAndSurcharge, payment);
        final SessionCourt sessionCourt = new SessionCourt(session.getCourtHouseCode(), session.getLocalJusticeAreaNationalCourtCode());

        aocpCostOffences.forEach(offence->{
            final FinancialPenalty financialPenalty = createFinancialPenalty(randomUUID(), new OffenceDecisionInformation(offence.getId(), FOUND_GUILTY, false), offence.getAocpStandardPenaltyAmount(),offence.getCompensation(), null, true, null, null, null);
            financialPenalty.setConvictingCourt(sessionCourt);
            financialPenalty.setConvictionDate(savedAt.toLocalDate());
            offenceDecisions.add(financialPenalty);
            pleas.add(new Plea(defendant.getId(), offence.getId(), GUILTY));
        });

        final Decision decision = new Decision(aocpDecision.getDecisionId(), aocpDecision.getSessionId(),  state.getCaseId(), null, savedAt, aocpDecision.getSavedBy(), offenceDecisions, financialImposition, aocpDecision.getDefendant(), null);

        addSetAocpPleaEvent(state.getCaseId(), pleas, state.getAocpAcceptedPleaDate(), streamBuilder);
        addDecisionSavedEvent(session, decision, state, streamBuilder, true);
        addCaseCompletedEventIfAllOffencesHasFinalDecision(decision, streamBuilder, state);

        return streamBuilder.build();
    }

    private void addSetAocpPleaEvent(final UUID caseId,
                                     final List<Plea> pleas,
                                     final ZonedDateTime pleaDate,
                                     final Stream.Builder<Object> streamBuilder){
        streamBuilder.add(new AocpPleasSet(caseId, pleas, pleaDate, PleaMethod.ONLINE));
    }

    private void addAocpAcceptanceResponseTimerExpiredEvent(final UUID caseId, final Stream.Builder<Object> streamBuilder){
        streamBuilder.add(new DefendantAocpResponseTimerExpired(caseId));
    }

}
