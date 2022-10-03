package uk.gov.moj.cpp.sjp.query.view.response;

import static java.util.Objects.nonNull;

import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.LegalEntityDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;

import java.time.LocalDate;

public class DefendantSummaryView {

    private final String id;
    private final String title;
    private final String firstName;
    private final String lastName;
    private final LocalDate dateOfBirth;
    private final Gender gender;
    private final String nationalInsuranceNumber;
    private final String driverNumber;
    private final String driverLicenceDetails;
    private final String legalEntityName;

    public DefendantSummaryView(final DefendantDetail defendant) {
        this.id = defendant.getId().toString();
        final PersonalDetails personalDetails = defendant.getPersonalDetails();
        final LegalEntityDetails legalEntityDetails = defendant.getLegalEntityDetails();
        this.title = nonNull(personalDetails) ? personalDetails.getTitle() : null;
        this.firstName = nonNull(personalDetails) ? personalDetails.getFirstName() : null;
        this.lastName = nonNull(personalDetails) ? personalDetails.getLastName() : null;
        this.dateOfBirth = nonNull(personalDetails) ? personalDetails.getDateOfBirth() : null;
        this.gender = nonNull(personalDetails) ? personalDetails.getGender() : null;
        this.nationalInsuranceNumber = nonNull(personalDetails) ? personalDetails.getNationalInsuranceNumber() : null;
        this.driverNumber = nonNull(personalDetails) ? personalDetails.getDriverNumber() : null;
        this.driverLicenceDetails = nonNull(personalDetails) ? personalDetails.getDriverLicenceDetails() : null;
        this.legalEntityName = nonNull(legalEntityDetails) ? legalEntityDetails.getLegalEntityName() : null;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
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

    public Gender getGender() {
        return gender;
    }

    public String getNationalInsuranceNumber() {
        return nationalInsuranceNumber;
    }

    public String getDriverNumber() {
        return driverNumber;
    }

    public String getDriverLicenceDetails() {
        return driverLicenceDetails;
    }

    public String getLegalEntityName() {
        return legalEntityName;
    }
}
