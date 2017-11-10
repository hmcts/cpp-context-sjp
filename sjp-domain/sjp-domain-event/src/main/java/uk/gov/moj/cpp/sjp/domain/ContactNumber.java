package uk.gov.moj.cpp.sjp.domain;

import java.io.Serializable;

public class ContactNumber implements Serializable {
    private final String home;
    private final String mobile;

    public ContactNumber(final String home, final String mobile) {
        this.home = home;
        this.mobile = mobile;
    }

    public String getHome() {
        return home;
    }

    public String getMobile() {
        return mobile;
    }
}
