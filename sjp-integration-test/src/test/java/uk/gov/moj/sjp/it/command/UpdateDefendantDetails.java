package uk.gov.moj.sjp.it.command;

import static java.util.Optional.ofNullable;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.getPostCallResponse;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;

import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.moj.sjp.it.command.builder.AddressBuilder;
import uk.gov.moj.sjp.it.command.builder.ContactDetailsBuilder;

import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

public class UpdateDefendantDetails {

    private final DefendantDetailsPayloadBuilder payloadBuilder;

    public UpdateDefendantDetails(final DefendantDetailsPayloadBuilder payloadBuilder) {
        this.payloadBuilder = payloadBuilder;
    }

    public static void updateDefendantDetailsForCaseAndPayload(UUID caseId, UUID defendantId, DefendantDetailsPayloadBuilder payloadBuilder) {
        new UpdateDefendantDetails(payloadBuilder).updateDefendantDetails(caseId, defendantId);
    }

    public static void acceptDefendantPendingChangesForCaseAndPayload(UUID caseId, UUID defendantId, DefendantDetailsPayloadBuilder payloadBuilder) {
        new UpdateDefendantDetails(payloadBuilder).acceptDefendantPendingChanges(caseId, defendantId);
    }

    public static String updateDefendantDetailsForCaseAndPayload(UUID caseId, UUID defendantId, DefendantDetailsPayloadBuilder payloadBuilder, Response.Status status) {
        return new UpdateDefendantDetails(payloadBuilder).updateDefendantDetails(caseId, defendantId, status);
    }

    public static void acknowledgeDefendantDetailsUpdates(final UUID caseId, final UUID defendantId) {
        String url = String.format("/cases/%s/defendant/%s", caseId, defendantId);
        makePostCall(url, "application/vnd.sjp.acknowledge-defendant-details-updates+json", null);
    }

    public void updateDefendantDetails(UUID caseId, UUID defendantId) {
        final JsonObject payload = preparePayload(payloadBuilder);
        String url = String.format("/cases/%s/defendant/%s", caseId, defendantId);
        makePostCall(url, "application/vnd.sjp.update-defendant-details+json", payload.toString());
    }

    public void acceptDefendantPendingChanges(UUID caseId, UUID defendantId) {
        final JsonObject payload = preparePayload(payloadBuilder);
        String url = String.format("/cases/%s/defendant/%s", caseId, defendantId);
        makePostCall(url, "application/vnd.sjp.accept-pending-defendant-changes+json", payload.toString());
    }

    public static void rejectDefendantPendingChanges(UUID caseId, UUID defendantId) {
        final JsonObject payload = createObjectBuilder().add("caseId", caseId.toString()).add("defendantId", defendantId.toString()).build();
        String url = String.format("/cases/%s/defendant/%s", caseId, defendantId);
        makePostCall(url, "application/vnd.sjp.reject-pending-defendant-changes+json", payload.toString());
    }

    private String updateDefendantDetails(final UUID caseId, final UUID defendantId, final Response.Status status) {
        final JsonObject payload = preparePayload(payloadBuilder);
        String url = String.format("/cases/%s/defendant/%s", caseId, defendantId);
        return getPostCallResponse(url, "application/vnd.sjp.update-defendant-details+json", payload.toString(), status);
    }

    private JsonObject preparePayload(DefendantDetailsPayloadBuilder payloadBuilder) {

        final JsonObjectBuilder defendantDetailsUpdateBuilder = createObjectBuilder();

        ofNullable(payloadBuilder.getTitle())
                .ifPresent(title -> defendantDetailsUpdateBuilder.add("title", title));
        ofNullable(payloadBuilder.getFirstName())
                .ifPresent(firstName -> defendantDetailsUpdateBuilder.add("firstName", payloadBuilder.getFirstName()));

        ofNullable(payloadBuilder.getLastName())
                .ifPresent(lastName -> defendantDetailsUpdateBuilder.add("lastName", payloadBuilder.getLastName()));

        ofNullable(payloadBuilder.getLegalEntityName())
                .ifPresent(lastName -> defendantDetailsUpdateBuilder.add("legalEntityName", payloadBuilder.getLegalEntityName()));

        ofNullable(payloadBuilder.getDateOfBirth())
                .map(dateOfBirth -> LocalDates.to(payloadBuilder.getDateOfBirth()))
                .ifPresent(dateOfBirth -> defendantDetailsUpdateBuilder.add("dateOfBirth", dateOfBirth));

        ofNullable(payloadBuilder.getGender())
                .ifPresent(gender -> defendantDetailsUpdateBuilder.add("gender", gender.toString()));

        ofNullable(payloadBuilder.getContactDetailsBuilder())
                .map(ContactDetailsBuilder::getEmail)
                .ifPresent(email -> defendantDetailsUpdateBuilder.add("email", email));

        ofNullable(payloadBuilder.getAddressBuilder()).ifPresent(builder -> {
            final JsonObjectBuilder addressBuilder = createObjectBuilder();
            ofNullable(builder.getAddress1()).ifPresent(a1 -> addressBuilder.add("address1", a1));
            ofNullable(builder.getAddress2()).ifPresent(a2 -> addressBuilder.add("address2", a2));
            ofNullable(builder.getAddress3()).ifPresent(a3 -> addressBuilder.add("address3", a3));
            ofNullable(builder.getAddress4()).ifPresent(a4 -> addressBuilder.add("address4", a4));
            ofNullable(builder.getAddress5()).ifPresent(a5 -> addressBuilder.add("address5", a5));
            ofNullable(builder.getPostcode()).ifPresent(p -> addressBuilder.add("postcode", p));

            defendantDetailsUpdateBuilder.add("address", addressBuilder);
        });


        ofNullable(payloadBuilder.getContactDetailsBuilder())
                .ifPresent(contactDetails -> defendantDetailsUpdateBuilder.add("contactNumber", createObjectBuilder()
                        .add("home", contactDetails.getHome())
                        .add("mobile", contactDetails.getMobile())
                ));

        ofNullable(payloadBuilder.getNationalInsuranceNumber())
                .ifPresent(nationalInsuranceNumber -> defendantDetailsUpdateBuilder.add("nationalInsuranceNumber", payloadBuilder.getNationalInsuranceNumber()));

        ofNullable(payloadBuilder.getDriverNumber())
                .ifPresent(driverNumber -> defendantDetailsUpdateBuilder.add("driverNumber", driverNumber));

        ofNullable(payloadBuilder.getDriverLicenceDetails())
                .ifPresent(driverLicenceDetails -> defendantDetailsUpdateBuilder.add("driverLicenceDetails", driverLicenceDetails));

        return defendantDetailsUpdateBuilder.build();
    }

    public static class DefendantDetailsPayloadBuilder {

        String title;
        String firstName;
        String lastName;
        String legalEntityName;
        LocalDate dateOfBirth;
        Gender gender;

        AddressBuilder addressBuilder;
        // this is current abstraction of defendant details, the fact the number and email is handled differently
        // by this particular endpoint is hidden here
        ContactDetailsBuilder contactDetailsBuilder;

        String nationalInsuranceNumber;
        String driverNumber;
        String driverLicenceDetails;

        private DefendantDetailsPayloadBuilder() {
        }

        public static DefendantDetailsPayloadBuilder withDefaults() {
            return new DefendantDetailsPayloadBuilder()
                    .withTitle("Mr")
                    .withFirstName("David")
                    .withLastName("SMITH")
                    .withDateOfBirth(LocalDates.from("1980-07-16"))
                    .withGender(Gender.MALE)
                    .withAddress(AddressBuilder.withDefaults()
                            .withAddress1("14 Shaftesbury Road"))
                    .withDriverNumber("MORGA753116SM9IJ")
                    .withContact(ContactDetailsBuilder.withDefaults())
                    .withNationalInsuranceNumber("QQ 12 34 56 C");
        }

        public static DefendantDetailsPayloadBuilder builder() {
            return new DefendantDetailsPayloadBuilder();
        }

        public DefendantDetailsPayloadBuilder withFirstName(final String firstName) {
            this.firstName = firstName;
            return this;
        }

        public DefendantDetailsPayloadBuilder withLastName(final String lastName) {
            this.lastName = lastName;
            return this;
        }

        public DefendantDetailsPayloadBuilder withTitle(final String title) {
            this.title = title;
            return this;
        }

        public DefendantDetailsPayloadBuilder withDateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public DefendantDetailsPayloadBuilder withGender(final Gender gender) {
            this.gender = gender;
            return this;
        }

        public DefendantDetailsPayloadBuilder withAddress(final AddressBuilder addressBuilder) {
            this.addressBuilder = addressBuilder;
            return this;
        }

        public DefendantDetailsPayloadBuilder withContact(final ContactDetailsBuilder contactDetailsBuilder) {
            this.contactDetailsBuilder = contactDetailsBuilder;
            return this;
        }

        public DefendantDetailsPayloadBuilder withDriverNumber(final String driverNumber) {
            this.driverNumber = driverNumber;
            return this;
        }

        public DefendantDetailsPayloadBuilder withDriverLicenceDetails(final String driverLicenceDetails) {
            this.driverLicenceDetails = driverLicenceDetails;
            return this;
        }

        public DefendantDetailsPayloadBuilder withNationalInsuranceNumber(final String nationalInsuranceNumber) {
            this.nationalInsuranceNumber = nationalInsuranceNumber;
            return this;
        }

        public DefendantDetailsPayloadBuilder withLegalEntityName(final String legalEntityName) {
            this.legalEntityName = legalEntityName;
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

        public String getDriverNumber() {
            return driverNumber;
        }

        public String getDriverLicenceDetails() {
            return driverLicenceDetails;
        }

        public String getLegalEntityName() {
            return legalEntityName;
        }
    }
}
