package uk.gov.moj.cpp.sjp.domain;

import java.io.Serializable;

public class Address implements Serializable {

    private static final long serialVersionUID = -3888942698775583847L;
    private final String address1;
    private final String address2;
    private final String address3;
    private final String address4;
    private final String postCode;

    public static final Address UNKNOWN = new Address(null, null, null, null, null);

    public Address(String address1, String address2, String address3, String address4, String postCode) {
        this.address1 = address1;
        this.address2 = address2;
        this.address3 = address3;
        this.address4 = address4;
        this.postCode = postCode;
    }

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

    public String getPostCode() {
        return postCode;
    }
}
