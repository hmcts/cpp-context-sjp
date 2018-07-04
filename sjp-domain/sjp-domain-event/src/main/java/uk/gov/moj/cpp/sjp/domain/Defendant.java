package uk.gov.moj.cpp.sjp.domain;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Defendant extends Person {

    private final UUID id;

    private final int numPreviousConvictions;

    private final List<Offence> offences;

    private final Language documentationLanguage;

    private final Language hearingLanguageIndicator;

    private final String languageNeeds;

    @SuppressWarnings("squid:S00107")
    public Defendant(UUID id, String title, String firstName, String lastName, LocalDate dateOfBirth, String gender,
                     String nationalInsuranceNumber, Address address, ContactDetails contactDetails, int numPreviousConvictions, List<Offence> offences) {
        this(id, title, firstName, lastName, null, null, dateOfBirth, gender, nationalInsuranceNumber, null, address, contactDetails, numPreviousConvictions, offences,
                null, null, null);
    }

    @JsonCreator
    public Defendant(@JsonProperty("id") UUID id,
                     @JsonProperty("title") String title,
                     @JsonProperty("firstName") String firstName,
                     @JsonProperty("lastName") String lastName,
                     @JsonProperty("forename2") String forename2,
                     @JsonProperty("forename3") String forename3,
                     @JsonProperty("dateOfBirth") LocalDate dateOfBirth,
                     @JsonProperty("gender") String gender,
                     @JsonProperty("nationalInsuranceNumber") String nationalInsuranceNumber,
                     @JsonProperty("driverNumber") String driverNumber,
                     @JsonProperty("address") Address address,
                     @JsonProperty("contactDetails") ContactDetails contactDetails,
                     @JsonProperty("numPreviousConvictions") int numPreviousConvictions,
                     @JsonProperty("offences") List<Offence> offences,
                     @JsonProperty("documentationLanguage") Language documentationLanguage,
                     @JsonProperty("hearingLanguageIndicator") Language hearingLanguageIndicator,
                     @JsonProperty("languageNeeds") String languageNeeds) {
        super(title, firstName, lastName, forename2, forename3, dateOfBirth, gender, nationalInsuranceNumber, driverNumber, address, contactDetails);
        this.id = id;
        this.numPreviousConvictions = numPreviousConvictions;
        this.offences = Optional.ofNullable(offences).map(Collections::unmodifiableList).orElseGet(Collections::emptyList);
        this.documentationLanguage = documentationLanguage;
        this.hearingLanguageIndicator = hearingLanguageIndicator;
        this.languageNeeds = languageNeeds;
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

    public Language getDocumentationLanguage() {
        return documentationLanguage;
    }

    public Language getHearingLanguageIndicator() {
        return hearingLanguageIndicator;
    }

    public String getLanguageNeeds() {
        return languageNeeds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Defendant defendant = (Defendant) o;
        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(numPreviousConvictions, defendant.numPreviousConvictions)
                .append(id, defendant.id)
                .append(offences, defendant.offences)
                .append(documentationLanguage, defendant.documentationLanguage)
                .append(hearingLanguageIndicator, defendant.hearingLanguageIndicator)
                .append(languageNeeds, defendant.languageNeeds)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, numPreviousConvictions, offences,
                documentationLanguage, hearingLanguageIndicator, languageNeeds);
    }

    public static class DefendantBuilder {

        private UUID id;

        public DefendantBuilder withId(final UUID id) {
            this.id = id;
            return this;
        }

        /**
         * Overwrite not-set values with the ones of the specified object.
         */
        public Defendant buildBasedFrom(final Defendant defendant) {
            return new Defendant(
                    Optional.ofNullable(id).orElse(defendant.getId()),
                    defendant.getTitle(),
                    defendant.getFirstName(),
                    defendant.getLastName(),
                    defendant.getForename2(),
                    defendant.getForename3(),
                    defendant.getDateOfBirth(),
                    defendant.getGender(),
                    defendant.getNationalInsuranceNumber(),
                    defendant.getDriverNumber(),
                    defendant.getAddress(),
                    defendant.getContactDetails(),
                    defendant.getNumPreviousConvictions(),
                    defendant.getOffences(),
                    defendant.getDocumentationLanguage(),
                    defendant.getHearingLanguageIndicator(),
                    defendant.getLanguageNeeds());
        }

    }

}
