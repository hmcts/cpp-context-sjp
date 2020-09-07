package uk.gov.moj.cpp.sjp.persistence.builder;

import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;

public class PersonalDetailsBuilder {

    private String region;

    private PersonalDetailsBuilder() {
    }

    public static PersonalDetailsBuilder aPersonalDetails() {
        return new PersonalDetailsBuilder();
    }

    public PersonalDetailsBuilder withRegion(final String region) {
        this.region = region;
        return this;
    }

    public PersonalDetails build() {
        final PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.setRegion(region);
        return personalDetails;
    }
}
