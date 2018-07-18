package uk.gov.moj.cpp.sjp.domain;

import java.io.Serializable;
import java.util.Objects;

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

    @Override
    @SuppressWarnings("squid:S1067") //This will be replaced by generated code
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ContactDetails)) {
            return false;
        }
        final ContactDetails that = (ContactDetails) o;
        return Objects.equals(home, that.home) &&
                Objects.equals(mobile, that.mobile) &&
                Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(home, mobile, email);
    }

}
