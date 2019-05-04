package uk.gov.moj.cpp.sjp.event.processor.converter;

import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.junit.Assert.assertEquals;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;

import uk.gov.justice.json.schemas.domains.sjp.Address;
import uk.gov.justice.json.schemas.domains.sjp.ContactDetails;
import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.justice.json.schemas.domains.sjp.PersonalDetails;
import uk.gov.justice.json.schemas.domains.sjp.PleaMethod;
import uk.gov.justice.json.schemas.domains.sjp.PleaType;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Defendant;
import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.resulting.CourtDetails;
import uk.gov.moj.cpp.sjp.domain.resulting.Prompt;
import uk.gov.moj.cpp.sjp.domain.resulting.SJPSession;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class ResultingToResultsConverterHelper {

    private static final String DEFAULT_NON_POLICE_PROSECUTOR_REFERENCE = "0800NP0100000000001H";
    private static final int DEFAULT_OFFENCE_DATE_CODE = 1;
    private static final String UNKNOWN = "0";
    private static final String OFFENCES_KEY = "offences";
    private static final String ADDRESS1_KEY = "address1";
    private static final String ADDRESS2_KEY = "address2";
    private static final String ADDRESS3_KEY = "address3";
    private static final String ADDRESS4_KEY = "address4";
    private static final String ADDRESS5_KEY = "address5";
    private static final String POSTCODE_KEY = "postcode";
    private static final String ADDRESS_KEY = "address";
    private static final String ROOM_NAME = "00";
    private static final String DEFAULT_BAIL_STATUS = "A";
    private static final String VERDICT_KEY = "verdict";
    private static final String ACCOUNT_DIVISION_CODE_KEY = "accountDivisionCode";
    private static final String ENFORCING_COURT_CODE_KEY = "enforcingCourtCode";
    private static final String MAGISTRATE_KEY = "magistrate";
    private static final String ENDED_AT_KEY = "endedAt";

    private static final String COURT_HOUSE_CODE = "DC";
    private static final String COURT_HOUSE_NAME = "Cardiff Magistrates' Court";
    private static final String TITLE = "Mr";
    private static final String BUSINESS_NUMBER = "99999999999";
    private static final String EMAIL = "somerandomemail1@random.random";
    private static final String EMAIL_2 = "somerandomemail2@random.random";
    private static final String HOME_NUMBER = "88888888888";
    private static final String MOBILE = "77777777777";
    private static final LocalDate DATE_OF_BIRTH = now(UTC).minusYears(20).toLocalDate();
    private static final String FIRST_NAME = "SomeFirstName";
    private static final String LAST_NAME = "SomeLastName";
    private static final String ADDRESS1 = "Fitzalan Place";
    private static final String ADDRESS2 = "Cardiff";
    private static final String ADDRESS3 = "addressline3";
    private static final String ADDRESS4 = "addressline4";
    private static final String ADDRESS5 = "addressline5";
    private static final UUID OFFENCE_ID = randomUUID();
    private static final UUID SJP_SESSION_ID = randomUUID();
    private static final UUID CASE_ID = randomUUID();
    private static final UUID USER_ID = randomUUID();
    private static final String URN = "1234567";
    private static final UUID COURT_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final String POSTCODE = "CF24 0RZ";
    private static final int OFFENCE_SEQUENCE_NUMBER = 1;
    private static final String CJS_CODE = "SUMRCC";
    private static final String WORDING = "Wording";
    private static final String OFFENCE_START_DATE = now(UTC).minusYears(5).toString();
    private static final String OFFENCE_END_DATE = now(UTC).minusDays(3).toString();
    private static final String CHARGE_DATE = now(UTC).minusDays(2).toString();
    private static final ZonedDateTime PLEA_DATE = now(UTC).minusDays(3);

    private static final String RESULTED_ON = now(UTC).minusHours(5).toString();
    private static final UUID DECISION_ID = randomUUID();
    private static final ZonedDateTime SESSION_START_DATE = now(UTC).minusHours(7);
    private static final ZonedDateTime SESSION_END_DATE = now(UTC).minusHours(6);
    private static final UUID RESULT_TYPE_ID = randomUUID();
    private static final String MAGISTRATE = "SomeMagistrate";
    private static final String VERDICT = "PSJ";
    private static final Integer accountDivisionCode = nextInt(1, 100);
    private static final Integer enforcingCourtCode = nextInt(100, 200);
    private static final String COUNTRY_CJS_CODE = "1";
    private static final String LJA = "LJA";
    private static final String OFFENCE_LOCATION = "Cardiff";
    private static final UUID RESULT_ID = randomUUID();
    private static final UUID REFERRAL_REASON_ID = randomUUID();
    private static final UUID HEARING_TYPE = randomUUID();
    private static final Integer ESTIMATED_HEARING_DURATION = nextInt(0, 999);
    private static final String LISTING_NOTES = randomAlphanumeric(100);
    private static final UUID PROMPT1_ID = randomUUID();
    private static final UUID PROMPT2_ID = randomUUID();
    private static final UUID PROMPT3_ID = randomUUID();
    private static final UUID PROMPT4_ID = randomUUID();

    public static void verifyCases(final JsonArray cases) {
        final JsonObject case1 = cases.getJsonObject(0);
        final JsonArray defendants = case1.getJsonArray("defendants");

        assertEquals(CASE_ID.toString(), case1.getString("caseId"));
        assertEquals(URN, case1.getString("urn"));
        verifyDefendants(defendants);
    }

    public static void verifyDefendants(final JsonArray defendants) {
        final JsonObject defendant = defendants.getJsonObject(0);
        final JsonObject individualDefendant = defendant.getJsonObject("individualDefendant");
        final JsonObject person = individualDefendant.getJsonObject("basePersonDetails");
        final JsonArray offences = defendant.getJsonArray("offences");
        final JsonObject offence = offences.getJsonObject(0);

        assertEquals(DEFENDANT_ID.toString(), defendant.getString("defendantId"));
        assertEquals(DEFAULT_NON_POLICE_PROSECUTOR_REFERENCE, defendant.getString("prosecutorReference"));
        assertEquals(DEFAULT_BAIL_STATUS, individualDefendant.getString("bailStatus"));
        assertEquals(false, individualDefendant.getBoolean("presentAtHearing"));
        verifyPerson(person);
        verifyOffence(offence);
    }

    public static void verifyOffence(final JsonObject offence) {
        final JsonObject baseOffenceDetails = offence.getJsonObject("baseOffenceDetails");
        final JsonObject plea = offence.getJsonObject("plea");
        final JsonArray results = offence.getJsonArray("results");
        final JsonObject result = results.getJsonObject(0);

        assertEquals(COURT_HOUSE_CODE, offence.getString("convictingCourt"));
        assertEquals(OFFENCE_ID.toString(), baseOffenceDetails.getString("offenceId"));
        assertEquals(OFFENCE_SEQUENCE_NUMBER, baseOffenceDetails.getInt("offenceSequenceNumber"));
        assertEquals(CJS_CODE, baseOffenceDetails.getString("offenceCode"));
        assertEquals(WORDING, baseOffenceDetails.getString("offenceWording"));
        assertEquals(DEFAULT_OFFENCE_DATE_CODE, baseOffenceDetails.getInt("offenceDateCode"));
        assertEquals(OFFENCE_START_DATE, baseOffenceDetails.getString("offenceStartDateTime"));
        assertEquals(OFFENCE_END_DATE, baseOffenceDetails.getString("offenceEndDateTime"));
        assertEquals(CHARGE_DATE, baseOffenceDetails.getString("chargeDate"));
        assertEquals(OFFENCE_LOCATION, baseOffenceDetails.getString("locationOfOffence"));
        assertEquals(PleaType.GUILTY.toString(), plea.getString("pleaType"));
        assertEquals(PleaMethod.ONLINE.toString(), plea.getString("pleaMethod"));
        assertEquals(PLEA_DATE.toString(), plea.getString("pleaDate"));
        verifyResult(result);
    }

    private static void verifyResult(final JsonObject result) {
        final JsonArray terminalEntries = result.getJsonArray("prompts");
        final JsonObject referralReasonPrompt = getPrompt(terminalEntries, PROMPT1_ID.toString());
        final JsonObject hearingTypePrompt = getPrompt(terminalEntries, PROMPT2_ID.toString());
        final JsonObject estimatedHearingDurationPrompt = getPrompt(terminalEntries, PROMPT3_ID.toString());
        final JsonObject listingNotesPrompt = getPrompt(terminalEntries, PROMPT4_ID.toString());

        assertEquals(RESULT_ID.toString(), result.getString("id"));
        verifyPrompt(referralReasonPrompt, PROMPT1_ID.toString(), REFERRAL_REASON_ID.toString());
        verifyPrompt(hearingTypePrompt, PROMPT2_ID.toString(), HEARING_TYPE.toString());
        verifyPrompt(estimatedHearingDurationPrompt, PROMPT3_ID.toString(), ESTIMATED_HEARING_DURATION.toString());
        verifyPrompt(listingNotesPrompt, PROMPT4_ID.toString(), LISTING_NOTES);
    }

    private static void verifyPrompt(final JsonObject prompt, final String expectedId, final String expectedValue) {
        assertEquals(expectedId, prompt.getString("id"));
        assertEquals(expectedValue, prompt.getString("value"));
    }

    private static JsonObject getPrompt(final JsonArray prompts, final String id) {
        return prompts.stream().map(p -> (JsonObject) p).filter(p -> p.getString("id").equals(id)).findFirst().get();
    }

    public static void verifyPerson(final JsonObject person) {
        assertEquals(TITLE, person.getString("personTitle"));
        assertEquals(FIRST_NAME, person.getString("firstName"));
        assertEquals(LAST_NAME, person.getString("lastName"));
        assertEquals(DATE_OF_BIRTH.toString(), person.getString("birthDate"));
        assertEquals(Gender.MALE.toString(), person.getString("gender"));
        assertEquals(BUSINESS_NUMBER, person.getString("telephoneNumberBusiness"));
        assertEquals(HOME_NUMBER, person.getString("telephoneNumberHome"));
        assertEquals(MOBILE, person.getString("telephoneNumberMobile"));
        assertEquals(EMAIL, person.getString("emailAddress1"));
        assertEquals(EMAIL_2, person.getString("emailAddress2"));

        final JsonObject sessionLocation = person.getJsonObject(ADDRESS_KEY);
        assertAddress(sessionLocation);
    }

    public static PersonalDetails buildPersonalDetails() {
        return PersonalDetails.personalDetails()
                .withTitle(TITLE)
                .withAddress(Address.address().withAddress1(ADDRESS1).withAddress2(ADDRESS2).withAddress3(ADDRESS3).withAddress4(ADDRESS4).withAddress5(ADDRESS5).withPostcode(POSTCODE).build())
                .withContactDetails(ContactDetails.contactDetails().withBusiness(BUSINESS_NUMBER).withEmail(EMAIL).withEmail2(EMAIL_2).withHome(HOME_NUMBER).withMobile(MOBILE).build())
                .withDateOfBirth(DATE_OF_BIRTH).withFirstName(FIRST_NAME).withGender(Gender.MALE).withLastName(LAST_NAME)
                .build();
    }

    public static CaseDetails buildCaseDetails() {
        final CaseDetails caseDetails = CaseDetails.caseDetails()
                .withId(CASE_ID)
                .withUrn(URN)
                .withDefendant(buildDefendant())
                .build();
        return caseDetails;
    }

    public static Defendant buildDefendant() {
        return Defendant.defendant()
                .withId(DEFENDANT_ID)
                .withOffences(buildOffences())
                .withPersonalDetails(buildPersonalDetails())
                .build();
    }

    public static List<Offence> buildOffences() {
        final List<Offence> offences = new ArrayList<>();
        final Offence offence = Offence.offence()
                .withId(OFFENCE_ID)
                .withOffenceSequenceNumber(OFFENCE_SEQUENCE_NUMBER)
                .withCjsCode(CJS_CODE)
                .withWording(WORDING)
                .withStartDate(OFFENCE_START_DATE)
                .withEndDate(OFFENCE_END_DATE)
                .withChargeDate(CHARGE_DATE)
                .withPlea(PleaType.GUILTY)
                .withPleaMethod(PleaMethod.ONLINE)
                .withPleaDate(PLEA_DATE)
                .build();
        offences.add(offence);
        return offences;
    }

    public static JsonEnvelope getReferenceDecisionSaved() {

        final Prompt referralReasonTerminalEntry = new Prompt(PROMPT1_ID, REFERRAL_REASON_ID.toString());
        final Prompt hearingTypeTerminalEntry = new Prompt(PROMPT2_ID, HEARING_TYPE.toString());
        final Prompt estimatedHearingDurationTerminalEntry = new Prompt(PROMPT3_ID, ESTIMATED_HEARING_DURATION.toString());
        final Prompt listingNotesTerminalEntry = new Prompt(PROMPT4_ID, LISTING_NOTES);
        final List<Prompt> prompts = Arrays.asList(referralReasonTerminalEntry, hearingTypeTerminalEntry, estimatedHearingDurationTerminalEntry, listingNotesTerminalEntry);
        final JsonArrayBuilder promptsBuilder = createArrayBuilder();

        for (final Prompt prompt : prompts) {
            promptsBuilder.add(Json.createObjectBuilder().add("id", prompt.getId().toString()).add("value", prompt.getValue()));
        }

        final JsonObject decision = createObjectBuilder()
                .add("id", DECISION_ID.toString())
                .add("sjpSessionId", SJP_SESSION_ID.toString())
                .add("resultedOn", RESULTED_ON)
                .add("verdict", VERDICT)
                .add("accountDivisionCode", accountDivisionCode)
                .add("enforcingCourtCode", enforcingCourtCode)
                .add("offences", createArrayBuilder().add(
                        createObjectBuilder()
                                .add("id", OFFENCE_ID.toString())
                                .add("results",
                                        createArrayBuilder().add(createObjectBuilder()
                                                .add("id", RESULT_ID.toString())
                                                .add("prompts", promptsBuilder)))
                )).build();

        return envelopeFrom(metadataWithRandomUUID("public.resulting.referenced-decisions-saved")
                .withStreamId(DECISION_ID)
                .withVersion(1), decision);
    }

    public static JsonObject getSJPSessionJsonObject() {

        return createObjectBuilder()
                .add("sessionId", SJP_SESSION_ID.toString())
                .add("userId", USER_ID.toString())
                .add("type", SessionType.MAGISTRATE.toString())
                .add("courtHouseCode", COURT_HOUSE_CODE)
                .add("courtHouseName", COURT_HOUSE_NAME)
                .add("localJusticeAreaNationalCourtCode", LJA)
//                .add("magistrate", MAGISTRATE)
                .add("startedAt", SESSION_START_DATE.toString())
//                .add("endedAt", SESSION_END_DATE.toString())
                .build();
    }

    public static void assertAddress(final JsonObject address) {
        assertEquals(ADDRESS1, address.getString(ADDRESS1_KEY));
        assertEquals(ADDRESS2, address.getString(ADDRESS2_KEY));
        assertEquals(ADDRESS3, address.getString(ADDRESS3_KEY));
        assertEquals(ADDRESS4, address.getString(ADDRESS4_KEY));
        assertEquals(ADDRESS5, address.getString(ADDRESS5_KEY));
        assertEquals(POSTCODE, address.getString(POSTCODE_KEY));
    }

    public static void assertSessionLocation(final JsonObject sessionLocation) {
        final JsonObject sessionAddress = sessionLocation.getJsonObject(ADDRESS_KEY);
        assertEquals(COURT_ID.toString(), sessionLocation.getString("courtId"));
        assertEquals(COURT_HOUSE_CODE, sessionLocation.getString("courtHouseCode"));
        assertEquals(COURT_HOUSE_NAME, sessionLocation.getString("name"));
        assertEquals(ROOM_NAME, sessionLocation.getString("roomName"));
        assertEquals(ADDRESS1, sessionAddress.getString(ADDRESS1_KEY));
        assertEquals(ADDRESS2, sessionAddress.getString(ADDRESS2_KEY));
        assertEquals(ADDRESS3, sessionAddress.getString(ADDRESS3_KEY));
        assertEquals(ADDRESS4, sessionAddress.getString(ADDRESS4_KEY));
        assertEquals(ADDRESS5, sessionAddress.getString(ADDRESS5_KEY));
        assertEquals(POSTCODE, sessionAddress.getString(POSTCODE_KEY));
    }

    public static Optional<JsonObject> buildCourt() {
        return Optional.of(createObjectBuilder()
                .add("id", COURT_ID.toString())
                .add(ADDRESS1_KEY, ADDRESS1)
                .add(ADDRESS2_KEY, ADDRESS2)
                .add(ADDRESS3_KEY, ADDRESS3)
                .add(ADDRESS4_KEY, ADDRESS4)
                .add(ADDRESS5_KEY, ADDRESS5)
                .add(POSTCODE_KEY, POSTCODE).build()
        );
    }

    public static SJPSession buildSjpSession() {
        final CourtDetails courtDetails = new CourtDetails(COURT_HOUSE_CODE, COURT_HOUSE_NAME, null);
        return new SJPSession(SJP_SESSION_ID,
                null,
                SessionType.MAGISTRATE,
                courtDetails,
                null,
                SESSION_START_DATE,
                SESSION_END_DATE);
    }

    public static void verifySession(final JsonObject session) {
        final JsonObject sessionLocation = session.getJsonObject("sessionLocation");
        assertEquals(SJP_SESSION_ID.toString(), session.getString("sessionId"));
        assertEquals(SESSION_START_DATE.toString(), session.getString("dateAndTimeOfSession"));
        assertEquals(COURT_HOUSE_CODE, session.getString("psaCode"));
        assertSessionLocation(sessionLocation);
    }

    public static JsonEnvelope getEmptyEnvelop() {
        return envelopeFrom(metadataWithRandomUUID("public.resulting.referenced-decisions-saved")
                .withStreamId(CASE_ID)
                .withVersion(1), NULL);
    }

    public static Optional<JsonObject> getCaseFileDefendantDetails() {
        return Optional.of(createObjectBuilder()
                .add("id", DEFENDANT_ID.toString())
                .add("selfDefinedInformation", createObjectBuilder()
                        .add("nationality", "GBR"))
                .add("offences", createArrayBuilder().add(
                        createObjectBuilder()
                                .add("id", OFFENCE_ID.toString())
                                .add("offenceLocation", OFFENCE_LOCATION)))
                .build());
    }

    public static Optional<JsonObject> getCountryNationality() {
        return Optional.of(createObjectBuilder()
                .add("cjsCode", COUNTRY_CJS_CODE)
                .build());
    }

    public static UUID getSjpSessionId() {
        return SJP_SESSION_ID;
    }

    public static UUID getCaseId() {
        return CASE_ID;
    }

    public static String getCountryCjsCode() {
        return COUNTRY_CJS_CODE;
    }
}
