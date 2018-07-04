package uk.gov.moj.sjp.it.command;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.sjp.it.command.builder.AddressBuilder;
import uk.gov.moj.sjp.it.command.builder.ContactDetailsBuilder;
import uk.gov.moj.sjp.it.util.UrnProvider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.RandomStringUtils;

public class CreateCase {

    private static final String WRITE_MEDIA_TYPE = "application/vnd.sjp.create-sjp-case+json";
    private final CreateCasePayloadBuilder payloadBuilder;

    public CreateCase(CreateCasePayloadBuilder payloadBuilder) {
        this.payloadBuilder = payloadBuilder;
    }

    public static void createCaseForPayloadBuilder(CreateCasePayloadBuilder payloadBuilder) {
        new CreateCase(payloadBuilder)
                .createCase();
    }

    private void createCase() {
        validatePayload(payloadBuilder);
        final JsonObject payload = toJsonObjectRepresentingPayload(payloadBuilder);
        makePostCall("/cases", WRITE_MEDIA_TYPE, payload.toString());
    }

    private void validatePayload(final CreateCasePayloadBuilder payloadBuilder) {
        Objects.requireNonNull(payloadBuilder.id, "ID is required");
        Objects.requireNonNull(payloadBuilder.urn, "URN is required");
        Objects.requireNonNull(payloadBuilder.prosecutingAuthority, "Prosecuting authority is required");
        Objects.requireNonNull(payloadBuilder.postingDate, "Posting date is required");
        Objects.requireNonNull(payloadBuilder.defendantBuilder, "Defendant is required");
        Objects.requireNonNull(payloadBuilder.offenceBuilders, "One offence is required");

        if (payloadBuilder.offenceBuilders.size() > 1) {
            throw new IllegalArgumentException("Only one offence is supported");
        }

        final OffenceBuilder offenceBuilder = payloadBuilder.offenceBuilders.get(0);
        Objects.requireNonNull(offenceBuilder.id, "ID is required for offence");
        Objects.requireNonNull(offenceBuilder.libraOffenceCode, "Libra offence code is required for offence");
        Objects.requireNonNull(offenceBuilder.chargeDate, "Charge date is required for offence");
        Objects.requireNonNull(offenceBuilder.offenceDate, "Offence date is required for offence");
        Objects.requireNonNull(offenceBuilder.offenceWording, "Offence wording is required for offence");
    }

    private JsonObject toJsonObjectRepresentingPayload(final CreateCasePayloadBuilder payloadBuilder) {
        final JsonObjectBuilder payload = createObjectBuilder();

        payload.add("id", payloadBuilder.id.toString());
        payload.add("urn", payloadBuilder.urn);
        payload.add("enterpriseId", payloadBuilder.enterpriseId);
        payload.add("prosecutingAuthority", payloadBuilder.prosecutingAuthority.name());
        payload.add("costs", payloadBuilder.costs.doubleValue());
        payload.add("postingDate", LocalDates.to(payloadBuilder.postingDate));

        final JsonObjectBuilder defendantBuilder = createObjectBuilder()
                .add("title", payloadBuilder.defendantBuilder.title)
                .add("firstName", payloadBuilder.defendantBuilder.firstName)
                .add("lastName", payloadBuilder.defendantBuilder.lastName)

                .add("gender", payloadBuilder.defendantBuilder.gender)
                .add("numPreviousConvictions", payloadBuilder.defendantBuilder.numPreviousConvictions)
                .add("address", createObjectBuilder()
                        .add("address1", payloadBuilder.defendantBuilder.addressBuilder.getAddress1())
                        .add("address2", payloadBuilder.defendantBuilder.addressBuilder.getAddress2())
                        .add("address3", payloadBuilder.defendantBuilder.addressBuilder.getAddress3())
                        .add("address4", payloadBuilder.defendantBuilder.addressBuilder.getAddress4())
                        .add("postcode", payloadBuilder.defendantBuilder.addressBuilder.getPostcode())
                )
                .add("offences", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("id", payloadBuilder.offenceBuilders.get(0).id.toString())
                                .add("prosecutorCaseId", "UNUSED")
                                .add("offenceSequenceNo", 1)
                                .add("libraOffenceCode", payloadBuilder.offenceBuilders.get(0).libraOffenceCode)
                                .add("chargeDate", LocalDates.to(payloadBuilder.offenceBuilders.get(0).chargeDate))
                                .add("libraOffenceDateCode", payloadBuilder.offenceBuilders.get(0).libraOffenceDateCode)
                                .add("offenceDate", LocalDates.to(payloadBuilder.offenceBuilders.get(0).offenceDate))
                                .add("offenceWording", payloadBuilder.offenceBuilders.get(0).offenceWording)
                                .add("prosecutionFacts", payloadBuilder.offenceBuilders.get(0).prosecutionFacts)
                                .add("witnessStatement", payloadBuilder.offenceBuilders.get(0).witnessStatement)
                                .add("compensation", payloadBuilder.offenceBuilders.get(0).compensation.doubleValue())
                        )
                );

        Optional.ofNullable(payloadBuilder.defendantBuilder.dateOfBirth).map(LocalDates::to)
                .ifPresent(dateOfBirth -> defendantBuilder.add("dateOfBirth", dateOfBirth));

        payload.add("defendant", defendantBuilder);

        return payload.build();
    }

    public static class CreateCasePayloadBuilder {
        private UUID id;
        private String urn;
        private String enterpriseId;
        private ProsecutingAuthority prosecutingAuthority;
        private BigDecimal costs;
        private LocalDate postingDate;
        private DefendantBuilder defendantBuilder;
        private List<OffenceBuilder> offenceBuilders;

        private CreateCasePayloadBuilder() {
            this.prosecutingAuthority = ProsecutingAuthority.TFL;
            this.costs = BigDecimal.valueOf(1.23);
            this.postingDate = LocalDate.of(2015, 12, 2);
            this.defendantBuilder = DefendantBuilder.withDefaults();
            this.offenceBuilders = Lists.newArrayList(OffenceBuilder.withDefaults());
            this.id = UUID.randomUUID();
            this.urn = UrnProvider.generate(prosecutingAuthority);
            this.enterpriseId = RandomStringUtils.randomAlphanumeric(12).toUpperCase();
            this.getOffenceBuilder().withId(UUID.randomUUID());
        }

        public static CreateCasePayloadBuilder withDefaults() {
            return new CreateCasePayloadBuilder();
        }

        public CreateCasePayloadBuilder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public CreateCasePayloadBuilder withUrn(final String urn) {
            this.urn = urn;
            return this;
        }

        public CreateCasePayloadBuilder withOffenceId(final UUID offenceId) {
            this.offenceBuilders.get(0).withId(offenceId);
            return this;
        }

        public CreateCasePayloadBuilder withProsecutingAuthority(final ProsecutingAuthority prosecutingAuthority) {
            this.prosecutingAuthority = prosecutingAuthority;
            this.urn = UrnProvider.generate(prosecutingAuthority);
            return this;
        }

        public CreateCasePayloadBuilder withPostingDate(final LocalDate postingDate) {
            this.postingDate = postingDate;
            return this;
        }

        public CreateCasePayloadBuilder withDefendantBuilder(final DefendantBuilder defendantBuilder) {
            this.defendantBuilder = defendantBuilder;
            return this;
        }

        public OffenceBuilder getOffenceBuilder() {
            return offenceBuilders.get(0);
        }

        public DefendantBuilder getDefendantBuilder() {
            return defendantBuilder;
        }

        public UUID getOffenceId() {
            return offenceBuilders.get(0).id;
        }

        public UUID getId() {
            return id;
        }

        public String getUrn() {
            return urn;
        }

        public String getEnterpriseId() {
            return enterpriseId;
        }
    }

    public static class DefendantBuilder {
        String title;
        String firstName;
        String lastName;
        LocalDate dateOfBirth;
        String gender;
        int numPreviousConvictions;
        String nationalInsuranceNumber;

        AddressBuilder addressBuilder;
        ContactDetailsBuilder contactDetailsBuilder;

        private DefendantBuilder() {

        }

        public static DefendantBuilder withDefaults() {
            final DefendantBuilder builder = new DefendantBuilder();

            builder.title = "Mr";
            builder.firstName = "David";
            builder.lastName = "LLOYD";
            builder.dateOfBirth = LocalDates.from("1980-07-15");
            builder.gender = "Male";
            builder.numPreviousConvictions = 2;
            builder.nationalInsuranceNumber = "NIN";
            builder.addressBuilder = AddressBuilder.withDefaults();
            builder.contactDetailsBuilder = ContactDetailsBuilder.withDefaults();

            return builder;
        }

        public DefendantBuilder withLastName(final String lastName) {
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

        public String getGender() {
            return gender;
        }

        public int getNumPreviousConvictions() {
            return numPreviousConvictions;
        }

        public String getNationalInsuranceNumber() {
            return nationalInsuranceNumber;
        }

        public AddressBuilder getAddressBuilder() {
            return addressBuilder;
        }

        public ContactDetailsBuilder getContactDetailsBuilder() {
            return contactDetailsBuilder;
        }

    }

    public static class OffenceBuilder {
        private UUID id;
        private String libraOffenceCode;
        private LocalDate chargeDate;
        private int libraOffenceDateCode;
        private LocalDate offenceDate;
        private String offenceWording;
        private String prosecutionFacts;
        private String witnessStatement;
        private BigDecimal compensation;

        private OffenceBuilder() {

        }

        public static OffenceBuilder withDefaults() {
            final OffenceBuilder builder = new OffenceBuilder();

            builder.libraOffenceCode = "PS00001";
            builder.chargeDate = LocalDate.of(2016, 1, 1);
            builder.libraOffenceDateCode = 1;
            builder.offenceDate = LocalDate.of(2016, 1, 1);
            builder.offenceWording = "Committed some offence";
            builder.prosecutionFacts = "No ticket at the gates, forgery";
            builder.witnessStatement = "Jumped over the barriers";
            builder.compensation = BigDecimal.valueOf(2.34);

            return builder;
        }

        public OffenceBuilder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public UUID getId() {
            return id;
        }

        public String getLibraOffenceCode() {
            return libraOffenceCode;
        }

        public LocalDate getChargeDate() {
            return chargeDate;
        }

        public int getLibraOffenceDateCode() {
            return libraOffenceDateCode;
        }

        public LocalDate getOffenceDate() {
            return offenceDate;
        }

        public String getOffenceWording() {
            return offenceWording;
        }

        public String getProsecutionFacts() {
            return prosecutionFacts;
        }

        public String getWitnessStatement() {
            return witnessStatement;
        }

        public BigDecimal getCompensation() {
            return compensation;
        }
    }
}
