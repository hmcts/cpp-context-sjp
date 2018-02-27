package uk.gov.moj.sjp.it.command;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.sjp.it.command.builder.AddressBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.google.common.collect.Lists;

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
        JsonObjectBuilder payload = createObjectBuilder();

        payload.add("id", payloadBuilder.id.toString());
        payload.add("urn", payloadBuilder.urn);
        payload.add("ptiUrn", payloadBuilder.urn);
        payload.add("prosecutingAuthority", payloadBuilder.prosecutingAuthority.name());
        payload.add("initiationCode", "J");
        payload.add("summonsCode", "M");
        payload.add("libraOriginatingOrg", "GAFTL00");
        payload.add("libraHearingLocation", "B01CE03");
        payload.add("dateOfHearing", "2016-01-01");
        payload.add("timeOfHearing", "11:00");
        payload.add("costs", payloadBuilder.costs.doubleValue());
        payload.add("postingDate", LocalDates.to(payloadBuilder.postingDate));
        payload.add("defendant", createObjectBuilder()
                .add("title", payloadBuilder.defendantBuilder.title)
                .add("firstName", payloadBuilder.defendantBuilder.firstName)
                .add("lastName", payloadBuilder.defendantBuilder.lastName)
                .add("dateOfBirth", LocalDates.to(payloadBuilder.defendantBuilder.dateOfBirth))
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
                )
        );

        return payload.build();
    }

    public static class CreateCasePayloadBuilder {
        public static final String PROSECUTING_AUTHORITY_PREFIX = TFL.name();

        UUID id;
        String urn;
        ProsecutingAuthority prosecutingAuthority;
        BigDecimal costs;
        LocalDate postingDate;

        DefendantBuilder defendantBuilder;

        List<OffenceBuilder> offenceBuilders;

        private CreateCasePayloadBuilder() {
            this.prosecutingAuthority = ProsecutingAuthority.TFL;
            this.costs = BigDecimal.valueOf(1.23);
            this.postingDate = LocalDate.of(2015, 12, 2);

            this.defendantBuilder = DefendantBuilder.withDefaults();
            this.offenceBuilders = Lists.newArrayList(OffenceBuilder.withDefaults());
            this.id = UUID.randomUUID();
            this.urn = PROSECUTING_AUTHORITY_PREFIX + RandomGenerator.integer(100000000, 999999999).next();
            this.prosecutingAuthority = ProsecutingAuthority.TFL;

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

        public CreateCasePayloadBuilder withProsecutingAuthority(final ProsecutingAuthority prosecutingAuthority) {
            this.prosecutingAuthority = prosecutingAuthority;
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

    }

    public static class DefendantBuilder {
        String title;
        String firstName;
        String lastName;
        LocalDate dateOfBirth;
        String gender;
        int numPreviousConvictions;

        AddressBuilder addressBuilder;

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
            builder.addressBuilder = AddressBuilder.withDefaults();

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

        public AddressBuilder getAddressBuilder() {
            return addressBuilder;
        }
    }

    public static class OffenceBuilder {
        UUID id;
        String libraOffenceCode;
        LocalDate chargeDate;
        int libraOffenceDateCode;
        LocalDate offenceDate;
        String offenceWording;
        String prosecutionFacts;
        String witnessStatement;
        BigDecimal compensation;

        private OffenceBuilder() {

        }

        public static OffenceBuilder withDefaults() {
            final OffenceBuilder builder = new OffenceBuilder();

            builder.libraOffenceCode = "PS00001";
            builder.chargeDate = LocalDates.from("2016-01-01");
            builder.libraOffenceDateCode = 1;
            builder.offenceDate = LocalDates.from("2016-01-01");
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
