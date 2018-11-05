package uk.gov.moj.cpp.sjp.persistence.entity;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import uk.gov.justice.services.common.jpa.converter.LocalDatePersistenceConverter;
import uk.gov.moj.cpp.sjp.domain.AssignmentCandidate;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.persistence.entity.view.CaseCountByAgeView;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

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
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.ToStringBuilder;

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

    @Column(name = "id")
    @Id
    private UUID id;

    @Column(name = "enterprise_id")
    private String enterpriseId;

    @Column(name = "urn")
    private String urn;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "caseId")
    private Set<CaseDocument> caseDocuments = new LinkedHashSet<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "caseId")
    private Set<CaseSearchResult> caseSearchResults = new LinkedHashSet<>();

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "caseDetail")
    private DefendantDetail defendant = new DefendantDetail();

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

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private CaseStatus status;

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
                      final ProsecutingAuthority prosecutingAuthority,
                      final Boolean completed,
                      final UUID assigneeId,
                      final ZonedDateTime createdOn, final DefendantDetail defendantDetail, final BigDecimal costs, final LocalDate postingDate) {
        this(id);
        this.urn = urn;
        this.enterpriseId = enterpriseId;
        this.prosecutingAuthority = prosecutingAuthority == null ? null : prosecutingAuthority.name();
        this.completed = completed;
        this.assigneeId = assigneeId;
        this.dateTimeCreated = createdOn;
        setDefendant(defendantDetail);
        this.costs = costs;
        this.postingDate = postingDate;
    }

    public void acknowledgeDefendantDetailsUpdates(ZonedDateTime acknowledgedAt) {
        defendant.acknowledgeDetailsUpdates(acknowledgedAt);
    }

    public String getUrn() {
        return urn;
    }

    public void setUrn(String urn) {
        this.urn = urn;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Set<CaseDocument> getCaseDocuments() {
        return caseDocuments;
    }

    public void addCaseDocuments(CaseDocument caseDocument) {
        this.caseDocuments.add(caseDocument);
        caseDocument.setCaseId(this.id);
    }

    public DefendantDetail getDefendant() {
        return defendant;
    }

    public void setDefendant(DefendantDetail defendantDetail) {
        Objects.requireNonNull(defendantDetail);
        defendantDetail.setCaseDetail(this);
        this.defendant = defendantDetail;
    }

    public ZonedDateTime getDateTimeCreated() {
        return dateTimeCreated;
    }

    public void setDateTimeCreated(ZonedDateTime dateTimeCreated) {
        this.dateTimeCreated = dateTimeCreated;
    }

    public ProsecutingAuthority getProsecutingAuthority() {
        return this.prosecutingAuthority == null ? null : ProsecutingAuthority.valueOf(prosecutingAuthority);
    }

    public void setProsecutingAuthority(ProsecutingAuthority prosecutingAuthority) {
        this.prosecutingAuthority = prosecutingAuthority == null ? null : prosecutingAuthority.name();
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
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

    public void setCosts(BigDecimal costs) {
        this.costs = costs;
    }

    public LocalDate getPostingDate() {
        return postingDate;
    }

    public void setPostingDate(LocalDate postingDate) {
        this.postingDate = postingDate;
    }

    public LocalDate getReopenedDate() {
        return reopenedDate;
    }

    public CaseDetail setReopenedDate(LocalDate reopenedDate) {
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

    public void setReopenedInLibraReason(String reopenedInLibraReason) {
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

    public void setEnterpriseId(String enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public Set<CaseSearchResult> getCaseSearchResults() {
        return caseSearchResults;
    }

    public void setCaseSearchResults(Set<CaseSearchResult> caseSearchResults) {
        this.caseSearchResults = caseSearchResults;
    }

    public String getDatesToAvoid() {
        return datesToAvoid;
    }

    public void setDatesToAvoid(final String datesToAvoid) {
        this.datesToAvoid = datesToAvoid;
    }

    public CaseStatus getStatus() {
        return status;
    }

    public void setStatus(CaseStatus status) {
        this.status = status;
    }

    public void markDefendantNameUpdated(ZonedDateTime updateDate) {
        defendant.markNameUpdated(updateDate);
    }

    public void markDefendantAddressUpdated(ZonedDateTime updateDate) {
        defendant.markAddressUpdated(updateDate);
    }

    public void markDefendantDateOfBirthUpdated(ZonedDateTime updateDate) {
        defendant.markDateOfBirthUpdated(updateDate);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

}
