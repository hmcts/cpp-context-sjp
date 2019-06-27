package uk.gov.moj.cpp.sjp.event.processor.converter;

import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.junit.Assert.assertEquals;
import static uk.gov.justice.json.schemas.domains.sjp.Gender.MALE;
import static uk.gov.justice.json.schemas.domains.sjp.results.PersonTitle.MR;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;

import uk.gov.justice.json.schemas.domains.sjp.Address;
import uk.gov.justice.json.schemas.domains.sjp.ContactDetails;
import uk.gov.justice.json.schemas.domains.sjp.PersonalDetails;
import uk.gov.justice.json.schemas.domains.sjp.PleaMethod;
import uk.gov.justice.json.schemas.domains.sjp.PleaType;
import uk.gov.justice.json.schemas.domains.sjp.ProsecutingAuthority;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Defendant;
import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;
import uk.gov.justice.json.schemas.domains.sjp.results.BaseCaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.results.BaseOffence;
import uk.gov.justice.json.schemas.domains.sjp.results.BasePersonDetail;
import uk.gov.justice.json.schemas.domains.sjp.results.BaseResult;
import uk.gov.justice.json.schemas.domains.sjp.results.BaseSessionStructure;
import uk.gov.justice.json.schemas.domains.sjp.results.CaseDefendant;
import uk.gov.justice.json.schemas.domains.sjp.results.CaseOffence;
import uk.gov.justice.json.schemas.domains.sjp.results.IndividualDefendant;
import uk.gov.justice.json.schemas.domains.sjp.results.Plea;
import uk.gov.justice.json.schemas.domains.sjp.results.Prompts;
import uk.gov.justice.json.schemas.domains.sjp.results.SessionLocation;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.resulting.CourtDetails;
import uk.gov.moj.cpp.sjp.domain.resulting.Prompt;
import uk.gov.moj.cpp.sjp.domain.resulting.ReferencedDecisionsSaved;
import uk.gov.moj.cpp.sjp.domain.resulting.Result;
import uk.gov.moj.cpp.sjp.domain.resulting.SJPSession;
import uk.gov.moj.cpp.sjp.event.processor.utils.GenericEnveloper;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class ResultingToResultsConverterHelper {

    private static final String DEFAULT_NON_POLICE_PROSECUTOR_REFERENCE = "0800NP0100000000001H";
    private static final Integer DEFAULT_OFFENCE_DATE_CODE = 1;
    private static final String ADDRESS1_KEY = "address1";
    private static final String ADDRESS2_KEY = "address2";
    private static final String ADDRESS3_KEY = "address3";
    private static final String ADDRESS4_KEY = "address4";
    private static final String ADDRESS5_KEY = "address5";
    private static final String POSTCODE_KEY = "postcode";
    private static final String ROOM_NAME = "00";
    private static final String DEFAULT_BAIL_STATUS = "A";

    private static final Integer COURT_HOUSE_CODE = 1022;
    private static final String COURT_HOUSE_NAME = "Cardiff Magistrates' Court";
    private static final String TITLE = "Mr";
    private static final String BUSINESS_NUMBER = "99999999999";
    private static final String EMAIL = "somerandomemail1@random.random";
    private static final String EMAIL_2 = "somerandomemail2@random.random";
    private static final String HOME_NUMBER = "88888888888";
    private static final String MOBILE = "77777777777";
    private static final ZonedDateTime DATE_OF_BIRTH = now().minusYears(20);
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
    private static final String PROSECUTING_AUTHORITY = "TFL";
    private static final UUID COURT_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final String POSTCODE = "CF24 0RZ";
    private static final Integer OFFENCE_SEQUENCE_NUMBER = 1;
    private static final String CJS_CODE = "SUMRCC";
    private static final String WORDING = "Wording";
    private static final LocalDate OFFENCE_START_DATE = LocalDate.now().minusDays(5);
    private static final LocalDate OFFENCE_END_DATE = LocalDate.now().minusDays(3);
    private static final LocalDate CHARGE_DATE = LocalDate.now().minusDays(2);
    private static final ZonedDateTime PLEA_DATE = now(UTC).minusDays(3);

    private static final ZonedDateTime RESULTED_ON = now(UTC).minusHours(5);
    private static final ZonedDateTime SESSION_START_DATE = now(UTC).minusHours(7);
    private static final ZonedDateTime SESSION_END_DATE = now(UTC).minusHours(6);
    private static final String VERDICT = "PSJ";
    private static final Integer accountDivisionCode = nextInt(1, 100);
    private static final Integer enforcingCourtCode = nextInt(100, 200);
    private static final String COUNTRY_ISO_CODE = "1";
    private static final String LJA = "lja";
    private static final String LJA_VALUE = "1022";
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

    private static final GenericEnveloper genericEnveloper = new GenericEnveloper();

    public static void verifyCases(final List<BaseCaseDetails> cases) {
        final BaseCaseDetails case1 = cases.get(0);
        final List<CaseDefendant> defendants = case1.getDefendants();

        assertEquals(CASE_ID, case1.getCaseId());
        assertEquals(URN, case1.getUrn());
        assertEquals(PROSECUTING_AUTHORITY, case1.getProsecutingAuthority());
        verifyDefendants(defendants);
    }

    public static void verifyDefendants(final List<CaseDefendant> defendants) {
        final CaseDefendant defendant = defendants.get(0);
        final IndividualDefendant individualDefendant = defendant.getIndividualDefendant();
        final BasePersonDetail person = individualDefendant.getBasePersonDetails();
        final List<CaseOffence> offences = defendant.getOffences();
        final CaseOffence offence = offences.get(0);

        assertEquals(DEFENDANT_ID, defendant.getDefendantId());
        assertEquals(DEFAULT_NON_POLICE_PROSECUTOR_REFERENCE, defendant.getProsecutorReference());
        assertEquals(DEFAULT_BAIL_STATUS, individualDefendant.getBailStatus());
        assertEquals(false, individualDefendant.getPresentAtHearing());
        verifyPerson(person);
        verifyOffence(offence);
    }

    public static void verifyOffence(final CaseOffence offence) {
        final BaseOffence baseOffenceDetails = offence.getBaseOffenceDetails();
        final Plea plea = offence.getPlea();
        final List<BaseResult> results = offence.getResults();
        final BaseResult result = results.get(0);

        assertEquals(COURT_HOUSE_CODE, offence.getConvictingCourt());
        assertEquals(OFFENCE_ID, baseOffenceDetails.getOffenceId());
        assertEquals(OFFENCE_SEQUENCE_NUMBER, baseOffenceDetails.getOffenceSequenceNumber());
        assertEquals(CJS_CODE, baseOffenceDetails.getOffenceCode());
        assertEquals(WORDING, baseOffenceDetails.getOffenceWording());
        assertEquals(DEFAULT_OFFENCE_DATE_CODE, baseOffenceDetails.getOffenceDateCode());
        assertEquals(OFFENCE_START_DATE, baseOffenceDetails.getOffenceStartDate());
        assertEquals(OFFENCE_END_DATE, baseOffenceDetails.getOffenceEndDate());
        assertEquals(CHARGE_DATE, baseOffenceDetails.getChargeDate());
        assertEquals(OFFENCE_LOCATION, baseOffenceDetails.getLocationOfOffence());
        assertEquals(PleaType.GUILTY, plea.getPleaType());
        assertEquals(PleaMethod.ONLINE, plea.getPleaMethod());
        assertEquals(PLEA_DATE, plea.getPleaDate());
        verifyResult(result);
    }

    private static void verifyResult(final BaseResult result) {
        final List<Prompts> terminalEntries = result.getPrompts();
        final Prompts referralReasonPrompt = getPrompt(terminalEntries, PROMPT1_ID);
        final Prompts hearingTypePrompt = getPrompt(terminalEntries, PROMPT2_ID);
        final Prompts estimatedHearingDurationPrompt = getPrompt(terminalEntries, PROMPT3_ID);
        final Prompts listingNotesPrompt = getPrompt(terminalEntries, PROMPT4_ID);

        assertEquals(RESULT_ID, result.getId());
        verifyPrompt(referralReasonPrompt, PROMPT1_ID, REFERRAL_REASON_ID.toString());
        verifyPrompt(hearingTypePrompt, PROMPT2_ID, HEARING_TYPE.toString());
        verifyPrompt(estimatedHearingDurationPrompt, PROMPT3_ID, ESTIMATED_HEARING_DURATION.toString());
        verifyPrompt(listingNotesPrompt, PROMPT4_ID, LISTING_NOTES);
    }

    private static void verifyPrompt(final Prompts prompt, final UUID expectedId, final String expectedValue) {
        assertEquals(expectedId, prompt.getId());
        assertEquals(expectedValue, prompt.getValue());
    }

    private static Prompts getPrompt(final List<Prompts> prompts, final UUID id) {
        return prompts.stream().filter(p -> p.getId().equals(id)).findFirst().get();
    }

    public static void verifyPerson(final BasePersonDetail person) {
        assertEquals(MR, person.getPersonTitle());
        assertEquals(FIRST_NAME, person.getFirstName());
        assertEquals(LAST_NAME, person.getLastName());
        assertEquals(DATE_OF_BIRTH.toLocalDate(), person.getBirthDate().toLocalDate());
        assertEquals(MALE, person.getGender());
        assertEquals(BUSINESS_NUMBER, person.getTelephoneNumberBusiness());
        assertEquals(HOME_NUMBER, person.getTelephoneNumberHome());
        assertEquals(MOBILE, person.getTelephoneNumberMobile());
        assertEquals(EMAIL, person.getEmailAddress1());
        assertEquals(EMAIL_2, person.getEmailAddress2());
        assertAddress(person.getAddress());
    }

    public static PersonalDetails buildPersonalDetails() {
        return PersonalDetails.personalDetails()
                .withTitle(TITLE)
                .withAddress(Address.address().withAddress1(ADDRESS1).withAddress2(ADDRESS2).withAddress3(ADDRESS3).withAddress4(ADDRESS4).withAddress5(ADDRESS5).withPostcode(POSTCODE).build())
                .withContactDetails(ContactDetails.contactDetails().withBusiness(BUSINESS_NUMBER).withEmail(EMAIL).withEmail2(EMAIL_2).withHome(HOME_NUMBER).withMobile(MOBILE).build())
                .withDateOfBirth(DATE_OF_BIRTH.toLocalDate()).withFirstName(FIRST_NAME).withGender(MALE).withLastName(LAST_NAME)
                .build();
    }

    public static CaseDetails buildCaseDetails() {
        final CaseDetails caseDetails = CaseDetails.caseDetails()
                .withId(CASE_ID)
                .withUrn(URN)
                .withProsecutingAuthority(ProsecutingAuthority.TFL)
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
                .withStartDate(OFFENCE_START_DATE.toString())
                .withEndDate(OFFENCE_END_DATE.toString())
                .withChargeDate(CHARGE_DATE.toString())
                .withPlea(PleaType.GUILTY)
                .withPleaMethod(PleaMethod.ONLINE)
                .withPleaDate(PLEA_DATE)
                .build();
        offences.add(offence);
        return offences;
    }

    public static Envelope<ReferencedDecisionsSaved> getReferenceDecisionSaved() {

        final Prompt referralReasonTerminalEntry = new Prompt(PROMPT1_ID, REFERRAL_REASON_ID.toString());
        final Prompt hearingTypeTerminalEntry = new Prompt(PROMPT2_ID, HEARING_TYPE.toString());
        final Prompt estimatedHearingDurationTerminalEntry = new Prompt(PROMPT3_ID, ESTIMATED_HEARING_DURATION.toString());
        final Prompt listingNotesTerminalEntry = new Prompt(PROMPT4_ID, LISTING_NOTES);
        final List<Prompt> prompts = Arrays.asList(referralReasonTerminalEntry, hearingTypeTerminalEntry, estimatedHearingDurationTerminalEntry, listingNotesTerminalEntry);
        final JsonArrayBuilder promptsBuilder = createArrayBuilder();

        for (final Prompt prompt : prompts) {
            promptsBuilder.add(Json.createObjectBuilder().add("id", prompt.getPromptDefinitionId().toString()).add("value", prompt.getValue()));
        }
        uk.gov.moj.cpp.sjp.domain.resulting.Offence offence = new uk.gov.moj.cpp.sjp.domain.resulting.Offence(OFFENCE_ID, asList(new Result(RESULT_ID, prompts)));
        final List<uk.gov.moj.cpp.sjp.domain.resulting.Offence> offences = asList(offence);

        return genericEnveloper.envelopeWithNewActionName(new ReferencedDecisionsSaved(CASE_ID, SJP_SESSION_ID, RESULTED_ON, VERDICT, offences, accountDivisionCode, enforcingCourtCode), Envelope.metadataBuilder()
                .withId(randomUUID())
                .withName("actionName")
                .createdAt(ZonedDateTime.now())
                .build(), "public.resulting.referenced-decisions-saved");
    }

    public static JsonObject getSJPSessionJsonObject() {

        return createObjectBuilder()
                .add("sessionId", SJP_SESSION_ID.toString())
                .add("userId", USER_ID.toString())
                .add("type", SessionType.MAGISTRATE.toString())
                .add("courtHouseCode", COURT_HOUSE_CODE.toString())
                .add("courtHouseName", COURT_HOUSE_NAME)
                .add("localJusticeAreaNationalCourtCode", LJA_VALUE)
//                .add("magistrate", MAGISTRATE)
                .add("startedAt", SESSION_START_DATE.toString())
//                .add("endedAt", SESSION_END_DATE.toString())
                .build();
    }

    public static void assertAddress(final Address address) {
        assertEquals(ADDRESS1, address.getAddress1());
        assertEquals(ADDRESS2, address.getAddress2());
        assertEquals(ADDRESS3, address.getAddress3());
        assertEquals(ADDRESS4, address.getAddress4());
        assertEquals(ADDRESS5, address.getAddress5());
        assertEquals(POSTCODE, address.getPostcode());
    }

    public static void assertSessionLocation(final SessionLocation sessionLocation) {
        final Address sessionAddress = sessionLocation.getAddress();
        assertEquals(COURT_ID, sessionLocation.getCourtId());
        assertEquals(COURT_HOUSE_CODE.toString(), sessionLocation.getCourtHouseCode());
        assertEquals(COURT_HOUSE_NAME, sessionLocation.getName());
        assertEquals(ROOM_NAME, sessionLocation.getRoomName());
        assertEquals(LJA_VALUE, sessionLocation.getLja());
        assertEquals(ADDRESS1, sessionAddress.getAddress1());
        assertEquals(ADDRESS2, sessionAddress.getAddress2());
        assertEquals(ADDRESS3, sessionAddress.getAddress3());
        assertEquals(ADDRESS4, sessionAddress.getAddress4());
        assertEquals(ADDRESS5, sessionAddress.getAddress5());
        assertEquals(POSTCODE, sessionAddress.getPostcode());
    }

    public static Optional<JsonObject> buildCourt() {
        return Optional.of(createObjectBuilder()
                .add("id", COURT_ID.toString())
                .add(LJA, LJA_VALUE)
                .add(ADDRESS1_KEY, ADDRESS1)
                .add(ADDRESS2_KEY, ADDRESS2)
                .add(ADDRESS3_KEY, ADDRESS3)
                .add(ADDRESS4_KEY, ADDRESS4)
                .add(ADDRESS5_KEY, ADDRESS5)
                .add(POSTCODE_KEY, POSTCODE).build()
        );
    }

    public static SJPSession buildSjpSession() {
        final CourtDetails courtDetails = new CourtDetails(COURT_HOUSE_CODE.toString(), COURT_HOUSE_NAME, COURT_HOUSE_CODE.toString());
        return new SJPSession(SJP_SESSION_ID,
                null,
                SessionType.MAGISTRATE,
                courtDetails,
                null,
                SESSION_START_DATE,
                SESSION_END_DATE);
    }

    public static void verifySession(final BaseSessionStructure session) {
        assertEquals(SJP_SESSION_ID, session.getSessionId());
        assertEquals(SESSION_START_DATE, session.getDateAndTimeOfSession());
        assertEquals(COURT_HOUSE_CODE.toString(), session.getOuCode());
        assertSessionLocation(session.getSessionLocation());
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
                                .add("offenceId", OFFENCE_ID.toString())
                                .add("offenceLocation", OFFENCE_LOCATION)))
                .build());
    }

    public static Optional<JsonObject> getCountryNationality() {
        return Optional.of(createObjectBuilder()
                .add("isoCode", COUNTRY_ISO_CODE)
                .build());
    }


    public static UUID getSjpSessionId() {
        return SJP_SESSION_ID;
    }

    public static UUID getCaseId() {
        return CASE_ID;
    }

    public static String getCountryIsoCode() {
        return COUNTRY_ISO_CODE;
    }
}
