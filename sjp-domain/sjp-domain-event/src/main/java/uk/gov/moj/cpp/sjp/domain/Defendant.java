package uk.gov.moj.cpp.sjp.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Defendant extends Person {

    private final UUID id;

    private final int numPreviousConvictions;

    private final List<Offence> offences;

    @JsonCreator
    public Defendant(@JsonProperty("id") UUID id,
                     @JsonProperty("title") String title,
                     @JsonProperty("firstName") String firstName,
                     @JsonProperty("lastName") String lastName,
                     @JsonProperty("dateOfBirth") LocalDate dateOfBirth,
                     @JsonProperty("gender") String gender,
                     @JsonProperty("address") Address address,
                     @JsonProperty("numPreviousConvictions") int numPreviousConvictions,
                     @JsonProperty("offences") List<Offence> offences) {
        super(title, firstName, lastName, dateOfBirth, gender, address);
        this.id = id;
        this.numPreviousConvictions = numPreviousConvictions;
        this.offences = offences; //TODO provide copy constructor to modify offences easily
    }

    public UUID getId() {
        return id;
    }

    public int getNumPreviousConvictions() {
        return numPreviousConvictions;
    }

    public List<Offence> getOffences() {
        return offences;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Defendant that = (Defendant) o;
        return super.equals(o) &&
                Objects.equals(this.id, that.id) &&
                Objects.equals(this.numPreviousConvictions, that.numPreviousConvictions) &&
                Objects.equals(this.offences, that.offences);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, numPreviousConvictions, offences);
    }

    public static class DefendantBuilder {

        private UUID id;

        public DefendantBuilder withId(UUID id) {
            this.id = id;
            return this;
        }

        /**
         * Overwrite not-set values with the ones of the specified object.
         */
        public Defendant buildBasedFrom(Defendant defendant) {
            return new Defendant(
                    Optional.ofNullable(id).orElse(defendant.getId()),
                    defendant.getTitle(),
                    defendant.getFirstName(),
                    defendant.getLastName(),
                    defendant.getDateOfBirth(),
                    defendant.getGender(),
                    defendant.getAddress(),
                    defendant.getNumPreviousConvictions(),
                    defendant.getOffences());
        }

    }

}
