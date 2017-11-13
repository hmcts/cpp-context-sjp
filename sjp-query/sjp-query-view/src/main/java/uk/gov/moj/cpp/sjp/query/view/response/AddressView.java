package uk.gov.moj.cpp.sjp.query.view.response;

public class AddressView {
    private String address;
    private String country;
    private String postCode;

    public AddressView() {
        super();
    }

    public AddressView(final String address,
                       final String postCode,
                       final String country) {
        this.address = address;
        this.country = country;
        this.postCode = postCode;
    }

    public String getAddress() {
        return address;
    }

    public String getCountry() {
        return country;
    }

    public String getPostCode() {
        return postCode;
    }
}