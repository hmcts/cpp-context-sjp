package uk.gov.moj.cpp.sjp.query.view.response;

import uk.gov.moj.cpp.sjp.persistence.entity.ContactDetails;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ContactDetailsView {

    private final ContactDetails contactDetails;

    public ContactDetailsView(final ContactDetails contactDetails) {
        this.contactDetails = Optional.ofNullable(contactDetails)
                .orElse(ContactDetails.EMPTY);
    }

    @JsonProperty("email")
    public String getEmail() {
        return contactDetails.getEmail();
    }

    @JsonProperty("home")
    public String getHome() {
        return contactDetails.getHome();
    }

    @JsonProperty("mobile")
    public String getMobile() {
        return contactDetails.getMobile();
    }

}
