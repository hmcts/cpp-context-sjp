package uk.gov.moj.sjp.it.command;

import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;

import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.moj.sjp.it.command.builder.AddressBuilder;
import uk.gov.moj.sjp.it.command.builder.ContactDetailsBuilder;

import java.time.LocalDate;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

public class UpdateDefendantDetails {


    private final DefendantDetailsPayloadBuilder payloadBuilder;

    public UpdateDefendantDetails(final DefendantDetailsPayloadBuilder payloadBuilder) {
        this.payloadBuilder = payloadBuilder;
    }

    public static void updateDefendantDetailsForCaseAndPayload(UUID caseId, UUID defendantId, DefendantDetailsPayloadBuilder payloadBuilder) {
        new UpdateDefendantDetails(payloadBuilder).updateDefendantDetails(caseId, defendantId);
    }

    public void updateDefendantDetails(UUID caseId, UUID defendantId) {
        final JsonObject payload = preparePayload(payloadBuilder);
        String url = String.format("/cases/%s/defendant/%s", caseId, defendantId);
        makePostCall(url, "application/vnd.sjp.update-defendant-details+json", payload.toString());
    }

    private JsonObject preparePayload(DefendantDetailsPayloadBuilder payloadBuilder) {
        return Json.createObjectBuilder()
                .add("title", payloadBuilder.getTitle())
                .add("firstName", payloadBuilder.getFirstName())
                .add("lastName", payloadBuilder.getLastName())
                .add("dateOfBirth", LocalDates.to(payloadBuilder.getDateOfBirth()))
                .add("gender", payloadBuilder.getGender().toString())
                .add("email", payloadBuilder.getContactDetailsBuilder().getEmail())
                .add("address", Json.createObjectBuilder()
                        .add("address1", payloadBuilder.getAddressBuilder().getAddress1())
                        .add("address2", payloadBuilder.getAddressBuilder().getAddress2())
                        .add("address3", payloadBuilder.getAddressBuilder().getAddress3())
                        .add("address4", payloadBuilder.getAddressBuilder().getAddress4())
                        .add("postcode", payloadBuilder.getAddressBuilder().getPostcode())
                )
                .add("contactNumber", Json.createObjectBuilder()
                        .add("home", payloadBuilder.getContactDetailsBuilder().getHome())
                        .add("mobile", payloadBuilder.getContactDetailsBuilder().getMobile())
                )
                .add("nationalInsuranceNumber", payloadBuilder.getNationalInsuranceNumber())
                .build();
    }

    public static class DefendantDetailsPayloadBuilder {

        String title;
        String firstName;
        String lastName;
        LocalDate dateOfBirth;
        Gender gender;

        AddressBuilder addressBuilder;
        // this is current abstraction of defendant details, the fact the number and email is handled differently
        // by this particular endpoint is hidden here
        ContactDetailsBuilder contactDetailsBuilder;

        String nationalInsuranceNumber;

        private DefendantDetailsPayloadBuilder() {
            title = "Mr";
            firstName = "David";
            lastName = "SMITH";
            dateOfBirth = LocalDates.from("1980-07-16");
            gender = Gender.MALE;

            contactDetailsBuilder = ContactDetailsBuilder.withDefaults();
            addressBuilder = AddressBuilder.withDefaults()
                    .withAddress1("14 Shaftesbury Road");

            nationalInsuranceNumber = "QQ 12 34 56 C";
        }

        public static DefendantDetailsPayloadBuilder withDefaults() {
            return new DefendantDetailsPayloadBuilder();
        }

        public DefendantDetailsPayloadBuilder withLastName(final String lastName) {
            this.lastName = lastName;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public LocalDate getDateOfBirth() {
            return dateOfBirth;
        }

        public Gender getGender() {
            return gender;
        }

        public AddressBuilder getAddressBuilder() {
            return addressBuilder;
        }

        public ContactDetailsBuilder getContactDetailsBuilder() {
            return contactDetailsBuilder;
        }

        public String getNationalInsuranceNumber() {
            return nationalInsuranceNumber;
        }
    }
}
