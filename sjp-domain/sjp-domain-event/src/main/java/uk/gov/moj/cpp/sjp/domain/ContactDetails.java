package uk.gov.moj.cpp.sjp.domain;

import java.io.Serializable;

public class ContactDetails implements Serializable {
    private final String home;
    private final String mobile;
    private final String email;

    public ContactDetails(final String home, final String mobile, final String email) {
        this.home = home;
        this.mobile = mobile;
        this.email = email;
    }

    public String getHome() {
        return home;
    }

    public String getMobile() {
        return mobile;
    }

    public String getEmail() {
        return email;
    }
}
