package uk.gov.moj.cpp.sjp.domain;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ContactDetails implements Serializable {

    private final String home;

    private final String mobile;

    private final String email;

    private final String email2;

    private final String business;

    @JsonCreator
    public ContactDetails(
            @JsonProperty("home") final String home,
            @JsonProperty("mobile") final String mobile,
            @JsonProperty("business") final String business,
            @JsonProperty("email") final String email,
            @JsonProperty("email2") final String email2) {
        this.home = home;
        this.mobile = mobile;
        this.business = business;
        this.email = email;
        this.email2 = email2;
    }

    public String getHome() {
        return home;
    }

    public String getMobile() {
        return mobile;
    }

    public String getBusiness() {
        return business;
    }

    public String getEmail() {
        return email;
    }

    public String getEmail2() {
        return email2;
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
                Objects.equals(business, that.business) &&
                Objects.equals(email, that.email) &&
                Objects.equals(email2, that.email2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(home, mobile, business, email, email2);
    }

}
