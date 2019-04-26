package uk.gov.moj.cpp.sjp.event.processor.converter;

import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.junit.Assert.assertEquals;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverter.ADDRESS1_KEY;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverter.ADDRESS2_KEY;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverter.ADDRESS3_KEY;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverter.ADDRESS4_KEY;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverter.ADDRESS5_KEY;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverter.ADDRESS_KEY;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverter.DEFAULT_BAIL_STATUS;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverter.DEFAULT_NON_POLICE_PROSECUTOR_REFERENCE;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverter.DEFAULT_OFFENCE_DATE_CODE;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverter.POSTCODE_KEY;
import static uk.gov.moj.cpp.sjp.event.processor.converter.ResultingToResultsConverter.ROOM_NAME;

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
import uk.gov.moj.cpp.sjp.domain.resulting.SJPSession;
import uk.gov.moj.cpp.sjp.domain.resulting.TerminalEntry;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class ResultingToResultsConverterHelper {

    public static final String COURT_HOUSE_CODE = "DC";
    public static final String COURT_HOUSE_NAME = "Cardiff Magistrates' Court";
    public static final String TITLE = "Mr";
    public static final String BUSINESS_NUMBER = "99999999999";
    public static final String EMAIL = "somerandomemail1@random.random";
    public static final String EMAIL_2 = "somerandomemail2@random.random";
    public static final String HOME_NUMBER = "88888888888";
    public static final String MOBILE = "77777777777";
    public static final LocalDate DATE_OF_BIRTH = now(UTC).minusYears(20).toLocalDate();
    public static final String FIRST_NAME = "SomeFirstName";
    public static final String LAST_NAME = "SomeLastName";
    public static final String ADDRESS1 = "Fitzalan Place";
    public static final String ADDRESS2 = "Cardiff";
    public static final String ADDRESS3 = "addressline3";
    public static final String ADDRESS4 = "addressline4";
    public static final String ADDRESS5 = "addressline5";
    public static final UUID OFFENCE_ID = randomUUID();;
    public static final UUID SJP_SESSION_ID = randomUUID();
    public static final UUID CASE_ID = randomUUID();
    public static final UUID USER_ID = randomUUID();
    public static final String URN = "1234567";
    public static final UUID COURT_ID = randomUUID();
    public static final UUID DEFENDANT_ID = randomUUID();
    public static final String POSTCODE = "CF24 0RZ";
    public static final int OFFENCE_SEQUENCE_NUMBER = 1;
    public static final String CJS_CODE = "SUMRCC";
    public static final String WORDING = "Wording";
    public static final String OFFENCE_START_DATE = now(UTC).minusYears(5).toString();
    public static final String OFFENCE_END_DATE = now(UTC).minusDays(3).toString();
    public static final String CHARGE_DATE = now(UTC).minusDays(2).toString();
    public static final ZonedDateTime PLEA_DATE = now(UTC).minusDays(3);

    public static final int REFERRAL_REASON_INDEX = -1;
    public static final int HEARING_TYPE_INDEX = -2;
    public static final int LISTING_NOTES_INDEX = 10;
    public static final int ESTIMATED_HEARING_DURATION_INDEX = 5;
    public static final String RESULTED_ON = now(UTC).minusHours(5).toString();
    public static final UUID DECISION_ID = randomUUID();
    private static final String LJA = "LJA";
    public static final ZonedDateTime SESSION_START_DATE = now(UTC).minusHours(7);
    public static final ZonedDateTime SESSION_END_DATE = now(UTC).minusHours(6);
    public static final UUID RESULT_TYPE_ID = randomUUID();
    public static final String MAGISTRATE = "SomeMagistrate";
    public static final String VERDICT = "PSJ";

    public static final Integer accountDivisionCode = nextInt(1, 100);
    public static final Integer enforcingCourtCode = nextInt(100, 200);


    public static void verifyCases(final JsonArray cases) {
        JsonObject case1 = cases.getJsonObject(0);
        JsonArray defendants = case1.getJsonArray("defendants");

        assertEquals(CASE_ID.toString(), case1.getString("caseId"));
        assertEquals(URN, case1.getString("urn"));
        verifyDefendants(defendants);
    }

    public static  void verifyDefendants(final JsonArray defendants) {
        JsonObject defendant = defendants.getJsonObject(0);
        JsonObject individualDefendant = defendant.getJsonObject("individualDefendant");
        JsonObject person = individualDefendant.getJsonObject("basePersonDetails");
        JsonArray offences = defendant.getJsonArray("offences");
        JsonObject offence = offences.getJsonObject(0);

        assertEquals(DEFENDANT_ID.toString(), defendant.getString("defendantId"));
        assertEquals(DEFAULT_NON_POLICE_PROSECUTOR_REFERENCE, defendant.getString("prosecutorReference"));
        assertEquals(DEFAULT_BAIL_STATUS, individualDefendant.getString("bailStatus"));
        assertEquals(false, individualDefendant.getBoolean("presentAtHearing"));

        verifyPerson(person);
        verifyOffence(offence);
    }

    public static  void verifyOffence(final JsonObject offence) {
        JsonObject baseOffenceDetails = offence.getJsonObject("baseOffenceDetails");
        JsonObject plea = offence.getJsonObject("plea");
        JsonArray results = offence.getJsonArray("results");
        JsonObject result = results.getJsonObject(0);

        assertEquals(COURT_HOUSE_CODE, offence.getString("convictingCourt"));
        assertEquals(OFFENCE_ID.toString(), baseOffenceDetails.getString("offenceId"));
        assertEquals(OFFENCE_SEQUENCE_NUMBER, baseOffenceDetails.getInt("offenceSequenceNumber"));
        assertEquals(CJS_CODE, baseOffenceDetails.getString("offenceCode"));
        assertEquals(WORDING, baseOffenceDetails.getString("offenceWording"));
        assertEquals(DEFAULT_OFFENCE_DATE_CODE, baseOffenceDetails.getInt("offenceDateCode"));
        assertEquals(OFFENCE_START_DATE, baseOffenceDetails.getString("offenceStartDateTime"));
        assertEquals(OFFENCE_END_DATE, baseOffenceDetails.getString("offenceEndDateTime"));
        assertEquals(CHARGE_DATE, baseOffenceDetails.getString("chargeDate"));
        assertEquals(PleaType.GUILTY.toString(), plea.getString("pleaType"));
        assertEquals(PleaMethod.ONLINE.toString(), plea.getString("pleaMethod"));
        assertEquals(PLEA_DATE.toString(), plea.getString("pleaDate"));
        assertEquals(CJS_CODE, result.getString("resultCode"));
    }

    public static  void verifyPerson(final JsonObject person) {
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

        JsonObject sessionLocation = person.getJsonObject(ADDRESS_KEY);
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

    public static  CaseDetails buildCaseDetails() {
        final CaseDetails caseDetails = CaseDetails.caseDetails()
                .withId(CASE_ID)
                .withUrn(URN)
                .withDefendant(buildDefendant())
                .build();
        return caseDetails;
    }

    public static  Defendant buildDefendant() {
        return Defendant.defendant()
                .withId(DEFENDANT_ID)
                .withOffences(buildOffences())
                .withPersonalDetails(buildPersonalDetails())
                .build();
    }

    public static  List<Offence> buildOffences() {
        List<Offence> offences = new ArrayList<>();
        Offence offence = Offence.offence()
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

    public static  JsonEnvelope getReferenceDecisionSaved() {

        final UUID referralReasonId = randomUUID();
        final UUID hearingType = randomUUID();
        final Integer estimatedHearingDuration = nextInt(0, 999);
        final String listingNotes = randomAlphanumeric(100);

        final TerminalEntry referralReasonTerminalEntry = new TerminalEntry(REFERRAL_REASON_INDEX, referralReasonId.toString());
        final TerminalEntry hearingTypeTerminalEntry = new TerminalEntry(HEARING_TYPE_INDEX, hearingType.toString());
        final TerminalEntry estimatedHearingDurationTerminalEntry = new TerminalEntry(ESTIMATED_HEARING_DURATION_INDEX, estimatedHearingDuration.toString());
        final TerminalEntry listingNotesTerminalEntry = new TerminalEntry(LISTING_NOTES_INDEX, listingNotes);

        List<TerminalEntry> terminalEntries = new ArrayList<>();
        terminalEntries.add(referralReasonTerminalEntry);
        terminalEntries.add(hearingTypeTerminalEntry);
        terminalEntries.add(estimatedHearingDurationTerminalEntry);
        terminalEntries.add(listingNotesTerminalEntry);


        final JsonArrayBuilder terminalEntriesArrayBuilder = createArrayBuilder();

        for (final TerminalEntry terminalEntry : terminalEntries) {
            terminalEntriesArrayBuilder.add(Json.createObjectBuilder().add("index", terminalEntry.getIndex()).add("value", terminalEntry.getValue()));
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
                                                .add("code", CJS_CODE)
                                                .add("resultTypeId", RESULT_TYPE_ID.toString())
                                                .add("terminalEntries", terminalEntriesArrayBuilder)))
                )).build();

        return envelopeFrom(metadataWithRandomUUID("public.resulting.referenced-decisions-saved")
                .withStreamId(DECISION_ID)
                .withVersion(1), decision);
    }

    public static  JsonObject getSJPSessionJsonObject(){

        return createObjectBuilder()
                .add("id", SJP_SESSION_ID.toString())
                .add("userId", USER_ID.toString())
                .add("type", SessionType.MAGISTRATE.toString())
                .add("courtHouseCode", COURT_HOUSE_CODE)
                .add("courtHouseName", COURT_HOUSE_NAME)
                .add("localJusticeAreaNationalCourtCode", LJA)
                .add("magistrate", MAGISTRATE)
                .add("startedAt", SESSION_START_DATE.toString())
                .add("endedAt", SESSION_END_DATE.toString())
                .build();
    }

    public static  void assertAddress(final JsonObject address) {
        assertEquals(ADDRESS1, address.getString(ADDRESS1_KEY));
        assertEquals(ADDRESS2, address.getString(ADDRESS2_KEY));
        assertEquals(ADDRESS3, address.getString(ADDRESS3_KEY));
        assertEquals(ADDRESS4, address.getString(ADDRESS4_KEY));
        assertEquals(ADDRESS5, address.getString(ADDRESS5_KEY));
        assertEquals(POSTCODE, address.getString(POSTCODE_KEY));
    }

    public static  void assertSessionLocation(final JsonObject sessionLocation) {
        JsonObject sessionAddress = sessionLocation.getJsonObject(ADDRESS_KEY);
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

    public static  Optional<JsonObject> buildCourt() {
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

    public static  SJPSession buildSjpSession() {
        final CourtDetails courtDetails = new CourtDetails(COURT_HOUSE_CODE, COURT_HOUSE_NAME, null);
        return new SJPSession(SJP_SESSION_ID,
                null,
                SessionType.MAGISTRATE,
                courtDetails,
                null,
                SESSION_START_DATE,
                SESSION_END_DATE);
    }

    public static  void verifySession(final JsonObject session) {
        JsonObject sessionLocation = session.getJsonObject("sessionLocation");
        assertEquals(SJP_SESSION_ID.toString(), session.getString("sessionId"));
        assertEquals(SESSION_START_DATE.toString(), session.getString("dateAndTimeOfSession"));
        assertEquals(COURT_HOUSE_CODE, session.getString("psaCode"));
        assertSessionLocation(sessionLocation);
    }

}
