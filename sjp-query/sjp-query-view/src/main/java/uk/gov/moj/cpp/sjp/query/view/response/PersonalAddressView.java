package uk.gov.moj.cpp.sjp.query.view.response;

import uk.gov.moj.cpp.sjp.persistence.entity.Address;

public class PersonalAddressView {

    private final String address1;

    private final String address2;

    private final String address3;

    private final String address4;

    private final String address5;

    private final String postcode;


    public PersonalAddressView(final Address address) {
        this.address1 = address.getAddress1();
        this.address2 = address.getAddress2();
        this.address3 = address.getAddress3();
        this.address4 = address.getAddress4();
        this.address5 = address.getAddress5();
        this.postcode = address.getPostcode();
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
}
