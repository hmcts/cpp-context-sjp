package uk.gov.moj.cpp.sjp.event.processor.model.referral;

public class PersonDefendantView {

    private final PersonDetailsView personDetails;

    public PersonDefendantView(final PersonDetailsView personDetails) {
        this.personDetails = personDetails;
    }

    public PersonDetailsView getPersonDetails() {
        return personDetails;
    }

}
