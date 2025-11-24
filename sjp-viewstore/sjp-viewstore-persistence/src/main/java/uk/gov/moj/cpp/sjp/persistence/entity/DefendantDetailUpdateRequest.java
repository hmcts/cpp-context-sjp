package uk.gov.moj.cpp.sjp.persistence.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Entity
@Table(name = "defendant_update_request")
public class DefendantDetailUpdateRequest implements Serializable {

    private static final long serialVersionUID = 97305852963115612L;

    @Column(name = "case_id")
    @Id
    private UUID caseId;

    @Column(name = "defendant_id")
    private UUID defendantId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "legal_entity_name")
    private String legalEntityName;

    @Column(name = "address1")
    private String address1;

    @Column(name = "address2")
    private String address2;

    @Column(name = "address3")
    private String address3;

    @Column(name = "address4")
    private String address4;

    @Column(name = "address5")
    private String address5;

    @Column(name = "postcode")
    private String postcode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DefendantDetailUpdateRequest.Status status;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @Column(name = "name_updated")
    private boolean nameUpdated;

    @Column(name = "address_updated")
    private boolean addressUpdated;

    @Column(name = "dob_updated")
    private boolean dobUpdated;

    public DefendantDetailUpdateRequest() {

    }

    @SuppressWarnings("squid:S00107")
    public DefendantDetailUpdateRequest(final String firstName, final String lastName, final String legalEntityName, final LocalDate dateOfBirth, final String address1, final String address2, final String address3, final String address4, final String address5, final String postcode, final Status status, final boolean nameUpdated, final boolean addressUpdated, final boolean dobUpdated, final UUID caseId, final UUID defendantId) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.legalEntityName = legalEntityName;
        this.dateOfBirth = dateOfBirth;
        this.address1 = address1;
        this.address2 = address2;
        this.address3 = address3;
        this.address4 = address4;
        this.address5 = address5;
        this.postcode = postcode;
        this.status = status;
        this.nameUpdated = nameUpdated;
        this.addressUpdated = addressUpdated;
        this.dobUpdated = dobUpdated;
        this.caseId = caseId;
        this.defendantId = defendantId;
    }

    public DefendantDetailUpdateRequest(final Builder builder) {
        this.firstName = builder.firstName;
        this.lastName = builder.lastName;
        this.legalEntityName = builder.legalEntityName;
        this.caseId = builder.caseId;
        this.defendantId = builder.defendantId;
        this.dateOfBirth = builder.dateOfBirth;
        this.address1 = builder.address1;
        this.address2 = builder.address2;
        this.address3 = builder.address3;
        this.address4 = builder.address4;
        this.address5 = builder.address5;
        this.postcode = builder.postcode;
        this.status = builder.status;
        this.updatedAt = builder.updatedAt;
        this.nameUpdated = builder.nameUpdated;
        this.addressUpdated = builder.addressUpdated;
        this.dobUpdated = builder.dobUpdated;
    }

    @Override
    @SuppressWarnings("squid:S1067")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DefendantDetailUpdateRequest that = (DefendantDetailUpdateRequest) o;
        return Objects.equals(firstName, that.firstName) && Objects.equals(lastName, that.lastName) && Objects.equals(legalEntityName, that.legalEntityName) && Objects.equals(dateOfBirth, that.dateOfBirth) && Objects.equals(address1, that.address1) && Objects.equals(address2, that.address2) && Objects.equals(address3, that.address3) && Objects.equals(address4, that.address4) && Objects.equals(address5, that.address5) && Objects.equals(postcode, that.postcode) && Objects.equals(status, that.status)&& Objects.equals(caseId, that.caseId)&& Objects.equals(defendantId, that.defendantId);
    }

    @Override
    @SuppressWarnings({"squid:S1067"})
    public int hashCode() {
        return Objects.hash(firstName, lastName, legalEntityName, dateOfBirth, address1, address2, address3, address4, address5, postcode, status, caseId, defendantId);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(final UUID defendantId) {
        this.defendantId = defendantId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
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

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(final LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getLegalEntityName() {
        return legalEntityName;
    }

    public void setLegalEntityName(final String legalEntityName) {
        this.legalEntityName = legalEntityName;
    }

    public String getAddress1() {
        return address1;
    }

    public void setAddress1(final String address1) {
        this.address1 = address1;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(final String address2) {
        this.address2 = address2;
    }

    public String getAddress3() {
        return address3;
    }

    public void setAddress3(final String address3) {
        this.address3 = address3;
    }

    public String getAddress4() {
        return address4;
    }

    public void setAddress4(final String address4) {
        this.address4 = address4;
    }

    public String getAddress5() {
        return address5;
    }

    public void setAddress5(final String address5) {
        this.address5 = address5;
    }

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(final String postcode) {
        this.postcode = postcode;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isNameUpdated() {
        return nameUpdated;
    }

    public void setNameUpdated(final boolean nameUpdated) {
        this.nameUpdated = nameUpdated;
    }

    public boolean isAddressUpdated() {
        return addressUpdated;
    }

    public void setAddressUpdated(final boolean addressUpdated) {
        this.addressUpdated = addressUpdated;
    }

    public boolean isDobUpdated() {
        return dobUpdated;
    }

    public void setDobUpdated(final boolean dobUpdated) {
        this.dobUpdated = dobUpdated;
    }

    public enum Status {
        PENDING, UPDATED, REJECTED
    }

    @SuppressWarnings("PMD.BeanMembersShouldSerialize")
    public static class Builder {

        private String firstName;
        private String lastName;
        private LocalDate dateOfBirth;
        private String legalEntityName;
        private UUID caseId;
        private UUID defendantId;
        private String address1;
        private String address2;
        private String address3;
        private String address4;
        private String address5;
        private String postcode;
        private Status status;
        private ZonedDateTime updatedAt;
        private boolean nameUpdated;
        private boolean addressUpdated;
        private boolean dobUpdated;

        public DefendantDetailUpdateRequest build() {
            return new DefendantDetailUpdateRequest(this);
        }

        public Builder withPostcode(final String postcode) {
            this.postcode = postcode;
            return this;
        }

        public Builder withUpdatedAt(final ZonedDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder withStatus(final Status status) {
            this.status = status;
            return this;
        }

        public Builder withFirstName(final String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder withLastName(final String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder withDateOfBirth(final LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public Builder withLegalEntityName(final String legalEntityName) {
            this.legalEntityName = legalEntityName;
            return this;
        }

        public Builder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public Builder withAddress1(final String address1) {
            this.address1 = address1;
            return this;
        }

        public Builder withAddress2(final String address2) {
            this.address2 = address2;
            return this;
        }

        public Builder withAddress3(final String address3) {
            this.address3 = address3;
            return this;
        }

        public Builder withAddress4(final String address4) {
            this.address4 = address4;
            return this;
        }

        public Builder withAddress5(final String address5) {
            this.address5 = address5;
            return this;
        }

        public Builder withDefendantId(final UUID defendantId) {
            this.defendantId = defendantId;
            return this;
        }

        public Builder withNameUpdated(final boolean nameUpdated) {
            this.nameUpdated = nameUpdated;
            return this;
        }

        public Builder withAddressUpdated(final boolean addressUpdated) {
            this.addressUpdated = addressUpdated;
            return this;
        }

        public Builder withDobUpdated(final boolean dobUpdated) {
            this.dobUpdated = dobUpdated;
            return this;
        }
    }
}