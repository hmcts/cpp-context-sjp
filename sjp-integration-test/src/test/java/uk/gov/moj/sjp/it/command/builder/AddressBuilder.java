package uk.gov.moj.sjp.it.command.builder;

public class AddressBuilder {
    String address1;
    String address2;
    String address3;
    String address4;
    String postcode;

    private AddressBuilder() {

    }

    public static AddressBuilder withDefaults() {
        final AddressBuilder addressBuilder = new AddressBuilder();

        addressBuilder.address1 = "14 Tottenham Court Road";
        addressBuilder.address2 = "London";
        addressBuilder.address3 = "England";
        addressBuilder.address4 = "UK";
        addressBuilder.postcode = "W1T 1JY";

        return addressBuilder;
    }

    public AddressBuilder withAddress1(final String address1) {
        this.address1 = address1;
        return this;
    }

    public AddressBuilder withAddress2(final String address2) {
        this.address2 = address2;
        return this;
    }

    public AddressBuilder withAddress3(final String address3) {
        this.address3 = address3;
        return this;
    }

    public AddressBuilder setAddress4(final String address4) {
        this.address4 = address4;
        return this;
    }

    public AddressBuilder withPostcode(final String postcode) {
        this.postcode = postcode;
        return this;
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
}
