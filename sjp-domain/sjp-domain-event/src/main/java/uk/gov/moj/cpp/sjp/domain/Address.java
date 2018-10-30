package uk.gov.moj.cpp.sjp.domain;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class Address implements Serializable {

    private static final long serialVersionUID = -136067348552556413L;

    private final String address1;
    private final String address2;
    private final String address3;
    private final String address4;
    private final String address5;
    private final String postcode;

    public static final Address UNKNOWN = new Address(null, null,null, null, null, null);

    @JsonCreator
    private Address(
            @JsonProperty("address1") final String address1,
            @JsonProperty("address2") final String address2,
            @JsonProperty("address3") final String address3,
            @JsonProperty("address4") final String address4,
            @JsonProperty("address5") final String address5,
            @JsonProperty("postcode") final String postcode,
            @JsonProperty("postCode") final String postCode // Backward compatibility
    ) {
        this(address1, address2, address3, address4, address5, firstNonNull(postcode, postCode));
    }

    public Address(String address1, String address2, String address3, String address4, String address5, String postcode) {
        this.address1 = address1;
        this.address2 = address2;
        this.address3 = address3;
        this.address4 = address4;
        this.address5 = address5;
        this.postcode = postcode;
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

    public String getAddress5() {
        return address5;
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
                Objects.equals(address5, other.address5) &&
                Objects.equals(postcode, other.postcode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address1, address2, address3, address4, address5, postcode);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
