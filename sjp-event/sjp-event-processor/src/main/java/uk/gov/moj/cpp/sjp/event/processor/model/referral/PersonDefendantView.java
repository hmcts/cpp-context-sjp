package uk.gov.moj.cpp.sjp.event.processor.model.referral;

public class PersonDefendantView {

    private final PersonDetailsView personDetails;
    private final EmployerOrganisationView employerOrganisation;

    public PersonDefendantView(final PersonDetailsView personDetails, final EmployerOrganisationView employerOrganisation) {
        this.personDetails = personDetails;
        this.employerOrganisation = employerOrganisation;
    }

    public PersonDetailsView getPersonDetails() {
        return personDetails;
    }

    public EmployerOrganisationView getEmployerOrganisation() {
        return employerOrganisation;
    }
}
