package uk.gov.moj.cpp.sjp.query.view.response.courtextract;

public class Address {

    private final String line1;

    private final String line2;

    private final String line3;

    private final String line4;

    private final String line5;

    private final String postcode;

    public Address(final String line1, final String line2,
                   final String line3, final String line4,
                   final String line5, final String postcode) {
        this.line1 = line1;
        this.line2 = line2;
        this.line3 = line3;
        this.line4 = line4;
        this.line5 = line5;
        this.postcode = postcode;
    }

    public String getLine1() {
        return line1;
    }

    public String getLine2() {
        return line2;
    }

    public String getLine3() {
        return line3;
    }

    public String getLine4() {
        return line4;
    }

    public String getLine5() {
        return line5;
    }

    public String getPostcode() {
        return postcode;
    }
}
