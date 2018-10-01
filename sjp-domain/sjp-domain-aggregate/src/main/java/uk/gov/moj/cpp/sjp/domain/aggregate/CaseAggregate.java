package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.time.ZoneOffset.UTC;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.sjp.domain.plea.EmploymentStatus.EMPLOYED;
import static uk.gov.moj.cpp.sjp.event.CaseUpdateRejected.RejectReason.PLEA_ALREADY_SUBMITTED;
import static uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated.DefendantDetailsUpdatedBuilder.defendantDetailsUpdated;
import static uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected.RejectReason.CASE_ASSIGNED_TO_OTHER_USER;
import static uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected.RejectReason.CASE_COMPLETED;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.sjp.CourtReferralNotFound;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;
import uk.gov.moj.cpp.sjp.domain.CaseDocument;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.CaseReopenDetails;
import uk.gov.moj.cpp.sjp.domain.Defendant;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.Interpreter;
import uk.gov.moj.cpp.sjp.domain.Person;
import uk.gov.moj.cpp.sjp.domain.PersonalName;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.domain.aggregate.domain.DocumentCountByDocumentType;
import uk.gov.moj.cpp.sjp.domain.aggregate.domain.PleadOnlineOutcomes;
import uk.gov.moj.cpp.sjp.domain.command.CancelPlea;
import uk.gov.moj.cpp.sjp.domain.command.ChangePlea;
import uk.gov.moj.cpp.sjp.domain.command.UpdatePlea;
import uk.gov.moj.cpp.sjp.domain.onlineplea.Offence;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PersonalDetails;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PleadOnline;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalDenied;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequestCancelled;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequested;
import uk.gov.moj.cpp.sjp.event.CaseAlreadyCompleted;
import uk.gov.moj.cpp.sjp.event.CaseAlreadyReopened;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.CaseCreationFailedBecauseCaseAlreadyExisted;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAdded;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAlreadyAdded;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAlreadyExists;
import uk.gov.moj.cpp.sjp.event.CaseDocumentUploaded;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseNotFound;
import uk.gov.moj.cpp.sjp.event.CaseNotReopened;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.CaseReopened;
import uk.gov.moj.cpp.sjp.event.CaseReopenedUndone;
import uk.gov.moj.cpp.sjp.event.CaseReopenedUpdated;
import uk.gov.moj.cpp.sjp.event.CaseStarted;
import uk.gov.moj.cpp.sjp.event.CaseUnmarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected.RejectReason;
import uk.gov.moj.cpp.sjp.event.CourtReferralActioned;
import uk.gov.moj.cpp.sjp.event.CourtReferralCreated;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidAdded;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantAddressUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDateOfBirthUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdateFailed;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdatesAcknowledged;
import uk.gov.moj.cpp.sjp.event.DefendantNotEmployed;
import uk.gov.moj.cpp.sjp.event.DefendantNotFound;
import uk.gov.moj.cpp.sjp.event.DefendantPersonalNameUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantsNationalInsuranceNumberUpdated;
import uk.gov.moj.cpp.sjp.event.EmployerDeleted;
import uk.gov.moj.cpp.sjp.event.EmployerUpdated;
import uk.gov.moj.cpp.sjp.event.EmploymentStatusUpdated;
import uk.gov.moj.cpp.sjp.event.EnterpriseIdAssociated;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.OffenceNotFound;
import uk.gov.moj.cpp.sjp.event.OnlinePleaReceived;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.PleaUpdateDenied;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.cpp.sjp.event.SjpCaseCreated;
import uk.gov.moj.cpp.sjp.event.TrialRequestCancelled;
import uk.gov.moj.cpp.sjp.event.TrialRequested;
import uk.gov.moj.cpp.sjp.event.decommissioned.CaseAssignmentDeleted;
import uk.gov.moj.cpp.sjp.event.session.CaseAlreadyAssigned;
import uk.gov.moj.cpp.sjp.event.session.CaseAssigned;
import uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassignmentRejected;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("WeakerAccess")
public class CaseAggregate implements Aggregate {

    private static final long serialVersionUID = 6L;

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseAggregate.class);

    private UUID caseId;
    private String urn;
    private boolean caseReopened;
    private LocalDate caseReopenedDate;
    private boolean caseCompleted;
    private boolean withdrawalAllOffencesRequested;
    private boolean hasCourtReferral;
    private UUID assigneeId;
    private String defendantTitle;
    private String defendantFirstName;
    private String defendantLastName;
    private LocalDate defendantDateOfBirth;
    private Address defendantAddress;
    private CaseReadinessReason readinessReason;
    private boolean caseReceived;

    private String datesToAvoid;

    private final Map<UUID, Set<UUID>> offenceIdsByDefendantId = new HashMap<>();

    private final Map<UUID, CaseDocument> caseDocuments = new HashMap<>();
    private final List<UUID> offenceIdsWithPleas = new ArrayList<>();

    private final Map<UUID, String> defendantInterpreterLanguages = new HashMap<>();
    private final Map<UUID, Boolean> defendantSpeakWelsh = new HashMap<>();

    private boolean trialRequested;
    private boolean trialRequestedPreviously;
    private String trialRequestedUnavailability;
    private String trialRequestedUWitnessDetails;
    private String trialRequestedWitnessDispute;

    private final DocumentCountByDocumentType documentCountByDocumentType = new DocumentCountByDocumentType();

    private ProsecutingAuthority prosecutingAuthority;

    private final Map<UUID, String> employmentStatusByDefendantId = new HashMap<>();

    /**
     * @param userId could be null if the user is not a legal adviser (only they can be assigned cases)
     * @param action that is being performed
     * @param defendantId of the case
     * @return an optional stream containing the reject event
     */
    private Optional<Stream<Object>> checkRejectReasons(final UUID userId, final String action, final UUID defendantId) {
        Object event = null;
        if (this.caseId == null) {
            LOGGER.warn("Case not found: {}", action);
            event = new CaseNotFound(null, action);
        } else if (defendantId != null && !hasDefendant(defendantId)) {
            LOGGER.warn("Defendant not found: {}", action);
            event = new DefendantNotFound(defendantId, action);
        } else if (assigneeId != null && !assigneeId.equals(userId)) {
            LOGGER.warn("Update rejected because case is assigned to another user: {}", action);
            event = new CaseUpdateRejected(this.caseId, RejectReason.CASE_ASSIGNED);
        } else if (isCaseCompleted()) {
            LOGGER.warn("Update rejected because case is already completed: {}", action);
            event = new CaseUpdateRejected(this.caseId, RejectReason.CASE_COMPLETED);
        }
        return Optional.ofNullable(event).map(Stream::of);
    }

    private Stream<Object> applyEventStreamIfNotRejected(final String action,
                                                         final UUID userId,
                                                         final UUID defendantId,
                                                         final Supplier<Stream<Object>> eventStream) {
        return apply(checkRejectReasons(userId, action, defendantId).orElseGet(eventStream));
    }

    public Stream<Object> receiveCase(final Case aCase, final ZonedDateTime createdOn) {
        final Object event;
        if (caseReceived) {
            event = new CaseCreationFailedBecauseCaseAlreadyExisted(this.caseId, this.urn);
        } else {
            final Defendant defendant = new Defendant.DefendantBuilder()
                    .withId(UUID.randomUUID())
                    .buildBasedFrom(aCase.getDefendant());

            event = new CaseReceived(
                    aCase.getId(),
                    aCase.getUrn(),
                    aCase.getEnterpriseId(),
                    aCase.getProsecutingAuthority(),
                    aCase.getCosts(),
                    aCase.getPostingDate(),
                    defendant,
                    createdOn);
        }

        return apply(Stream.of(event));
    }

    public Stream<Object> updateFinancialMeans(final UUID userId, final FinancialMeans financialMeans) {
        return applyEventStreamIfNotRejected("Update financial means", userId, financialMeans.getDefendantId(),
                () -> Stream.of(FinancialMeansUpdated.createEvent(financialMeans.getDefendantId(),
                        financialMeans.getIncome(), financialMeans.getBenefits(), financialMeans.getEmploymentStatus())));
    }

    public Stream<Object> updateEmployer(final UUID userId, final Employer employer) {
        return applyEventStreamIfNotRejected("Update employer", userId, employer.getDefendantId(),
                () -> getEmployerEventStream(employer, employer.getDefendantId()));
    }

    private Stream<Object> getEmployerEventStream(final Employer employer, final UUID defendantId) {
        return getEmployerEventStream(employer, defendantId, false, null);
    }

    private Stream<Object> getEmployerEventStream(final Employer employer,
                                                  final UUID defendantId,
                                                  final boolean updatedByOnlinePlea,
                                                  final ZonedDateTime createdOn) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (updatedByOnlinePlea) {
            streamBuilder.add(EmployerUpdated.createEventForOnlinePlea(defendantId, employer, createdOn));
        } else {
            streamBuilder.add(EmployerUpdated.createEvent(defendantId, employer));
        }

        final String actualEmploymentStatus = employmentStatusByDefendantId.get(defendantId);

        if (!EMPLOYED.name().equals(actualEmploymentStatus)) {
            streamBuilder.add(new EmploymentStatusUpdated(defendantId, EMPLOYED.name()));
        }

        return streamBuilder.build();
    }

    public Stream<Object> deleteEmployer(final UUID userId, final UUID defendantId) {
        return applyEventStreamIfNotRejected("Delete employer", userId, null, () -> Stream.of(
                employmentStatusByDefendantId.containsKey(defendantId) ?
                        new EmployerDeleted(defendantId) :
                        new DefendantNotEmployed(defendantId)
        ));
    }

    public int getNumberOfDocumentOfGivenType(final String documentType) {
        return documentCountByDocumentType.getCount(documentType);
    }

    public Stream<Object> addCaseDocument(final UUID caseId, final CaseDocument caseDocument) {
        if (caseDocuments.containsKey(caseDocument.getId())) {
            LOGGER.warn("Case Document already exists with ID {}", caseDocument.getId());
            return apply(Stream.of(new CaseDocumentAlreadyExists(caseDocument.getId(), "Add Case Document")));
        }

        final Stream.Builder<Object> streamBuilder = Stream.builder();

        final Integer documentCount = documentCountByDocumentType.getCount(caseDocument.getDocumentType());

        final CaseDocumentAdded caseDocumentAdded = new CaseDocumentAdded(caseId, caseDocument, documentCount + 1);
        streamBuilder.add(caseDocumentAdded);

        return apply(streamBuilder.build());
    }

    public Stream<Object> uploadCaseDocument(final UUID caseId, final UUID documentReference, final String documentType) {
        return apply(Stream.of(new CaseDocumentUploaded(caseId, documentReference, documentType)));
    }

    public Stream<Object> completeCase() {
        //TODO: check case created
        if (caseCompleted) {
            LOGGER.warn("Case has already been completed {}", caseId);
            return apply(Stream.of(new CaseAlreadyCompleted(caseId, "Complete Case")));
        }

        return apply(Stream.of(new CaseUnassigned(caseId), new CaseCompleted(caseId)));
    }

    private Optional<UUID> getDefendantIdByOffenceId(final UUID offenceId) {
        return offenceIdsByDefendantId.entrySet().stream().filter(
                entry -> entry.getValue().contains(offenceId)
        ).map(Map.Entry::getKey).findFirst();
    }

    private boolean offenceExists(final UUID offenceId) {
        return offenceIdsByDefendantId.values().stream()
                .flatMap(Set::stream)
                .anyMatch(offenceId::equals);
    }

    private Stream<Object> changePlea(final UUID userId, final ChangePlea changePleaCommand, final ZonedDateTime updatedOn) {

        final Optional<UUID> defendantId = getDefendantIdByOffenceId(changePleaCommand.getOffenceId());
        if (!defendantId.isPresent()) {
            final UUID offenceId = changePleaCommand.getOffenceId();
            LOGGER.warn("Cannot change plea for offence which doesn't exist, ID: {}", offenceId);
            return apply(Stream.of(new OffenceNotFound(offenceId, "Update Plea")));
        }

        return applyEventStreamIfNotRejected("Change plea", userId, null, () -> {
            final Stream.Builder<Object> streamBuilder = Stream.builder();
            if (changePleaCommand instanceof UpdatePlea) {
                final UpdatePlea updatePlea = (UpdatePlea) changePleaCommand;
                streamBuilder.add(new PleaUpdated(updatePlea.getCaseId(), updatePlea.getOffenceId(), updatePlea.getPlea(), PleaMethod.POSTAL));

                handleTrialRequestEventsForUpdatePlea(updatePlea, streamBuilder, updatedOn);
                updateHearingRequirementsForPostalPlea(defendantId.get(), updatePlea.getInterpreterLanguage(), updatePlea.getSpeakWelsh())
                        .forEach(streamBuilder::add);
            } else if (changePleaCommand instanceof CancelPlea) {
                final CancelPlea cancelPlea = (CancelPlea) changePleaCommand;
                streamBuilder.add(new PleaCancelled(cancelPlea.getCaseId(), cancelPlea.getOffenceId()));
                if (trialRequested) {
                    streamBuilder.add(new TrialRequestCancelled(caseId));
                }
                updateHearingRequirementsForPostalPlea(defendantId.get(), null, null)
                        .forEach(streamBuilder::add);
            }
            return apply(streamBuilder.build());
        });
    }

    private void handleTrialRequestEventsForUpdatePlea(final UpdatePlea updatePlea,
                                                       final Stream.Builder<Object> streamBuilder,
                                                       final ZonedDateTime updatedOn) {
        if (hasNeverRaisedTrialRequestedEventAndTrialRequired(updatePlea)) {
            streamBuilder.add(new TrialRequested(caseId, updatedOn));
        } else if (isTrialRequestCancellationRequired(updatePlea)) {
            streamBuilder.add(new TrialRequestCancelled(caseId));
        } else if (wasTrialRequestedThenCancelledAndIsTrialRequiredAgain(updatePlea)) {
            streamBuilder.add(new TrialRequested(caseId, trialRequestedUnavailability, trialRequestedUWitnessDetails, trialRequestedWitnessDispute, updatedOn));
        }
    }

    private boolean hasNeverRaisedTrialRequestedEventAndTrialRequired(final UpdatePlea updatePlea) {
        return !trialRequested && !trialRequestedPreviously && trialRequired(updatePlea);
    }

    private boolean wasTrialRequestedThenCancelledAndIsTrialRequiredAgain(final UpdatePlea updatePlea) {
        return !trialRequested && this.trialRequestedPreviously && trialRequired(updatePlea);
    }

    private boolean isTrialRequestCancellationRequired(final UpdatePlea updatePlea) {
        return trialRequested && !trialRequired(updatePlea);
    }

    private static boolean trialRequired(final UpdatePlea updatePlea) {
        return PleaType.NOT_GUILTY.equals(updatePlea.getPlea());
    }

    public Stream<Object> updateHearingRequirements(final UUID userId,
                                                    final UUID defendantId,
                                                    final String interpreterLanguage,
                                                    final Boolean speakWelsh) {
        return applyEventStreamIfNotRejected("Update hearing requirements", userId, defendantId,
                () -> updateHearingRequirementsForPostalPlea(defendantId, interpreterLanguage, speakWelsh));
    }

    private Stream<Object> updateHearingRequirementsForPostalPlea(final UUID defendantId, final String interpreterLanguage, final Boolean speakWelsh) {
        return updateHearingRequirements(false, null, defendantId, interpreterLanguage, speakWelsh);
    }

    private Stream<Object> updateHearingRequirementsForOnlinePlea(final ZonedDateTime createdOn, final UUID defendantId, final String interpreterLanguage, final Boolean speakWelsh) {
        return updateHearingRequirements(true, createdOn, defendantId, interpreterLanguage, speakWelsh);
    }

    private Stream<Object> updateHearingRequirements(final boolean updatedByOnlinePlea, final ZonedDateTime createdOn, final UUID defendantId, final String interpreterLanguage, final Boolean speakWelsh) {
        assert !updatedByOnlinePlea || createdOn != null; // createdOn must be specified when is updated-by-OnlinePlea

        return Stream.of(
                updateInterpreterLanguage(interpreterLanguage, defendantId, updatedByOnlinePlea, createdOn),
                updateSpeakWelsh(speakWelsh, defendantId, updatedByOnlinePlea, createdOn))
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    public Stream<Object> addDatesToAvoid(final String datesToAvoid) {
        return applyEventStreamIfNotRejected("Add dates to avoid", null, null,
                () -> Stream.of(this.datesToAvoid == null ?
                        new DatesToAvoidAdded(this.caseId, datesToAvoid) :
                        new DatesToAvoidUpdated(this.caseId, datesToAvoid)));
    }

    public Stream<Object> updateDefendantNationalInsuranceNumber(final UUID userId, final UUID defendantId, final String newNationalInsuranceNumber) {
        return applyEventStreamIfNotRejected("Update national insurance number", userId, defendantId,
                () -> Stream.of(new DefendantsNationalInsuranceNumberUpdated(caseId, defendantId, newNationalInsuranceNumber)));

    }

    private Optional<Object> updateInterpreterLanguage(final String newInterpreterLanguage, final UUID defendantId,
                                                       final boolean updatedByOnlinePlea, final ZonedDateTime createdOn) {
        final Object event;
        final Interpreter interpreter = Interpreter.of(newInterpreterLanguage);

        if (!Objects.equals(this.getDefendantInterpreterLanguage(defendantId), interpreter.getLanguage())) {
            if (interpreter.isNeeded()) {
                if (updatedByOnlinePlea) {
                    event = InterpreterUpdatedForDefendant.createEventForOnlinePlea(caseId, defendantId, newInterpreterLanguage, createdOn);
                } else {
                    event = InterpreterUpdatedForDefendant.createEvent(caseId, defendantId, newInterpreterLanguage);
                }
            } else {
                event = new InterpreterCancelledForDefendant(caseId, defendantId);
            }
        } else {
            event = null;
        }

        return Optional.ofNullable(event);
    }

    private Optional<Object> updateSpeakWelsh(final Boolean newSpeakWelsh, final UUID defendantId,
                                              final boolean updatedByOnlinePlea, final ZonedDateTime createdOn) {
        final Object event;

        if ( ! Objects.equals(this.getDefendantSpeakWelsh(defendantId), newSpeakWelsh)) {
            if (newSpeakWelsh != null) {
                if (updatedByOnlinePlea) {
                    event = HearingLanguagePreferenceUpdatedForDefendant.createEventForOnlinePlea(caseId, defendantId, newSpeakWelsh, createdOn);
                } else {
                    event = HearingLanguagePreferenceUpdatedForDefendant.createEvent(caseId, defendantId, newSpeakWelsh);
                }
            } else {
                event = new HearingLanguagePreferenceCancelledForDefendant(caseId, defendantId);
            }
        } else {
            event = null;
        }

        return Optional.ofNullable(event);
    }

    public Stream<Object> updatePlea(final UUID userId, final UpdatePlea updatePleaCommand, final ZonedDateTime updatedOn) {
        return changePlea(userId, updatePleaCommand, updatedOn);
    }

    public Stream<Object> cancelPlea(final UUID userId, final CancelPlea cancelPleaCommand, final ZonedDateTime cancelledOn) {
        return changePlea(userId, cancelPleaCommand, cancelledOn);
    }

    public Stream<Object> pleadOnline(final UUID caseId, final PleadOnline pleadOnline, final ZonedDateTime createdOn) {
        return applyEventStreamIfNotRejected("Plead online", null, pleadOnline.getDefendantId(), () -> {
            final Stream.Builder<Object> streamBuilder = Stream.builder();

            final PleadOnlineOutcomes pleadOnlineOutcomes = addPleaEventsToStreamForStoreOnlinePlea(caseId, pleadOnline, streamBuilder, createdOn);
            if (pleadOnlineOutcomes.isPleaForOffencePreviouslySubmitted()) {
                return Stream.of(new CaseUpdateRejected(this.caseId, PLEA_ALREADY_SUBMITTED));
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
            addAdditionalEventsToStreamForStoreOnlinePlea(streamBuilder, pleadOnline, createdOn);

            return streamBuilder.build();
        });
    }

    private PleadOnlineOutcomes addPleaEventsToStreamForStoreOnlinePlea(final UUID caseId,
                                                                        final PleadOnline pleadOnline,
                                                                        final Stream.Builder<Object> streamBuilder,
                                                                        final ZonedDateTime createdOn) {
        final PleadOnlineOutcomes pleadOnlineOutcomes = new PleadOnlineOutcomes();
        pleadOnline.getOffences().forEach(offence -> {
            if (canPleaOnOffence(offence, pleadOnlineOutcomes)) {
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

    private boolean canPleaOnOffence(Offence offence, final PleadOnlineOutcomes pleadOnlineOutcomes) {
        if (!offenceExists(offence.getId())) {
            LOGGER.warn("Cannot update plea for offence which doesn't exist, ID: {}", offence.getId());
            pleadOnlineOutcomes.getOffenceNotFoundIds().add(offence.getId());
            return false;
        } else if (this.offenceIdsWithPleas.contains(offence.getId())) {
            pleadOnlineOutcomes.setPleaForOffencePreviouslySubmitted(true);
            return false;
        }
        return true;
    }

    private void addAdditionalEventsToStreamForStoreOnlinePlea(final Stream.Builder<Object> streamBuilder,
                                                               final PleadOnline pleadOnline,
                                                               final ZonedDateTime createdOn) {
        //TODO: we need to query the defendant to see if any of the incoming defendant data is different from the pre-existing defendant data. If no changes, no event
        final PersonalDetails personalDetails = pleadOnline.getPersonalDetails();
        final UUID defendantId = pleadOnline.getDefendantId();
        final boolean updatedByOnlinePlea = true;

        streamBuilder.add(defendantDetailsUpdated()
                .withCaseId(caseId)
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

        getDefendantWarningEvents(personalDetails, createdOn, updatedByOnlinePlea)
                .forEach(streamBuilder::add);

        streamBuilder.add(FinancialMeansUpdated.createEventForOnlinePlea(defendantId, pleadOnline.getFinancialMeans().getIncome(),
                pleadOnline.getFinancialMeans().getBenefits(), pleadOnline.getFinancialMeans().getEmploymentStatus(),
                pleadOnline.getOutgoings(), createdOn));

        if (pleadOnline.getEmployer() != null) {
            getEmployerEventStream(pleadOnline.getEmployer(), defendantId, updatedByOnlinePlea, createdOn)
                    .forEach(streamBuilder::add);
        }
        updateHearingRequirementsForOnlinePlea(createdOn, pleadOnline.getDefendantId(), pleadOnline.getInterpreterLanguage(), pleadOnline.getSpeakWelsh())
                .forEach(streamBuilder::add);
        streamBuilder.add(new OnlinePleaReceived(urn, this.caseId, defendantId,
                pleadOnline.getUnavailability(), pleadOnline.getInterpreterLanguage(), pleadOnline.getSpeakWelsh(),
                pleadOnline.getWitnessDetails(), pleadOnline.getWitnessDispute(), personalDetails,
                pleadOnline.getFinancialMeans(), pleadOnline.getEmployer(), pleadOnline.getOutgoings()
        ));
    }

    public Stream<Object> createCourtReferral(final UUID caseId, final LocalDate hearingDate) {
        return apply(Stream.of(checkCaseNotFound(caseId, "Create court referral")
                .orElse(new CourtReferralCreated(this.caseId, hearingDate))));
    }

    public Stream<Object> actionCourtReferral(final UUID caseId) {

        if (this.hasCourtReferral) {
            return apply(Stream.of(new CourtReferralActioned(this.caseId, ZonedDateTime.now(UTC))));
        } else {
            LOGGER.warn("Cannot action court referral that does not exist");
            return apply(Stream.of(new CourtReferralNotFound(caseId)));
        }
    }

    public Stream<Object> requestWithdrawalAllOffences() {
        return applyEventStreamIfNotRejected("Request withdrawal all offences", null, null,
                () -> Stream.of(new AllOffencesWithdrawalRequested(this.caseId)));
    }

    public Stream<Object> cancelRequestWithdrawalAllOffences() {
        return applyEventStreamIfNotRejected("Cancel request withdrawal all offences", null, null,
                () -> {
                    if (withdrawalAllOffencesRequested) {
                        return Stream.of(new AllOffencesWithdrawalRequestCancelled(this.caseId));
                    } else {
                        LOGGER.warn("Cannot Cancel request withdrawal all offences for Case with ID {}", caseId);
                        return Stream.empty();
                    }
                });
    }

    public Stream<Object> markCaseReopened(final CaseReopenDetails caseReopenDetails) {
        return apply(Stream.of(checkCaseNotFound(caseReopenDetails.getCaseId(), "Mark case reopened")
                .orElseGet(() -> {
                    if (caseReopened) {
                        LOGGER.warn("Cannot reopen case. Case already reopened with ID {}", caseId);
                        return new CaseAlreadyReopened(caseReopenDetails.getCaseId(), "Cannot mark case reopened");
                    } else {
                        return new CaseReopened(caseReopenDetails);
                    }
                })));
    }

    public Stream<Object> updateCaseReopened(final CaseReopenDetails caseReopenDetails) {
        return apply(Stream.of(checkCaseNotFound(caseReopenDetails.getCaseId(), "Update case reopened")
                .orElseGet(() -> {
                    if (caseReopened) {
                        return new CaseReopenedUpdated(caseReopenDetails);
                    } else {
                        LOGGER.warn("Cannot update reopened case. Case not yet reopened with ID {}", caseId);
                        return new CaseNotReopened(caseReopenDetails.getCaseId(), "Cannot update case reopened");
                    }
                })));
    }

    public Stream<Object> acknowledgeDefendantDetailsUpdates(
            final UUID defendantId,
            final ZonedDateTime acknowledgedAt) {

        Object event;
        if (this.caseId == null) {
            LOGGER.warn("Case not found: {}", caseId);
            event = new CaseNotFound(null, "Acknowledge defendant details updates");
        } else if (defendantId != null && !hasDefendant(defendantId)) {
            LOGGER.warn("Defendant not found: {}", defendantId);
            event = new DefendantNotFound(defendantId, "Acknowledge defendant details updates");
        } else {
            event = new DefendantDetailsUpdatesAcknowledged(caseId, defendantId, acknowledgedAt);
        }

        return Stream.of(event);
    }

    public Stream<Object> undoCaseReopened(final UUID caseId) {
        return apply(Stream.of(checkCaseNotFound(caseId, "Undo case reopened")
                .orElseGet(() -> {
                    if (!caseReopened || caseReopenedDate == null) {
                        LOGGER.warn("Cannot undo reopened case. Case not yet reopened with ID: {}", caseId);
                        return new CaseNotReopened(caseId, "Cannot undo case reopened");
                    } else {
                        return new CaseReopenedUndone(caseId, caseReopenedDate);
                    }
                })));
    }

    public Stream<Object> assignCase(final UUID assigneeId, final ZonedDateTime assignedAt, final CaseAssignmentType assignmentType) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();

        if (caseCompleted) {
            streamBuilder.add(new CaseAssignmentRejected(CASE_COMPLETED));
        } else if (this.assigneeId != null) {
            if (!this.assigneeId.equals(assigneeId)) {
                streamBuilder.add(new CaseAssignmentRejected(CASE_ASSIGNED_TO_OTHER_USER));
            } else {
                streamBuilder.add(new CaseAlreadyAssigned(caseId, assigneeId));
            }
        } else {
            streamBuilder.add(new CaseAssigned(caseId, assigneeId, assignedAt, assignmentType));
        }

        return apply(streamBuilder.build());
    }

    public Stream<Object> unassignCase() {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (assigneeId != null) {
            streamBuilder.add(new CaseUnassigned(caseId));
        } else {
            streamBuilder.add(new CaseUnassignmentRejected(CaseUnassignmentRejected.RejectReason.CASE_NOT_ASSIGNED));
        }
        return apply(streamBuilder.build());
    }

    private void validateDefendantAddress(final Address address) {
        if (this.defendantAddress != null) {
            ensureFieldIsNotBlankIfWasDefined(this.defendantAddress.getAddress1(), address.getAddress1(),
                    "street (address1) can not be blank as previous value is: " + this.defendantAddress.getAddress1());

            ensureFieldIsNotBlankIfWasDefined(this.defendantAddress.getAddress4(), address.getAddress4(),
                    "town (address4) can not be blank as previous value is: " + this.defendantAddress.getAddress4());

            ensureFieldIsNotBlankIfWasDefined(this.defendantAddress.getPostcode(), address.getPostcode(),
                    "postcode can not be blank as previous value is: " + this.defendantAddress.getPostcode());
        }
    }

    private void ensureFieldIsNotBlankIfWasDefined(String oldValue, String newValue, String errorMessage) {
        if (StringUtils.isNotBlank(oldValue) && StringUtils.isBlank(newValue)) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private void validateDefendantTitle(final String title) {
        if (StringUtils.isBlank(title) && StringUtils.isNotBlank(this.defendantTitle)) {
            throw new IllegalArgumentException(String.format("title parameter can not be null as previous value is : %s", this.defendantTitle));
        }
    }

    public Stream<Object> updateDefendantDetails(final UUID caseId,
                                                 final UUID defendantId,
                                                 final Person person,
                                                 final ZonedDateTime updatedDate) {
        //TODO check reject reasons

        final Stream.Builder<Object> events = Stream.builder();
        final boolean updatedByOnlinePlea = false;

        try {
            validateDefendantTitle(person.getTitle());
            validateDefendantAddress(person.getAddress());
        } catch (IllegalArgumentException | IllegalStateException e) {
            LOGGER.error("Defendant details update failed for ID: {} with message {} ", defendantId, e);
            return apply(Stream.of(new DefendantDetailsUpdateFailed(caseId, defendantId, e.getMessage())));
        }

        getDefendantWarningEvents(person, updatedDate, updatedByOnlinePlea).forEach(events::add);

        final DefendantDetailsUpdated defendantDetailsUpdated = defendantDetailsUpdated()
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .withTitle(person.getTitle())
                .withFirstName(person.getFirstName())
                .withLastName(person.getLastName())
                .withDateOfBirth(person.getDateOfBirth())
                .withGender(person.getGender())
                .withNationalInsuranceNumber(person.getNationalInsuranceNumber())
                .withContactDetails(person.getContactDetails())
                .withAddress(person.getAddress())
                .withUpdateByOnlinePlea(updatedByOnlinePlea)
                .withUpdatedDate(updatedDate)
                .build();
        events.add(defendantDetailsUpdated);
        return apply(events.build());
    }

    private Stream<Object> getDefendantWarningEvents(final Person person,
                                                     final ZonedDateTime updatedDate,
                                                     final boolean isOnlinePlea) {

        final Stream.Builder<Object> events = Stream.builder();

        if (defendantDateOfBirth != null && !defendantDateOfBirth.equals(person.getDateOfBirth())) {
            events.add(new DefendantDateOfBirthUpdated(
                    caseId,
                    defendantDateOfBirth,
                    person.getDateOfBirth(),
                    updatedDate));
        }

        if (defendantAddress != null && !defendantAddress.equals(person.getAddress())) {
            events.add(new DefendantAddressUpdated(
                    caseId,
                    defendantAddress,
                    person.getAddress(),
                    updatedDate));
        }

        // Online plea doesn't update title
        if (isTitleChanged(isOnlinePlea, person.getTitle()) ||
                !StringUtils.equalsIgnoreCase(defendantFirstName, person.getFirstName()) ||
                !StringUtils.equalsIgnoreCase(defendantLastName, person.getLastName())) {

            events.add(new DefendantPersonalNameUpdated(
                    caseId,
                    new PersonalName(defendantTitle, defendantFirstName, defendantLastName),
                    new PersonalName(person.getTitle(), person.getFirstName(), person.getLastName()),
                    updatedDate));
        }

        return events.build();
    }

    private boolean isTitleChanged(final boolean isOnlinePlea, final String title) {
        return !isOnlinePlea && defendantTitle != null && !defendantTitle.equalsIgnoreCase(title);
    }

    public Stream<Object> markCaseReadyForDecision(final CaseReadinessReason readinessReason, final ZonedDateTime markedAt) {
        final Stream.Builder<Object> events = Stream.builder();

        if (this.readinessReason != readinessReason) {
            events.add(new CaseMarkedReadyForDecision(caseId, readinessReason, markedAt));
        }

        return events.build();
    }

    public Stream<Object> unmarkCaseReadyForDecision() {
        final Stream.Builder<Object> events = Stream.builder();

        if (readinessReason != null) {
            events.add(new CaseUnmarkedReadyForDecision(caseId));
        }

        return events.build();
    }

    private Optional<Object> checkCaseNotFound(final UUID caseId, final String action) {
        if (!Objects.equals(this.caseId, caseId)) {
            LOGGER.error("Mismatch of IDs in aggregate: {} != {}", this.caseId, caseId);
            return Optional.of(new CaseNotFound(caseId, action));
        } else {
            return Optional.empty();
        }
    }

    @Override
    @SuppressWarnings({"deprecation", "squid:S1602"})
    public Object apply(Object event) {
        return match(event).with(
                when(CaseCreationFailedBecauseCaseAlreadyExisted.class).apply(e -> {
                    //nothing to update
                }),
                when(CaseStarted.class)
                        .apply(e -> caseId = e.getId()),
                when(CaseReceived.class).apply(e -> {
                    caseId = e.getCaseId();
                    urn = e.getUrn();
                    prosecutingAuthority = e.getProsecutingAuthority();
                    offenceIdsByDefendantId.computeIfAbsent(e.getDefendant().getId(), id -> new HashSet<>())
                            .addAll(e.getDefendant().getOffences().stream()
                                    .map(uk.gov.moj.cpp.sjp.domain.Offence::getId)
                                    .collect(Collectors.toSet()));
                    defendantTitle = e.getDefendant().getTitle();
                    defendantFirstName = e.getDefendant().getFirstName();
                    defendantLastName = e.getDefendant().getLastName();
                    defendantDateOfBirth = e.getDefendant().getDateOfBirth();
                    defendantAddress = e.getDefendant().getAddress();

                    caseReceived = true;
                }),
                when(DatesToAvoidAdded.class).apply(e -> datesToAvoid = e.getDatesToAvoid()),
                when(DatesToAvoidUpdated.class).apply(e -> datesToAvoid = e.getDatesToAvoid()),
                when(CaseCompleted.class)
                        .apply(e -> this.caseCompleted = true),
                when(CaseDocumentAdded.class).apply(e -> {
                    caseDocuments.put(e.getCaseDocument().getId(), e.getCaseDocument());
                    documentCountByDocumentType.increaseCount(e.getCaseDocument().getDocumentType());
                }),
                when(CaseDocumentUploaded.class).apply(e -> {
                    //nothing to update
                }),
                when(PleaUpdated.class).apply(e -> {
                    this.offenceIdsWithPleas.add(e.getOffenceId());
                }),
                when(PleaCancelled.class).apply(e -> {
                    this.offenceIdsWithPleas.remove(e.getOffenceId());
                }),
                // Old event. Replaced by CaseUpdateRejected
                when(PleaUpdateDenied.class).apply(e -> {
                    //nothing to update
                }),
                when(TrialRequested.class).apply(e -> {
                    this.trialRequested = true;
                    this.trialRequestedPreviously = true;
                    this.trialRequestedUnavailability = e.getUnavailability();
                    this.trialRequestedUWitnessDetails = e.getWitnessDetails();
                    this.trialRequestedWitnessDispute = e.getWitnessDispute();
                }),
                when(TrialRequestCancelled.class).apply(e -> {
                    this.trialRequested = false;
                }),
                when(DefendantsNationalInsuranceNumberUpdated.class).apply(e -> {
                    //nothing to update
                }),
                when(InterpreterUpdatedForDefendant.class).apply(e -> {
                    this.defendantInterpreterLanguages.compute(
                            e.getDefendantId(),
                            (defendantId, previousValue) -> Optional.ofNullable(e.getInterpreter())
                                    .map(Interpreter::getLanguage)
                                    .orElse(null));
                }),
                when(HearingLanguagePreferenceUpdatedForDefendant.class).apply(e -> {
                    this.defendantSpeakWelsh.compute(
                            e.getDefendantId(),
                            (defendantId, previousValue) -> e.getSpeakWelsh());
                }),
                when(HearingLanguagePreferenceCancelledForDefendant.class).apply(e ->
                        this.defendantSpeakWelsh.remove(e.getDefendantId())),
                when(InterpreterCancelledForDefendant.class).apply(e ->
                        this.defendantInterpreterLanguages.remove(e.getDefendantId())),
                when(AllOffencesWithdrawalRequested.class).apply(e ->
                        this.withdrawalAllOffencesRequested = true
                ),
                // Old event. Replaced by CaseUpdateRejected
                when(AllOffencesWithdrawalDenied.class).apply(e -> {
                    //nothing to update
                }),
                when(CaseUpdateRejected.class).apply(e -> {
                    //nothing to update
                }),
                when(AllOffencesWithdrawalRequestCancelled.class).apply(e ->
                        // TODO check there is a withdrawal request
                        this.withdrawalAllOffencesRequested = false
                ),
                when(CaseNotFound.class).apply(e -> {
                    //nothing to update
                }),
                when(CaseAlreadyCompleted.class).apply(e -> {
                    //nothing to update
                }),
                when(CaseDocumentAlreadyExists.class).apply(e -> {
                    //nothing to update
                }),
                when(CaseDocumentAlreadyAdded.class).apply(e -> {
                    //nothing to update
                }),
                when(DefendantNotFound.class).apply(e -> {
                    //nothing to update
                }),
                when(OffenceNotFound.class).apply(e -> {
                    //nothing to update
                }),
                when(CaseReopened.class).apply(e -> {
                    caseReopened = true;
                    caseReopenedDate = e.getCaseReopenDetails().getReopenedDate();
                }),
                when(CaseReopenedUpdated.class).apply(e ->
                        caseReopenedDate = e.getCaseReopenDetails().getReopenedDate()),
                when(CaseReopenedUndone.class).apply(e -> {
                    caseReopened = false;
                    caseReopenedDate = null;
                }),
                when(CaseAlreadyReopened.class).apply(e -> {
                    //nothing to update
                }),
                when(CaseNotReopened.class).apply(e -> {
                    //nothing to update
                }),
                // Old event
                when(EnterpriseIdAssociated.class).apply(e -> {
                    //nothing to update
                }),
                when(FinancialMeansUpdated.class).apply(e ->
                        employmentStatusByDefendantId.put(e.getDefendantId(), e.getEmploymentStatus())
                ),
                when(EmploymentStatusUpdated.class).apply(e ->
                        employmentStatusByDefendantId.put(e.getDefendantId(), e.getEmploymentStatus())
                ),
                when(EmployerDeleted.class).apply(e ->
                        employmentStatusByDefendantId.remove(e.getDefendantId())
                ),
                when(DefendantNotEmployed.class).apply(e -> {
                    //nothing to update
                }),
                when(EmployerUpdated.class).apply(e -> {
                    //nothing to update
                }), when(CourtReferralCreated.class).apply(e ->
                        this.hasCourtReferral = true),
                when(CourtReferralActioned.class).apply(e -> {
                    //nothing to update
                }),
                when(CourtReferralNotFound.class).apply(e -> {
                    //nothing to update
                }),
                when(CaseAssigned.class).apply(e -> assigneeId = e.getAssigneeId()),
                when(CaseUnassigned.class).apply(e -> assigneeId = null),
                when(CaseAssignmentDeleted.class).apply(e -> assigneeId = null),
                when(DefendantDetailsUpdated.class).apply(e -> {
                    defendantTitle = e.getTitle();
                    defendantFirstName = e.getFirstName();
                    defendantLastName = e.getLastName();
                    defendantDateOfBirth = e.getDateOfBirth();
                    defendantAddress = e.getAddress();
                }),
                when(DefendantDetailsUpdateFailed.class).apply(e -> {
                    // no change in aggregate state
                }),
                when(DefendantDateOfBirthUpdated.class).apply(e -> {
                    // no change in aggregate state
                }),
                when(DefendantPersonalNameUpdated.class).apply(e -> {
                    // no change in aggregate state
                }),
                when(DefendantAddressUpdated.class).apply(e -> {
                    // no change in aggregate state
                }),
                when(CaseMarkedReadyForDecision.class).apply(e -> this.readinessReason = e.getReason()),
                when(CaseUnmarkedReadyForDecision.class).apply(e -> this.readinessReason = null),
                when(SjpCaseCreated.class).apply(e -> apply(Stream.of(convertSjpCaseCreatedToCaseReceived(e)))),
                otherwiseDoNothing()
        );
    }

    /**
     * Ensure backward compatibility for {@link SjpCaseCreated} events
     *
     * @param sjpCaseCreated deprecated case creation event
     * @return conversion to new event
     */
    @SuppressWarnings("deprecation")
    private static CaseReceived convertSjpCaseCreatedToCaseReceived(final SjpCaseCreated sjpCaseCreated) {
        final Defendant defendant = new Defendant(sjpCaseCreated.getDefendantId(), null, null, null, null, null, null, null, null, null,
                sjpCaseCreated.getNumPreviousConvictions(), sjpCaseCreated.getOffences(), null);

        return new CaseReceived(sjpCaseCreated.getId(), sjpCaseCreated.getUrn(), null,
                sjpCaseCreated.getProsecutingAuthority(), sjpCaseCreated.getCosts(), sjpCaseCreated.getPostingDate(),
                defendant, sjpCaseCreated.getCreatedOn());
    }

    UUID getCaseId() {
        return caseId;
    }

    String getUrn() {
        return urn;
    }

    boolean isCaseCompleted() {
        return caseCompleted;
    }

    boolean isWithdrawalAllOffencesRequested() {
        return withdrawalAllOffencesRequested;
    }

    Map<UUID, CaseDocument> getCaseDocuments() {
        return caseDocuments;
    }

    ProsecutingAuthority getProsecutingAuthority() {
        return prosecutingAuthority;
    }

    boolean hasDefendant(final UUID defendantId) {
        return offenceIdsByDefendantId.containsKey(defendantId);
    }

    boolean isCaseAssigned() {
        return assigneeId != null;
    }

    String getDefendantLastName() {
        return defendantLastName;
    }

    String getDefendantFirstName() {
        return defendantFirstName;
    }

    String getDefendantTitle() {
        return defendantTitle;
    }

    Address getDefendantAddress() {
        return defendantAddress;
    }

    LocalDate getDefendantDob() {
        return defendantDateOfBirth;
    }

    String getDefendantInterpreterLanguage(final UUID defendantID) {
        return defendantInterpreterLanguages.get(defendantID);
    }

    Boolean getDefendantSpeakWelsh(final UUID defendantID) {
        return defendantSpeakWelsh.get(defendantID);
    }

    // TODO fix the tests that require the following to be public
    public boolean isCaseReopened() {
        return caseReopened;
    }

    public boolean isCaseReceived() {
        return caseReceived;
    }

    public Map<UUID, Set<UUID>> getOffenceIdsByDefendantId() {
        return offenceIdsByDefendantId;
    }

}
