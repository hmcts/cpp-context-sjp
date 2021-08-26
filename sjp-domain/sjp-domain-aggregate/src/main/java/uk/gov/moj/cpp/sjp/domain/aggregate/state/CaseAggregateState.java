package uk.gov.moj.cpp.sjp.domain.aggregate.state;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.REOPENING_GRANTED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.REOPENING_PENDING;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.REOPENING_REFUSED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.STATUTORY_DECLARATION_GRANTED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.STATUTORY_DECLARATION_PENDING;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.STATUTORY_DECLARATION_REFUSED;
import static uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated.DefendantDetailsUpdatedBuilder.defendantDetailsUpdated;

import uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus;
import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.CaseDocument;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.ContactDetails;
import uk.gov.moj.cpp.sjp.domain.Interpreter;
import uk.gov.moj.cpp.sjp.domain.aggregate.domain.DocumentCountByDocumentType;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PersonalDetails;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;

/**
 * Defines the case aggregate state.
 */
public class

CaseAggregateState implements AggregateState {


    public static final String FINANCIAL_MEANS_DOCUMENT_TYPE = "FINANCIAL_MEANS";
    private static final List<ApplicationStatus> PENDING_APPLICATION_STATUSES = asList(STATUTORY_DECLARATION_PENDING, REOPENING_PENDING);
    private static final List<ApplicationStatus> GRANTED_APPLICATION_STATUSES = asList(STATUTORY_DECLARATION_GRANTED, REOPENING_GRANTED);
    private static final List<ApplicationStatus> REFUSED_APPLICATION_STATUSES = asList(STATUTORY_DECLARATION_REFUSED, REOPENING_REFUSED);
    private UUID caseId;
    private String urn;
    private boolean caseReopened;
    private boolean caseCompleted;
    private boolean caseRelisted;
    private boolean caseAppealed;


    private boolean caseReferredForCourtHearing;
    private LocalDate caseReopenedDate;
    private boolean withdrawalAllOffencesRequested;
    private UUID assigneeId;
    private UUID defendantId;
    private String defendantTitle;
    private String defendantFirstName;
    private String defendantLastName;

    private String defendantNationalInsuranceNumber;
    private LocalDate defendantDateOfBirth;
    private Gender defendantGender;
    private ContactDetails defendantContactDetails;
    private Address defendantAddress;
    private CaseReadinessReason readinessReason;
    private LocalDate expectedDateReady;
    private boolean caseReceived;
    private List<Plea> pleas = new ArrayList<>();
    private LocalDate postingDate;
    private BigDecimal costs;

    private ZonedDateTime markedReadyForDecision;

    private String datesToAvoid;

    private final Map<UUID, Set<UUID>> offenceIdsByDefendantId = new HashMap<>();

    private final Map<UUID, CaseDocument> caseDocuments = new HashMap<>();
    private final Set<UUID> offenceIdsWithPleas = new HashSet<>();
    private final Map<UUID, LocalDate> offencePleaDates = new HashMap<>();

    private final Map<UUID, String> defendantsInterpreterLanguages = new HashMap<>();
    private final Map<UUID, Boolean> defendantsSpeakWelsh = new HashMap<>();

    private boolean trialRequested;
    private boolean trialRequestedPreviously;
    private String trialRequestedUnavailability;
    private String trialRequestedWitnessDetails;
    private String trialRequestedWitnessDispute;

    private DocumentCountByDocumentType documentCountByDocumentType = new DocumentCountByDocumentType();

    private final Map<UUID, String> employmentStatusByDefendantId = new HashMap<>();
    private boolean employerDetailsUpdated;

    private String prosecutingAuthority;

    private final Set<WithdrawalRequestsStatus> withdrawalRequests = new HashSet<>();

    private final Map<UUID, OffenceDecision> offenceDecisionsByOffenceId = new HashMap<>();
    private final Map<UUID, ZonedDateTime> offenceConvictionDates = new HashMap<>();

    private Set<UUID> sessionIds = new HashSet<>();

    private boolean defendantsResponseTimerExpired;
    private boolean datesToAvoidPreviouslyRequested;
    private LocalDate datesToAvoidExpirationDate;
    private LocalDate adjournedTo;
    private String defendantRegion;
    private String defendantDriverNumber;
    private String defendantDriverLicenceDetails;
    private boolean setAside;
    private boolean deleteDocsStarted;
    private Application currentApplication;


    private boolean managedByAtcm;

    private final Set<UUID> pressRestrictableOffenceIds = new HashSet<>();
    private final Set<UUID> offencesHavingPreviousPressRestriction = new HashSet<>();
    private final Map<UUID, DisabilityNeeds> defendantsDisabilityNeeds = new HashMap<>();
    private final Map<UUID, FinancialImpositionExportDetails> defendantFinancialImpositionExportDetails = new HashMap<>();

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
    }

    public void unMarkCaseCompleted() {
        this.caseCompleted = false;
    }

    public void markCaseReferredForCourtHearing() {
        this.caseReferredForCourtHearing = true;
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

    public boolean isCaseReadyForDecision() {
        return readinessReason != null;
    }

    public boolean isCaseReceived() {
        return caseReceived;
    }

    public void setCaseReceived(final boolean caseReceived) {
        this.caseReceived = caseReceived;
    }

    public String getDatesToAvoid() {
        return datesToAvoid;
    }

    public void setDatesToAvoid(final String datesToAvoid) {
        this.datesToAvoid = datesToAvoid;
    }

    public String getDefendantNationalInsuranceNumber() {
        return defendantNationalInsuranceNumber;
    }

    public void setDefendantNationalInsuranceNumber(final String defendantNationalInsuranceNumber) {
        this.defendantNationalInsuranceNumber = defendantNationalInsuranceNumber;
    }

    public ContactDetails getDefendantContactDetails() {
        return defendantContactDetails;
    }

    public void setDefendantContactDetails(final ContactDetails defendantContactDetails) {
        this.defendantContactDetails = defendantContactDetails;
    }

    public Gender getDefendantGender() {
        return defendantGender;
    }

    public void setDefendantGender(final Gender defendantGender) {
        this.defendantGender = defendantGender;
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

    public Map<UUID, FinancialImpositionExportDetails> getDefendantFinancialImpositionExportDetails() {
        return defendantFinancialImpositionExportDetails;
    }

    public FinancialImpositionExportDetails getDefendantFinancialImpositionExportDetails(final UUID defendantId) {
        return defendantFinancialImpositionExportDetails.get(defendantId);
    }

    public Optional<UUID> getDefendantForCorrelationId(final UUID correlationId) {
        return defendantFinancialImpositionExportDetails.entrySet().stream()
                .filter(fiExportDetailsEntry -> nonNull(fiExportDetailsEntry.getValue().getCorrelationId()))
                .filter(fiExportDetailsEntry -> Objects.equals(fiExportDetailsEntry.getValue().getCorrelationId(), correlationId))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public void addFinancialImpositionExportDetails(final UUID defendantId, FinancialImpositionExportDetails exportDetails) {
        this.defendantFinancialImpositionExportDetails.put(defendantId, exportDetails);
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

    public String getProsecutingAuthority() {
        return prosecutingAuthority;
    }

    public void setProsecutingAuthority(final String prosecutingAuthority) {
        this.prosecutingAuthority = prosecutingAuthority;
    }

    public Map<UUID, String> getEmploymentStatusByDefendantId() {
        return employmentStatusByDefendantId;
    }

    public boolean hasEmployerDetailsUpdated() {
        return employerDetailsUpdated;
    }

    public void setEmployerDetailsUpdated(final boolean employerDetailsUpdated) {
        this.employerDetailsUpdated = employerDetailsUpdated;
    }

    public String getDefendantRegion() {
        return defendantRegion;
    }

    public void setDefendantRegion(String region) {
        this.defendantRegion = region;
    }

    public String getDefendantDriverNumber() {
        return defendantDriverNumber;
    }

    public void setDefendantDriverNumber(String driverNumber) {
        this.defendantDriverNumber = driverNumber;
    }

    public String getDefendantDriverLicenceDetails() {
        return defendantDriverLicenceDetails;
    }

    public void setDefendantDriverLicenceDetails(String driverLicenseDetails) {
        this.defendantDriverLicenceDetails = driverLicenseDetails;
    }

    public boolean hasDefendant(final UUID defendantId) {
        return offenceIdsByDefendantId.containsKey(defendantId);
    }

    public String getDefendantInterpreterLanguage(final UUID defendantID) {
        return defendantsInterpreterLanguages.get(defendantID);
    }

    public DisabilityNeeds getDefendantDisabilityNeeds(final UUID defendantID) {
        return defendantsDisabilityNeeds.get(defendantID);
    }

    public Map<UUID, DisabilityNeeds> getDefendantsDisabilityNeeds() {
        return unmodifiableMap(defendantsDisabilityNeeds);
    }

    public Boolean defendantSpeakWelsh(final UUID defendantID) {
        return defendantsSpeakWelsh.get(defendantID);
    }

    public void addCaseDocument(final UUID id, final CaseDocument caseDocument) {
        caseDocuments.put(id, caseDocument);
    }

    public void updateOffenceWithPlea(final UUID offenceId) {
        offenceIdsWithPleas.add(offenceId);
    }

    public void removePleaFromOffence(final UUID offenceId) {
        offenceIdsWithPleas.remove(offenceId);
    }

    public void updateDefendantInterpreterLanguage(final UUID defendantId, final Interpreter interpreter) {
        defendantsInterpreterLanguages.compute(
                defendantId,
                (defendant, previousValue) -> ofNullable(interpreter)
                        .map(Interpreter::getLanguage)
                        .orElse(null));
    }

    public void updateDefendantDisabilityNeeds(final UUID defendantId, final DisabilityNeeds disabilityNeeds) {
        defendantsDisabilityNeeds.put(defendantId, disabilityNeeds);
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
        return ofNullable(employmentStatusByDefendantId.get(defendantId));
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

    public void addWithdrawnOffences(final WithdrawalRequestsStatus withdrawalRequest) {
        withdrawalRequests.add(withdrawalRequest);
    }

    public Set<WithdrawalRequestsStatus> getWithdrawalRequests() {
        return unmodifiableSet(withdrawalRequests);
    }

    public void cancelWithdrawnOffence(final UUID offenceId) {
        withdrawalRequests.removeIf(withdrawalRequestsStatus -> withdrawalRequestsStatus.getOffenceId().equals(offenceId));
    }

    public void updateWithdrawnOffence(final WithdrawalRequestsStatus withdrawalRequest) {
        withdrawalRequests.removeIf(e -> e.getOffenceId().equals(withdrawalRequest.getOffenceId()));
        withdrawalRequests.add(withdrawalRequest);
    }

    public LocalDate getExpectedDateReady() {
        return expectedDateReady;
    }

    public void setExpectedDateReady(final LocalDate expectedDateReady) {
        this.expectedDateReady = expectedDateReady;
    }

    public Collection<OffenceDecision> getOffenceDecisions() {
        return unmodifiableCollection(offenceDecisionsByOffenceId.values());
    }

    public Map<UUID, OffenceDecision> getOffenceDecisionsWithOffenceIds() {
        return unmodifiableMap(offenceDecisionsByOffenceId);
    }

    public OffenceDecision getOffenceDecision(UUID offenceId) {
        return offenceDecisionsByOffenceId.get(offenceId);
    }

    public void updateOffenceDecisions(final List<OffenceDecision> offenceDecisions, final UUID sessionId) {
        offenceDecisions.forEach(
                offencesDecision -> offencesDecision.getOffenceIds().forEach(
                        offenceId -> {
                            this.offenceDecisionsByOffenceId.put(offenceId, offencesDecision);
                            if (isPressRestrictable(offenceId) && offencesDecision.hasPressRestriction()) {
                                this.offencesHavingPreviousPressRestriction.add(offenceId);
                            }
                        }
                )
        );

        this.sessionIds.add(sessionId);
    }

    public void clearOffenceDecisions() {
        this.offenceDecisionsByOffenceId.clear();
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(final UUID defendantId) {
        this.defendantId = defendantId;
    }

    public Set<UUID> getOffences() {
        return offenceIdsByDefendantId.get(defendantId);
    }

    public LocalDate getPostingDate() {
        return postingDate;
    }

    public void setPostingDate(final LocalDate postingDate) {
        this.postingDate = postingDate;
    }

    public boolean withdrawalRequestedOnAllOffences() {
        return this.getWithdrawalRequests().size()
                == offenceIdsByDefendantId  // as of now only single defendant so should be ok
                .values()
                .stream()
                .flatMap(Set::stream)
                .count();
    }

    public List<Plea> getPleas() {
        return pleas;
    }

    public boolean isPleaPresent() {
        return nonNull(this.pleas) && !this.getPleas().isEmpty();
    }

    public void setPleas(final List<Plea> pleas) {
        this.pleas = pleas;
    }

    public Map<UUID, LocalDate> getOffencePleaDates() {
        return offencePleaDates;
    }


    public void putOffencePleaDate(UUID offenceId, LocalDate pleaDate) {
        offencePleaDates.put(offenceId, pleaDate);
    }

    public void removeOffencePleaDate(UUID offenceId) {
        offencePleaDates.remove(offenceId);
    }

    public void markReady(final ZonedDateTime markedAt, final CaseReadinessReason readinessReason) {
        if (!isAlreadyMarkedAsReadyForDecision()) {
            this.markedReadyForDecision = markedAt;
        }
        this.setExpectedDateReady(null);
        this.readinessReason = readinessReason;
    }

    public boolean isAlreadyMarkedAsReadyForDecision() {
        return nonNull(this.readinessReason);
    }

    public void unmarkReady() {
        this.readinessReason = null;
    }

    public ZonedDateTime getMarkedReadyForDecision() {
        return markedReadyForDecision;
    }

    public boolean isDefendantsResponseTimerExpired() {
        return defendantsResponseTimerExpired;
    }

    public boolean isDatesToAvoidTimerExpired() {
        return isNull(datesToAvoidExpirationDate);
    }

    public boolean isAdjourned() {
        return nonNull(adjournedTo);
    }

    public LocalDate getAdjournedTo() {
        return adjournedTo;
    }

    public void setDefendantsResponseTimerExpired() {
        this.defendantsResponseTimerExpired = true;
    }

    public void makeNonAdjourned() {
        this.adjournedTo = null;
    }

    public void setAdjournedTo(final LocalDate adjournedTo) {
        this.adjournedTo = adjournedTo;
    }

    public Application getCurrentApplication() {
        return currentApplication;
    }

    public PleaType getPleaTypeForOffenceId(final UUID offenceId) {
        if (this.pleas == null) {
            return null;
        }

        return this.pleas.stream()
                .filter(plea -> nonNull(plea.getPleaType()))
                .filter(plea -> plea.getOffenceId().equals(offenceId))
                .map(Plea::getPleaType)
                .findFirst()
                .orElse(null);
    }

    public boolean isDatesToAvoidTimerPreviouslyStarted() {
        return datesToAvoidPreviouslyRequested;
    }

    public void setDatesToAvoidExpirationDate(final LocalDate datesToAvoidExpirationDate) {
        this.datesToAvoidExpirationDate = datesToAvoidExpirationDate;
    }

    public void setDatesToAvoidPreviouslyRequested() {
        this.datesToAvoidPreviouslyRequested = true;
    }

    public void datesToAvoidTimerExpired() {
        this.datesToAvoidExpirationDate = null;
    }

    public LocalDate getDatesToAvoidExpirationDate() {
        return datesToAvoidExpirationDate;
    }

    public boolean isPostConviction() {
        return !this.offenceConvictionDates.isEmpty() &&
                this.offenceConvictionDates.values()
                        .stream()
                        .anyMatch(Objects::nonNull);
    }

    public void deleteFinancialMeansData() {

        final List<UUID> uuids = this.caseDocuments.entrySet().stream()
                .map(Map.Entry::getValue)
                .filter(caseDocument ->
                        FINANCIAL_MEANS_DOCUMENT_TYPE
                                .equals(caseDocument.getDocumentType()))
                .map(CaseDocument::getId)
                .collect(toList());
        uuids.stream().forEach(this.caseDocuments::remove);
    }



    /**
     * Generates a DefendantDetailsUpdated containing a summary of the fields of the defendant which
     * have been updated by the provided personal details.
     *
     * @param personalDetails     personal details containing the new defendant details (if any)
     * @param updatedByOnlinePlea wether the update comes from online plea
     * @param updatedOn           time when the update happened
     * @return DefendantDetailsUpdate cotaining values for the updated of fields or null if no
     * fields were updated.
     */
    public DefendantDetailsUpdated getDefendantDetailsUpdateSummary(final PersonalDetails personalDetails,
                                                                    final boolean updatedByOnlinePlea,
                                                                    final ZonedDateTime updatedOn) {

        final DefendantDetailsUpdated.DefendantDetailsUpdatedBuilder builder = defendantDetailsUpdated()
                .withCaseId(getCaseId())
                .withDefendantId(getDefendantId());

        getNameAndTitleDetailsUpdateSummary(personalDetails, updatedByOnlinePlea, builder);
        getAddressAndContactDetailsUpdateSummary(personalDetails, builder);

        if (!Objects.equals(personalDetails.getDateOfBirth(), getDefendantDateOfBirth())) {
            builder.withDateOfBirth(personalDetails.getDateOfBirth());
        }

        if (!StringUtils.equals(personalDetails.getNationalInsuranceNumber(), getDefendantNationalInsuranceNumber())) {
            builder.withNationalInsuranceNumber(personalDetails.getNationalInsuranceNumber());
        }

        if (!StringUtils.equals(personalDetails.getRegion(), getDefendantRegion())) {
            builder.withRegion(personalDetails.getRegion());
        }

        if (!StringUtils.equals(personalDetails.getDriverNumber(), getDefendantDriverNumber())) {
            builder.withDriverNumber(personalDetails.getDriverNumber());
        }

        if (!StringUtils.equals(personalDetails.getDriverLicenceDetails(), getDefendantDriverLicenceDetails())) {
            builder.withDriverLicenceDetails(personalDetails.getDriverLicenceDetails());
        }

        if (builder.containsUpdate()) {
            return builder
                    .withUpdateByOnlinePlea(updatedByOnlinePlea)
                    .withUpdatedDate(updatedOn)
                    .build();
        } else {
            return null;
        }
    }

    private void getAddressAndContactDetailsUpdateSummary(final PersonalDetails personalDetails, final DefendantDetailsUpdated.DefendantDetailsUpdatedBuilder builder) {
        if (!Objects.equals(personalDetails.getContactDetails(), getDefendantContactDetails())) {
            builder.withContactDetails(personalDetails.getContactDetails());
        }

        if (!Objects.equals(personalDetails.getAddress(), getDefendantAddress())) {
            builder.withAddress(personalDetails.getAddress());
        }
    }

    private void getNameAndTitleDetailsUpdateSummary(final PersonalDetails personalDetails, final boolean updatedByOnlinePlea, final DefendantDetailsUpdated.DefendantDetailsUpdatedBuilder builder) {
        if (!StringUtils.equals(personalDetails.getFirstName(), getDefendantFirstName())) {
            builder.withFirstName(personalDetails.getFirstName());
        }

        if (!StringUtils.equals(personalDetails.getLastName(), getDefendantLastName())) {
            builder.withLastName(personalDetails.getLastName());
        }

        if (!updatedByOnlinePlea && !StringUtils.equals(personalDetails.getTitle(), getDefendantTitle())) {
            builder.withTitle(personalDetails.getTitle());
        }

        if (personalDetails.getGender() != null && !Objects.equals(personalDetails.getGender(), getDefendantGender())) {
            builder.withGender(personalDetails.getGender());
        }
    }

    public Set<UUID> getSessionIds() {
        return unmodifiableSet(sessionIds);
    }

    public void updateOffenceConvictionDates(final ZonedDateTime decisionSavedAt, final List<OffenceDecision> offenceDecisions) {
        final boolean setAsideDecision = offenceDecisions
                .stream()
                .allMatch(e -> e.getType().equals(DecisionType.SET_ASIDE));

        if (setAsideDecision) {
            this.clearOffenceConvictionDates();
        } else {
            offenceDecisions.forEach(
                    offencesDecision -> offencesDecision.getOffenceIds().forEach(
                            offenceId -> {
                                if (offencesDecision.isConviction(offenceId)) {
                                    this.offenceConvictionDates.put(offenceId, decisionSavedAt);
                                }
                            }
                    )
            );
        }
    }

    public boolean offenceHasPreviousConviction(final UUID offenceId) {
        return offenceConvictionDates.containsKey(offenceId);
    }

    public Set<UUID> getOffencesWithConviction() {
        return offenceConvictionDates.keySet();
    }

    public ZonedDateTime getOffencePreviousConvictionDate(final UUID offenceId) {
        return offenceConvictionDates.get(offenceId);
    }

    public void clearOffenceConvictionDates() {
        offenceConvictionDates.clear();
    }

    public void removeOffenceConvictionDate(final UUID offenceId) {
        this.offenceConvictionDates.remove(offenceId);
    }

    public boolean isSetAside() {
        return setAside;
    }

    public boolean isDeleteDocsStarted() {
        return deleteDocsStarted;
    }

    public void setSetAside(final boolean setAside) {
        this.setAside = setAside;
    }

    public void markOffenceAsPressRestrictable(final UUID offenceId) {
        pressRestrictableOffenceIds.add(offenceId);
    }

    public boolean isPressRestrictable(final UUID offenceId) {
        return pressRestrictableOffenceIds.contains(offenceId);
    }

    public boolean hasPreviousPressRestriction(final UUID offenceId) {
        return offencesHavingPreviousPressRestriction.contains(offenceId);
    }

    public boolean isManagedByAtcm() { return managedByAtcm; }

    public void setManagedByAtcm(final boolean managedByAtcm) { this.managedByAtcm = managedByAtcm; }

    public BigDecimal getCosts() {
        return costs;
    }

    public void setCosts(final BigDecimal costs) {
        this.costs = costs;
    }

    public void setDeleteDocsStarted(final boolean deleteDocsStarted) {
        this.deleteDocsStarted = deleteDocsStarted;
    }

    public void setCurrentApplication(final Application currentApplication) {
        this.currentApplication = currentApplication;
    }

    public boolean hasPendingApplication() {
        return this.hasApplicationWithOneStatusOf(PENDING_APPLICATION_STATUSES);
    }

    public boolean hasGrantedApplication() {
        return this.hasApplicationWithOneStatusOf(GRANTED_APPLICATION_STATUSES);
    }

    public boolean hasRefusedApplication() {
        return this.hasApplicationWithOneStatusOf(REFUSED_APPLICATION_STATUSES);
    }

    private boolean hasApplicationWithOneStatusOf(final List<ApplicationStatus> possibleStatus) {
        return ofNullable(this.currentApplication)
                .map(Application::getStatus)
                .map(possibleStatus::contains)
                .orElse(false);
    }

    public void removePlea(final UUID defendantId, final UUID offenceId) {
        final Optional<Plea> plea = this.getPleas().stream()
                .filter(pl -> pl.getDefendantId().equals(defendantId) && pl.getOffenceId().equals(offenceId))
                .findFirst();
        plea.ifPresent(pl -> this.getPleas().remove(pl));
    }

    public boolean isCaseRelisted() {
        return caseRelisted;
    }

    public void setCaseRelisted(boolean caseRelisted) {
        this.caseRelisted = caseRelisted;
    }

    public boolean isCaseAppealed() {
        return caseAppealed;
    }

    public void setCaseAppealed(boolean caseAppealed) {
        this.caseAppealed = caseAppealed;
    }
}
