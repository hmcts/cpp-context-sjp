package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.time.ZoneOffset.UTC;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.sjp.domain.plea.EmploymentStatus.EMPLOYED;
import static uk.gov.moj.cpp.sjp.event.CaseUpdateRejected.RejectReason.CASE_ASSIGNED;
import static uk.gov.moj.cpp.sjp.event.CaseUpdateRejected.RejectReason.PLEA_ALREADY_SUBMITTED;
import static uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated.DefendantDetailsUpdatedBuilder.defendantDetailsUpdated;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.sjp.CourtReferralNotFound;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;
import uk.gov.moj.cpp.sjp.domain.CaseDocument;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.CaseReopenDetails;
import uk.gov.moj.cpp.sjp.domain.ContactDetails;
import uk.gov.moj.cpp.sjp.domain.Defendant;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.Interpreter;
import uk.gov.moj.cpp.sjp.domain.Person;
import uk.gov.moj.cpp.sjp.domain.PersonalName;
import uk.gov.moj.cpp.sjp.domain.PleaType;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.domain.aggregate.domain.DocumentCountByDocumentType;
import uk.gov.moj.cpp.sjp.domain.aggregate.domain.PleadOnlineOutcomes;
import uk.gov.moj.cpp.sjp.domain.command.CancelPlea;
import uk.gov.moj.cpp.sjp.domain.command.ChangePlea;
import uk.gov.moj.cpp.sjp.domain.command.CompleteCase;
import uk.gov.moj.cpp.sjp.domain.command.UpdatePlea;
import uk.gov.moj.cpp.sjp.domain.onlineplea.Offence;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PleadOnline;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalDenied;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequestCancelled;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequested;
import uk.gov.moj.cpp.sjp.event.CaseAlreadyCompleted;
import uk.gov.moj.cpp.sjp.event.CaseAlreadyReopened;
import uk.gov.moj.cpp.sjp.event.session.CaseAssigned;
import uk.gov.moj.cpp.sjp.event.CaseAssignmentDeleted;
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
import uk.gov.moj.cpp.sjp.event.CourtReferralActioned;
import uk.gov.moj.cpp.sjp.event.CourtReferralCreated;
import uk.gov.moj.cpp.sjp.event.DefendantAddressUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDateOfBirthUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsMovedFromPeople;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdateFailed;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantNotEmployed;
import uk.gov.moj.cpp.sjp.event.DefendantNotFound;
import uk.gov.moj.cpp.sjp.event.DefendantPersonalNameUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantsNationalInsuranceNumberUpdated;
import uk.gov.moj.cpp.sjp.event.EmployerDeleted;
import uk.gov.moj.cpp.sjp.event.EmployerUpdated;
import uk.gov.moj.cpp.sjp.event.EmploymentStatusUpdated;
import uk.gov.moj.cpp.sjp.event.EnterpriseIdAssociated;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;
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
import uk.gov.moj.cpp.sjp.event.decommissioned.CaseAssignmentCreated;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("WeakerAccess")
public class CaseAggregate implements Aggregate {

    private static final long serialVersionUID = 4L;

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseAggregate.class);

    private UUID caseId;
    private String urn;
    private boolean caseReopened = false;
    private LocalDate caseReopenedDate = null;
    private boolean caseCompleted = false;
    private boolean withdrawalAllOffencesRequested = false;
    private boolean hasCourtReferral;
    private boolean assigned = false;
    private String defendantTitle;
    private String defendantFirstName;
    private String defendantLastName;
    private LocalDate defendantDateOfBirth;
    private Address defendantAddress;
    private CaseReadinessReason readinessReason;

    private Map<UUID, Set<UUID>> offenceIdsByDefendantId = new HashMap<>();

    private Map<UUID, CaseDocument> caseDocuments = new HashMap<>();
    private List<String> offenceIdsWithPleas = new ArrayList<>();

    private Map<UUID, String> defendantInterpreterLanguages = new HashMap<>();

    private boolean trialRequested;
    private boolean trialRequestedPreviously;
    private String trialRequestedUnavailability;
    private String trialRequestedUWitnessDetails;
    private String trialRequestedWitnessDispute;

    private DocumentCountByDocumentType documentCountByDocumentType = new DocumentCountByDocumentType();

    private ProsecutingAuthority prosecutingAuthority;

    private final Map<UUID, String> employmentStatusByDefendantId = new HashMap<>();

    public Stream<Object> dummy() {
        return apply(Stream.of());
    }

    public Stream<Object> receiveCase(final Case aCase, final ZonedDateTime createdOn) {
        final Object event;
        if (this.caseId != null && this.urn != null) {
            event = new CaseCreationFailedBecauseCaseAlreadyExisted(this.caseId, this.urn);
        } else {
            Defendant caseDefendant = aCase.getDefendant();
            Defendant eventDefendant = new Defendant(
                    UUID.randomUUID(),
                    caseDefendant.getTitle(),
                    caseDefendant.getFirstName(),
                    caseDefendant.getLastName(),
                    caseDefendant.getDateOfBirth(),
                    caseDefendant.getGender(),
                    caseDefendant.getAddress(),
                    caseDefendant.getNumPreviousConvictions(),
                    caseDefendant.getOffences()
            );

            event = new CaseReceived(
                    aCase.getId(),
                    aCase.getUrn(),
                    aCase.getProsecutingAuthority(),
                    aCase.getCosts(),
                    aCase.getPostingDate(),
                    eventDefendant,
                    createdOn);
        }

        return apply(Stream.of(event));
    }

    public Stream<Object> updateFinancialMeans(final FinancialMeans financialMeans) {
        final UUID defendantId = financialMeans.getDefendantId();
        final Object event;

        if (hasDefendant(defendantId)) {
            event = FinancialMeansUpdated.createEvent(financialMeans.getDefendantId(), financialMeans.getIncome(),
                    financialMeans.getBenefits(), financialMeans.getEmploymentStatus());
        } else {
            event = new DefendantNotFound(defendantId.toString(), "Update financial means");
        }

        return apply(Stream.of(event));
    }

    public Stream<Object> updateEmployer(final Employer employer) {
        final UUID defendantId = employer.getDefendantId();
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        addEmployerEventToStream(streamBuilder, employer, defendantId, false);
        return apply(streamBuilder.build());
    }

    private void addEmployerEventToStream(final Stream.Builder<Object> streamBuilder, final Employer employer,
                                          final UUID defendantId, final boolean updatedByOnlinePlea) {
        addEmployerEventToStream(streamBuilder, employer, defendantId, updatedByOnlinePlea, null);
    }

    private void addEmployerEventToStream(final Stream.Builder streamBuilder, final Employer employer,
                                          final UUID defendantId, final boolean updatedByOnlinePlea,
                                          final ZonedDateTime createdOn) {
        if (updatedByOnlinePlea) {
            streamBuilder.add(EmployerUpdated.createEventForOnlinePlea(defendantId, employer, createdOn));
        } else {
            streamBuilder.add(EmployerUpdated.createEvent(defendantId, employer));
        }

        final String actualEmploymentStatus = employmentStatusByDefendantId.get(defendantId);

        if (!EMPLOYED.name().equals(actualEmploymentStatus)) {
            streamBuilder.add(new EmploymentStatusUpdated(defendantId, EMPLOYED.name()));
        }
    }

    public Stream<Object> deleteEmployer(final UUID defendantId) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();

        if (employmentStatusByDefendantId.containsKey(defendantId)) {
            streamBuilder.add(new EmployerDeleted(defendantId));
        } else {
            streamBuilder.add(new DefendantNotEmployed(defendantId));
        }

        return apply(streamBuilder.build());
    }


    public int getNumberOfDocumentOfGivenType(final String documentType) {
        return documentCountByDocumentType.getCount(documentType);
    }

    public Stream<Object> addCaseDocument(final UUID caseId, final CaseDocument caseDocument) {
        if (caseDocuments.containsKey(UUID.fromString(caseDocument.getId()))) {
            LOGGER.warn("Case Document already exists with ID {}", caseDocument.getId());
            return apply(Stream.of(new CaseDocumentAlreadyExists(caseDocument.getId(), "Add Case Document")));
        }

        final Stream.Builder<Object> streamBuilder = Stream.builder();

        final Integer documentCount = documentCountByDocumentType.getCount(caseDocument.getDocumentType());

        final CaseDocumentAdded caseDocumentAdded = new CaseDocumentAdded(caseId.toString(), caseDocument, documentCount + 1);
        streamBuilder.add(caseDocumentAdded);

        return apply(streamBuilder.build());
    }

    public Stream<Object> uploadCaseDocument(final UUID caseId, final UUID documentReference, final String documentType) {
        final Stream.Builder<Object> events = Stream.builder();

        final CaseDocumentUploaded caseDocumentUploaded = new CaseDocumentUploaded(caseId, documentReference, documentType);
        events.add(caseDocumentUploaded);

        return apply(events.build());
    }

    public Stream<Object> completeCase(final CompleteCase completeCase) {
        if (caseCompleted) {
            LOGGER.warn("CaseAggregate has already been completed {}", completeCase.getCaseId());
            return apply(Stream.of(new CaseAlreadyCompleted(completeCase.getCaseId().toString(), "Complete Case")));
        }

        return apply(Stream.of(new CaseCompleted(completeCase.getCaseId())));
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

    private Stream<Object> changePlea(final ChangePlea changePleaCommand, final ZonedDateTime updatedOn) {
        final Optional<UUID> defendantId = getDefendantIdByOffenceId(changePleaCommand.getOffenceId());
        if (!defendantId.isPresent()) {
            final String offenceId = changePleaCommand.getOffenceId().toString();
            LOGGER.warn("Cannot update plea for offence which doesn't exist, ID: {}", offenceId);
            return apply(Stream.of(new OffenceNotFound(offenceId, "Update Plea")));
        }

        if (isCaseAssigned()) {
            return caseUpdateRejected(changePleaCommand.getCaseId().toString(), CASE_ASSIGNED);
        }

        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (changePleaCommand instanceof UpdatePlea) {
            final UpdatePlea updatePlea = (UpdatePlea) changePleaCommand;
            final PleaUpdated pleaUpdated = new PleaUpdated(
                    updatePlea.getCaseId().toString(),
                    updatePlea.getOffenceId().toString(),
                    updatePlea.getPlea(),
                    PleaMethod.POSTAL);
            streamBuilder.add(pleaUpdated);

            handleTrialRequestEventsForUpdatePlea(updatePlea, streamBuilder, updatedOn);
            updateInterpreter(updatePlea.getInterpreterLanguage(), defendantId.get(), false)
                    .ifPresent(streamBuilder::add);

        } else if (changePleaCommand instanceof CancelPlea) {
            final CancelPlea cancelPlea = (CancelPlea) changePleaCommand;
            final PleaCancelled pleaCancelled = new PleaCancelled(
                    cancelPlea.getCaseId().toString(),
                    cancelPlea.getOffenceId().toString());
            streamBuilder.add(pleaCancelled);
            if (trialRequested) {
                streamBuilder.add(new TrialRequestCancelled(caseId));
            }
            updateInterpreter(null, defendantId.get(), false)
                    .ifPresent(streamBuilder::add);
        }
        return apply(streamBuilder.build());
    }

    private void handleTrialRequestEventsForUpdatePlea(final UpdatePlea updatePlea, final Stream.Builder<Object> streamBuilder, final ZonedDateTime updatedOn) {
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

    private boolean trialRequired(final UpdatePlea updatePlea) {
        return updatePlea.getPlea().equals(PleaType.NOT_GUILTY.name());
    }

    public Stream<Object> updateInterpreter(final UUID defendantId, final String language) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (hasDefendant(defendantId)) {
            updateInterpreter(language, defendantId, false).ifPresent(streamBuilder::add);
        } else {
            streamBuilder.add(new DefendantNotFound(defendantId.toString(), "Update interpreter"));
        }
        return apply(streamBuilder.build());

    }

    public Stream<Object> updateDefendantNationalInsuranceNumber(final UUID defendantId, final String newNationalInsuranceNumber) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (hasDefendant(defendantId)) {
            streamBuilder.add(new DefendantsNationalInsuranceNumberUpdated(caseId, defendantId, newNationalInsuranceNumber));
        } else {
            streamBuilder.add(new DefendantNotFound(defendantId.toString(), "Update defendant's national insurance number"));
        }
        return apply(streamBuilder.build());
    }

    private Optional<Object> updateInterpreter(final String newInterpreterLanguage, final UUID defendantId,
                                               boolean updatedByOnlinePlea) {
        return updateInterpreter(newInterpreterLanguage, defendantId, updatedByOnlinePlea, null);
    }

    private Optional<Object> updateInterpreter(final String newInterpreterLanguage, final UUID defendantId,
                                               boolean updatedByOnlinePlea, final ZonedDateTime createdOn) {
        // Assuming that if there is an interpreterLanguage interpreterRequired should always be true
        final String existingInterpreterLanguage = this.defendantInterpreterLanguages.get(defendantId);
        Object event = null;

        if (existingInterpreterLanguage != null && newInterpreterLanguage == null) {
            event = new InterpreterCancelledForDefendant(caseId, defendantId);
        } else if (!Objects.equals(existingInterpreterLanguage, newInterpreterLanguage)) {
            if (updatedByOnlinePlea) {
                event = InterpreterUpdatedForDefendant.createEventForOnlinePlea(caseId, defendantId, new Interpreter(newInterpreterLanguage), createdOn);
            } else {
                event = InterpreterUpdatedForDefendant.createEvent(caseId, defendantId, new Interpreter(newInterpreterLanguage));
            }
        }
        return Optional.ofNullable(event);
    }

    public Stream<Object> updatePlea(final UpdatePlea updatePleaCommand, final ZonedDateTime updatedOn) {
        return changePlea(updatePleaCommand, updatedOn);
    }

    public Stream<Object> cancelPlea(final CancelPlea cancelPleaCommand, final ZonedDateTime cancelledOn) {
        return changePlea(cancelPleaCommand, cancelledOn);
    }

    public Stream<Object> pleadOnline(final UUID caseId, final PleadOnline pleadOnline, final ZonedDateTime createdOn) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        final UUID defendantId = UUID.fromString(pleadOnline.getDefendantId());
        if (isCaseAssigned()) {
            streamBuilder.add(generateCaseUpdateRejected(caseId.toString(), CASE_ASSIGNED));
            return apply(streamBuilder.build());
        }
        if (!hasDefendant(defendantId)) {
            return apply(Stream.of(new DefendantNotFound(caseId.toString(), "Store Online Plea")));
        }
        final PleadOnlineOutcomes pleadOnlineOutcomes = addPleaEventsToStreamForStoreOnlinePlea(caseId, pleadOnline, streamBuilder, createdOn);
        if (pleadOnlineOutcomes.isPleaForOffencePreviouslySubmitted()) {
            Object caseUpdateRejectedEvent = generateCaseUpdateRejected(caseId.toString(), PLEA_ALREADY_SUBMITTED);
            return apply(Stream.of(caseUpdateRejectedEvent));
        }
        if (!pleadOnlineOutcomes.getOffenceNotFoundIds().isEmpty()) {
            final Stream.Builder<Object> offenceNotFoundStreamBuilder = Stream.builder();
            pleadOnlineOutcomes.getOffenceNotFoundIds().forEach(offenceNotFoundId ->
                    offenceNotFoundStreamBuilder.add(new OffenceNotFound(offenceNotFoundId, "Store Online Plea"))
            );
            return apply(offenceNotFoundStreamBuilder.build());
        }
        if (pleadOnlineOutcomes.isTrialRequested()) {
            streamBuilder.add(new TrialRequested(caseId, pleadOnline.getUnavailability(), pleadOnline.getWitnessDetails(),
                    pleadOnline.getWitnessDispute(), createdOn));
        }
        addAdditionalEventsToStreamForStoreOnlinePlea(streamBuilder, pleadOnline, defendantId, createdOn);
        return apply(streamBuilder.build());
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
                        caseId.toString(),
                        offence.getId(),
                        pleaType.name(),
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
        if (!offenceExists(UUID.fromString(offence.getId()))) {
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
                                                               final UUID defendantId,
                                                               final ZonedDateTime createdOn) {
        //TODO: we need to query the defendant to see if any of the incoming defendant data is different from the pre-existing defendant data. If no changes, no event
        final ContactDetails contactDetails = pleadOnline.getPersonalDetails().getContactDetails();
        final DefendantDetailsUpdated defendantDetailsUpdated = defendantDetailsUpdated()
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .withFirstName(pleadOnline.getPersonalDetails().getFirstName())
                .withLastName(pleadOnline.getPersonalDetails().getLastName())
                .withDateOfBirth(pleadOnline.getPersonalDetails().getDateOfBirth())
                .withNationalInsuranceNumber(pleadOnline.getPersonalDetails().getNationalInsuranceNumber())
                .withContactDetails(new ContactDetails(contactDetails.getHome(), contactDetails.getMobile(), contactDetails.getEmail()))
                .withAddress(pleadOnline.getPersonalDetails().getAddress())
                .withUpdateByOnlinePlea(true)
                .withUpdatedDate(createdOn)
                .build();
        streamBuilder.add(defendantDetailsUpdated);
        streamBuilder.add(FinancialMeansUpdated.createEventForOnlinePlea(defendantId, pleadOnline.getFinancialMeans().getIncome(),
                pleadOnline.getFinancialMeans().getBenefits(), pleadOnline.getFinancialMeans().getEmploymentStatus(),
                pleadOnline.getOutgoings(), createdOn));
        if (pleadOnline.getEmployer() != null) {
            addEmployerEventToStream(streamBuilder, pleadOnline.getEmployer(), defendantId, true, createdOn);
        }
        updateInterpreter(pleadOnline.getInterpreterLanguage(), defendantId, true, createdOn)
                .ifPresent(streamBuilder::add);
        streamBuilder.add(new OnlinePleaReceived(urn, this.caseId.toString(), pleadOnline.getDefendantId(), pleadOnline.getUnavailability(), pleadOnline.getInterpreterLanguage(),
                pleadOnline.getWitnessDetails(), pleadOnline.getWitnessDispute(), pleadOnline.getPersonalDetails(),
                pleadOnline.getFinancialMeans(), pleadOnline.getEmployer(), pleadOnline.getOutgoings()));
    }

    public Stream<Object> createCourtReferral(final String caseId, final LocalDate hearingDate) {

        if (this.caseId == null) {
            LOGGER.warn("Cannot create court hearing before case is created");
            return apply(Stream.of(new CaseNotFound(caseId, "Create Court Referral")));
        } else {
            return apply(Stream.of(new CourtReferralCreated(this.caseId, hearingDate)));
        }
    }

    public Stream<Object> actionCourtReferral(final String caseId) {

        if (this.hasCourtReferral) {
            return apply(Stream.of(new CourtReferralActioned(this.caseId, ZonedDateTime.now(UTC))));
        } else {
            LOGGER.warn("Cannot action court referral that does not exist");
            return apply(Stream.of(new CourtReferralNotFound(caseId)));
        }
    }

    public Stream<Object> requestWithdrawalAllOffences(final String caseId) {

        if (this.caseId == null) {
            LOGGER.warn("Cannot request withdrawal all offences before case is created");
            return apply(Stream.of(new CaseNotFound(caseId, "Request Withdrawal All Offences")));
        }
        return apply(Stream.of(new AllOffencesWithdrawalRequested(this.caseId)));
    }

    public Stream<Object> caseUpdateRejected(final String caseId, final CaseUpdateRejected.RejectReason reason) {
        return apply(Stream.of(generateCaseUpdateRejected(caseId, reason)));
    }

    private Object generateCaseUpdateRejected(final String caseId, final CaseUpdateRejected.RejectReason reason) {
        if (this.caseId == null) {
            return new CaseNotFound(caseId, "Case not found when attempting to apply update reject command");
        }
        return new CaseUpdateRejected(this.caseId, reason);
    }

    public Stream<Object> cancelRequestWithdrawalAllOffences(final String caseId) {

        if (this.caseId == null) {
            LOGGER.warn("Cannot cancel request withdrawal all offences before case is created");
            return apply(Stream.of(new CaseNotFound(caseId, "Cancel Request Withdrawal All Offences")));
        }
        return apply(Stream.of(new AllOffencesWithdrawalRequestCancelled(this.caseId)));
    }

    public Stream<Object> markCaseReopened(final CaseReopenDetails caseReopenDetails) {

        if (!assertCaseIdNotNullAndMatch(caseReopenDetails.getCaseId())) {

            return apply(Stream.of(
                    new CaseNotFound(caseReopenDetails.getCaseId(), "Cannot mark case reopened")));

        } else if (caseReopened) {

            LOGGER.warn("Cannot reopen case. Case already reopened with ID {}", caseId);
            return apply(Stream.of(
                    new CaseAlreadyReopened(caseReopenDetails.getCaseId(), "Cannot mark case reopened")));
        }

        return apply(Stream.of(new CaseReopened(caseReopenDetails)));
    }

    public Stream<Object> updateCaseReopened(final CaseReopenDetails caseReopenDetails) {

        if (!assertCaseIdNotNullAndMatch(caseReopenDetails.getCaseId())) {

            return apply(Stream.of(
                    new CaseNotFound(caseReopenDetails.getCaseId(), "Cannot update case reopened")));
        }

        if (!caseReopened) {

            LOGGER.warn("Cannot update reopened case. Case not yet reopened with ID {}", caseId);
            return apply(Stream.of(
                    new CaseNotReopened(caseReopenDetails.getCaseId(), "Cannot update case reopened")));
        }

        return apply(Stream.of(new CaseReopenedUpdated(caseReopenDetails)));
    }

    public Stream<Object> undoCaseReopened(final String caseId) {

        if (!assertCaseIdNotNullAndMatch(caseId)) {

            return apply(Stream.of(new CaseNotFound(caseId, "Cannot undo case reopened")));
        }

        if (!caseReopened || caseReopenedDate == null) {

            LOGGER.warn("Cannot undo reopened case. Case not yet reopened with ID: {}", caseId);
            return apply(Stream.of(new CaseNotReopened(caseId, "Cannot undo case reopened")));
        }

        return apply(Stream.of(new CaseReopenedUndone(caseId, caseReopenedDate)));
    }

    public Stream<Object> assignCase(final UUID assigneeId, final CaseAssignmentType assignmentType) {
        return apply(Stream.builder().add(new CaseAssigned(caseId, assigneeId, assignmentType)).build());
    }

    public Stream<Object> caseAssignmentCreated(final UUID caseId, final UUID assigneeId, final CaseAssignmentType caseAssignmentType) {
        return apply(Stream.builder().add(new CaseAssigned(caseId, assigneeId, caseAssignmentType)).build());
    }

    public Stream<Object> caseAssignmentDeleted(final UUID caseId, final CaseAssignmentType caseAssignmentType) {
        return apply(Stream.builder().add(new CaseAssignmentDeleted(caseId, caseAssignmentType)).build());
    }

    private void validateDefendant(String title, LocalDate dateOfBirth, Address address) {
        validateDefendantTitle(title);
        validateDefendantDateOfBirth(dateOfBirth);
        validateDefendantAddress(address);
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

    private void validateDefendantDateOfBirth(final LocalDate dateOfBirth) {
        if (this.defendantDateOfBirth != null && dateOfBirth == null) {
            throw new IllegalArgumentException(
                    "dob parameter can not be null");
        }
    }

    private void validateDefendantTitle(final String title) {
        if (StringUtils.isBlank(title) && StringUtils.isNotBlank(this.defendantTitle)) {
            throw new IllegalArgumentException(String.format("title parameter can not be null as previous value is : %s", this.defendantTitle));
        }
    }

    public Stream<Object> fixDefendantDetails(UUID caseId, UUID personId, String gender,
                                              String nationalInsuranceNumber, String email,
                                              String homeNumber, String mobileNumber,
                                              Person person) {

        final DefendantDetailsMovedFromPeople defendantDetailsMovedFromPeople = new DefendantDetailsMovedFromPeople.DefendantDetailsMovedFromPeopleBuilder()
                .withCaseId(caseId)
                .withPersonId(personId)
                .withTitle(person.getTitle())
                .withFirstName(person.getFirstName())
                .withLastName(person.getLastName())
                .withDateOfBirth(person.getDateOfBirth())
                .withGender(gender)
                .withEmail(email)
                .withNationalInsuranceNumber(nationalInsuranceNumber)
                .withContactNumber(new ContactDetails(homeNumber, mobileNumber, email))
                .withAddress(person.getAddress())
                .build();
        return apply(Stream.of(defendantDetailsMovedFromPeople));
    }

    @SuppressWarnings("squid:S00107") //Proper fix requires proper remodelling / guidance
    public Stream<Object> updateDefendantDetails(UUID caseId, UUID defendantId, String gender,
                                                 String nationalInsuranceNumber, String email,
                                                 String homeNumber, String mobileNumber,
                                                 Person person, ZonedDateTime updatedDate) {

        Stream.Builder<Object> events = Stream.builder();

        try {
            validateDefendant(person.getTitle(), person.getDateOfBirth(), person.getAddress());
        } catch (IllegalArgumentException | IllegalStateException e) {
            LOGGER.error("Defendant details update failed for ID: {} with message {} ", defendantId, e);
            return apply(Stream.of(new DefendantDetailsUpdateFailed(caseId.toString(), defendantId.toString(), e.getMessage())));
        }

        if ((defendantDateOfBirth != null) && (!person.getDateOfBirth().isEqual(defendantDateOfBirth))) {
            final DefendantDateOfBirthUpdated defendantDateOfBirthUpdated = new DefendantDateOfBirthUpdated(caseId, defendantDateOfBirth, person.getDateOfBirth());
            events.add(defendantDateOfBirthUpdated);
        }

        if ((defendantAddress != null) && (!defendantAddress.equals(person.getAddress()))) {
            final DefendantAddressUpdated defendantAddressUpdated = new DefendantAddressUpdated(caseId, defendantAddress, person.getAddress());
            events.add(defendantAddressUpdated);
        }

        if ((defendantTitle != null) && ((!defendantTitle.equalsIgnoreCase(person.getTitle())) ||
                (!defendantFirstName.equalsIgnoreCase(person.getFirstName())) ||
                (!defendantLastName.equalsIgnoreCase(person.getLastName())))
                ) {
            final DefendantPersonalNameUpdated defendantPersonalNameUpdated = new DefendantPersonalNameUpdated(caseId,
                    new PersonalName(defendantTitle, defendantFirstName, defendantLastName),
                    new PersonalName(person.getTitle(), person.getFirstName(), person.getLastName()));
            events.add(defendantPersonalNameUpdated);
        }

        final DefendantDetailsUpdated defendantDetailsUpdated = defendantDetailsUpdated()
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .withTitle(person.getTitle())
                .withFirstName(person.getFirstName())
                .withLastName(person.getLastName())
                .withDateOfBirth(person.getDateOfBirth())
                .withGender(gender)
                .withNationalInsuranceNumber(nationalInsuranceNumber)
                .withContactDetails(new ContactDetails(homeNumber, mobileNumber, email))
                .withAddress(person.getAddress())
                .withUpdateByOnlinePlea(false)
                .withUpdatedDate(updatedDate)
                .build();
        events.add(defendantDetailsUpdated);
        return apply(events.build());
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

    private boolean assertCaseIdNotNullAndMatch(String caseId) {
        if (caseId == null) {
            LOGGER.warn("Case ID is null");
            return false;
        } else if (!Objects.equals(this.caseId, UUID.fromString(caseId))) {
            LOGGER.error("Mismatch of IDs in aggregate: {} != {}", this.caseId, caseId);
            return false;
        }

        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
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
                }),
                when(CaseCompleted.class)
                        .apply(e -> this.caseCompleted = true),
                when(CaseDocumentAdded.class).apply(e -> {
                    caseDocuments.put(UUID.fromString(e.getCaseDocument().getId()), e.getCaseDocument());
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
                    if (e.getInterpreter() != null && e.getInterpreter().getLanguage() != null) {
                        this.defendantInterpreterLanguages.put(e.getDefendantId(), e.getInterpreter().getLanguage());
                    }
                    // should never happen (but just in case)
                    else {
                        this.defendantInterpreterLanguages.remove(e.getDefendantId());
                    }
                }),
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
                when(CaseAssigned.class).apply(caseAssignment -> assigned = true),
                when(CaseAssignmentCreated.class).apply(caseAssignment -> assigned = true),
                when(CaseAssignmentDeleted.class).apply(ignored -> assigned = false),

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
    private static CaseReceived convertSjpCaseCreatedToCaseReceived(SjpCaseCreated sjpCaseCreated) {
        Defendant defendant = new Defendant(sjpCaseCreated.getDefendantId(),
                null, null, null, null, null, null,
                sjpCaseCreated.getNumPreviousConvictions(),
                sjpCaseCreated.getOffences());

        return new CaseReceived(UUID.fromString(sjpCaseCreated.getId()), sjpCaseCreated.getUrn(),
                sjpCaseCreated.getProsecutingAuthority(), sjpCaseCreated.getCosts(), sjpCaseCreated.getPostingDate(),
                defendant, sjpCaseCreated.getCreatedOn());
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getUrn() {
        return urn;
    }

    public boolean isCaseCompleted() {
        return caseCompleted;
    }

    public boolean isCaseReopened() {
        return caseReopened;
    }

    public boolean isWithdrawalAllOffencesRequested() {
        return withdrawalAllOffencesRequested;
    }

    public Map<UUID, Set<UUID>> getOffenceIdsByDefendantId() {
        return offenceIdsByDefendantId;
    }

    public Map<UUID, CaseDocument> getCaseDocuments() {
        return caseDocuments;
    }

    public ProsecutingAuthority getProsecutingAuthority() {
        return prosecutingAuthority;
    }

    private boolean hasDefendant(final UUID defendantId) {
        return offenceIdsByDefendantId.containsKey(defendantId);
    }

    public boolean isCaseAssigned() {
        return assigned;
    }

    public String getDefendantLastName() {
        return defendantLastName;
    }

    public String getDefendantFirstName() {
        return defendantFirstName;
    }

    public String getDefendantTitle() {
        return defendantTitle;
    }

    public Address getDefendantAddress() {
        return defendantAddress;
    }

    public LocalDate getDefendantDob() {
        return defendantDateOfBirth;
    }

}
