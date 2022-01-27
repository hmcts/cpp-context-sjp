package uk.gov.moj.cpp.sjp.persistence.entity;

import static javax.persistence.ConstraintMode.NO_CONSTRAINT;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
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


    private Set<OffenceSummary> offenceSummary = new HashSet<>();

    // because there is no foreign key we can create this entity before the CaseSummary if we want

    @Column(name = "case_id", updatable = false)
    private UUID caseId;

    @Column(name = "defendant_id", updatable = false)
    private UUID defendantId;

    //sjp (offence)
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

    public CaseSearchResult() {
        this.id = UUID.randomUUID();
    }

    public CaseSearchResult(final UUID caseId, final UUID defendantId, final String firstName, final String lastName, final LocalDate dateOfBirth, final ZonedDateTime dateAdded) {
        this();
        this.caseId = caseId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.currentFirstName = firstName;
        this.currentLastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.dateAdded = dateAdded;
        this.defendantId = defendantId;
    }

    public CaseSummary getCaseSummary() {
        return caseSummary;
    }

    public void setCaseSummary(final CaseSummary caseSummary) {
        this.caseSummary = caseSummary;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    @SuppressWarnings("squid:S2384")
    @Access(AccessType.PROPERTY)
    @OneToMany()
    @JoinColumn(name = "defendant_id", referencedColumnName = "defendant_id" )
    public Set<OffenceSummary> getOffenceSummary() {
        return offenceSummary;
    }

    @SuppressWarnings("squid:S2384")
    public void setOffenceSummary(final Set<OffenceSummary> offenceSummary) {
        this.offenceSummary.addAll(offenceSummary);
    }


    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(final UUID defendantId) {
        this.defendantId = defendantId;
    }


    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public LocalDate getWithdrawalRequestedDate() {
        return withdrawalRequestedDate;
    }

    public void setWithdrawalRequestedDate(final LocalDate withdrawalRequestedDate) {
        this.withdrawalRequestedDate = withdrawalRequestedDate;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public Boolean isAssigned() {
        return assigned != null && assigned;
    }

    public String getCurrentFirstName() {
        return currentFirstName;
    }

    public void setCurrentFirstName(final String currentFirstName) {
        this.currentFirstName = currentFirstName;
    }

    public String getCurrentLastName() {
        return currentLastName;
    }

    public void setCurrentLastName(final String currentLastName) {
        this.currentLastName = currentLastName;
    }

    public Boolean getAssigned() {
        return assigned;
    }

    public void setAssigned(final Boolean assigned) {
        this.assigned = assigned;
    }

    public ZonedDateTime getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(final ZonedDateTime dateAdded) {
        this.dateAdded = dateAdded;
    }

    public Boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(final Boolean deprecated) {
        this.deprecated = deprecated;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(final LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CaseSearchResult that = (CaseSearchResult) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}