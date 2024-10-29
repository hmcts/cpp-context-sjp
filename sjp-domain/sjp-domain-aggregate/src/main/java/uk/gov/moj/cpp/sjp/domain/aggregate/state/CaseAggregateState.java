package uk.gov.moj.cpp.sjp.domain.aggregate.state;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
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
import uk.gov.moj.cpp.sjp.domain.AOCPCost;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.CaseDocument;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.ContactDetails;
import uk.gov.moj.cpp.sjp.domain.Interpreter;
import uk.gov.moj.cpp.sjp.domain.aggregate.domain.DocumentCountByDocumentType;
import uk.gov.moj.cpp.sjp.domain.decision.ConvictingDecision;
import uk.gov.moj.cpp.sjp.domain.decision.ConvictingInformation;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.ReferForCourtHearing;
import uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds;
import uk.gov.moj.cpp.sjp.domain.legalentity.LegalEntityDefendant;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PersonalDetails;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.ApplicationResultsRecorded;
import uk.gov.moj.cpp.sjp.event.CaseOffenceListedInCriminalCourts;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;

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

import org.apache.commons.lang3.StringUtils;

/**
 * Defines the case aggregate state.
 */
@SuppressWarnings({"PMD.BeanMembersShouldSerialize",  "pmd:NullAssignment", "squid:S2384"})
public class CaseAggregateState implements AggregateState {


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
    private String defendantLegalEntityName;

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
    private UUID pcqId;

    private ZonedDateTime markedReadyForDecision;

    private String datesToAvoid;

    private final Map<UUID, Set<UUID>> offenceIdsByDefendantId = new HashMap<>();

    private final Map<UUID, CaseDocument> caseDocuments = new HashMap<>();
    private final Set<UUID> offenceIdsWithPleas = new HashSet<>();
    private final Map<UUID, LocalDate> offencePleaDates = new HashMap<>();

    private final Map<UUID, String> defendantsInterpreterLanguages = new HashMap<>();
    private final Map<UUID, Boolean> defendantsSpeakWelsh = new HashMap<>();
    private static final Map<UUID, AOCPCost> aocpCostMap = new HashMap<>();

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
    private final Map<UUID, ConvictingInformation> offenceConvictionDetails = new HashMap<>();

    private Set<UUID> sessionIds = new HashSet<>();

    private boolean defendantsResponseTimerExpired;
    private boolean aocpAcceptanceResponseTimerExpired;
    private boolean defendantAcceptedAocp;
    private ZonedDateTime aocpAcceptedPleaDate;
    private boolean datesToAvoidPreviouslyRequested;
    private LocalDate datesToAvoidExpirationDate;
    private LocalDate adjournedTo;
    private String defendantRegion;
    private String defendantDriverNumber;
    private String defendantDriverLicenceDetails;
    private boolean setAside;
    private boolean deleteDocsStarted;
    private Application currentApplication;
    private boolean aocpEligible;
    private BigDecimal aocpTotalCost;
    private BigDecimal aocpVictimSurcharge;

    private boolean managedByAtcm;
    private boolean paymentTermsUpdated;

    private boolean decisionResubmitted;
    private boolean correlationIdAllreadyGenerated;

    private final Set<UUID> pressRestrictableOffenceIds = new HashSet<>();
    private final Set<UUID> offencesHavingPreviousPressRestriction = new HashSet<>();
    private final Map<UUID, DisabilityNeeds> defendantsDisabilityNeeds = new HashMap<>();
    private final Map<UUID, FinancialImpositionExportDetails> defendantFinancialImpositionExportDetails = new HashMap<>();

    private DecisionSaved decisionSavedWithFinancialImposition;

    private final List<CaseOffenceListedInCriminalCourts> offenceHearings = new ArrayList<>();
    private DecisionSaved latestReferToCourtDecision;
    private boolean caseListed;
    private boolean caseReserved;
    private LocalDate savedAt;
    private ApplicationResultsRecorded applicationResults;

    public ApplicationResultsRecorded getApplicationResults() {return applicationResults;}

    public void setApplicationResults(final ApplicationResultsRecorded applicationResults) {this.applicationResults = applicationResults;}

    public LocalDate getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(final LocalDate savedAt) {
        this.savedAt = savedAt;
    }

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

    public String getDefendantLegalEntityName() {
        return defendantLegalEntityName;
    }

    public void setDefendantLegalEntityName(final String defendantLegalEntityName) {
        this.defendantLegalEntityName = defendantLegalEntityName;
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

    public void setPcqId(final UUID pcqId) {
        this.pcqId = pcqId;
    }

    public UUID getPcqId() {
        return pcqId;
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

    public void updateOffenceHearings(CaseOffenceListedInCriminalCourts event) {
        this.offenceHearings.add(event);
    }

    public void markCaseListed() {
        this.caseListed = true;
    }

    public boolean getCaseListed() {
        return this.caseListed;
    }

    public void markCaseReserved() {
        this.caseReserved = true;
    }

    public void markCaseUnReserved() {
        this.caseReserved = false;
    }

    public boolean getCaseReserved() {
        return this.caseReserved;
    }

    public boolean checkAllOffencesHavingHearings() {
        return this.getLatestReferToCourtDecision()
                .getOffenceDecisions()
                .stream()
                .filter(e -> e instanceof ReferForCourtHearing)
                .flatMap(e -> e.getOffenceIds().stream())
                .allMatch(offenceId -> offenceHearings
                        .stream()
                        .anyMatch(e -> e.getDefendantOffences().contains(offenceId)));
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

    public void addAOCPCost(final UUID caseId, final AOCPCost aocpCost) {
        aocpCostMap.put(caseId, aocpCost);
    }

    public Map<UUID, AOCPCost> getAOCPCost() {
        return aocpCostMap;
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

    public void setAocpAcceptanceResponseTimerExpired() {
        this.aocpAcceptanceResponseTimerExpired = true;
    }

    public boolean isAocpAcceptanceResponseTimerExpired() {
        return aocpAcceptanceResponseTimerExpired;
    }

    public void setDefendantAcceptedAocp(final boolean defendantAcceptedAocp) {
        this.defendantAcceptedAocp = defendantAcceptedAocp;
    }

    public boolean isDefendantAcceptedAocp() {
        return defendantAcceptedAocp;
    }


    public void setAocpEligible() {
        this.aocpEligible = true;
    }

    public boolean isAocpEligible() {
        return this.aocpEligible;
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
        return !this.offenceConvictionDetails.isEmpty() &&
                this.offenceConvictionDetails.values()
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

        getTitleDetailsUpdateSummary(personalDetails, updatedByOnlinePlea, builder);
        getContactDetailsUpdateSummary(personalDetails, builder);

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

    public DefendantDetailsUpdated getDefendantLegalEntityDetailsUpdateSummary(final LegalEntityDefendant legalEntityDefendant,
                                                                    final boolean updatedByOnlinePlea,
                                                                    final ZonedDateTime updatedOn) {

        final DefendantDetailsUpdated.DefendantDetailsUpdatedBuilder builder = defendantDetailsUpdated()
                .withCaseId(getCaseId())
                .withDefendantId(getDefendantId());

        getContactDetailsUpdateSummary(legalEntityDefendant, builder);

        if (builder.containsUpdate()) {
            return builder
                    .withUpdateByOnlinePlea(updatedByOnlinePlea)
                    .withUpdatedDate(updatedOn)
                    .build();
        } else {
            return null;
        }
    }

    public DefendantDetailsUpdated getDefendantDetailsUpdated(final boolean updatedByOnlinePlea,
                                                              final ZonedDateTime updatedOn,
                                                              final UUID pcqId) {

        final DefendantDetailsUpdated.DefendantDetailsUpdatedBuilder builder = defendantDetailsUpdated()
                .withCaseId(getCaseId())
                .withPcqId(pcqId)
                .withDefendantId(getDefendantId());

        if (builder.containsUpdate()) {
            return builder
                    .withUpdateByOnlinePlea(updatedByOnlinePlea)
                    .withUpdatedDate(updatedOn)
                    .build();
        } else {
            return null;
        }
    }

    private void getContactDetailsUpdateSummary(final PersonalDetails personalDetails, final DefendantDetailsUpdated.DefendantDetailsUpdatedBuilder builder) {
        if (!Objects.equals(personalDetails.getContactDetails(), getDefendantContactDetails())) {
            builder.withContactDetails(personalDetails.getContactDetails());
        }
    }

    private void getContactDetailsUpdateSummary(final LegalEntityDefendant legalEntityDefendant, final DefendantDetailsUpdated.DefendantDetailsUpdatedBuilder builder) {
        if (!Objects.equals(legalEntityDefendant.getContactDetails(), getDefendantContactDetails())) {
            builder.withContactDetails(legalEntityDefendant.getContactDetails());
        }
    }

    private void getTitleDetailsUpdateSummary(final PersonalDetails personalDetails, final boolean updatedByOnlinePlea, final DefendantDetailsUpdated.DefendantDetailsUpdatedBuilder builder) {
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

    public void updateOffenceConvictionDetails(final ZonedDateTime decisionSavedAt, final List<OffenceDecision> offenceDecisions, final UUID sessionId) {
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
                                    this.offenceConvictionDetails.put(offenceId, new ConvictingInformation(decisionSavedAt, ((ConvictingDecision) offencesDecision).getConvictingCourt(), sessionId, offenceId));
                                }
                            }
                    )
            );
        }
    }

    public void resolveConvictionCourtDetails(final List<ConvictingInformation> convictingInformation) {
        convictingInformation.forEach(
                convictingInfo ->
                        this.offenceConvictionDetails.put(convictingInfo.getOffenceId(), convictingInfo)
        );
    }

    public boolean offenceHasPreviousConviction(final UUID offenceId) {
        return offenceConvictionDetails.containsKey(offenceId);
    }

    public Set<UUID> getOffencesWithConviction() {
        return offenceConvictionDetails.keySet();
    }

    public ConvictingInformation getOffenceConvictionInfo(final UUID offenceId) {
        return offenceConvictionDetails.get(offenceId);
    }

    public void clearOffenceConvictionDates() {
        offenceConvictionDetails.clear();
    }

    public void removeOffenceConvictionDate(final UUID offenceId) {
        this.offenceConvictionDetails.remove(offenceId);
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

    public boolean isManagedByAtcm() {
        return managedByAtcm;
    }

    public void setManagedByAtcm(final boolean managedByAtcm) {
        this.managedByAtcm = managedByAtcm;
    }

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

    public DecisionSaved getDecisionSavedWithFinancialImposition() {
        return decisionSavedWithFinancialImposition;
    }

    public void setDecisionSavedWithFinancialImposition(final DecisionSaved decisionSavedWithFinancialImposition) {
        this.decisionSavedWithFinancialImposition = decisionSavedWithFinancialImposition;
    }

    public boolean isPaymentTermsUpdated() {
        return paymentTermsUpdated;
    }

    public boolean isDecisionResubmitted() {
        return decisionResubmitted;
    }

    public boolean isCorrelationIdAllreadyGenerated() {
        return correlationIdAllreadyGenerated;
    }

    public void setPaymentTermsUpdated(final boolean paymentTermsUpdated) {
        this.paymentTermsUpdated = paymentTermsUpdated;
    }

    public void setDecisionResubmitted(final boolean decisionResubmitted) {
        this.decisionResubmitted = decisionResubmitted;
    }

    public void setFinancialImpositionCorelationId(final boolean correlationIdAllreadyGenerated) {
        this.correlationIdAllreadyGenerated = correlationIdAllreadyGenerated;
    }


    public DecisionSaved getLatestReferToCourtDecision() {
        return latestReferToCourtDecision;
    }

    public void setLatestReferToCourtDecision(final DecisionSaved latestReferToCourtDecision) {
        this.latestReferToCourtDecision = latestReferToCourtDecision;
    }

    public List<CaseOffenceListedInCriminalCourts> getOffenceHearings() {
        return unmodifiableList(offenceHearings);
    }

    public void setAocpTotalCost(final BigDecimal aocpTotalCost){
        this.aocpTotalCost = aocpTotalCost;
    }

    public BigDecimal getAocpTotalCost(){
        return this.aocpTotalCost;
    }

    public void setAocpVictimSurcharge(final BigDecimal aocpVictimSurcharge){
        this.aocpVictimSurcharge = aocpVictimSurcharge;
    }

    public BigDecimal getAocpVictimSurcharge(){
        return this.aocpVictimSurcharge;
    }

    public ZonedDateTime getAocpAcceptedPleaDate() {
        return aocpAcceptedPleaDate;
    }

    public void setAocpAcceptedPleaDate(final ZonedDateTime aocpAcceptedPleaDate) {
        this.aocpAcceptedPleaDate = aocpAcceptedPleaDate;
    }

}
