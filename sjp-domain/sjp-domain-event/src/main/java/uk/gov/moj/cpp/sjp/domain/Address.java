package uk.gov.moj.cpp.sjp.domain;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Address implements Serializable {

    private static final long serialVersionUID = -3888942698775583847L;
    private final String address1;
    private final String address2;
    private final String address3;
    private final String address4;
    private final String postcode;

    public static final Address UNKNOWN = new Address(null, null, null, null, null);

    @JsonCreator
    public Address(@JsonProperty("address1") String address1,
                   @JsonProperty("address2") String address2,
                   @JsonProperty("address3") String address3,
                   @JsonProperty("address4") String address4,
                   @JsonProperty("postcode") String postcode) {
        this.address1 = address1;
        this.address2 = address2;
        this.address3 = address3;
        this.address4 = address4;
        this.postcode = postcode;
    }

    @JsonCreator(mode = Mode.DISABLED)
    public Address(String address1) {
        this(address1, null, null, null, null);
    }

    public String getAddress1() {
        return address1;
    }

    public String getAddress2() {
        return address2;
    }

    public String getAddress3() {
        return address3;
    }

    public String getAddress4() {
        return address4;
    }

    public String getPostcode() {
        return postcode;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Address)) {
            return false;
        }
        final Address other = (Address) o;
        return Objects.equals(address1, other.address1) &&
                Objects.equals(address2, other.address2) &&
                Objects.equals(address3, other.address3) &&
                Objects.equals(address4, other.address4) &&
                Objects.equals(postcode, other.postcode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address1, address2, address3, address4, postcode);
    }
}
