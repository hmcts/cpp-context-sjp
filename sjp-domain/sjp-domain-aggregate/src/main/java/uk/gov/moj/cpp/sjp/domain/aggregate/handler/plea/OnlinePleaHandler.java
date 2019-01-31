package uk.gov.moj.cpp.sjp.domain.aggregate.handler.plea;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.HandlerUtils.createRejectionEvents;
import static uk.gov.moj.cpp.sjp.event.CaseUpdateRejected.RejectReason.PLEA_ALREADY_SUBMITTED;
import static uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated.DefendantDetailsUpdatedBuilder.defendantDetailsUpdated;

import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.Outgoing;
import uk.gov.moj.cpp.sjp.domain.aggregate.domain.PleadOnlineOutcomes;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseDefendantHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseEmployerHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseLanguageHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.onlineplea.Offence;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PersonalDetails;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PleadOnline;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;
import uk.gov.moj.cpp.sjp.event.OffenceNotFound;
import uk.gov.moj.cpp.sjp.event.OnlinePleaReceived;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
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
        caseDefendantHandler = CaseDefendantHandler.INSTANCE;
        caseEmployerHandler = CaseEmployerHandler.INSTANCE;
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

    public Stream<Object> pleadOnline(final UUID caseId, final PleadOnline pleadOnline, final ZonedDateTime createdOn, CaseAggregateState state) {
        return createRejectionEvents(
                null,
                "Plead online",
                pleadOnline.getDefendantId(),
                state
        ).orElse(this.createPleadOnlineEvents(caseId, pleadOnline, createdOn, state));
    }

    private Stream<Object> createPleadOnlineEvents(final UUID caseId, final PleadOnline pleadOnline, final ZonedDateTime createdOn, CaseAggregateState state) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();

        final PleadOnlineOutcomes pleadOnlineOutcomes = addPleaEventsToStreamForStoreOnlinePlea(caseId, pleadOnline, streamBuilder, createdOn, state);
        if (pleadOnlineOutcomes.isPleaForOffencePreviouslySubmitted()) {
            return Stream.of(new CaseUpdateRejected(state.getCaseId(), PLEA_ALREADY_SUBMITTED));
        }
        if (!pleadOnlineOutcomes.getOffenceNotFoundIds().isEmpty()) {
            final Stream.Builder<Object> offenceNotFoundStreamBuilder = Stream.builder();
            pleadOnlineOutcomes.getOffenceNotFoundIds().forEach(offenceNotFoundId ->
                    offenceNotFoundStreamBuilder.add(new OffenceNotFound(offenceNotFoundId, "Plead online")));
            return offenceNotFoundStreamBuilder.build();
        }
        if (pleadOnlineOutcomes.isTrialRequested()) {
            streamBuilder.add(new TrialRequested(caseId, pleadOnline.getUnavailability(),
                    pleadOnline.getWitnessDetails(), pleadOnline.getWitnessDispute(), createdOn));
        }
        addAdditionalEventsToStreamForStoreOnlinePlea(streamBuilder, pleadOnline, createdOn, state);

        return streamBuilder.build();
    }

    private PleadOnlineOutcomes addPleaEventsToStreamForStoreOnlinePlea(final UUID caseId,
                                                                        final PleadOnline pleadOnline,
                                                                        final Stream.Builder<Object> streamBuilder,
                                                                        final ZonedDateTime createdOn,
                                                                        final CaseAggregateState state) {

        final PleadOnlineOutcomes pleadOnlineOutcomes = new PleadOnlineOutcomes();
        pleadOnline.getOffences().forEach(offence -> {
            if (canPleaOnOffence(offence, pleadOnlineOutcomes, state)) {
                PleaType pleaType = offence.getPlea();
                if (pleaType.equals(PleaType.GUILTY) && offence.getComeToCourt() != null && offence.getComeToCourt()) {
                    pleaType = PleaType.GUILTY_REQUEST_HEARING;
                }
                final PleaUpdated pleaUpdated = new PleaUpdated(
                        caseId,
                        offence.getId(),
                        pleaType,
                        offence.getMitigation(),
                        offence.getNotGuiltyBecause(),
                        PleaMethod.ONLINE,
                        createdOn);
                streamBuilder.add(pleaUpdated);
                if (pleaType.equals(PleaType.NOT_GUILTY)) {
                    pleadOnlineOutcomes.setTrialRequested(true);
                }
            }
        });
        return pleadOnlineOutcomes;
    }

    private boolean canPleaOnOffence(Offence offence, final PleadOnlineOutcomes pleadOnlineOutcomes, final CaseAggregateState state) {
        if (!state.offenceExists(offence.getId())) {
            LOGGER.warn("Cannot update plea for offence which doesn't exist, ID: {}", offence.getId());
            pleadOnlineOutcomes.getOffenceNotFoundIds().add(offence.getId());
            return false;
        } else if (state.getOffenceIdsWithPleas().contains(offence.getId())) {
            pleadOnlineOutcomes.setPleaForOffencePreviouslySubmitted(true);
            return false;
        }
        return true;
    }

    private void addAdditionalEventsToStreamForStoreOnlinePlea(final Stream.Builder<Object> streamBuilder,
                                                               final PleadOnline pleadOnline,
                                                               final ZonedDateTime createdOn,
                                                               final CaseAggregateState state) {
        //TODO: we need to query the defendant to see if any of the incoming defendant data is different from the pre-existing defendant data. If no changes, no event
        final PersonalDetails personalDetails = pleadOnline.getPersonalDetails();
        final UUID defendantId = pleadOnline.getDefendantId();
        final boolean updatedByOnlinePlea = true;

        streamBuilder.add(defendantDetailsUpdated()
                .withCaseId(state.getCaseId())
                .withDefendantId(defendantId)
                .withFirstName(personalDetails.getFirstName())
                .withLastName(personalDetails.getLastName())
                .withDateOfBirth(personalDetails.getDateOfBirth())
                .withNationalInsuranceNumber(personalDetails.getNationalInsuranceNumber())
                .withContactDetails(personalDetails.getContactDetails())
                .withAddress(personalDetails.getAddress())
                .withUpdateByOnlinePlea(updatedByOnlinePlea)
                .withUpdatedDate(createdOn)
                .build());

        caseDefendantHandler.getDefendantWarningEvents(personalDetails, createdOn, updatedByOnlinePlea, state)
                .forEach(streamBuilder::add);

        if (anyUpdatesOnFinancialMeans(pleadOnline.getFinancialMeans(), pleadOnline.getOutgoings())) {
            final Optional<FinancialMeans> optionalFinancialMeans = Optional.ofNullable(
                    pleadOnline.getFinancialMeans());

            streamBuilder.add(FinancialMeansUpdated.createEventForOnlinePlea(
                    defendantId,
                    optionalFinancialMeans.map(FinancialMeans::getIncome).orElse(null),
                    optionalFinancialMeans.map(FinancialMeans::getBenefits).orElse(null),
                    optionalFinancialMeans.map(FinancialMeans::getEmploymentStatus).orElse(null),
                    pleadOnline.getOutgoings(),
                    createdOn));
        }

        if (pleadOnline.getEmployer() != null) {
            caseEmployerHandler.getEmployerEventStream(pleadOnline.getEmployer(), defendantId, updatedByOnlinePlea, createdOn, state)
                    .forEach(streamBuilder::add);
        }
        caseLanguageHandler.updateHearingRequirements(true, createdOn, pleadOnline.getDefendantId(), pleadOnline.getInterpreterLanguage(), pleadOnline.getSpeakWelsh(), state)
                .forEach(streamBuilder::add);
        streamBuilder.add(new OnlinePleaReceived(state.getUrn(), state.getCaseId(), defendantId,
                pleadOnline.getUnavailability(), pleadOnline.getInterpreterLanguage(), pleadOnline.getSpeakWelsh(),
                pleadOnline.getWitnessDetails(), pleadOnline.getWitnessDispute(), personalDetails,
                pleadOnline.getFinancialMeans(), pleadOnline.getEmployer(), pleadOnline.getOutgoings()
        ));
    }

    private static boolean anyUpdatesOnFinancialMeans(final FinancialMeans financialMeans, final List<Outgoing> outgoings) {
        return financialMeans != null || !isEmpty(outgoings);
    }
}
