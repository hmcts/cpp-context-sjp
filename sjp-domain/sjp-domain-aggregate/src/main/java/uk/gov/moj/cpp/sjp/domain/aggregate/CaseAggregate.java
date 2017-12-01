package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.time.ZoneOffset.UTC;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.sjp.domain.plea.EmploymentStatus.EMPLOYED;
import static uk.gov.moj.cpp.sjp.event.CaseUpdateRejected.RejectReason.CASE_ASSIGNED;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.sjp.CourtReferralNotFound;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.CaseAssignment;
import uk.gov.moj.cpp.sjp.domain.CaseDocument;
import uk.gov.moj.cpp.sjp.domain.CaseReopenDetails;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.Interpreter;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.domain.SjpOffence;
import uk.gov.moj.cpp.sjp.domain.aggregate.domain.DocumentCountByDocumentType;
import uk.gov.moj.cpp.sjp.domain.command.CancelPlea;
import uk.gov.moj.cpp.sjp.domain.command.ChangePlea;
import uk.gov.moj.cpp.sjp.domain.command.CompleteCase;
import uk.gov.moj.cpp.sjp.domain.command.UpdatePlea;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalDenied;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequestCancelled;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequested;
import uk.gov.moj.cpp.sjp.event.CaseAlreadyCompleted;
import uk.gov.moj.cpp.sjp.event.CaseAlreadyReopened;
import uk.gov.moj.cpp.sjp.event.CaseAssignmentCreated;
import uk.gov.moj.cpp.sjp.event.CaseAssignmentDeleted;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.CaseCreationFailedBecauseCaseAlreadyExisted;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAdded;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAlreadyAdded;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAlreadyExists;
import uk.gov.moj.cpp.sjp.event.CaseDocumentUploaded;
import uk.gov.moj.cpp.sjp.event.CaseNotFound;
import uk.gov.moj.cpp.sjp.event.CaseNotReopened;
import uk.gov.moj.cpp.sjp.event.CaseReopened;
import uk.gov.moj.cpp.sjp.event.CaseReopenedUndone;
import uk.gov.moj.cpp.sjp.event.CaseReopenedUpdated;
import uk.gov.moj.cpp.sjp.event.CaseStarted;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;
import uk.gov.moj.cpp.sjp.event.CourtReferralActioned;
import uk.gov.moj.cpp.sjp.event.CourtReferralCreated;
import uk.gov.moj.cpp.sjp.event.DefendantNotEmployed;
import uk.gov.moj.cpp.sjp.event.DefendantNotFound;
import uk.gov.moj.cpp.sjp.event.EmployerDeleted;
import uk.gov.moj.cpp.sjp.event.EmployerUpdated;
import uk.gov.moj.cpp.sjp.event.EmploymentStatusUpdated;
import uk.gov.moj.cpp.sjp.event.EnterpriseIdAssociated;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;
import uk.gov.moj.cpp.sjp.event.InterpreterCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.OffenceNotFound;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.PleaUpdateDenied;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.cpp.sjp.event.SjpCaseCreated;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("WeakerAccess")
public class CaseAggregate implements Aggregate {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseAggregate.class);

    private UUID caseId;
    private String urn;
    private boolean caseReopened = false;
    private LocalDate caseReopenedDate = null;
    private boolean caseCompleted = false;
    private boolean withdrawalAllOffencesRequested = false;
    private boolean hasCourtReferral;
    private boolean caseAssigned = false;


    private Map<UUID, Set<UUID>> offenceIdsByDefendantId = new HashMap<>();

    private Map<UUID, CaseDocument> caseDocuments = new HashMap<>();
    private Map<UUID, Plea.Type> offencePleas = new HashMap<>();
    private Map<UUID, String> defendantInterpreterLanguages = new HashMap<>();

    private DocumentCountByDocumentType documentCountByDocumentType = new DocumentCountByDocumentType();

    private ProsecutingAuthority prosecutingAuthority;

    private final Map<UUID, String> employmentStatusByDefendantId = new HashMap<>();


    public Stream<Object> createCase(final Case aCase, final ZonedDateTime now) {
        final Stream.Builder<Object> events = Stream.builder();

        if (this.caseId != null && this.urn != null) {
            events.add(new CaseCreationFailedBecauseCaseAlreadyExisted(this.caseId, this.urn));
        } else {
            final SjpCaseCreated sjpCaseCreated =
                    new SjpCaseCreated(
                            aCase.getId().toString(), aCase.getUrn(),
                            aCase.getPtiUrn(), aCase.getInitiationCode(),
                            aCase.getSummonsCode(), aCase.getProsecutingAuthority(),
                            aCase.getLibraOriginatingOrg(),
                            aCase.getLibraHearingLocation(),
                            aCase.getDateOfHearing(), aCase.getTimeOfHearing(),
                            aCase.getPersonId(),
                            UUID.randomUUID(),
                            aCase.getNumPreviousConvictions(),
                            aCase.getCosts(),
                            aCase.getPostingDate(),
                            aCase.getOffences(),
                            now);
            events.add(sjpCaseCreated);
        }
        return apply(events.build());
    }

    public Stream<Object> updateFinancialMeans(final FinancialMeans financialMeans) {
        final UUID defendantId = financialMeans.getDefendantId();
        final Object event;

        if (hasDefendant(defendantId)) {
            event = new FinancialMeansUpdated(financialMeans.getDefendantId(), financialMeans.getIncome(),
                    financialMeans.getBenefits(), financialMeans.getEmploymentStatus());
        } else {
            event = new DefendantNotFound(defendantId.toString(), "Update financial means");
        }

        return apply(Stream.of(event));
    }

    public Stream<Object> updateEmployer(final Employer employer) {
        final UUID defendantId = employer.getDefendantId();
        final Stream.Builder streamBuilder = Stream.builder();

        if (hasDefendant(defendantId)) {
            streamBuilder.add(new EmployerUpdated(employer));

            final String actualEmploymentStatus = employmentStatusByDefendantId.get(defendantId);

            if (!EMPLOYED.name().equals(actualEmploymentStatus)) {
                streamBuilder.add(new EmploymentStatusUpdated(defendantId, EMPLOYED.name()));
            }
        } else {
            streamBuilder.add(new DefendantNotFound(defendantId.toString(), "Update employer"));
        }

        return apply(streamBuilder.build());
    }

    public Stream<Object> deleteEmployer(final UUID defendantId) {
        final Stream.Builder streamBuilder = Stream.builder();

        if (hasDefendant(defendantId)) {
            if (employmentStatusByDefendantId.containsKey(defendantId)) {
                streamBuilder.add(new EmployerDeleted(defendantId));
            } else {
                streamBuilder.add(new DefendantNotEmployed(defendantId));
            }
        } else {
            streamBuilder.add(new DefendantNotFound(defendantId.toString(), "Update employer"));
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
                .anyMatch(offenceIds -> offenceIds.contains(offenceId));
    }

    public Stream<Object> changePlea(final ChangePlea changePleaCommand) {
        if (!offenceExists(changePleaCommand.getOffenceId())) {
            LOGGER.warn("Cannot update plea for offence which doesn't exist, ID: {}", changePleaCommand.getOffenceId());
            return apply(Stream.of(new OffenceNotFound(changePleaCommand.getOffenceId().toString(), "Update Plea")));
        }

        final Optional<UUID> defendantId = getDefendantIdByOffenceId(changePleaCommand.getOffenceId());
        if (!defendantId.isPresent()) {
            LOGGER.warn("Cannot update plea for defendant which doesn't exist, offence ID: {}", changePleaCommand.getOffenceId());
            return apply(Stream.of(new DefendantNotFound(changePleaCommand.getOffenceId().toString(), "Update Plea")));
        }

        if (caseAssigned) {
            return caseUpdateRejected(changePleaCommand.getCaseId().toString(), CASE_ASSIGNED);
        }

        if (withdrawalAllOffencesRequested) {
            return apply(Stream.of(new CaseUpdateRejected(changePleaCommand.getCaseId(),
                    CaseUpdateRejected.RejectReason.WITHDRAWAL_PENDING)));
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

            updateInterpreter(updatePlea.getInterpreterLanguage(), defendantId.get())
                    .ifPresent(streamBuilder::add);

        } else if (changePleaCommand instanceof CancelPlea) {
            final CancelPlea cancelPlea = (CancelPlea) changePleaCommand;
            final PleaCancelled pleaCancelled = new PleaCancelled(
                    cancelPlea.getCaseId().toString(),
                    cancelPlea.getOffenceId().toString());

            streamBuilder.add(pleaCancelled);
            updateInterpreter(null, defendantId.get())
                    .ifPresent(streamBuilder::add);
        }
        return apply(streamBuilder.build());
    }

    public Stream<Object> updateInterpreter(final UUID defendantId, final String language) {
        final Stream.Builder streamBuilder = Stream.builder();
        if (hasDefendant(defendantId)) {
            updateInterpreter(language, defendantId).ifPresent(streamBuilder::add);
        } else {
            streamBuilder.add(new DefendantNotFound(defendantId.toString(), "Update interpreter"));
        }
        return apply(streamBuilder.build());

    }

    private Optional<Object> updateInterpreter(final String newInterpreterLanguage, final UUID defendantId) {
        // Assuming that if there is an interpreterLanguage interpreterRequired should always be true
        final String existingInterpreterLanguage = this.defendantInterpreterLanguages.get(defendantId);
        Object event = null;

        if (existingInterpreterLanguage != null && newInterpreterLanguage == null) {
            event = new InterpreterCancelledForDefendant(caseId, defendantId);
        } else if (!Objects.equals(existingInterpreterLanguage, newInterpreterLanguage)) {
            event = new InterpreterUpdatedForDefendant(caseId, defendantId, new Interpreter(true, newInterpreterLanguage));
        }
        return Optional.ofNullable(event);
    }

    public Stream<Object> updatePlea(final UpdatePlea updatePleaCommand) {
        return changePlea(updatePleaCommand);
    }

    public Stream<Object> cancelPlea(final CancelPlea cancelPleaCommand) {
        return changePlea(cancelPleaCommand);
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
        if (this.caseId == null) {
            return apply(Stream.of(new CaseNotFound(caseId, "Case not found when attempting to apply update reject command")));
        }
        return apply(Stream.of(
                new CaseUpdateRejected(this.caseId, reason)
        ));
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

    public Stream<Object> caseAssignmentCreated(final CaseAssignment caseAssignment) {
        return apply(Stream.builder().add(new CaseAssignmentCreated(caseAssignment)).build());
    }

    public Stream<Object> caseAssignmentDeleted(final CaseAssignment caseAssignment) {
        return apply(Stream.builder().add(new CaseAssignmentDeleted(caseAssignment)).build());
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
    public Object apply(Object event) {
        return match(event).with(
                when(CaseCreationFailedBecauseCaseAlreadyExisted.class).apply(e -> {
                    //nothing to update
                }),
                when(CaseStarted.class)
                        .apply(e -> caseId = e.getId()),
                when(SjpCaseCreated.class).apply(e -> {
                    caseId = UUID.fromString(e.getId());
                    urn = e.getUrn();
                    prosecutingAuthority = e.getProsecutingAuthority();
                    offenceIdsByDefendantId.putIfAbsent(e.getDefendantId(), new HashSet<>());
                    offenceIdsByDefendantId.get(e.getDefendantId()).addAll(
                            e.getOffences()
                                    .stream()
                                    .map(SjpOffence::getId)
                                    .collect(Collectors.toSet())
                    );
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
                when(PleaUpdated.class).apply(e ->
                        this.offencePleas.put(UUID.fromString(e.getOffenceId()), Plea.Type.valueOf(e.getPlea()))),
                when(PleaCancelled.class).apply(e ->
                        //TODO: check there is a plea
                        this.offencePleas.remove(e.getOffenceId())
                ),
                // Old event. Replaced by CaseUpdateRejected
                when(PleaUpdateDenied.class).apply(e -> {
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
                when(CaseAssignmentCreated.class).apply(ignored -> caseAssigned = true),
                when(CaseAssignmentDeleted.class).apply(ignored -> caseAssigned = false),
                otherwiseDoNothing()
        );
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
        return offenceIdsByDefendantId.keySet().contains(defendantId);
    }

    public boolean isCaseAssigned() {
        return caseAssigned;
    }
}
