package uk.gov.moj.cpp.sjp.domain;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ContactDetails implements Serializable {
    private final String home;
    private final String mobile;
    private final String email;

    @JsonCreator
    public ContactDetails(
            @JsonProperty(value = "home") final String home,
            @JsonProperty(value = "mobile") final String mobile,
            @JsonProperty(value = "email") final String email) {
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
