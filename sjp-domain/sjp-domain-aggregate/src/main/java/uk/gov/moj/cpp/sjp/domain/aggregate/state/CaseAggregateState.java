package uk.gov.moj.cpp.sjp.domain.aggregate.state;

import static java.util.Objects.nonNull;

import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.CaseDocument;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.Interpreter;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.domain.aggregate.domain.DocumentCountByDocumentType;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Defines the case aggregate state.
 */
public class CaseAggregateState implements AggregateState {

    private UUID caseId;
    private String urn;
    private boolean caseReopened;
    private boolean caseCompleted;
    private boolean caseReferredForCourtHearing;
    private LocalDate caseReopenedDate;
    private boolean withdrawalAllOffencesRequested;
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
    private final Set<UUID> offenceIdsWithPleas = new HashSet<>();

    private final Map<UUID, String> defendantsInterpreterLanguages = new HashMap<>();
    private final Map<UUID, Boolean> defendantsSpeakWelsh = new HashMap<>();

    private boolean trialRequested;
    private boolean trialRequestedPreviously;
    private String trialRequestedUnavailability;
    private String trialRequestedWitnessDetails;
    private String trialRequestedWitnessDispute;

    private DocumentCountByDocumentType documentCountByDocumentType = new DocumentCountByDocumentType();

    private final Map<UUID, String> employmentStatusByDefendantId = new HashMap<>();

    private ProsecutingAuthority prosecutingAuthority;

    private CaseStatus status;

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public String getUrn() {
        return urn;
    }

    public void setUrn(final String urn) {
        this.urn = urn;
    }

    public boolean isCaseReopened() {
        return caseReopened;
    }

    public void setCaseReopened(final boolean caseReopened) {
        this.caseReopened = caseReopened;
    }

    public LocalDate getCaseReopenedDate() {
        return caseReopenedDate;
    }

    public void setCaseReopenedDate(final LocalDate caseReopenedDate) {
        this.caseReopenedDate = caseReopenedDate;
    }

    public boolean isCaseCompleted() {
        return caseCompleted;
    }

    public void markCaseCompleted() {
        this.caseCompleted = true;
        //TODO ATCM-3121 implement allowed transition in CaseStatus enum - there is likely race condition between case completed and case referred for court hearing
        if (this.status != CaseStatus.REFERRED_FOR_COURT_HEARING) {
            this.status = CaseStatus.COMPLETED;
        }
    }

    public void markCaseReferredForCourtHearing() {
        this.caseReferredForCourtHearing = true;
        this.status = CaseStatus.REFERRED_FOR_COURT_HEARING;
    }

    public boolean isCaseReferredForCourtHearing() {
        return this.caseReferredForCourtHearing;
    }

    public boolean isWithdrawalAllOffencesRequested() {
        return withdrawalAllOffencesRequested;
    }

    public void setWithdrawalAllOffencesRequested(final boolean withdrawalAllOffencesRequested) {
        this.withdrawalAllOffencesRequested = withdrawalAllOffencesRequested;
    }

    public UUID getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(final UUID assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getDefendantTitle() {
        return defendantTitle;
    }

    public void setDefendantTitle(final String defendantTitle) {
        this.defendantTitle = defendantTitle;
    }

    public String getDefendantFirstName() {
        return defendantFirstName;
    }

    public void setDefendantFirstName(final String defendantFirstName) {
        this.defendantFirstName = defendantFirstName;
    }

    public String getDefendantLastName() {
        return defendantLastName;
    }

    public void setDefendantLastName(final String defendantLastName) {
        this.defendantLastName = defendantLastName;
    }

    public LocalDate getDefendantDateOfBirth() {
        return defendantDateOfBirth;
    }

    public void setDefendantDateOfBirth(final LocalDate defendantDateOfBirth) {
        this.defendantDateOfBirth = defendantDateOfBirth;
    }

    public Address getDefendantAddress() {
        return defendantAddress;
    }

    public void setDefendantAddress(final Address defendantAddress) {
        this.defendantAddress = defendantAddress;
    }

    public CaseReadinessReason getReadinessReason() {
        return readinessReason;
    }

    public void setReadinessReason(final CaseReadinessReason readinessReason) {
        this.readinessReason = readinessReason;
    }

    public boolean isCaseReceived() {
        return caseReceived;
    }

    public void setCaseReceived(final boolean caseReceived) {
        this.caseReceived = caseReceived;
        this.status = CaseStatus.NO_PLEA_RECEIVED;
    }

    public String getDatesToAvoid() {
        return datesToAvoid;
    }

    public void setDatesToAvoid(final String datesToAvoid) {
        this.datesToAvoid = datesToAvoid;
    }

    public void addOffenceIdsForDefendant(UUID defendantId, Set<UUID> offenceIds) {
        offenceIdsByDefendantId.putIfAbsent(defendantId, offenceIds);
    }

    public Map<UUID, Set<UUID>> getOffenceIdsByDefendantId() {
        return offenceIdsByDefendantId;
    }

    public Map<UUID, CaseDocument> getCaseDocuments() {
        return caseDocuments;
    }

    public Set<UUID> getOffenceIdsWithPleas() {
        return offenceIdsWithPleas;
    }

    public Map<UUID, String> getDefendantsInterpreterLanguages() {
        return defendantsInterpreterLanguages;
    }

    public Map<UUID, Boolean> getDefendantsSpeakWelsh() {
        return defendantsSpeakWelsh;
    }

    public boolean isTrialRequested() {
        return trialRequested;
    }

    public void setTrialRequested(final boolean trialRequested) {
        this.trialRequested = trialRequested;
    }

    public boolean isTrialRequestedPreviously() {
        return trialRequestedPreviously;
    }

    public void setTrialRequestedPreviously(final boolean trialRequestedPreviously) {
        this.trialRequestedPreviously = trialRequestedPreviously;
    }

    public String getTrialRequestedUnavailability() {
        return trialRequestedUnavailability;
    }

    public void setTrialRequestedUnavailability(final String trialRequestedUnavailability) {
        this.trialRequestedUnavailability = trialRequestedUnavailability;
    }

    public String getTrialRequestedWitnessDetails() {
        return trialRequestedWitnessDetails;
    }

    public void setTrialRequestedWitnessDetails(final String trialRequestedUWitnessDetails) {
        this.trialRequestedWitnessDetails = trialRequestedUWitnessDetails;
    }

    public String getTrialRequestedWitnessDispute() {
        return trialRequestedWitnessDispute;
    }

    public void setTrialRequestedWitnessDispute(final String trialRequestedWitnessDispute) {
        this.trialRequestedWitnessDispute = trialRequestedWitnessDispute;
    }

    public DocumentCountByDocumentType getDocumentCountByDocumentType() {
        return documentCountByDocumentType;
    }

    public ProsecutingAuthority getProsecutingAuthority() {
        return prosecutingAuthority;
    }

    public void setProsecutingAuthority(final ProsecutingAuthority prosecutingAuthority) {
        this.prosecutingAuthority = prosecutingAuthority;
    }

    public Map<UUID, String> getEmploymentStatusByDefendantId() {
        return employmentStatusByDefendantId;
    }

    public boolean hasDefendant(final UUID defendantId) {
        return offenceIdsByDefendantId.containsKey(defendantId);
    }

    public String getDefendantInterpreterLanguage(final UUID defendantID) {
        return defendantsInterpreterLanguages.get(defendantID);
    }

    public Boolean defendantSpeakWelsh(final UUID defendantID) {
        return defendantsSpeakWelsh.get(defendantID);
    }

    public void addCaseDocument(final UUID id, final CaseDocument caseDocument) {
        caseDocuments.put(id, caseDocument);
    }

    public void addOffenceIdWithPleas(final UUID offenceId) {
        offenceIdsWithPleas.add(offenceId);
    }

    public void removePleaFromOffence(final UUID offenceId) {
        offenceIdsWithPleas.remove(offenceId);
    }

    public CaseStatus getStatus() {
        return status;
    }

    public void updateDefendantInterpreterLanguage(final UUID defendantId, final Interpreter interpreter) {
        defendantsInterpreterLanguages.compute(
                defendantId,
                (defendant, previousValue) -> Optional.ofNullable(interpreter)
                        .map(Interpreter::getLanguage)
                        .orElse(null));
    }

    public void updateDefendantSpeakWelsh(final UUID defendantId, final Boolean speakWelsh) {
        defendantsSpeakWelsh.put(defendantId, speakWelsh);
    }

    public void removeDefendantSpeakWelshPreference(final UUID defendantId) {
        defendantsSpeakWelsh.remove(defendantId);
    }

    public void removeInterpreterForDefendant(final UUID defendantId) {
        defendantsInterpreterLanguages.remove(defendantId);
    }

    public void updateEmploymentStatusForDefendant(final UUID defendantId, final String employmentStatus) {
        employmentStatusByDefendantId.put(defendantId, employmentStatus);
    }

    public void removeEmploymentStatusForDefendant(final UUID defendantId) {
        employmentStatusByDefendantId.remove(defendantId);
    }

    public Optional<String> getDefendantEmploymentStatus(final UUID defendantId) {
        return Optional.ofNullable(employmentStatusByDefendantId.get(defendantId));
    }

    public Optional<UUID> getDefendantForOffence(final UUID offenceId) {
        return offenceIdsByDefendantId.entrySet().stream()
                .filter(entry -> entry.getValue().contains(offenceId))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public boolean isAssignee(final UUID userId) {
        return nonNull(assigneeId) && assigneeId.equals(userId);
    }

    public boolean offenceExists(final UUID offenceId) {
        return offenceIdsByDefendantId.values().stream()
                .flatMap(Set::stream)
                .anyMatch(offenceId::equals);
    }

    public boolean isCaseIdEqualTo(final UUID id) {
        return nonNull(caseId) && caseId.equals(id);
    }

}
