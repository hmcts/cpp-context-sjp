package uk.gov.moj.cpp.sjp.query.view.response.courtextract;

import static java.lang.String.format;
import static java.time.LocalDate.now;
import static java.time.Period.between;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Optional.ofNullable;

import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.LegalEntityDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;

import java.time.format.DateTimeFormatter;

/**
 * Representation of the 'defendant' section in the court extract json payload.
 */
public class DefendantDetailsView {

    private final String address;

    private final String firstName;

    private final String lastName;

    private final String dateOfBirth;

    private final String age;

    private final String legalEntityName;

    private static final DateTimeFormatter DATE_FORMAT = ofPattern("dd MMMM yyyy");

    public DefendantDetailsView(final DefendantDetail defendant) {
        this.address = ofNullable(defendant.getAddress())
                .map(this::addressString).
                orElse(null);

        this.firstName = ofNullable(defendant.getPersonalDetails()).
                map(PersonalDetails::getFirstName).orElse(null);
        this.lastName = ofNullable(defendant.getPersonalDetails()).
                map(PersonalDetails::getLastName).orElse(null);

        this.dateOfBirth = ofNullable(defendant.getPersonalDetails()).
                map(PersonalDetails::getDateOfBirth).
                map(dob -> dob.format(DATE_FORMAT)).
                orElse(null);
        this.age = ofNullable(defendant.getPersonalDetails()).
                map(PersonalDetails::getDateOfBirth).
                map(birthDate -> String.valueOf(between(birthDate, now()).getYears())).
                orElse(null);

        this.legalEntityName = ofNullable(defendant.getLegalEntityDetails()).
                map(LegalEntityDetails::getLegalEntityName).orElse(null);

    }

    private String addressString(Address address) {
        return format("%s %s %s %s %s %s",
                ofNullable(address.getAddress1()).orElse(""),
                ofNullable(address.getAddress2()).orElse(""),
                ofNullable(address.getAddress3()).orElse(""),
                ofNullable(address.getAddress4()).orElse(""),
                ofNullable(address.getAddress5()).orElse(""),
                ofNullable(address.getPostcode()).map(", "::concat).orElse("")
        ).replaceAll("\\s{2,}", " ");
    }

    public String getAddress() {
        return address;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getAge() {
        return age;
    }

    public String getLegalEntityName() {
        return legalEntityName;
    }
}
