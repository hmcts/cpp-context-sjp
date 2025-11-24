package uk.gov.moj.cpp.sjp.model.prosecution;

import static java.util.Optional.ofNullable;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class PersonDefendantView {

    private final PersonDetailsView personDetails;
    private final EmployerOrganisationView employerOrganisation;
    private final String selfDefinedEthnicityId;
    private final String driverNumber;
    private final List<String> aliases;
    private final String arrestSummonsNumber;
    private final String pncId;

    public PersonDefendantView(final PersonDetailsView personDetails,
                               final EmployerOrganisationView employerOrganisation,
                               final String selfDefinedEthnicityId,
                               final String driverNumber,
                               final List<String> aliases,
                               final String arrestSummonsNumber,
                               final String pncId) {
        this.personDetails = personDetails;
        this.employerOrganisation = employerOrganisation;
        this.selfDefinedEthnicityId = selfDefinedEthnicityId;
        this.driverNumber = driverNumber;
        this.aliases = ofNullable(aliases).map(LinkedList::new).orElse(null);
        this.arrestSummonsNumber = arrestSummonsNumber;
        this.pncId = pncId;
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
        return ofNullable(aliases).map(Collections::unmodifiableList).orElse(null);
    }

    public String getArrestSummonsNumber() {
        return arrestSummonsNumber;
    }

    public String getPncId() {
        return pncId;
    }

    public static Builder personDefendantView() {
        return new Builder();
    }

    public static class Builder {
        private PersonDetailsView personDetails;
        private EmployerOrganisationView employerOrganisation;
        private String selfDefinedEthnicityId;
        private String driverNumber;
        private List<String> aliases;
        private String arrestSummonsNumber;
        private String pncId;

        public Builder withPersonDetails(final PersonDetailsView personDetailsView) {
            this.personDetails = personDetailsView;
            return this;
        }

        public Builder withEmployerOrganisation(final EmployerOrganisationView employerOrganisation) {
            this.employerOrganisation = employerOrganisation;
            return this;
        }

        public Builder withSelfDefinedEthnicityId(final String selfDefinedEthnicityId) {
            this.selfDefinedEthnicityId = selfDefinedEthnicityId;
            return this;
        }

        public Builder withDriverNumber(final String driverNumber) {
            this.driverNumber = driverNumber;
            return this;
        }

        public Builder withAliases(final List<String> aliases) {
            this.aliases = ofNullable(aliases).map(LinkedList::new).orElse(null);
            return this;
        }

        public Builder withArrestSummonsNumber(final String arrestSummonsNumber) {
            this.arrestSummonsNumber = arrestSummonsNumber;
            return this;
        }

        public Builder withPncId(final String pncId) {
            this.pncId = pncId;
            return this;
        }

        public PersonDefendantView build() {
            return new PersonDefendantView(
                    personDetails,employerOrganisation, selfDefinedEthnicityId,
                    driverNumber, aliases, arrestSummonsNumber,
                    pncId
            );
        }
    }
}
