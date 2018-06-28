package uk.gov.moj.sjp.it.model;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import lombok.Builder;

@Builder
public class Defendant {

    @Builder.Default
    public String title = "Mr";

    @Builder.Default
    public String firstName = "David";

    @Builder.Default
    public String lastName = "LLOYD";

    @Builder.Default
    public String gender = "Male";

    @Builder.Default
    public String dateOfBirth = "1980-07-15";

    @Builder.Default
    public Integer numPreviousConvictions = 2;

    @Valid
    @Builder.Default
    public Address address = Address.builder().build();

    @Valid
    @Builder.Default
    @Size(min = 1, max = 1, message = "Only one offence is supported")
    public Offence[] offences = {Offence.builder().build()};

}
