package uk.gov.moj.cpp.sjp.persistence.entity.view;

import static java.util.Objects.nonNull;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import com.google.common.base.Objects;

public class UpdatedDefendantDetails {

    private final String firstName;
    private final String lastName;
    private final LocalDate dateOfBirth;
    private final UUID defendantId;
    private final ZonedDateTime addressUpdatedAt;
    private final ZonedDateTime dateOfBirthUpdatedAt;
    private final ZonedDateTime nameUpdatedAt;
    private final String caseUrn;
    private final UUID caseId;

    @SuppressWarnings("squid:S00107")
    public UpdatedDefendantDetails(
            final String firstName,
            final String lastName,
            final LocalDate dateOfBirth,
            final UUID defendantId,
            final ZonedDateTime addressUpdatedAt,
            final ZonedDateTime dateOfBirthUpdatedAt,
            final ZonedDateTime nameUpdatedAt,
            final String caseUrn,
            final UUID caseId) {

        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.defendantId = defendantId;
        this.addressUpdatedAt = addressUpdatedAt;
        this.dateOfBirthUpdatedAt = dateOfBirthUpdatedAt;
        this.nameUpdatedAt = nameUpdatedAt;
        this.caseUrn = caseUrn;
        this.caseId = caseId;
    }

    /**
     * Compares the update dates, returning the latest one.
     *
     * @return the latest update date
     */
    public Optional<ZonedDateTime> getMostRecentUpdateDate() {
        return Optional.ofNullable(
                later(
                        later(addressUpdatedAt, dateOfBirthUpdatedAt),
                        nameUpdatedAt));
    }

    // Safely compare two date time instances, null being considered "earlier" than an instance
    private static ZonedDateTime later(ZonedDateTime a, ZonedDateTime b) {
        if (nonNull(a) && nonNull(b)) {
            return a.isAfter(b) ? a : b;
        }

        return nonNull(a) ? a : b;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getCaseUrn() {
        return caseUrn;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public ZonedDateTime getAddressUpdatedAt() {
        return addressUpdatedAt;
    }

    public ZonedDateTime getDateOfBirthUpdatedAt() {
        return dateOfBirthUpdatedAt;
    }

    public ZonedDateTime getNameUpdatedAt() {
        return nameUpdatedAt;
    }

    @SuppressWarnings("squid:S1067")
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final UpdatedDefendantDetails that = (UpdatedDefendantDetails) o;

        return Objects.equal(firstName, that.firstName) &&
                Objects.equal(lastName, that.lastName) &&
                Objects.equal(dateOfBirth, that.dateOfBirth) &&
                Objects.equal(defendantId, that.defendantId) &&
                Objects.equal(addressUpdatedAt, that.addressUpdatedAt) &&
                Objects.equal(dateOfBirthUpdatedAt, that.dateOfBirthUpdatedAt) &&
                Objects.equal(nameUpdatedAt, that.nameUpdatedAt) &&
                Objects.equal(caseUrn, that.caseUrn) &&
                Objects.equal(caseId, that.caseId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                firstName,
                lastName,
                dateOfBirth,
                defendantId,
                addressUpdatedAt,
                dateOfBirthUpdatedAt,
                nameUpdatedAt,
                caseUrn,
                caseId);
    }
}
