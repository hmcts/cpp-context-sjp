package uk.gov.moj.cpp.sjp.event.processor.model.referral;

public class PersonDefendantView {

    private final PersonDetailsView personDetails;
    private final EmployerOrganisationView employerOrganisation;
    private final String selfDefinedEthnicityId;

    public PersonDefendantView(final PersonDetailsView personDetails,
                               final EmployerOrganisationView employerOrganisation,
                               final String selfDefinedEthnicityId) {
        this.personDetails = personDetails;
        this.employerOrganisation = employerOrganisation;
        this.selfDefinedEthnicityId = selfDefinedEthnicityId;
    }

    public PersonDetailsView getPersonDetails() {
        return personDetails;
    }

    public EmployerOrganisationView getEmployerOrganisation() {
        return employerOrganisation;
    }

    public String getSelfDefinedEthnicityId() { return selfDefinedEthnicityId; }
}
