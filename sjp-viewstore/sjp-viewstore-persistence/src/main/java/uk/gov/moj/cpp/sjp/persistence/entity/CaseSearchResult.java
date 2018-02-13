package uk.gov.moj.cpp.sjp.persistence.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "case_id", nullable = false, updatable = false, insertable = false)
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
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    @Column(name = "post_code")
    private String postCode;

    @Column(name = "assigned")
    private Boolean assigned = false;


    public CaseSearchResult() {
    }

    public CaseSearchResult(UUID id, UUID caseId, String firstName, String lastName, LocalDate dateOfBirth, String postCode) {
        this.id = id;
        this.caseId = caseId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.postCode = postCode;
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

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getPostCode() {
        return postCode;
    }

    public void setPostCode(String postCode) {
        this.postCode = postCode;
    }

    public Boolean isAssigned() {
        return assigned != null && assigned;
    }

    public void setAssigned(Boolean assigned) {
        this.assigned = assigned;
    }
}
