package uk.gov.moj.cpp.sjp.query.view.response;

import uk.gov.moj.cpp.sjp.persistence.entity.ContactDetails;

public class ContactDetailsView {

    private final String email;

    private final String home;

    private final String mobile;

    public ContactDetailsView(final ContactDetails contactDetails) {
        if (contactDetails != null) {
            this.email = contactDetails.getEmail();
            this.home = contactDetails.getHome();
            this.mobile = contactDetails.getMobile();
        }
        else {
            this.email = null;
            this.home = null;
            this.mobile = null;
        }
    }

    public String getEmail() {
        return email;
    }

    public String getHome() {
        return home;
    }

    public String getMobile() {
        return mobile;
    }
}
