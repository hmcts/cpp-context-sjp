package uk.gov.moj.cpp.sjp.persistence.entity;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Embeddable
public class ContactDetails implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "email")
    private String email;

    @Column(name = "telephone_number_home")
    private String home;

    @Column(name = "telephone_number_mobile")
    private String mobile;

    public ContactDetails() {
    }

    public ContactDetails(final String email, final String home, final String mobile) {
        this();
        this.email = email;
        this.home = home;
        this.mobile = mobile;
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

    public void setEmail(String email) {
        this.email = email;
    }

    public void setHome(String home) {
        this.home = home;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ContactDetails that = (ContactDetails) o;
        return Objects.equals(email, that.email) &&
                Objects.equals(home, that.home) &&
                Objects.equals(mobile, that.mobile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, home, mobile);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
