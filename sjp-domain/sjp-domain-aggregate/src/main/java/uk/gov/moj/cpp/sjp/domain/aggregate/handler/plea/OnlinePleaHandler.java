package uk.gov.moj.cpp.sjp.domain.aggregate.handler.plea;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang.BooleanUtils.isTrue;
import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.HandlerUtils.createRejectionEvents;
import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.plea.PleaHandlerUtils.createSetPleasEvents;
import static uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds.NO_DISABILITY_NEEDS;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaMethod.ONLINE;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.event.CaseUpdateRejected.RejectReason.PLEA_ALREADY_SUBMITTED;

import uk.gov.moj.cpp.sjp.domain.DefendantCourtInterpreter;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.Interpreter;
import uk.gov.moj.cpp.sjp.domain.Outgoing;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseDefendantHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseEmployerHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseLanguageHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.HandlerUtils;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.legalentity.LegalEntityDefendant;
import uk.gov.moj.cpp.sjp.domain.onlineplea.Offence;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PersonalDetails;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PleadOnline;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.domain.plea.SetPleas;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;
import uk.gov.moj.cpp.sjp.event.OffenceNotFound;
import uk.gov.moj.cpp.sjp.event.OnlinePleaReceived;
import uk.gov.moj.cpp.sjp.event.OutstandingFinesUpdated;
import uk.gov.moj.cpp.sjp.event.TrialRequested;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class OnlinePleaHandler {

    public static final OnlinePleaHandler INSTANCE = new OnlinePleaHandler();

    private static final Logger LOGGER = LoggerFactory.getLogger(OnlinePleaHandler.class);

    private final CaseDefendantHandler caseDefendantHandler;
    private final CaseEmployerHandler caseEmployerHandler;
    private final CaseLanguageHandler caseLanguageHandler;

    private OnlinePleaHandler() {
        caseEmployerHandler = CaseEmployerHandler.INSTANCE;
        caseDefendantHandler = CaseDefendantHandler.INSTANCE;
        caseLanguageHandler = CaseLanguageHandler.INSTANCE;
    }

    @VisibleForTesting
    OnlinePleaHandler(
            CaseDefendantHandler caseDefendantHandler,
            CaseEmployerHandler caseEmployerHandler,
            CaseLanguageHandler caseLanguageHandler) {

        this.caseDefendantHandler = caseDefendantHandler;
        this.caseEmployerHandler = caseEmployerHandler;
        this.caseLanguageHandler = caseLanguageHandler;
    }

    public Stream<Object> pleadOnline(final UUID caseId, final PleadOnline pleadOnline, final ZonedDateTime createdOn, CaseAggregateState state, final UUID userId) {
        return createRejectionEvents(
                userId,
                "Plead online",
                pleadOnline.getDefendantId(),
                state
        ).orElse(this.createPleadOnlineEvents(caseId, pleadOnline, createdOn, state, userId));
    }

    private Stream<Object> createPleadOnlineEvents(final UUID caseId, final PleadOnline pleadOnline, final ZonedDateTime createdOn, CaseAggregateState state, final UUID userId) {

        final Optional<Offence> offencePreviouslySubmitted = pleadOnline.getOffences().stream().filter(offence -> state.getOffenceIdsWithPleas().contains(offence.getId())).findAny();

        if (offencePreviouslySubmitted.isPresent()) {
            return of(new CaseUpdateRejected(state.getCaseId(), PLEA_ALREADY_SUBMITTED));
        }

        final List<Offence> offencesNotFound = pleadOnline.getOffences().stream().filter(offence -> !state.offenceExists(offence.getId())).collect(toList());
        if (!offencesNotFound.isEmpty()) {
            final Stream.Builder<Object> offenceNotFoundStreamBuilder = Stream.builder();
            offencesNotFound.forEach(missingOffence -> {
                LOGGER.warn("Cannot update plea for offence which doesn't exist, ID: {}", missingOffence.getId());
                offenceNotFoundStreamBuilder.add(new OffenceNotFound(missingOffence.getId(), "Plead online"));
            });
            return offenceNotFoundStreamBuilder.build();
        }

        final SetPleas setPleas = mapToSetPleas(pleadOnline);
        return HandlerUtils.createRejectionEvents(
                userId,
                state,
                setPleas.getPleas(),
                "Plead Online"
        ).orElse(concat(createSetPleasEvents(caseId, setPleas, state, userId, createdOn, caseLanguageHandler, ONLINE),
                addAdditionalEventsToStreamForStoreOnlinePlea(pleadOnline, createdOn, state)));

    }

    private SetPleas mapToSetPleas(final PleadOnline pleadOnline){
        final Interpreter interpreter = Interpreter.of(pleadOnline.getInterpreterLanguage());
        final DefendantCourtInterpreter defendantCourtInterpreter = new DefendantCourtInterpreter(interpreter.getLanguage(), interpreter.isNeeded());
        final Boolean welshHearing = pleadOnline.getSpeakWelsh();
        final DefendantCourtOptions defendantCourtOptions = new DefendantCourtOptions(
                defendantCourtInterpreter,
                isTrue(welshHearing),
                pleadOnline.getDisabilityNeeds());

        final List<Plea> pleas = pleadOnline.getOffences().stream().map( offence ->
        {
            PleaType type = offence.getPlea();
            if (offence.getPlea().equals(GUILTY) && isTrue(pleadOnline.getComeToCourt())) {
                type = GUILTY_REQUEST_HEARING;
            }
            return new Plea(pleadOnline.getDefendantId(), offence.getId(), type, offence.getNotGuiltyBecause(), offence.getMitigation());
        }).collect(toList());

        return new SetPleas(defendantCourtOptions, pleas);
    }

    private Stream<Object> addAdditionalEventsToStreamForStoreOnlinePlea(final PleadOnline pleadOnline,
                                                               final ZonedDateTime createdOn,
                                                               final CaseAggregateState state) {

        if (!isThePleaNewOrDifferentThanPrevious(pleadOnline, state)){
            return Stream.builder().build();
        }

        final Stream.Builder<Object> streamBuilder = Stream.builder();

        if (isTrialRequested(pleadOnline)) {
            streamBuilder.add(new TrialRequested(state.getCaseId(), pleadOnline.getUnavailability(),
                    pleadOnline.getWitnessDetails(), pleadOnline.getWitnessDispute(), createdOn));
        }

        final PersonalDetails personalDetails = pleadOnline.getPersonalDetails();
        final UUID defendantId = pleadOnline.getDefendantId();
        final boolean updatedByOnlinePlea = true;
        if(nonNull(personalDetails)) {
            ofNullable(state.getDefendantDetailsUpdateSummary(personalDetails, updatedByOnlinePlea, createdOn))
                    .ifPresent(streamBuilder::add);
            caseDefendantHandler.getDefendantWarningEvents(personalDetails, createdOn, updatedByOnlinePlea, state)
                    .forEach(streamBuilder::add);
        }

        final LegalEntityDefendant legalEntityDefendant = pleadOnline.getLegalEntityDefendant();

        if(nonNull(legalEntityDefendant)) {
            ofNullable(state.getDefendantLegalEntityDetailsUpdateSummary(legalEntityDefendant, updatedByOnlinePlea, createdOn))
                    .ifPresent(streamBuilder::add);
            caseDefendantHandler.getLegalEntityDefendantWarningEvents(legalEntityDefendant, createdOn, state)
                    .forEach(streamBuilder::add);
        }

        if (anyUpdatesOnFinancialMeans(pleadOnline.getFinancialMeans(), pleadOnline.getOutgoings())) {
            final Optional<FinancialMeans> optionalFinancialMeans = Optional.ofNullable(
                    pleadOnline.getFinancialMeans());

            streamBuilder.add(FinancialMeansUpdated.createEventForOnlinePlea(
                    defendantId,
                    optionalFinancialMeans.map(FinancialMeans::getIncome).orElse(null),
                    optionalFinancialMeans.map(FinancialMeans::getBenefits).orElse(null),
                    optionalFinancialMeans.map(FinancialMeans::getEmploymentStatus).orElse(null),
                    pleadOnline.getOutgoings(),
                    createdOn,
                    optionalFinancialMeans.map(FinancialMeans::getGrossTurnover).orElse(null),
                    optionalFinancialMeans.map(FinancialMeans::getNetTurnover).orElse(null),
                    optionalFinancialMeans.map(FinancialMeans::getNumberOfEmployees).orElse(null),
                    optionalFinancialMeans.map(FinancialMeans::getTradingMoreThanTwelveMonths).orElse(null)
                    ));
        }

        if (nonNull(pleadOnline.getEmployer())) {
            caseEmployerHandler.getEmployerEventStream(pleadOnline.getEmployer(), defendantId, updatedByOnlinePlea, createdOn, state)
                    .forEach(streamBuilder::add);
        }

        if (nonNull(pleadOnline.getOutstandingFines())) {
            streamBuilder.add(new OutstandingFinesUpdated(state.getCaseId(), pleadOnline.getOutstandingFines(), createdOn));
        }

        streamBuilder.add(new OnlinePleaReceived(state.getUrn(), state.getCaseId(), defendantId,
                pleadOnline.getUnavailability(), pleadOnline.getInterpreterLanguage(), pleadOnline.getSpeakWelsh(),
                pleadOnline.getWitnessDetails(), pleadOnline.getWitnessDispute(), pleadOnline.getOutstandingFines(), personalDetails,
                pleadOnline.getFinancialMeans(), pleadOnline.getEmployer(), pleadOnline.getOutgoings(),
                ofNullable(pleadOnline.getDisabilityNeeds()).orElse(NO_DISABILITY_NEEDS), pleadOnline.getLegalEntityDefendant(), pleadOnline.getLegalEntityFinancialMeans()));
        return streamBuilder.build();
    }

    private boolean isTrialRequested(final PleadOnline pleadOnline) {
        return pleadOnline.getOffences().stream().anyMatch(offence -> offence.getPlea().equals(NOT_GUILTY));
    }

    private static boolean anyUpdatesOnFinancialMeans(final FinancialMeans financialMeans, final List<Outgoing> outgoings) {
        return financialMeans != null || !isEmpty(outgoings);
    }

    private static boolean isThePleaNewOrDifferentThanPrevious(final PleadOnline pleaseOnline, final CaseAggregateState state) {
        return pleaseOnline.getOffences().stream().anyMatch(offence -> !offence.getPlea().equals(state.getPleaTypeForOffenceId(offence.getId())));
    }
}
