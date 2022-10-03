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
    private LegalEntityDetails legalEntityDetails;

    @Embedded
    private InterpreterDetail interpreter;

    @Column(name = "speak_welsh")
    private Boolean speakWelsh;

    @Column(name = "num_previous_convictions")
    private Integer numPreviousConvictions;

    @OneToOne
    @JoinColumn(name = "case_id", nullable = false)
    private CaseDetail caseDetail;

    @Column(name = "disability_needs")
    private String disabilityNeeds;

    @Column(name = "asn")
    private String asn;

    @Column(name = "pnc_identifier")
    private String pncIdentifier;

    @Column(name = "region")
    private String region;

    @Column(name = "name_updated_at")
    private ZonedDateTime nameUpdatedAt;

    @Column(name = "address_updated_at")
    private ZonedDateTime addressUpdatedAt;

    @Column(name = "updates_acknowledged_at")
    private ZonedDateTime updatesAcknowledgedAt;

    @Embedded
    private Address address;

    @Embedded
    private ContactDetails contactDetails;

    /**
     * Correlation / request id provided to staging enforcement
     * when exporting financial impositions.
     */
    @Column(name = "correlation_id")
    private UUID correlationId;

    /**
     * Libra / GOB account number provided by staging enforcement
     * once financial impositions have been exported.
     */
    @Column(name = "account_number")
    private String accountNumber;

    public DefendantDetail() {
        this(UUID.randomUUID());
    }

    public DefendantDetail(final UUID id) {
        this(id, null, null, 0, null, null, null, null, null, null, null);
    }

    public DefendantDetail(final UUID id,
                           final PersonalDetails personalDetails,
                           final List<OffenceDetail> offences,
                           final Integer numPreviousConvictions,
                           final LegalEntityDetails legalEntityDetails,
                           final Address address,
                           final ContactDetails contactDetails) {
        this.id = id;
        this.numPreviousConvictions = numPreviousConvictions;
        setOffences(offences);
        setPersonalDetails(personalDetails);
        setLegalEntityDetails(legalEntityDetails);
        setAddress(address);
        setContactDetails(contactDetails);
    }

    public DefendantDetail(final UUID id,
                           final PersonalDetails personalDetails,
                           final List<OffenceDetail> offences,
                           final Integer numPreviousConvictions,
                           final Boolean speakWelsh,
                           final String asn,
                           final String pncIdentifier,
                           final String region,
                           final LegalEntityDetails legalEntityDetails,
                           final Address address,
                           final ContactDetails contactDetails) {
        this.id = id;
        this.numPreviousConvictions = numPreviousConvictions;
        this.speakWelsh = speakWelsh;
        this.asn = asn;
        this.pncIdentifier = pncIdentifier;
        this.region = region;
        setOffences(offences);
        setPersonalDetails(personalDetails);
        setLegalEntityDetails(legalEntityDetails);
        setAddress(address);
        setContactDetails(contactDetails);
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


    public ZonedDateTime getNameUpdatedAt() {
        return nameUpdatedAt;
    }

    public void markNameUpdated(final ZonedDateTime updateDate) {
        this.nameUpdatedAt = updateDate;
    }

    public ZonedDateTime getAddressUpdatedAt() {
        return addressUpdatedAt;
    }

    public void markAddressUpdated(final ZonedDateTime updateDate) {
        this.addressUpdatedAt = updateDate;
    }

    public void markDateOfBirthUpdated(ZonedDateTime updateDate) {
        personalDetails.markDateOfBirthUpdated(updateDate);
    }

    public String getDisabilityNeeds() {
        return disabilityNeeds;
    }

    public void setDisabilityNeeds(final String disabilityNeeds) {
        this.disabilityNeeds = disabilityNeeds;
    }

    public String getAsn() {
        return asn;
    }

    public void setAsn(final String asn) {
        this.asn = asn;
    }

    public String getPncIdentifier() {
        return pncIdentifier;
    }

    public void setPncIdentifier(final String pncIdentifier) {
        this.pncIdentifier = pncIdentifier;
    }

    public void setCorrelationId(final UUID correlationId) {
        this.correlationId = correlationId;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(final String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public LegalEntityDetails getLegalEntityDetails() {
        return legalEntityDetails;
    }

    public void setLegalEntityDetails(final LegalEntityDetails legalEntityDetails) {
        this.legalEntityDetails = Optional.ofNullable(legalEntityDetails).orElseGet(LegalEntityDetails::new);
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(final String region) {
        this.region = region;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(final Address address) {
        this.address = address;
    }

    public ContactDetails getContactDetails() {
        return contactDetails;
    }

    public void setContactDetails(final ContactDetails contactDetails) {
        this.contactDetails = contactDetails;
    }

    public ZonedDateTime getUpdatesAcknowledgedAt() {
        return updatesAcknowledgedAt;
    }

    public void setUpdatesAcknowledgedAt(final ZonedDateTime updatesAcknowledgedAt) {
        this.updatesAcknowledgedAt = updatesAcknowledgedAt;
    }

    public void setNameUpdatedAt(final ZonedDateTime nameUpdatedAt) {
        this.nameUpdatedAt = nameUpdatedAt;
    }

    public void setAddressUpdatedAt(final ZonedDateTime addressUpdatedAt) {
        this.addressUpdatedAt = addressUpdatedAt;
    }
}