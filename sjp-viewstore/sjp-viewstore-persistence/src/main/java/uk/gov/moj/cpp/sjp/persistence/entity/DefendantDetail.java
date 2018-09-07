package uk.gov.moj.cpp.sjp.persistence.entity;

import static java.util.Collections.emptySet;
import static org.apache.deltaspike.core.util.CollectionUtils.isEmpty;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Entity
@Table(name = "defendant", uniqueConstraints = @UniqueConstraint(columnNames = "id"))
public class DefendantDetail implements Serializable {

    private static final long serialVersionUID = 97305852963115611L;

    @Column(name = "id", nullable = false, unique = true)
    @Id
    private UUID id;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "defendantDetail", orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private Set<OffenceDetail> offences;

    @Embedded
    private PersonalDetails personalDetails;

    @Embedded
    private InterpreterDetail interpreter;

    @Column(name = "num_previous_convictions")
    private Integer numPreviousConvictions;

    @OneToOne
    @JoinColumn(name = "case_id", nullable = false)
    private CaseDetail caseDetail;

    public DefendantDetail() {
        this(UUID.randomUUID());
    }

    public DefendantDetail(final UUID id) {
        this(id, null, null, 0);
    }

    public DefendantDetail(final UUID id,
                           final PersonalDetails personalDetails,
                           final Collection<OffenceDetail> offences,
                           final Integer numPreviousConvictions) {
        this.id = id;
        this.numPreviousConvictions = numPreviousConvictions;
        setOffences(offences);
        setPersonalDetails(personalDetails);
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
        return Objects.equals(id, that.id) && Objects.equals(personalDetails, that.personalDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, personalDetails);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public PersonalDetails getPersonalDetails() {
        return personalDetails;
    }

    public void setPersonalDetails(PersonalDetails personalDetails) {
        this.personalDetails = Optional.ofNullable(personalDetails).orElseGet(PersonalDetails::new);
    }

    public Set<OffenceDetail> getOffences() {
        return offences;
    }

    public void setOffences(final Collection<OffenceDetail> offences) {
        this.offences = isEmpty(offences) ? emptySet() : new HashSet<>(offences);
        this.offences.forEach(offence -> offence.setDefendantDetail(this));
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