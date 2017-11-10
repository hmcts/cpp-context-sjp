package uk.gov.moj.cpp.sjp.persistence.entity;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "defendant", uniqueConstraints = @UniqueConstraint(columnNames = "id"))
public class DefendantDetail implements Serializable {

    private static final long serialVersionUID = 97305852963115611L;

    @Column(name = "id", nullable = false, unique = true)
    @Id
    private UUID id;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "defendantDetail", orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private Set<OffenceDetail> offences = new HashSet<>();

    @Column(name = "person_id")
    private UUID personId;

    @Embedded
    private InterpreterDetail interpreter;

    @Column(name = "num_previous_convictions")
    private Integer numPreviousConvictions;

    @ManyToOne
    @JoinColumn(name = "case_id", nullable = false)
    private CaseDetail caseDetail;

    public DefendantDetail() {
        super();
    }

    public DefendantDetail(final UUID id, final UUID personId, final Set<OffenceDetail> offences) {
        this.id = id;
        this.personId = personId;
        setOffences(offences);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefendantDetail that = (DefendantDetail) o;
        return Objects.equals(id, that.id) && Objects.equals(personId, that.personId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, personId);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPersonId() {
        return personId;
    }


    public void setPersonId(UUID personId) {
        this.personId = personId;
    }

    public Set<OffenceDetail> getOffences() {
        return offences;
    }

    public void setOffences(Set<OffenceDetail> offences) {
        if (offences != null) {
            this.offences = new HashSet<>(offences);
            this.offences.forEach(offence -> offence.setDefendantDetail(this));
        } else {
            this.offences = new HashSet<>();
        }
    }

    public void addOffences(final Set<OffenceDetail> newOffences) {
        if (newOffences != null && newOffences.size() > 0) {
            newOffences.forEach(offence -> offence.setDefendantDetail(this));
            this.offences.addAll(newOffences);
        }
    }

    public void addOffence(OffenceDetail offenceDetail) {
        Objects.requireNonNull(offenceDetail);
        offences.add(offenceDetail);
        offenceDetail.setDefendantDetail(this);
    }

    public CaseDetail getCaseDetail() {
        return caseDetail;
    }

    public void setCaseDetail(CaseDetail caseDetail) {
        this.caseDetail = caseDetail;
    }

    public Integer getNumPreviousConvictions() {
        return numPreviousConvictions;
    }

    public void setNumPreviousConvictions(Integer numPreviousConvictions) {
        this.numPreviousConvictions = numPreviousConvictions;
    }

    public InterpreterDetail getInterpreter() {
        return interpreter;
    }

    public void setInterpreter(InterpreterDetail interpreter) {
        this.interpreter = interpreter;
    }
}
