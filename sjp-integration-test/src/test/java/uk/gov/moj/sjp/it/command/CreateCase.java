package uk.gov.moj.sjp.it.command;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.sjp.it.Constants.DEFAULT_OFFENCE_CODE;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.getPostCallResponse;

import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.justice.json.schemas.domains.sjp.Language;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.moj.sjp.it.command.builder.AddressBuilder;
import uk.gov.moj.sjp.it.command.builder.ContactDetailsBuilder;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.util.UrnProvider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.RandomStringUtils;

public class CreateCase {

    private static final String WRITE_MEDIA_TYPE = "application/vnd.sjp.create-sjp-case+json";
    private final CreateCasePayloadBuilder payloadBuilder;

    private CreateCase(final CreateCasePayloadBuilder payloadBuilder) {
        this.payloadBuilder = payloadBuilder;
    }

    public static void createCaseForPayloadBuilder(final CreateCasePayloadBuilder payloadBuilder) {
        new CreateCase(payloadBuilder).createCase();
    }

    public static String createCaseForPayloadBuilder(final CreateCasePayloadBuilder payloadBuilder, final Response.Status status) {
        return new CreateCase(payloadBuilder).createCase(status);
    }

    private void createCase() {
        createCase(ACCEPTED);
    }

    private String createCase(final Response.Status status) {
        validatePayload(payloadBuilder);
        final JsonObject payload = toJsonObjectRepresentingPayload(payloadBuilder);
        return getPostCallResponse("/cases", WRITE_MEDIA_TYPE, payload.toString(), status);
    }

    private void validatePayload(final CreateCasePayloadBuilder payloadBuilder) {
        requireNonNull(payloadBuilder.id, "ID is required");
        requireNonNull(payloadBuilder.urn, "URN is required");
        requireNonNull(payloadBuilder.prosecutingAuthority, "Prosecuting authority is required");
        requireNonNull(payloadBuilder.postingDate, "Posting date is required");
        requireNonNull(payloadBuilder.defendantBuilder, "Defendant is required");
        requireNonNull(payloadBuilder.offenceBuilders, "One offence is required");


        payloadBuilder.offenceBuilders.forEach(offenceBuilder -> {
            requireNonNull(offenceBuilder.id, "ID is required for offence");
            requireNonNull(offenceBuilder.libraOffenceCode, "Libra offence code is required for offence");
            requireNonNull(offenceBuilder.chargeDate, "Charge date is required for offence");
            requireNonNull(offenceBuilder.offenceCommittedDate, "Offence committed date is required for offence");
            requireNonNull(offenceBuilder.offenceWording, "Offence wording is required for offence");
        });
    }

    private JsonObject toJsonObjectRepresentingPayload(final CreateCasePayloadBuilder payloadBuilder) {
        final JsonObjectBuilder payload = createObjectBuilder();

        payload.add("id", payloadBuilder.id.toString());
        payload.add("urn", payloadBuilder.urn);
        payload.add("enterpriseId", payloadBuilder.enterpriseId);
        payload.add("prosecutingAuthority", payloadBuilder.prosecutingAuthority.name());
        payload.add("costs", payloadBuilder.costs.doubleValue());
        payload.add("postingDate", LocalDates.to(payloadBuilder.postingDate));

        final JsonObjectBuilder addressBuilder = createObjectBuilder();
        final JsonObjectBuilder contactDetailsBuilder = createObjectBuilder();

        ofNullable(payloadBuilder.defendantBuilder.addressBuilder.getAddress1()).ifPresent(a1 -> addressBuilder.add("address1", a1));
        ofNullable(payloadBuilder.defendantBuilder.addressBuilder.getAddress2()).ifPresent(a2 -> addressBuilder.add("address2", a2));
        ofNullable(payloadBuilder.defendantBuilder.addressBuilder.getAddress3()).ifPresent(a3 -> addressBuilder.add("address3", a3));
        ofNullable(payloadBuilder.defendantBuilder.addressBuilder.getAddress4()).ifPresent(a4 -> addressBuilder.add("address4", a4));
        ofNullable(payloadBuilder.defendantBuilder.addressBuilder.getAddress5()).ifPresent(a5 -> addressBuilder.add("address5", a5));
        ofNullable(payloadBuilder.defendantBuilder.addressBuilder.getPostcode()).ifPresent(p -> addressBuilder.add("postcode", p));

        ofNullable(payloadBuilder.defendantBuilder.contactDetailsBuilder.getHome()).ifPresent(h -> contactDetailsBuilder.add("home", h));
        ofNullable(payloadBuilder.defendantBuilder.contactDetailsBuilder.getMobile()).ifPresent(m -> contactDetailsBuilder.add("mobile", m));
        ofNullable(payloadBuilder.defendantBuilder.contactDetailsBuilder.getEmail()).ifPresent(e -> contactDetailsBuilder.add("email", e));
        ofNullable(payloadBuilder.defendantBuilder.contactDetailsBuilder.getEmail2()).ifPresent(e2 -> contactDetailsBuilder.add("email2", e2));

        final JsonObjectBuilder defendantBuilder = createObjectBuilder();
        if (payloadBuilder.defendantBuilder.legalEntityName != null) {
            defendantBuilder.add("legalEntityName", payloadBuilder.defendantBuilder.legalEntityName);
        } else if (payloadBuilder.defendantBuilder.firstName != null && payloadBuilder.defendantBuilder.lastName != null) {
            defendantBuilder.add("firstName", payloadBuilder.defendantBuilder.firstName)
                    .add("lastName", payloadBuilder.defendantBuilder.lastName);
        }
        defendantBuilder
                .add("id", payloadBuilder.defendantBuilder.id.toString())
                .add("numPreviousConvictions", payloadBuilder.defendantBuilder.numPreviousConvictions)
                .add("address", addressBuilder)
                .add("contactDetails", contactDetailsBuilder)
                .add("offences", createOffencesBuilder())
                .add("asn", payloadBuilder.defendantBuilder.getAsn())
                .add("pncIdentifier", payloadBuilder.defendantBuilder.getPncIdentifier());

        ofNullable(payloadBuilder.defendantBuilder.getPcqId())
                .ifPresent(pcqId -> defendantBuilder.add("pcqId", payloadBuilder.defendantBuilder.getPcqId().toString()));

        if (nonNull(payloadBuilder.defendantBuilder.title)) {
            defendantBuilder.add("title", payloadBuilder.defendantBuilder.title);
        }
        if (nonNull(payloadBuilder.defendantBuilder.hearingLanguage)) {
            defendantBuilder.add("hearingLanguage", payloadBuilder.defendantBuilder.hearingLanguage.toString());
        }
        ofNullable(payloadBuilder.defendantBuilder.dateOfBirth).map(LocalDates::to)
                .ifPresent(dateOfBirth -> defendantBuilder.add("dateOfBirth", dateOfBirth));

        ofNullable(payloadBuilder.defendantBuilder.driverLicenceDetails)
                .ifPresent(driverLicenceDetails -> defendantBuilder.add("driverLicenceDetails", driverLicenceDetails));

        ofNullable(payloadBuilder.defendantBuilder.driverNumber)
                .ifPresent(driverNumber -> defendantBuilder.add("driverNumber", driverNumber));

        ofNullable(payloadBuilder.defendantBuilder.gender)
                .ifPresent(gender -> defendantBuilder.add("gender",gender.toString()));

        ofNullable(payloadBuilder.defendantBuilder.nationalInsuranceNumber)
                .ifPresent(nino -> defendantBuilder.add("nationalInsuranceNumber",nino));

        payload.add("defendant", defendantBuilder);
        return payload.build();
    }

    private JsonArrayBuilder createOffencesBuilder() {
        final JsonArrayBuilder offenceArrayBuilder = createArrayBuilder();
        final AtomicInteger sequenceNumber = new AtomicInteger(1);

        payloadBuilder.offenceBuilders.stream().map(offenceBuilder -> {
            final JsonObjectBuilder offence = createObjectBuilder()
                    .add("id", offenceBuilder.id.toString())
                    .add("offenceSequenceNo", sequenceNumber.getAndAdd(1))
                    .add("libraOffenceCode", offenceBuilder.libraOffenceCode)
                    .add("chargeDate", LocalDates.to(offenceBuilder.chargeDate))
                    .add("libraOffenceDateCode", offenceBuilder.libraOffenceDateCode)
                    .add("offenceCommittedDate", LocalDates.to(offenceBuilder.offenceCommittedDate))
                    .add("offenceWording", offenceBuilder.offenceWording)
                    .add("prosecutionFacts", offenceBuilder.prosecutionFacts)
                    .add("witnessStatement", offenceBuilder.witnessStatement)
                    .add("compensation", offenceBuilder.compensation.doubleValue())
                    .add("vehicleMake", offenceBuilder.vehicleMake)
                    .add("vehicleRegistrationMark", offenceBuilder.vehicleRegistrationMark)
                    .add("backDuty", offenceBuilder.backDuty.doubleValue())
                    .add("backDutyDateFrom", LocalDates.to(offenceBuilder.backDutyDateFrom))
                    .add("backDutyDateTo", LocalDates.to(offenceBuilder.backDutyDateTo));

            ofNullable(offenceBuilder.offenceWordingWelsh)
                    .ifPresent(offenceWordingWelsh -> offence.add("offenceWordingWelsh", offenceWordingWelsh));

            ofNullable(offenceBuilder.endorsable)
                    .ifPresent(endorsable -> offence.add("endorsable", endorsable));

            ofNullable(offenceBuilder.pressRestrictable)
                    .ifPresent(pressRestrictable -> offence.add("pressRestrictable", pressRestrictable));

            ofNullable(offenceBuilder.prosecutorOfferAOCP)
                    .ifPresent(prosecutorOfferAOCP -> offence.add("prosecutorOfferAOCP", prosecutorOfferAOCP));

            return offence;
        }).forEach(offenceArrayBuilder::add);

        return offenceArrayBuilder;
    }

    public static class CreateCasePayloadBuilder {
        private final String enterpriseId;
        private final BigDecimal costs;
        private UUID id;
        private String urn;
        private ProsecutingAuthority prosecutingAuthority;
        private LocalDate postingDate;
        private DefendantBuilder defendantBuilder;
        private List<OffenceBuilder> offenceBuilders;

        private CreateCasePayloadBuilder() {
            this.prosecutingAuthority = ProsecutingAuthority.TFL;
            this.costs = BigDecimal.valueOf(1.23);
            this.postingDate = LocalDate.of(2015, 12, 2);
            this.defendantBuilder = DefendantBuilder.withDefaults();
            this.offenceBuilders = newArrayList(OffenceBuilder.withDefaults());
            this.id = randomUUID();
            this.urn = UrnProvider.generate(prosecutingAuthority);
            this.enterpriseId = RandomStringUtils.randomAlphanumeric(12).toUpperCase();
            this.getOffenceBuilder().withId(randomUUID());
        }

        public static CreateCasePayloadBuilder defaultCaseBuilder() {
            return withDefaults();
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

        public CreateCasePayloadBuilder withDefendantId(final UUID defendantId) {
            this.defendantBuilder.withId(defendantId);
            return this;
        }

        public CreateCasePayloadBuilder withDefendantDateOfBirth(final LocalDate dateOfBirth) {
            this.defendantBuilder.withDateOfBirth(dateOfBirth);
            return this;
        }

        public CreateCasePayloadBuilder withOffenceId(final UUID offenceId) {
            this.offenceBuilders.get(0).withId(offenceId);
            return this;
        }

        public CreateCasePayloadBuilder withOffenceCode(final String offenceCode) {
            this.offenceBuilders.get(0).withLibraOffenceCode(offenceCode);
            return this;
        }

        public CreateCasePayloadBuilder withLibraOffenceDateCode(final Integer offenceDateCode) {
            this.offenceBuilders.get(0).withLibraOffenceDateCode(offenceDateCode);
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

        public List<OffenceBuilder> getOffenceBuilders() {
            return offenceBuilders;
        }

        public Collection<UUID> getOffenceIds() {
            return offenceBuilders.stream()
                    .map(OffenceBuilder::getId)
                    .collect(Collectors.toSet());
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

        public LocalDate getPostingDate() {
            return postingDate;
        }

        public ProsecutingAuthority getProsecutingAuthority() {
            return prosecutingAuthority;
        }

        public String getEnterpriseId() {
            return enterpriseId;
        }

        public CreateCasePayloadBuilder withOffenceBuilders(final List<OffenceBuilder> offenceBuilders) {
            this.offenceBuilders = offenceBuilders;
            return this;
        }

        public CreateCasePayloadBuilder withOffenceBuilders(final OffenceBuilder... offenceBuilders) {
            this.offenceBuilders = Arrays.asList(offenceBuilders);
            return this;
        }

        public CreateCasePayloadBuilder withOffenceBuilder(final OffenceBuilder offenceBuilder) {
            this.offenceBuilders = newArrayList(offenceBuilder);
            return this;
        }

        public CreateCasePayloadBuilder setOffencesPressRestrictable(final boolean pressRestrictable) {
            this.getOffenceBuilders().stream().forEach(o -> o.withPressRestrictable(pressRestrictable));
            return this;
        }
    }

    public static class DefendantBuilder {
        UUID id;
        String title;
        String firstName;
        String lastName;
        LocalDate dateOfBirth;
        Gender gender;
        int numPreviousConvictions;
        String nationalInsuranceNumber;
        String driverNumber;
        String driverLicenceDetails;
        AddressBuilder addressBuilder;
        ContactDetailsBuilder contactDetailsBuilder;
        Language hearingLanguage;
        String asn;
        String pncIdentifier;
        String legalEntityName;
        UUID pcqId;

        private DefendantBuilder() {

        }

        public static DefendantBuilder defaultDefendant() {
            return withDefaults();
        }

        public static DefendantBuilder defaultLegalEntityDefendant(){
            final DefendantBuilder builder = new DefendantBuilder();
            builder.id = randomUUID();
            builder.legalEntityName = "Kellogs and Co";
            builder.addressBuilder = AddressBuilder.withDefaults();
            builder.contactDetailsBuilder = ContactDetailsBuilder.withDefaults();
            builder.asn = "asn";
            builder.pncIdentifier = "pncId";
            return builder;
        }

        public static DefendantBuilder withDefaults() {
            final DefendantBuilder builder = new DefendantBuilder();

            builder.id = randomUUID();
            builder.title = "Mr";
            builder.firstName = "David";
            builder.lastName = "LLOYD";
            builder.dateOfBirth = LocalDates.from("1980-07-15");
            builder.gender = Gender.MALE;
            builder.numPreviousConvictions = 2;
            builder.nationalInsuranceNumber = "BB123456B";
            builder.driverNumber = "MORGA753116SM9IJ";
            builder.driverLicenceDetails = "test";
            builder.addressBuilder = AddressBuilder.withDefaults();
            builder.contactDetailsBuilder = ContactDetailsBuilder.withDefaults();
            builder.hearingLanguage = null;
            builder.asn = "asn";
            builder.pncIdentifier = "pncId";
            builder.legalEntityName = null;

            builder.pcqId = randomUUID();
            return builder;
        }

        public static DefendantBuilder withDefaults(UUID pcqId) {
            final DefendantBuilder builder = new DefendantBuilder();

            builder.id = randomUUID();
            builder.title = "Mr";
            builder.firstName = "David";
            builder.lastName = "LLOYD";
            builder.dateOfBirth = LocalDates.from("1980-07-15");
            builder.gender = Gender.MALE;
            builder.numPreviousConvictions = 2;
            builder.nationalInsuranceNumber = "BB123456B";
            builder.driverNumber = "MORGA753116SM9IJ";
            builder.driverLicenceDetails = "test";
            builder.addressBuilder = AddressBuilder.withDefaults();
            builder.contactDetailsBuilder = ContactDetailsBuilder.withDefaults();
            builder.hearingLanguage = null;
            builder.asn = "asn";
            builder.pncIdentifier = "pncId";
            builder.pcqId = pcqId;
            builder.legalEntityName = null;
            return builder;
        }


        public DefendantBuilder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public DefendantBuilder withLastName(final String lastName) {
            this.lastName = lastName;
            return this;
        }

        public DefendantBuilder withRandomLastName() {
            this.lastName = "LastName" + randomUUID();
            return this;
        }

        public DefendantBuilder withFirstName(final String firstName) {
            this.firstName = firstName;
            return this;
        }

        public DefendantBuilder withTitle(final String title) {
            this.title = title;
            return this;
        }

        public DefendantBuilder withNationalInsuranceNumber(final String nationalInsuranceNumber) {
            this.nationalInsuranceNumber = nationalInsuranceNumber;
            return this;
        }

        public DefendantBuilder withDriverNumber(final String driverNumber) {
            this.driverNumber = driverNumber;
            return this;
        }

        public DefendantBuilder withDriverLicenceDetails(final String driverLicenceDetails) {
            this.driverLicenceDetails = driverLicenceDetails;
            return this;
        }

        public DefendantBuilder withAddressBuilder(final AddressBuilder addressBuilder) {
            this.addressBuilder = addressBuilder;
            return this;
        }

        public DefendantBuilder withDateOfBirth(final LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public DefendantBuilder withDefaultShortAddress() {
            this.addressBuilder = AddressBuilder.addressWithMandatoryFieldsOnly();
            return this;
        }

        public DefendantBuilder withHearingLanguage(Language language) {
            this.hearingLanguage = language;
            return this;
        }

        public DefendantBuilder withAsn(final String asn) {
            this.asn = asn;
            return this;
        }

        public DefendantBuilder withPncIdentifier(final String pncIdentifier) {
            this.pncIdentifier = pncIdentifier;
            return this;
        }

        public DefendantBuilder withGender(final Gender gender) {
            this.gender = gender;
            return this;
        }

        public DefendantBuilder withLegalEntityName(final String legalEntityName) {
            this.legalEntityName = legalEntityName;
            return this;
        }

        public DefendantBuilder withPcqId(final UUID pcqId) {
            this.pcqId = pcqId;
            return this;
        }

        public UUID getId() {
            return id;
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

        public Language getHearingLanguage() {
            return hearingLanguage;
        }

        public String getDriverNumber() {
            return driverNumber;
        }

        public String getDriverLicenceDetails() {
            return driverLicenceDetails;
        }

        public String getAsn() {
            return asn;
        }

        public String getPncIdentifier() {
            return pncIdentifier;
        }

        public String getLegalEntityName() {
            return legalEntityName;
        }

        public UUID getPcqId() {
            return pcqId;
        }

    }

    public static class OffenceBuilder {
        private UUID id;
        private String libraOffenceCode;
        private LocalDate chargeDate;
        private int libraOffenceDateCode;
        private LocalDate offenceCommittedDate;
        private String offenceWording;
        private String offenceWordingWelsh;
        private String prosecutionFacts;
        private String witnessStatement;
        private BigDecimal compensation;
        private BigDecimal backDuty;
        private LocalDate backDutyDateFrom;
        private LocalDate backDutyDateTo;
        private String vehicleMake;
        private String vehicleRegistrationMark;
        private int fineLevel;
        private BigDecimal maxFineValue;
        private Boolean endorsable;
        private Boolean pressRestrictable;
        private Boolean prosecutorOfferAOCP;

        private OffenceBuilder() {

        }

        public static OffenceBuilder defaultOffenceBuilder() {
            return withDefaults();
        }

        public static OffenceBuilder withDefaults() {
            final OffenceBuilder builder = new OffenceBuilder();

            builder.libraOffenceCode = DEFAULT_OFFENCE_CODE;
            builder.chargeDate = LocalDate.of(2016, 1, 1);
            builder.libraOffenceDateCode = 1;
            builder.offenceCommittedDate = LocalDate.of(2016, 1, 1);
            builder.offenceWording = "Committed some offence";
            builder.prosecutionFacts = "No ticket at the gates, forgery";
            builder.witnessStatement = "Jumped over the barriers";
            builder.compensation = BigDecimal.valueOf(2.34);
            builder.backDuty = BigDecimal.valueOf(340.30);
            builder.backDutyDateFrom = LocalDate.of(2015, 04, 30);
            builder.backDutyDateTo = LocalDate.of(2015, 05, 30);
            builder.vehicleMake = "Ford";
            builder.vehicleRegistrationMark = "FG59 4FD";
            builder.fineLevel = 3;
            builder.maxFineValue = BigDecimal.valueOf(1000);
            builder.endorsable = false;
            builder.pressRestrictable = false;
            builder.prosecutorOfferAOCP = true;

            return builder;
        }

        public OffenceBuilder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public OffenceBuilder withLibraOffenceCode(final String libraOffenceCode) {
            this.libraOffenceCode = libraOffenceCode;
            return this;
        }

        public OffenceBuilder withLibraOffenceDateCode(final Integer libraOffenceDateCode) {
            this.libraOffenceDateCode = libraOffenceDateCode;
            return this;
        }

        public OffenceBuilder withOffenceCommittedDate(final LocalDate offenceCommittedDate) {
            this.offenceCommittedDate = offenceCommittedDate;
            return this;
        }

        public OffenceBuilder withOffenceChargeDate(final LocalDate offenceChargeDate) {
            this.chargeDate = offenceChargeDate;
            return this;
        }


        public OffenceBuilder withBackDuty(BigDecimal backDuty) {
            this.backDuty = backDuty;
            return this;
        }

        public OffenceBuilder withBackDutyDateFrom(LocalDate backDutyDateFrom) {
            this.backDutyDateFrom = backDutyDateFrom;
            return this;
        }

        public OffenceBuilder withBackDutyDateTo(LocalDate backDutyDateTo) {
            this.backDutyDateTo = backDutyDateTo;
            return this;
        }

        public OffenceBuilder withVehicleMake(String vehicleMake) {
            this.vehicleMake = vehicleMake;
            return this;
        }

        public OffenceBuilder withVehicleRegistrationMark(String vehicleRegistrationMark) {
            this.vehicleRegistrationMark = vehicleRegistrationMark;
            return this;
        }

        public OffenceBuilder withFineLevel(int fineLevel) {
            this.fineLevel = fineLevel;
            return this;
        }

        public OffenceBuilder withMaxFineValue(BigDecimal maxFineValue) {
            this.maxFineValue = maxFineValue;
            return this;
        }

        public OffenceBuilder withEndorsable(final Boolean endorsable) {
            this.endorsable = endorsable;
            return this;
        }

        public OffenceBuilder withPressRestrictable(final Boolean pressRestrictable) {
            this.pressRestrictable = pressRestrictable;
            return this;
        }

        public OffenceBuilder withProsecutorOfferAOCP(final Boolean prosecutorOfferAOCP) {
            this.prosecutorOfferAOCP = prosecutorOfferAOCP;
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

        public LocalDate getOffenceCommittedDate() {
            return offenceCommittedDate;
        }

        public String getOffenceWording() {
            return offenceWording;
        }

        public String getOffenceWordingWelsh() {
            return offenceWordingWelsh;
        }

        public OffenceBuilder setOffenceWordingWelsh() {
            this.offenceWordingWelsh = offenceWordingWelsh;
            return this;
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

        public BigDecimal getBackDuty() {
            return backDuty;
        }

        public LocalDate getBackDutyDateFrom() {
            return backDutyDateFrom;
        }

        public LocalDate getBackDutyDateTo() {
            return backDutyDateTo;
        }

        public String getVehicleMake() {
            return vehicleMake;
        }

        public String getVehicleRegistrationMark() {
            return vehicleRegistrationMark;
        }

        public int getFineLevel() {
            return fineLevel;
        }

        public BigDecimal getMaxFineValue() {
            return maxFineValue;
        }

        public Boolean getEndorsable() {
            return endorsable;
        }

        public Boolean getPressRestrictable() {
            return pressRestrictable;
        }
    }
}
