package uk.gov.moj.cpp.sjp.event.processor.model.referral;

import java.util.List;

public class PersonDefendantView {

    private final PersonDetailsView personDetails;
    private final EmployerOrganisationView employerOrganisation;
    private final String selfDefinedEthnicityId;
    private final String driverNumber;
    private final List<String> aliases;
    private final String arrestSummonsNumber;

    public PersonDefendantView(final PersonDetailsView personDetails,
                               final EmployerOrganisationView employerOrganisation,
                               final String selfDefinedEthnicityId,
                               final String driverNumber,
                               final List<String> aliases,
                               final String arrestSummonsNumber) {
        this.personDetails = personDetails;
        this.employerOrganisation = employerOrganisation;
        this.selfDefinedEthnicityId = selfDefinedEthnicityId;
        this.driverNumber = driverNumber;
        this.aliases = aliases;
        this.arrestSummonsNumber = arrestSummonsNumber;
    }

    public PersonDetailsView getPersonDetails() {
        return personDetails;
    }

    public EmployerOrganisationView getEmployerOrganisation() {
        return employerOrganisation;
    }

    public String getSelfDefinedEthnicityId() { return selfDefinedEthnicityId; }

    public String getDriverNumber() {
        return driverNumber;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public String getArrestSummonsNumber() {
        return arrestSummonsNumber;
    }
}
