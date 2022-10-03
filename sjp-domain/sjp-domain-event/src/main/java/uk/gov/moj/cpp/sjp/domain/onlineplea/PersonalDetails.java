package uk.gov.moj.cpp.sjp.domain.onlineplea;

import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.ContactDetails;
import uk.gov.moj.cpp.sjp.domain.Defendant;
import uk.gov.moj.cpp.sjp.domain.Person;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @deprecated please use {@link Person} or {@link Defendant}
 * WARNING:
 * This is a quick patch to avoid duplications and merge PersonalDetails with {@link Defendant}
 * Here there is a wrong domain: Person is-not a PersonalDetails.
 * TODO: replace the usage with Defendant.personalDetails, major change what will affect events historical data.
 */
@Deprecated
@JsonIgnoreProperties(ignoreUnknown = true)
public class PersonalDetails extends Person {

    @JsonCreator
    public PersonalDetails(
            @JsonProperty("firstName") final String firstName,
            @JsonProperty("lastName") final String lastName,
            @JsonProperty("address") final Address address,
            @JsonProperty("contactDetails") final ContactDetails contactDetails,
            @JsonProperty("dateOfBirth") final LocalDate dateOfBirth,
            @JsonProperty("nationalInsuranceNumber") final String nationalInsuranceNumber,
            @JsonProperty("region") final String region,
            @JsonProperty("driverNumber") final String driverNumber,
            @JsonProperty("driverLicenceDetails") final String driverLicenceDetails,
            @JsonProperty("legalEntityName") final String legalEntityName
    ) {
        super(null, firstName, lastName, dateOfBirth, null, nationalInsuranceNumber, driverNumber, driverLicenceDetails, address, contactDetails, region, legalEntityName);
    }
}