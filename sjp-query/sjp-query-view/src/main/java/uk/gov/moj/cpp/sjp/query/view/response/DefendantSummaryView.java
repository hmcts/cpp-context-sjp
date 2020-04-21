package uk.gov.moj.cpp.sjp.query.view.response;

import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
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

    public DefendantSummaryView(final DefendantDetail defendant){
        this.id = defendant.getId().toString();
        final PersonalDetails personalDetails = defendant.getPersonalDetails();
        this.title = personalDetails.getTitle();
        this.firstName = personalDetails.getFirstName();
        this.lastName = personalDetails.getLastName();
        this.dateOfBirth = personalDetails.getDateOfBirth();
        this.gender = personalDetails.getGender();
        this.nationalInsuranceNumber = personalDetails.getNationalInsuranceNumber();
        this.driverNumber = personalDetails.getDriverNumber();
        this.driverLicenceDetails = personalDetails.getDriverLicenceDetails();
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
}
