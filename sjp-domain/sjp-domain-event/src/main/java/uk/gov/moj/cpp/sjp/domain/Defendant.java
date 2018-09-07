package uk.gov.moj.cpp.sjp.domain;

import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.justice.json.schemas.domains.sjp.Language;

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

    @JsonCreator
    public Defendant(@JsonProperty("id") final UUID id,
                     @JsonProperty("title") final String title,
                     @JsonProperty("firstName") final String firstName,
                     @JsonProperty("lastName") final String lastName,
                     @JsonProperty("dateOfBirth") final LocalDate dateOfBirth,
                     @JsonProperty("gender") final Gender gender,
                     @JsonProperty("nationalInsuranceNumber") final String nationalInsuranceNumber,
                     @JsonProperty("driverNumber") final String driverNumber,
                     @JsonProperty("address") final Address address,
                     @JsonProperty("contactDetails") final ContactDetails contactDetails,
                     @JsonProperty("numPreviousConvictions") final int numPreviousConvictions,
                     @JsonProperty("offences") final List<Offence> offences,
                     @JsonProperty("documentationLanguage") final Language documentationLanguage,
                     @JsonProperty("hearingLanguageIndicator") final Language hearingLanguageIndicator,
                     @JsonProperty("languageNeeds") final String languageNeeds) {
        super(title, firstName, lastName, dateOfBirth, gender, nationalInsuranceNumber, driverNumber, address, contactDetails);
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
                .append(id, defendant.id)
                .append(numPreviousConvictions, defendant.numPreviousConvictions)
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
