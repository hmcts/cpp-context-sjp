package uk.gov.moj.cpp.sjp.domain.aggregate.handler.plea;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.moj.cpp.sjp.domain.DomainConstants.NUMBER_DAYS_WAITING_FOR_DATES_TO_AVOID;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaMethod.POSTAL;

import uk.gov.moj.cpp.sjp.domain.DefendantCourtInterpreter;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseLanguageHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.domain.plea.SetPleas;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidRequired;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.PleadedGuilty;
import uk.gov.moj.cpp.sjp.event.PleadedGuiltyCourtHearingRequested;
import uk.gov.moj.cpp.sjp.event.PleadedNotGuilty;
import uk.gov.moj.cpp.sjp.event.PleasSet;
import uk.gov.moj.cpp.sjp.event.TrialRequestCancelled;
import uk.gov.moj.cpp.sjp.event.TrialRequested;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

final class PleaHandlerUtils {

    private PleaHandlerUtils() {
        // static methods only
    }

    static boolean hasNeverRaisedTrialRequestedEventAndTrialRequired(final List<Plea> pleas, final CaseAggregateState state) {
        return !state.isTrialRequested() && !state.isTrialRequestedPreviously()
                && trialRequired(pleas.stream().map(Plea::getPleaType).toArray(PleaType[]::new));
    }

    static boolean wasTrialRequestedThenCancelledAndIsTrialRequiredAgain(final List<Plea> pleas,
                                                                         final CaseAggregateState state) {
        return !state.isTrialRequested() && state.isTrialRequestedPreviously()
                && trialRequired(pleas.stream().map(Plea::getPleaType).toArray(PleaType[]::new));
    }

    static boolean isTrialRequestCancellationRequired(final List<Plea> pleas,
                                                      final CaseAggregateState state) {
        return state.isTrialRequested() && !trialRequired(pleas.stream().map(Plea::getPleaType).toArray(PleaType[]::new));
    }

    static boolean trialRequired(final PleaType... pleaType) {
        return Arrays.asList(pleaType).contains(PleaType.NOT_GUILTY);
    }

    static Stream<Object> createSetPleasEvents(final UUID caseId,
                                               final SetPleas pleasRequested,
                                               final CaseAggregateState state,
                                               final UUID userId,
                                               final ZonedDateTime pleadAt,
                                               final CaseLanguageHandler caseLanguageHandler,
                                               final PleaMethod pleaMethod) {

        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(new PleasSet(caseId, pleasRequested.getDefendantCourtOptions(),
                pleasRequested.getPleas()));

        defendantIds(pleasRequested).forEach(defendantId -> {
            final List hearingEvents = updateHearingRequirements(userId, defendantId,
                    pleasRequested.getDefendantCourtOptions(), state, caseLanguageHandler, pleaMethod, pleadAt);
            hearingEvents.forEach(streamBuilder::add);

        });

        if (POSTAL.equals(pleaMethod)) {
            handleTrialRequestEvents(pleasRequested.getPleas(), streamBuilder, pleadAt, state);
        }

        pleasRequested.getPleas().forEach(plea -> processPlea(caseId, state, pleadAt, streamBuilder, plea, pleaMethod));

        raiseDatesToAvoidRequiredIfNecessary(caseId, pleasRequested.getPleas(), state, pleadAt, streamBuilder);

        return streamBuilder.build();
    }

    private static void raiseDatesToAvoidRequiredIfNecessary(final UUID caseId, final List<Plea> pleasRequested, final CaseAggregateState state, final ZonedDateTime pleadAt, final Stream.Builder<Object> streamBuilder) {
        if (datesToAvoidNotSetAndDatesToAvoidTimerNotStarted(state) && isAnyPleaNotGuilty(pleasRequested)) {
            final LocalDate expectedDateReady = pleadAt.toLocalDate().plusDays(NUMBER_DAYS_WAITING_FOR_DATES_TO_AVOID);
            streamBuilder.add(new DatesToAvoidRequired(caseId, expectedDateReady));
        }
    }

    private static boolean datesToAvoidNotSetAndDatesToAvoidTimerNotStarted(final CaseAggregateState state) {
        return !state.isDatesToAvoidTimerPreviouslyStarted() && isEmpty(state.getDatesToAvoid());
    }

    private static boolean isAnyPleaNotGuilty(final List<Plea> pleasRequested) {
        return pleasRequested.stream()
                .map(Plea::getPleaType)
                .anyMatch(pleaType -> PleaType.NOT_GUILTY == pleaType);
    }

    private static void processPlea(final UUID caseId, final CaseAggregateState state, final ZonedDateTime pleadAt, final Stream.Builder<Object> streamBuilder, final Plea plea, final PleaMethod pleaMethod) {
        if (plea.getPleaType() == null && state.getOffenceIdsWithPleas().contains(plea.getOffenceId())) {
            streamBuilder.add(new PleaCancelled(caseId, plea.getOffenceId(), plea.getDefendantId()));
        } else if (plea.getPleaType() != null && isThePleaNewOrDifferentThanPrevious(plea, state)) {
            processNonCancellingPlea(caseId, pleadAt, streamBuilder, plea, pleaMethod);
        }
    }

    private static Set<UUID> defendantIds(final SetPleas pleas) {
        return pleas.getPleas().stream().map(Plea::getDefendantId).collect(toSet());
    }

    private static List<Object> updateHearingRequirements(final UUID userId,
                                                          final UUID defendantId,
                                                          final DefendantCourtOptions courtOptions,
                                                          final CaseAggregateState state,
                                                          final CaseLanguageHandler caseLanguageHandler,
                                                          final PleaMethod pleaMethod,
                                                          final ZonedDateTime createdOn) {
        Stream<Object> hearingEvents;
        if ((ofNullable(courtOptions).isPresent())) {
            hearingEvents = caseLanguageHandler.updateHearingRequirements(
                    userId,
                    defendantId,
                    ofNullable(courtOptions.getInterpreter()).
                            map(DefendantCourtInterpreter::getLanguage).
                            orElse(null),
                    courtOptions.getWelshHearing(),
                    state,
                    pleaMethod,
                    createdOn);
        } else {
            hearingEvents = caseLanguageHandler.updateHearingRequirements(userId, defendantId, null, null, state, pleaMethod, createdOn);
        }

        return hearingEvents.collect(toList());
    }

    private static void handleTrialRequestEvents(final List<Plea> pleas,
                                                 final Stream.Builder<Object> streamBuilder,
                                                 final ZonedDateTime updatedOn,
                                                 final CaseAggregateState state) {

        if (hasNeverRaisedTrialRequestedEventAndTrialRequired(pleas, state)) {
            streamBuilder.add(new TrialRequested(state.getCaseId(), updatedOn));
        } else if (isTrialRequestCancellationRequired(pleas, state)) {
            streamBuilder.add(new TrialRequestCancelled(state.getCaseId()));
        } else if (wasTrialRequestedThenCancelledAndIsTrialRequiredAgain(pleas, state)) {
            streamBuilder.add(
                    new TrialRequested(
                            state.getCaseId(),
                            state.getTrialRequestedUnavailability(),
                            state.getTrialRequestedWitnessDetails(),
                            state.getTrialRequestedWitnessDispute(),
                            updatedOn));
        }
    }

    private static boolean isThePleaNewOrDifferentThanPrevious(final Plea plea, final CaseAggregateState state) {
        return !plea.getPleaType().equals(state.getPleaTypeForOffenceId(plea.getOffenceId()));
    }

    private static void processNonCancellingPlea(final UUID caseId, final ZonedDateTime pleadAt, final Stream.Builder<Object> streamBuilder, final Plea plea, final PleaMethod pleaMethod) {
        switch (plea.getPleaType()) {
            case GUILTY:
                streamBuilder.add(new PleadedGuilty(caseId, plea.getDefendantId(), plea.getOffenceId(),
                        pleaMethod, plea.getMitigation(), pleadAt));
                break;
            case GUILTY_REQUEST_HEARING:
                streamBuilder.add(new PleadedGuiltyCourtHearingRequested(caseId, plea.getDefendantId(), plea.getOffenceId(), pleaMethod, plea.getMitigation(), pleadAt));
                break;
            case NOT_GUILTY:
                streamBuilder.add(new PleadedNotGuilty(caseId, plea.getDefendantId(), plea.getOffenceId(), plea.getNotGuiltyBecause(), pleadAt, pleaMethod));
                break;
            default:
                throw new UnsupportedOperationException("Event does not exit for " + plea.getPleaType());
        }
    }

}

