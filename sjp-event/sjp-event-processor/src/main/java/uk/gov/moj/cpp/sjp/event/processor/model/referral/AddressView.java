package uk.gov.moj.cpp.sjp.event.processor.model.referral;

public class AddressView {

    private final String address1;
    private final String address2;
    private final String address3;
    private final String address4;
    private final String address5;
    private final String postcode;

    public AddressView(final String address1,
                       final String address2,
                       final String address3,
                       final String address4,
                       final String address5,
                       final String postcode) {

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

}
