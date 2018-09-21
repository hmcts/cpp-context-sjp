package uk.gov.moj.sjp.it.model;

import uk.gov.justice.json.schemas.domains.sjp.Language;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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

    @Builder.Default
    public Language documentationLanguage = Language.W;

    @Builder.Default
    public String languageNeeds = "No special needs";

    @Builder.Default
    public String nationalInsuranceNumber = "AB123456C";

    @Builder.Default
    public String driverNumber = "Driver number";

    @Valid
    @NotNull(message = "ContactDetails is required")
    @Builder.Default
    public ContactDetails contactDetails = ContactDetails.builder().build();

}
