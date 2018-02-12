package uk.gov.moj.cpp.sjp.persistence.entity;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import uk.gov.justice.services.common.jpa.converter.LocalDatePersistenceConverter;
import uk.gov.moj.cpp.sjp.persistence.entity.view.CaseCountByAgeView;
import uk.gov.moj.cpp.sjp.persistence.entity.view.CaseReferredToCourt;

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
                name = "caseReferredToCourt",
                classes = @ConstructorResult(
                        targetClass = CaseReferredToCourt.class,
                        columns = {
                                @ColumnResult(name = "case_id", type = UUID.class),
                                @ColumnResult(name = "urn", type = String.class),
                                @ColumnResult(name = "first_name", type = String.class),
                                @ColumnResult(name = "last_name", type = String.class),
                                @ColumnResult(name = "interpreter_language", type = String.class),
                                @ColumnResult(name = "hearing_date", type = LocalDate.class)
                        }))
})
@Entity
@Table(name = "case_details")
public class CaseDetail implements Serializable {

    private static final long serialVersionUID = -5400670786416162433L;

    @Column(name = "id")
    @Id
    private UUID id;

    @Column(name = "enterprise_id")
    private String enterpriseId;

    @Column(name = "urn")
    private String urn;

    @Column(name = "pti_urn")
    private String ptiUrn;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "caseId")
    private Set<CaseDocument> caseDocuments = new LinkedHashSet<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "caseId")
    private Set<CaseSearchResult> caseSearchResults = new LinkedHashSet<>();

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "caseDetail")
    private DefendantDetail defendant = new DefendantDetail();

    @Column(name = "date_time_created")
    private ZonedDateTime dateTimeCreated;

    @Column(name = "initiation_code")
    private String initiationCode;

    @Column(name = "prosecuting_authority")
    private String prosecutingAuthority;

    @Column(name = "completed")
    private Boolean completed = Boolean.FALSE;

    @Column(name = "assigned")
    private Boolean assigned = Boolean.FALSE;

    @Column(name = "summons_code")
    private String summonsCode;

    @Column(name = "libra_originating_org")
    private String libraOriginatingOrg;

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

    public CaseDetail() {
        defendant.setCaseDetail(this);
    }

    public CaseDetail(final UUID id,
                      final String urn,
                      final String prosecutingAuthority,
                      final String initiationCode,
                      final Boolean completed,
                      final Boolean assigned,
                      final ZonedDateTime createdOn, final DefendantDetail defendantDetail, final BigDecimal costs, final LocalDate postingDate) {
        this();
        this.id = id;
        this.urn = urn;
        this.prosecutingAuthority = prosecutingAuthority;
        this.initiationCode = initiationCode;
        this.completed = completed;
        this.assigned = assigned;
        this.dateTimeCreated = createdOn;
        setDefendant(defendantDetail);
        this.costs = costs;
        this.postingDate = postingDate;
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

    public String getInitiationCode() {
        return initiationCode;
    }

    public void setInitiationCode(String initiationCode) {
        this.initiationCode = initiationCode;
    }

    public String getProsecutingAuthority() {
        return prosecutingAuthority;
    }

    public void setProsecutingAuthority(String prosecutingAuthority) {
        this.prosecutingAuthority = prosecutingAuthority;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public Boolean getAssigned() {
        return assigned;
    }

    public void setAssigned(Boolean assigned) {
        this.assigned = assigned;
    }

    public String getSummonsCode() {
        return summonsCode;
    }

    public void setSummonsCode(String summonsCode) {
        this.summonsCode = summonsCode;
    }

    public String getLibraOriginatingOrg() {
        return libraOriginatingOrg;
    }

    public void setLibraOriginatingOrg(String libraOriginatingOrg) {
        this.libraOriginatingOrg = libraOriginatingOrg;
    }

    public String getPtiUrn() {
        return ptiUrn;
    }

    public void setPtiUrn(String ptiUrn) {
        this.ptiUrn = ptiUrn;
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

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }
}
