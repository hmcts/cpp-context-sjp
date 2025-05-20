package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.Address;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Issued when LA accepts the changes for given defendant and case.
 */
@Event("sjp.events.defendant-pending-changes-accepted")
public class DefendantPendingChangesAccepted {
    private final UUID caseId;
    private final UUID defendantId;
    private final ZonedDateTime acceptedAt;
    private final String firstName;
    private final String lastName;
    private final LocalDate dateOfBirth;
    private final Address address;
    private final String legalEntityName;

    @JsonCreator
    public DefendantPendingChangesAccepted(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("defendantId") final UUID defendantId,
            @JsonProperty("acceptedAt") final ZonedDateTime acceptedAt,
            @JsonProperty("firstName") final String firstName,
            @JsonProperty("lastName") final String lastName,
            @JsonProperty("dateOfBirth") final LocalDate dateOfBirth,
            @JsonProperty("address") final Address address,
            @JsonProperty("legalEntityName") final String legalEntityName) {

        this.caseId = caseId;
        this.defendantId = defendantId;
        this.acceptedAt = acceptedAt;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.legalEntityName = legalEntityName;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public ZonedDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public Address getAddress() {
        return address;
    }

    public String getLegalEntityName() {
        return legalEntityName;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public static class DefendantPendingChangesAcceptedBuilder {
        private UUID caseId;
        private UUID defendantId;
        private ZonedDateTime acceptedAt;
        private String firstName;
        private String lastName;
        private LocalDate dateOfBirth;
        private Address address;
        private String legalEntityName;

        public static DefendantPendingChangesAcceptedBuilder defendantPendingChangesAccepted() {
            return new DefendantPendingChangesAcceptedBuilder();
        }

        public DefendantPendingChangesAcceptedBuilder withCaseId(UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public DefendantPendingChangesAcceptedBuilder withDefendantId(UUID defendantId) {
            this.defendantId = defendantId;
            return this;
        }

        public DefendantPendingChangesAcceptedBuilder withAcceptedAt(ZonedDateTime acceptedAt) {
            this.acceptedAt = acceptedAt;
            return this;
        }

        public DefendantPendingChangesAcceptedBuilder withFirstName(final String firstName) {
            this.firstName = firstName;
            return this;
        }

        public DefendantPendingChangesAcceptedBuilder withLastName(final String lastName) {
            this.lastName = lastName;
            return this;
        }

        public DefendantPendingChangesAcceptedBuilder withDateOfBirth(final LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public DefendantPendingChangesAcceptedBuilder withAddress(final Address address) {
            this.address = address;
            return this;
        }

        public DefendantPendingChangesAcceptedBuilder withLegalEntityName(final String legalEntityName) {
            this.legalEntityName = legalEntityName;
            return this;
        }


        public DefendantPendingChangesAccepted build() {
            return new DefendantPendingChangesAccepted(caseId, defendantId, acceptedAt, firstName, lastName, dateOfBirth, address, legalEntityName);
        }
    }
}
