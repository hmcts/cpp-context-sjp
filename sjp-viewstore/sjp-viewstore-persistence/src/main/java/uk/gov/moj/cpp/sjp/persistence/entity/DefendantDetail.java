package uk.gov.moj.cpp.sjp.persistence.entity;

import static java.util.Collections.emptySet;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
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
    @OrderBy("sequenceNumber ASC")
    private Set<OffenceDetail> offences;

    @Embedded
    private PersonalDetails personalDetails;

    @Embedded
    private InterpreterDetail interpreter;

    @Column(name = "speak_welsh")
    private Boolean speakWelsh;

    @Column(name = "num_previous_convictions")
    private Integer numPreviousConvictions;

    @OneToOne
    @JoinColumn(name = "case_id", nullable = false)
    private CaseDetail caseDetail;

    public DefendantDetail() {
        this(UUID.randomUUID());
    }

    public DefendantDetail(final UUID id) {
        this(id, null, null, 0, null);
    }

    public DefendantDetail(final UUID id,
                           final PersonalDetails personalDetails,
                           final List<OffenceDetail> offences,
                           final Integer numPreviousConvictions) {
        this.id = id;
        this.numPreviousConvictions = numPreviousConvictions;
        setOffences(offences);
        setPersonalDetails(personalDetails);
    }

    public DefendantDetail(final UUID id,
                           final PersonalDetails personalDetails,
                           final List<OffenceDetail> offences,
                           final Integer numPreviousConvictions,
                           final Boolean speakWelsh) {
        this.id = id;
        this.numPreviousConvictions = numPreviousConvictions;
        this.speakWelsh = speakWelsh;
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

    public List<OffenceDetail> getOffences() {
        return new ArrayList(offences);
    }

    public void setOffences(final List<OffenceDetail> offences) {
        this.offences = offences == null ? emptySet() : new TreeSet<>(offences);
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

    public Boolean getSpeakWelsh() {
        return speakWelsh;
    }

    public void setSpeakWelsh(final Boolean speakWelsh) {
        this.speakWelsh = speakWelsh;
    }

    public void markNameUpdated(ZonedDateTime updateDate) {
        personalDetails.markNameUpdated(updateDate);
    }

    public void markAddressUpdated(ZonedDateTime updateDate) {
        personalDetails.markAddressUpdated(updateDate);
    }

    public void markDateOfBirthUpdated(ZonedDateTime updateDate) {
        personalDetails.markDateOfBirthUpdated(updateDate);
    }

    public void acknowledgeDetailsUpdates(ZonedDateTime acknowledgedAt) {
        personalDetails.acknowledgeUpdates(acknowledgedAt);
    }
}