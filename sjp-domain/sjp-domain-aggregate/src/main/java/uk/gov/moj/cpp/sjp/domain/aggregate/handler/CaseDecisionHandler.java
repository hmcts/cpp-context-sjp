package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.json.schemas.domains.sjp.Note.note;
import static uk.gov.justice.json.schemas.domains.sjp.NoteType.ADJOURNMENT;
import static uk.gov.justice.json.schemas.domains.sjp.NoteType.DECISION;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.ADJOURN;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISCHARGE;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISMISS;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.FINANCIAL_PENALTY;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.REFER_FOR_COURT_HEARING;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.WITHDRAW;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;
import static uk.gov.moj.cpp.sjp.event.CaseNoteAdded.caseNoteAdded;
import static uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing.caseReferredForCourtHearing;

import uk.gov.justice.json.schemas.domains.sjp.Note;
import uk.gov.justice.json.schemas.domains.sjp.NoteType;
import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtInterpreter;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.aggregate.Session;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.decision.Adjourn;
import uk.gov.moj.cpp.sjp.domain.decision.Decision;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.decision.Defendant;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.decision.ReferForCourtHearing;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.CaseAdjournedToLaterSjpHearingRecorded;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.CaseNoteAdded;
import uk.gov.moj.cpp.sjp.event.decision.DecisionRejected;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;

import java.util.ArrayList;
import java.util.Arrays;
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

    private CaseDecisionHandler() {
    }

    private static void addDecisionSavedEvent(final Session session, Decision decision, final Stream.Builder<Object> streamBuilder) {

        if(decisionHasFinancialImposition(decision) && sessionCourtAndDefendantCourtAreDifferent(session, decision.getDefendant())){
            decision.getFinancialImposition().getPayment().setFineTransferredTo(decision.getDefendant().getCourt());
        }

        streamBuilder.add(new DecisionSaved(
                decision.getDecisionId(),
                decision.getSessionId(),
                decision.getCaseId(),
                decision.getSavedAt(),
                decision.getOffenceDecisions(),
                decision.getFinancialImposition()));
    }

    private static boolean sessionCourtAndDefendantCourtAreDifferent(final Session session, final Defendant defendant) {
        return defendant!=null && defendant.getCourt()!=null &&
                !session.getLocalJusticeAreaNationalCourtCode().equals(defendant.getCourt().getNationalCourtCode());
    }

    private static boolean decisionHasFinancialImposition(final Decision decision) {
        return decision.getFinancialImposition()!=null && decision.getOffenceDecisions().stream()
                .map(OffenceDecision::getType)
                .anyMatch(decisionType -> decisionType.equals(DISCHARGE) || decisionType.equals(FINANCIAL_PENALTY));
    }

    private static void addCaseNoteAddedEventIfNoteIsPresent(final Decision decision, final Stream.Builder<Object> streamBuilder) {
        if (isNotBlank(decision.getNote())) {
            streamBuilder.add(new CaseNoteAdded(decision.getSavedBy(), decision.getCaseId(), decision.getDecisionId(), new Note(decision.getSavedAt(), randomUUID(), decision.getNote(), DECISION)));
        }
    }

    private static void addCaseUnassignedEvent(final Decision decision, final Stream.Builder<Object> streamBuilder) {
        streamBuilder.add(new CaseUnassigned(decision.getCaseId()));
    }

    private static void addCaseCompletedEventIfAllOffencesHasFinalDecision(final Decision decision, final Stream.Builder<Object> streamBuilder, final CaseAggregateState state) {
        final Set<UUID> offencesWithFinalDecision = getOffenceIds(state.getOffenceDecisions(), OffenceDecision::isFinalDecision);
        final Set<UUID> incomingOffencesWithFinalDecision = getOffenceIds(decision.getOffenceDecisions(), OffenceDecision::isFinalDecision);

        offencesWithFinalDecision.addAll(incomingOffencesWithFinalDecision);

        if (offencesWithFinalDecision.equals(state.getOffences())) {
            streamBuilder.add(new CaseCompleted(decision.getCaseId()));
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
        //TODO ATCM-4695 use offenceDecisionInformation from decision
        if (referForCourtHearingDecision.isPresent()) {
            final ReferForCourtHearing referForCourtHearing = referForCourtHearingDecision.get();
            final List<OffenceDecisionInformation> offenceDecisionInformationList = referForCourtHearing.getOffenceDecisionInformation();

            streamBuilder.add(caseReferredForCourtHearing()
                    .withCaseId(decision.getCaseId())
                    .withDecisionId(decision.getDecisionId())
                    .withReferredAt(decision.getSavedAt())
                    .withReferralReasonId(referForCourtHearing.getReferralReasonId())
                    .withEstimatedHearingDuration(referForCourtHearing.getEstimatedHearingDuration())
                    .withListingNotes(referForCourtHearing.getListingNotes())
                    .withReferredOffences(offenceDecisionInformationList)
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

    private static void validateOffencesHaveDecision(final Decision decision, final CaseAggregateState state, final List<String> rejectionReason) {
        final Set<UUID> offenceIds = decision.getOffenceIds();

        getOffencesRequiringDecision(state)
                .stream()
                .filter(offenceId -> !offenceIds.contains(offenceId))
                .map(offenceId -> format("Offence with id %s must have a decision", offenceId))
                .forEach(rejectionReason::add);
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
        }
    }

    private static Stream<Object> updateHearingRequirements(final User user, final DefendantCourtOptions courtOptions, final CaseAggregateState state) {
        final String interpreterLanguage = ofNullable(courtOptions.getInterpreter())
                .map(DefendantCourtInterpreter::getLanguage)
                .orElse(null);

        return CaseLanguageHandler.INSTANCE.updateHearingRequirements(user.getUserId(), state.getDefendantId(), interpreterLanguage, courtOptions.getWelshHearing(), state, PleaMethod.POSTAL, null);
    }

    private static Collection<UUID> getOffencesRequiringDecision(final CaseAggregateState state) {
        return state.getOffenceDecisions().isEmpty() ?
                state.getOffences() :
                getOffenceIds(state.getOffenceDecisions(), OffenceDecision::isNotFinalDecision);
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
        addDecisionSavedEvent(session, decision, streamBuilder);
        addCaseNoteAddedEventIfNoteIsPresent(decision, streamBuilder);
        addCaseUnassignedEvent(decision, streamBuilder);

        handleAdjournDecision(decision, streamBuilder);
        handleReferToCriminalCourtDecision(decision, state, streamBuilder);

        addCaseCompletedEventIfAllOffencesHasFinalDecision(decision, streamBuilder, state);

        return streamBuilder.build();
    }

    private List<String> validateDecision(final Decision decision, final CaseAggregateState state, final SessionType sessionType) {
        final List<String> rejectionReason = new ArrayList<>();

        validateCaseIsNotCompleted(state, rejectionReason);
        validateCaseIsAssignedToTheCaller(decision, state, rejectionReason);
        validateOffencesBelongToTheCase(decision, state, rejectionReason);
        validateOffencesHaveDecision(decision, state, rejectionReason);
        validateOffencesHasOnlyOneDecision(decision, rejectionReason);
        validateDecisionsCombinations(decision, rejectionReason);
        validateOffencesDoNotHavePreviousFinalDecision(decision, state, rejectionReason);
        validateDecisionTypesAndVerdict(decision, rejectionReason);
        validateEmployerDetailsAppliedWhenPaymentTypeIsAttachedToEarnings(decision, state, rejectionReason);

        validateDelegatedPowerCanNotSubmitVerdictForReferToCourtHearing(decision, sessionType, rejectionReason);
        validateForMagistrateSessionNotGuiltyCanNotBeWithReferToCourtHearing(decision, state, sessionType, rejectionReason);
        validateNoPleaShouldHaveProvedSJPVerdictForReferToCourtHearing(decision, state, rejectionReason);
        validateIfPleaExistsThenVerdictCanNotBeProvedSJPVerdict(decision, state, rejectionReason);
        validateNotGuiltyPleaShouldHaveNoVerdictForReferToCourtHearing(decision, state, rejectionReason);
        validateGuiltyPleaShouldHaveFoundGuiltyVerdictForReferToCourtHearing(decision, state, rejectionReason);
        return rejectionReason;
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
                verdict -> verdict == FOUND_GUILTY || verdict == PROVED_SJP) > 0) {
            rejectionReasons.add(format(message, "Financial Penalty and Discharge", "either FOUND_GUILTY or PROVED_SJP"));
        }

        if (unmatchedDecisionTypeAndVerdicts.apply(a -> a == WITHDRAW, verdict -> verdict == NO_VERDICT) > 0) {
            rejectionReasons.add(format(message, "Withdraw", "NO_VERDICT"));
        }

        if (unmatchedDecisionTypeAndVerdicts.apply(a -> a == DISMISS, verdict -> verdict == FOUND_NOT_GUILTY) > 0) {
            rejectionReasons.add(format(message, "Dismiss", "FOUND_NOT_GUILTY"));
        }
    }

    private static void validateEmployerDetailsAppliedWhenPaymentTypeIsAttachedToEarnings(final Decision decision, final CaseAggregateState state, final List<String> rejectionReason) {
        if (decision.getFinancialImposition() != null && decision.getFinancialImposition().getPayment() != null && decision.getFinancialImposition().getPayment().getPaymentType() == PaymentType.ATTACH_TO_EARNINGS && !state.hasEmployerDetailsUpdated()) {
            rejectionReason.add("Decision with payment type attach to earnings requires employer details");
        }
    }

    private void validateIfPleaExistsThenVerdictCanNotBeProvedSJPVerdict(Decision decision, CaseAggregateState state, List<String> rejectionReasons) {
        decision.getOffenceDecisions().stream()
                .map(offenceDecision ->
                    offenceDecision.offenceDecisionInformationAsList().stream().filter(offenceDecisionInformation ->  {
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
                        offenceDecision.offenceDecisionInformationAsList().stream().filter(offenceDecisionInformation ->  {
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
                PleaType.GUILTY.equals(pleaType) && !Arrays.asList(FOUND_GUILTY, VerdictType.NO_VERDICT).contains(verdictType),
                "Guilty plea should have Found Guilty Verdict");
    }

    private void validateNotGuiltyPleaShouldHaveNoVerdictForReferToCourtHearing(Decision decision, CaseAggregateState state, List<String> rejectionReasons) {
        validateVerdictForDecisionReferToCourtIsCorrect(decision, state, rejectionReasons, (pleaType, verdictType) ->
                PleaType.NOT_GUILTY.equals(pleaType) && VerdictType.NO_VERDICT != verdictType,
                "Not Guilty plea should have no verdict");
    }

    private void validateNoPleaShouldHaveProvedSJPVerdictForReferToCourtHearing(Decision decision, CaseAggregateState state, List<String> rejectionReasons) {

        validateVerdictForDecisionReferToCourtIsCorrect(decision, state, rejectionReasons, (pleaType, verdictType) ->
                isNull(pleaType) && VerdictType.NO_VERDICT != verdictType && PROVED_SJP != verdictType,
                "Offence with No Plea should have verdict as either NO_VERDICT or PROVED_SJP");
    }

    private void validateForMagistrateSessionNotGuiltyCanNotBeWithReferToCourtHearing(Decision decision, final CaseAggregateState state, final SessionType sessionType, List<String> rejectionReasons) {

        validateVerdictForDecisionReferToCourtIsCorrect(decision, state, rejectionReasons,
                (pleaType, verdictType) -> SessionType.MAGISTRATE == sessionType && PleaType.NOT_GUILTY == pleaType,
                "For Magistrate Session, NOT GUILTY cannot be with refer to court hearing post conviction");
    }

    private void validateDelegatedPowerCanNotSubmitVerdictForReferToCourtHearing(final Decision decision, final SessionType sessionType, final List<String> rejectionReasons) {

        if(SessionType.DELEGATED_POWERS == sessionType){
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
}
