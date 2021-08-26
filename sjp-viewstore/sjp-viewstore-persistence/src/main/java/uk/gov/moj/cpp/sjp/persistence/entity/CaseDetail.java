package uk.gov.moj.cpp.sjp.persistence.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import uk.gov.justice.services.common.jpa.converter.LocalDatePersistenceConverter;
import uk.gov.moj.cpp.sjp.domain.AssignmentCandidate;
import uk.gov.moj.cpp.sjp.domain.common.CaseManagementStatus;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.persistence.entity.view.CaseCountByAgeView;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.google.common.collect.ImmutableList.copyOf;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.NO_PLEA_RECEIVED;

@SqlResultSetMappings({
        @SqlResultSetMapping(
                name = "caseCountByAge",
                classes = @ConstructorResult(
                        targetClass = CaseCountByAgeView.class,
                        columns = {
                                @ColumnResult(name = "age", type = Integer.class),
                                @ColumnResult(name = "count", type = Integer.class)
                        })),
        @SqlResultSetMapping(
                name = CaseDetail.RESULT_SET_MAPPING_ASSIGNMENT_CANDIDATES,
                classes = @ConstructorResult(
                        targetClass = AssignmentCandidate.class,
                        columns = {
                                @ColumnResult(name = "case_id", type = UUID.class),
                                @ColumnResult(name = "case_stream_version", type = Integer.class)
                        }))
})
@Entity
@Table(name = "case_details")
public class CaseDetail implements Serializable {

    public static final String RESULT_SET_MAPPING_ASSIGNMENT_CANDIDATES = "assignmentCandidates";

    private static final long serialVersionUID = -5400670786416162433L;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "caseId")
    private final Set<CaseDocument> caseDocuments = new LinkedHashSet<>();
    @Column(name = "id")
    @Id
    private UUID id;
    @Column(name = "enterprise_id")
    private String enterpriseId;
    @Column(name = "urn")
    private String urn;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "caseId")
    private Set<CaseSearchResult> caseSearchResults = new LinkedHashSet<>();

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "caseDetail")
    private DefendantDetail defendant = new DefendantDetail();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "caseId")
    private List<CaseDecision> caseDecisions = new ArrayList<>();

    @Column(name = "date_time_created")
    private ZonedDateTime dateTimeCreated;

    @Column(name = "prosecuting_authority")
    private String prosecutingAuthority;

    @Column(name = "completed")
    private Boolean completed = Boolean.FALSE;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "costs")
    private BigDecimal costs;

    @Column(name = "posting_date")
    @Convert(converter = LocalDatePersistenceConverter.class)
    private LocalDate postingDate;

    @Column(name = "reopened_date")
    private LocalDate reopenedDate;

    @Column(name = "libra_case_number")
    private String libraCaseNumber;

    @Column(name = "reopened_in_libra_reason")
    private String reopenedInLibraReason;

    @Column(name = "online_plea_received")
    private Boolean onlinePleaReceived = Boolean.FALSE;

    @Column(name = "dates_to_avoid")
    private String datesToAvoid;

    @Column(name = "listed_in_criminal_courts")
    private Boolean listedInCriminalCourts = Boolean.FALSE;

    @Column(name = "referred_for_court_hearing")
    private Boolean referredForCourtHearing;

    @Column(name = "hearing_court_name")
    private String hearingCourtName;

    @Column(name = "hearing_time")
    private ZonedDateTime hearingTime;

    @Column(name = "adjourned_to")
    private LocalDate adjournedTo;

    @Column(name = "set_aside")
    private Boolean setAside;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_status")
    private CaseStatus caseStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_management_status")
    private CaseManagementStatus caseManagementStatus;

    @Column(name = "managed_by_atcm")
    private Boolean managedByAtcm;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "current_application_id")
    private CaseApplication currentApplication;

    @OneToMany(mappedBy = "caseDetail")
    private List<CaseApplication> applications;

    @Column(name = "cc_application_status")
    @Enumerated(EnumType.STRING)
    private ApplicationStatus ccApplicationStatus;

    public CaseDetail() {
        defendant.setCaseDetail(this);
    }

    public CaseDetail(final UUID id) {
        this();
        this.id = id;
    }

    public CaseDetail(final UUID id,
                      final String urn,
                      final String enterpriseId,
                      final String prosecutingAuthority,
                      final Boolean completed,
                      final UUID assigneeId,
                      final ZonedDateTime createdOn,
                      final DefendantDetail defendantDetail,
                      final BigDecimal costs,
                      final LocalDate postingDate) {
        this(id);
        this.urn = urn;
        this.enterpriseId = enterpriseId;
        this.prosecutingAuthority = prosecutingAuthority;
        this.completed = completed;
        this.assigneeId = assigneeId;
        this.dateTimeCreated = createdOn;
        setDefendant(defendantDetail);
        this.costs = costs;
        this.postingDate = postingDate;
        this.caseStatus = NO_PLEA_RECEIVED;
    }

    public void acknowledgeDefendantDetailsUpdates(final ZonedDateTime acknowledgedAt) {
        defendant.acknowledgeDetailsUpdates(acknowledgedAt);
    }

    public String getUrn() {
        return urn;
    }

    public void setUrn(final String urn) {
        this.urn = urn;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public Set<CaseDocument> getCaseDocuments() {
        return caseDocuments;
    }

    public void addCaseDocuments(final CaseDocument caseDocument) {
        this.caseDocuments.add(caseDocument);
        caseDocument.setCaseId(this.id);
    }

    public DefendantDetail getDefendant() {
        return defendant;
    }

    public void setDefendant(final DefendantDetail defendantDetail) {
        Objects.requireNonNull(defendantDetail);
        defendantDetail.setCaseDetail(this);
        this.defendant = defendantDetail;
    }

    public List<CaseDecision> getCaseDecisions() {
        return copyOf(caseDecisions);
    }

    public void setCaseDecisions(List<CaseDecision> caseDecisions) {
        this.caseDecisions = copyOf(caseDecisions);
    }

    public ZonedDateTime getDateTimeCreated() {
        return dateTimeCreated;
    }

    public void setDateTimeCreated(final ZonedDateTime dateTimeCreated) {
        this.dateTimeCreated = dateTimeCreated;
    }

    public String getProsecutingAuthority() {
        return this.prosecutingAuthority;
    }

    public void setProsecutingAuthority(final String prosecutingAuthority) {
        this.prosecutingAuthority = prosecutingAuthority;
    }

    public boolean isCompleted() {
        return isTrue(completed);
    }

    public void setCompleted(final Boolean completed) {
        this.completed = completed;
    }

    public UUID getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(final UUID assigneeId) {
        this.assigneeId = assigneeId;
    }

    public BigDecimal getCosts() {
        return costs;
    }

    public void setCosts(final BigDecimal costs) {
        this.costs = costs;
    }

    public LocalDate getPostingDate() {
        return postingDate;
    }

    public void setPostingDate(final LocalDate postingDate) {
        this.postingDate = postingDate;
    }

    public LocalDate getReopenedDate() {
        return reopenedDate;
    }

    public CaseDetail setReopenedDate(final LocalDate reopenedDate) {
        this.reopenedDate = reopenedDate;
        return this;
    }

    public String getLibraCaseNumber() {
        return libraCaseNumber;
    }

    public CaseDetail setLibraCaseNumber(final String libraCaseNumber) {
        this.libraCaseNumber = libraCaseNumber;
        return this;
    }

    public String getReopenedInLibraReason() {
        return reopenedInLibraReason;
    }

    public void setReopenedInLibraReason(final String reopenedInLibraReason) {
        this.reopenedInLibraReason = reopenedInLibraReason;
    }

    public void undoReopenCase() {
        this.reopenedDate = null;
        this.reopenedInLibraReason = null;
        this.libraCaseNumber = null;
    }

    public Boolean getOnlinePleaReceived() {
        return onlinePleaReceived;
    }

    public void setOnlinePleaReceived(final Boolean onlinePleaReceived) {
        this.onlinePleaReceived = onlinePleaReceived;
    }

    public String getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(final String enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public Set<CaseSearchResult> getCaseSearchResults() {
        return caseSearchResults;
    }

    public void setCaseSearchResults(final Set<CaseSearchResult> caseSearchResults) {
        this.caseSearchResults = caseSearchResults;
    }

    public String getDatesToAvoid() {
        return datesToAvoid;
    }

    public void setDatesToAvoid(final String datesToAvoid) {
        this.datesToAvoid = datesToAvoid;
    }

    public Boolean getListedInCriminalCourts() {
        return listedInCriminalCourts;
    }

    public void setListedInCriminalCourts(final Boolean listedInCriminalCourts) {
        this.listedInCriminalCourts = listedInCriminalCourts;
    }

    public void markDefendantNameUpdated(final ZonedDateTime updateDate) {
        defendant.markNameUpdated(updateDate);
    }

    public void markDefendantAddressUpdated(final ZonedDateTime updateDate) {
        defendant.markAddressUpdated(updateDate);
    }

    public void markDefendantDateOfBirthUpdated(final ZonedDateTime updateDate) {
        defendant.markDateOfBirthUpdated(updateDate);
    }

    public boolean isReferredForCourtHearing() {
        return isTrue(referredForCourtHearing);
    }

    public void setReferredForCourtHearing(final Boolean referredForCourtHearing) {
        this.referredForCourtHearing = referredForCourtHearing;
    }

    public String getHearingCourtName() {
        return hearingCourtName;
    }

    public void setHearingCourtName(final String hearingCourtName) {
        this.hearingCourtName = hearingCourtName;
    }

    public LocalDate getAdjournedTo() {
        return adjournedTo;
    }

    public void setAdjournedTo(final LocalDate adjournedTo) {
        this.adjournedTo = adjournedTo;
    }

    public ZonedDateTime getHearingTime() {
        return hearingTime;
    }

    public void setHearingTime(final ZonedDateTime hearingTime) {
        this.hearingTime = hearingTime;
    }

    public CaseStatus getCaseStatus() {
        return caseStatus;
    }

    public void setCaseManagementStatus(final CaseManagementStatus caseManagementStatus) {
        this.caseManagementStatus = caseManagementStatus;
    }

    public CaseManagementStatus getCaseManagementStatus() {
        return caseManagementStatus;
    }

    public Boolean getSetAside() {
        return setAside;
    }

    public void setSetAside(final Boolean setAside) {
        this.setAside = setAside;
    }

    public void setCaseStatus(final CaseStatus caseStatus) {
        this.caseStatus = caseStatus;
    }

    public Boolean getManagedByAtcm() { return managedByAtcm; }

    public void setManagedByAtcm(final Boolean managedByATCM) { this.managedByAtcm = managedByATCM; }

    public CaseApplication getCurrentApplication() {
        return currentApplication;
    }

    public void setCurrentApplication(final CaseApplication currentApplication) {
        this.currentApplication = currentApplication;
    }

    public ApplicationStatus getCcApplicationStatus() {
        return ccApplicationStatus;
    }

    public void setCcApplicationStatus(ApplicationStatus ccApplicationStatus) {
        this.ccApplicationStatus = ccApplicationStatus;
    }

    @SuppressWarnings("squid:S2384")
    public List<CaseApplication> getApplications() {
        return applications;
    }

    @SuppressWarnings("squid:S2384")
    public void setApplications(final List<CaseApplication> applications) {
        this.applications = applications;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

}
