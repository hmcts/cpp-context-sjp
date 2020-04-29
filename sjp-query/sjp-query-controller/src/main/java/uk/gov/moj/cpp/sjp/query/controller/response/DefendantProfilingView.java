package uk.gov.moj.cpp.sjp.query.controller.response;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"squid:S00121", "squid:S1067"})
public class DefendantProfilingView {

    private UUID id;

    private String firstName;

    private String lastName;

    private LocalDate dateOfBirth;

    private String nationalInsuranceNumber;

    public DefendantProfilingView() {
    }

    public DefendantProfilingView(final UUID id, final String firstName, final String lastName, final LocalDate dateOfBirth, final String nationalInsuranceNumber) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.nationalInsuranceNumber = nationalInsuranceNumber;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(final DefendantProfilingView copy) {
        final Builder builder = new Builder();
        builder.id = copy.getId();
        builder.firstName = copy.getFirstName();
        builder.lastName = copy.getLastName();
        builder.dateOfBirth = copy.getDateOfBirth();
        builder.nationalInsuranceNumber = copy.getNationalInsuranceNumber();
        return builder;
    }

    public UUID getId() {
        return id;
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

    public String getNationalInsuranceNumber() {
        return nationalInsuranceNumber;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof DefendantProfilingView)) return false;
        final DefendantProfilingView that = (DefendantProfilingView) o;
        return Objects.equals(id, that.id) &&
                       Objects.equals(firstName, that.firstName) &&
                       Objects.equals(lastName, that.lastName) &&
                       Objects.equals(dateOfBirth, that.dateOfBirth) &&
                       Objects.equals(nationalInsuranceNumber, that.nationalInsuranceNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, firstName, lastName, dateOfBirth, nationalInsuranceNumber);
    }

    public static final class Builder {
        private UUID id;
        private String firstName;
        private String lastName;
        private LocalDate dateOfBirth;
        private String nationalInsuranceNumber;

        private Builder() {
        }

        public Builder withId(final UUID id) {
            this.id = id;
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

        public Builder withNationalInsuranceNumber(final String nationalInsuranceNumber) {
            this.nationalInsuranceNumber = nationalInsuranceNumber;
            return this;
        }

        public DefendantProfilingView build() {
            return new DefendantProfilingView(id, firstName, lastName, dateOfBirth, nationalInsuranceNumber);
        }
    }
}
