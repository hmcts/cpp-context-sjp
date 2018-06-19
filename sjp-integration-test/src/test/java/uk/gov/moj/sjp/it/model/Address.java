package uk.gov.moj.sjp.it.model;

import lombok.Builder;

@Builder
public class Address {

    @Builder.Default
    public String address1 = "14 Tottenham Court Road";

    @Builder.Default
    public String address2 = "London";

    @Builder.Default
    public String address3 = "England";

    @Builder.Default
    public String address4 = "UK";

    @Builder.Default
    public String postcode = "W1T 1JY";

}

