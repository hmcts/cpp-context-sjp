package uk.gov.moj.cpp.sjp.persistence.entity;

import static javax.persistence.ConstraintMode.NO_CONSTRAINT;

import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "case_search_result")
public class CaseSearchResult implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    private UUID id;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", updatable = false, insertable = false, foreignKey = @ForeignKey(name = "none", value = NO_CONSTRAINT))
    private CaseSummary caseSummary;

    // because there is no foreign key we can create this entity before the CaseSummary if we want

    @Column(name = "case_id", updatable = false)
    private UUID caseId;

    //sjp (offence)
    @Column(name = "plea_date")
    private LocalDate pleaDate;
    @Column(name = "withdrawal_requested_date")
    private LocalDate withdrawalRequestedDate;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;
    @Column(name = "current_first_name")
    private String currentFirstName;
    @Column(name = "current_last_name")
    private String currentLastName;
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    @Column(name = "assigned")
    private Boolean assigned = false;
    @Column(name = "date_added")
    private ZonedDateTime dateAdded;
    @Column(name = "deprecated")
    private Boolean deprecated = false;

    @Column(name = "plea_type")
    @Enumerated(value = EnumType.STRING)
    private PleaType pleaType;

    public CaseSearchResult() {
        this.id = UUID.randomUUID();
    }

    public CaseSearchResult(final UUID caseId, final String firstName, final String lastName, final LocalDate dateOfBirth, final ZonedDateTime dateAdded) {
        this();
        this.caseId = caseId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.currentFirstName = firstName;
        this.currentLastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.dateAdded = dateAdded;
    }

    public CaseSummary getCaseSummary() {
        return caseSummary;
    }

    public void setCaseSummary(CaseSummary caseSummary) {
        this.caseSummary = caseSummary;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LocalDate getPleaDate() {
        return pleaDate;
    }

    public void setPleaDate(LocalDate pleaDate) {
        this.pleaDate = pleaDate;
    }

    public LocalDate getWithdrawalRequestedDate() {
        return withdrawalRequestedDate;
    }

    public void setWithdrawalRequestedDate(LocalDate withdrawalRequestedDate) {
        this.withdrawalRequestedDate = withdrawalRequestedDate;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Boolean isAssigned() {
        return assigned != null && assigned;
    }

    public void setAssigned(Boolean assigned) {
        this.assigned = assigned;
    }

    public String getCurrentFirstName() {
        return currentFirstName;
    }

    public void setCurrentFirstName(String currentFirstName) {
        this.currentFirstName = currentFirstName;
    }

    public String getCurrentLastName() {
        return currentLastName;
    }

    public void setCurrentLastName(String currentLastName) {
        this.currentLastName = currentLastName;
    }

    public Boolean getAssigned() {
        return assigned;
    }

    public ZonedDateTime getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(ZonedDateTime dateAdded) {
        this.dateAdded = dateAdded;
    }

    public Boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public PleaType getPleaType() {
        return pleaType;
    }

    public void setPleaType(final PleaType pleaType) {
        this.pleaType = pleaType;
    }
}
