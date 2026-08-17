package uk.gov.moj.sjp.it.test;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonassert.JsonAssert.with;
import static java.lang.String.format;
import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.defaultCaseBuilder;
import static uk.gov.moj.sjp.it.command.CreateCase.OffenceBuilder.defaultOffenceBuilder;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.CaseProsecutingAuthorityHelper.getProsecutingAuthority;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.METLI;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubOffenceFineLevelsQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryOffencesByCode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.TIMEOUT_IN_SECONDS;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.event.CaseEligibleForAOCP;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.command.builder.AddressBuilder;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.core.Response;

import com.jayway.jsonpath.matchers.JsonPathMatchers;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration test to create a case and verify the case can be read using ID and URN
 */
public class CreateCaseIT extends BaseIntegrationTest {

    private static final String DEFENDANT_REGION = "croydon";
    private static final String NATIONAL_COURT_CODE = "1080";

    private final static int fineLevel = 3;
    private final static BigDecimal maxValue = BigDecimal.valueOf(1000);

    private final static String offenceCode1 = "CA03010";
    private final static String offenceCode2 = "CA03011";

    private static final String WRITE_URL_PATTERN = "/cases/%s";
    private static final String UPDATE_OFFENCE_MEDIA_TYPE = "application/vnd.sjp.update-offence-code+json";

    @BeforeEach
    public void setUp() {
        stubOffenceFineLevelsQuery(fineLevel, maxValue);
        stubQueryOffencesByCode(offenceCode1);
        stubQueryOffencesByCode(offenceCode2);
    }


    @Test
    public void shouldSchemaValidationFailWhenNoLegalEntityAndPersonalDetails() {
        final CreateCase.CreateCasePayloadBuilder createCase = createMultiOffenceCase(randomUUID(), TFL, newArrayList("CA03010"));
        createCase.getDefendantBuilder().withFirstName(null);
        createCase.getDefendantBuilder().withLastName(null);
        createCase.getDefendantBuilder().withLegalEntityName(null);

        final String response = createCaseForPayloadBuilder(createCase, BAD_REQUEST);

        JsonObject responseJson = responseToJsonObject(response);
        JsonValue validationErrors = responseJson.get("validationErrors");
        String validationTrace = validationErrors.toString();

        with(validationTrace)
                .assertEquals("$.message", "#/defendant: #: only 1 subschema matches out of 2");
        assertThat(validationTrace, containsString(format("#/defendant: required key [%s] not found", "firstName")));
        assertThat(validationTrace, containsString(format("#/defendant: required key [%s] not found", "lastName")));
        assertThat(validationTrace, containsString(format("#/defendant: required key [%s] not found", "legalEntityName")));
    }

    @Test
    public void shouldCreateCaseWhenDefendantIsALegalEntity() {
        final UUID caseId = randomUUID();
        final ProsecutingAuthority prosecutingAuthority = TFL;
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());

        final String offenceCode1 = "CA03010";
        final String offenceCode2 = "CA03011";

        stubQueryOffencesByCode(offenceCode1);
        stubQueryOffencesByCode(offenceCode2);

        final int fineLevel = 3;
        final BigDecimal maxValue = BigDecimal.valueOf(1000);

        stubOffenceFineLevelsQuery(fineLevel, maxValue);

        final CreateCase.CreateCasePayloadBuilder createCase = createMultiOffenceCase(caseId, prosecutingAuthority,
                newArrayList(offenceCode1, offenceCode2));

        final CreateCase.DefendantBuilder defendant = createCase.getDefendantBuilder();
        defendant.withAsn("12345");
        defendant.withPncIdentifier("6789");
        defendant.withLegalEntityName("test");
        defendant.withFirstName(null);
        defendant.withLastName(null);
        defendant.withTitle(null);
        defendant.withDateOfBirth(null);
        defendant.withDriverNumber(null);
        defendant.withGender(null);
        defendant.withNationalInsuranceNumber(null);
        defendant.withDriverLicenceDetails(null);

        final Optional<JsonEnvelope> caseReceivedEvent = new EventListener()
                .subscribe(CaseReceived.EVENT_NAME)
                .run(() -> createCaseForPayloadBuilder(createCase))
                .popEvent(CaseReceived.EVENT_NAME);

        assertTrue(caseReceivedEvent.isPresent());

        final JsonPath jsonResponse = pollUntilCaseByIdIsOk(caseId, JsonPathMatchers.withJsonPath("$.status", equalTo(CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION.name())));
        assertThat(jsonResponse.get("id"), equalTo(caseId.toString()));
        assertThat(jsonResponse.get("urn"), equalTo(createCase.getUrn()));
        assertThat(jsonResponse.get("prosecutingAuthorityName"), equalTo(TFL.getFullName()));
        assertThat(jsonResponse.get("defendant.asn"), equalTo(defendant.getAsn()));
        assertThat(jsonResponse.get("defendant.pncIdentifier"), equalTo(defendant.getPncIdentifier()));
        assertThat(jsonResponse.get("defendant.legalEntityDetails.legalEntityName"), equalTo(defendant.getLegalEntityName()));
        assertThat(jsonResponse.get("defendant.legalEntityDetails.address.address1"), equalTo(defendant.getAddressBuilder().getAddress1()));
        assertThat(jsonResponse.get("defendant.legalEntityDetails.address.address2"), equalTo(defendant.getAddressBuilder().getAddress2()));
        assertThat(jsonResponse.get("defendant.legalEntityDetails.address.address3"), equalTo(defendant.getAddressBuilder().getAddress3()));
        assertThat(jsonResponse.get("defendant.legalEntityDetails.address.address4"), equalTo(defendant.getAddressBuilder().getAddress4()));
        assertThat(jsonResponse.get("defendant.legalEntityDetails.address.address5"), equalTo(defendant.getAddressBuilder().getAddress5()));
        assertThat(jsonResponse.get("defendant.legalEntityDetails.address.postcode"), equalTo(defendant.getAddressBuilder().getPostcode()));

        assertThat(jsonResponse.get("defendant.legalEntityDetails.contactDetails.mobile"), equalTo(defendant.getContactDetailsBuilder().getMobile()));
        assertThat(jsonResponse.get("defendant.legalEntityDetails.contactDetails.email"), equalTo(defendant.getContactDetailsBuilder().getEmail()));
        assertThat(jsonResponse.get("defendant.legalEntityDetails.contactDetails.home"), equalTo(defendant.getContactDetailsBuilder().getHome()));

    }

    @Test
    void shouldUpdateOffenceCodeForMetroLinkProsecutorAuthority() {
        stubForUserDetails(USER_ID, "ALL");
        final UUID caseId = randomUUID();
        final ProsecutingAuthority prosecutingAuthority = METLI;
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());

        final String offenceCode1 = "GM00001";
        final String offenceCode2 = "GM00002";

        stubQueryOffencesByCode(offenceCode1);
        stubQueryOffencesByCode(offenceCode2);

        final int fineLevel = 3;
        final BigDecimal maxValue = BigDecimal.valueOf(1000);

        stubOffenceFineLevelsQuery(fineLevel, maxValue);

        final CreateCase.CreateCasePayloadBuilder createCase = createMultiOffenceCase(caseId, prosecutingAuthority,
                newArrayList(offenceCode1));

        final CreateCase.DefendantBuilder defendant = createCase.getDefendantBuilder();
        defendant.withAsn("12345");
        defendant.withPncIdentifier("6789");
        defendant.withLegalEntityName("test");
        defendant.withFirstName(null);
        defendant.withLastName(null);
        defendant.withTitle(null);
        defendant.withDateOfBirth(null);
        defendant.withDriverNumber(null);
        defendant.withGender(null);
        defendant.withNationalInsuranceNumber(null);
        defendant.withDriverLicenceDetails(null);

        final Optional<JsonEnvelope> caseReceivedEvent = new EventListener()
                .subscribe(CaseReceived.EVENT_NAME)
                .run(() -> createCaseForPayloadBuilder(createCase))
                .popEvent(CaseReceived.EVENT_NAME);

        assertTrue(caseReceivedEvent.isPresent());

        final JsonPath jsonResponse = pollUntilCaseByIdIsOk(caseId, JsonPathMatchers.withJsonPath("$.status", equalTo(CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION.name())));
        assertThat(jsonResponse.get("id"), equalTo(caseId.toString()));
        assertThat(jsonResponse.get("urn"), equalTo(createCase.getUrn()));
        assertThat(jsonResponse.get("prosecutingAuthorityName"), equalTo(METLI.getFullName()));

        //fire new event
        final JsonObject updateOffenceCodePayload = createObjectBuilder()
                .add("offenceCode", offenceCode2)
                .build();

        makePostCall(USER_ID, format(WRITE_URL_PATTERN, caseId),
                UPDATE_OFFENCE_MEDIA_TYPE,
                updateOffenceCodePayload.toString(),
                Response.Status.ACCEPTED);

        verifyOffenceCode(caseId, offenceCode2);
    }

    @Test
    public void shouldMultiOffenceCaseBeCreatedWithEnterpriseId() {
        final UUID caseId = randomUUID();
        final ProsecutingAuthority prosecutingAuthority = TFL;
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());

        final String offenceCode1 = "CA03010";
        final String offenceCode2 = "CA03011";

        final JsonObject offence1Definition = stubQueryOffencesByCode(offenceCode1);
        final JsonObject offence2Definition = stubQueryOffencesByCode(offenceCode2);

        final CreateCase.CreateCasePayloadBuilder createCase = createMultiOffenceCase(caseId, prosecutingAuthority,
                newArrayList(offenceCode1, offenceCode2));

        final CreateCase.DefendantBuilder defendant = createCase.getDefendantBuilder();
        final CreateCase.OffenceBuilder offence = createCase.getOffenceBuilder();
        stubEnforcementAreaByPostcode(defendant.getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, DEFENDANT_REGION);

        createCaseForPayloadBuilder(createCase);

        final JsonPath jsonResponse = pollUntilCaseByIdIsOk(caseId, JsonPathMatchers.withJsonPath("$.status", equalTo(CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION.name())));
        assertThat(jsonResponse.get("id"), equalTo(caseId.toString()));
        assertThat(jsonResponse.get("urn"), equalTo(createCase.getUrn()));
        assertThat(jsonResponse.get("prosecutingAuthorityName"), equalTo(TFL.getFullName()));
        assertThat(jsonResponse.get("enterpriseId"), equalTo(createCase.getEnterpriseId()));
        assertThat(jsonResponse.get("status"), equalTo(CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION.name()));
        assertThat(jsonResponse.get("defendant.id"), equalTo(defendant.getId().toString()));
        assertThat(jsonResponse.get("defendant.pcqId"), is(notNullValue()));
        assertThat(jsonResponse.get("defendant.personalDetails.title"), equalTo(defendant.getTitle()));
        assertThat(jsonResponse.get("defendant.personalDetails.firstName"), equalTo(defendant.getFirstName()));
        assertThat(jsonResponse.get("defendant.personalDetails.lastName"), equalTo(defendant.getLastName()));
        assertThat(jsonResponse.get("defendant.personalDetails.dateOfBirth"), equalTo(defendant.getDateOfBirth().toString()));
        assertThat(jsonResponse.get("defendant.personalDetails.gender"), equalTo(defendant.getGender().toString()));
        assertThat(jsonResponse.get("defendant.numPreviousConvictions"), equalTo(defendant.getNumPreviousConvictions()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address1"), equalTo(defendant.getAddressBuilder().getAddress1()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address2"), equalTo(defendant.getAddressBuilder().getAddress2()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address3"), equalTo(defendant.getAddressBuilder().getAddress3()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address4"), equalTo(defendant.getAddressBuilder().getAddress4()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address5"), equalTo(defendant.getAddressBuilder().getAddress5()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.postcode"), equalTo(defendant.getAddressBuilder().getPostcode()));
        assertThat(jsonResponse.get("defendant.personalDetails.contactDetails.home"), equalTo(defendant.getContactDetailsBuilder().getHome()));
        assertThat(jsonResponse.get("defendant.personalDetails.contactDetails.mobile"), equalTo(defendant.getContactDetailsBuilder().getMobile()));
        assertThat(jsonResponse.get("defendant.personalDetails.contactDetails.email"), equalTo(defendant.getContactDetailsBuilder().getEmail()));
        assertThat(jsonResponse.get("defendant.personalDetails.nationalInsuranceNumber"), equalTo(defendant.getNationalInsuranceNumber()));

        assertOffenceData(jsonResponse, offence, offenceCode1, offence1Definition, true, false, 0);
        assertOffenceData(jsonResponse, offence, offenceCode2, offence2Definition, false, true, 1);

        assertThat(getProsecutingAuthority(caseId), is(prosecutingAuthority.name()));
    }

    @Test
    public void shouldMultiOffenceCaseBeCreatedWithUnknownEnforcementArea() {
        final UUID caseId = randomUUID();
        final ProsecutingAuthority prosecutingAuthority = TFL;
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());

        final JsonObject offence1Definition = stubQueryOffencesByCode(offenceCode1);
        final JsonObject offence2Definition = stubQueryOffencesByCode(offenceCode2);

        final CreateCase.CreateCasePayloadBuilder createCase = createMultiOffenceCase(caseId, prosecutingAuthority,
                newArrayList(offenceCode1, offenceCode2));

        final CreateCase.DefendantBuilder defendant = createCase.getDefendantBuilder();
        defendant.getAddressBuilder().withPostcode("ML9 1NQ");
        final CreateCase.OffenceBuilder offence = createCase.getOffenceBuilder();
        stubEnforcementAreaByPostcode(defendant.getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, DEFENDANT_REGION);

        createCaseForPayloadBuilder(createCase);

        final JsonPath jsonResponse = pollUntilCaseByIdIsOk(caseId, JsonPathMatchers.withJsonPath("$.status", equalTo(CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION.name())));
        assertThat(jsonResponse.get("id"), equalTo(caseId.toString()));
        assertThat(jsonResponse.get("urn"), equalTo(createCase.getUrn()));
        assertThat(jsonResponse.get("prosecutingAuthorityName"), equalTo(TFL.getFullName()));
        assertThat(jsonResponse.get("enterpriseId"), equalTo(createCase.getEnterpriseId()));
        assertThat(jsonResponse.get("status"), equalTo(CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION.name()));
        assertThat(jsonResponse.get("defendant.id"), equalTo(defendant.getId().toString()));
        assertThat(jsonResponse.get("defendant.pcqId"), is(notNullValue()));
        assertThat(jsonResponse.get("defendant.personalDetails.title"), equalTo(defendant.getTitle()));
        assertThat(jsonResponse.get("defendant.personalDetails.firstName"), equalTo(defendant.getFirstName()));
        assertThat(jsonResponse.get("defendant.personalDetails.lastName"), equalTo(defendant.getLastName()));
        assertThat(jsonResponse.get("defendant.personalDetails.dateOfBirth"), equalTo(defendant.getDateOfBirth().toString()));
        assertThat(jsonResponse.get("defendant.personalDetails.gender"), equalTo(defendant.getGender().toString()));
        assertThat(jsonResponse.get("defendant.numPreviousConvictions"), equalTo(defendant.getNumPreviousConvictions()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address1"), equalTo(defendant.getAddressBuilder().getAddress1()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address2"), equalTo(defendant.getAddressBuilder().getAddress2()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address3"), equalTo(defendant.getAddressBuilder().getAddress3()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address4"), equalTo(defendant.getAddressBuilder().getAddress4()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address5"), equalTo(defendant.getAddressBuilder().getAddress5()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.postcode"), equalTo(defendant.getAddressBuilder().getPostcode()));
        assertThat(jsonResponse.get("defendant.personalDetails.contactDetails.home"), equalTo(defendant.getContactDetailsBuilder().getHome()));
        assertThat(jsonResponse.get("defendant.personalDetails.contactDetails.mobile"), equalTo(defendant.getContactDetailsBuilder().getMobile()));
        assertThat(jsonResponse.get("defendant.personalDetails.contactDetails.email"), equalTo(defendant.getContactDetailsBuilder().getEmail()));
        assertThat(jsonResponse.get("defendant.personalDetails.nationalInsuranceNumber"), equalTo(defendant.getNationalInsuranceNumber()));

        assertOffenceData(jsonResponse, offence, offenceCode1, offence1Definition, true, false, 0);
        assertOffenceData(jsonResponse, offence, offenceCode2, offence2Definition, false, true, 1);

        assertThat(getProsecutingAuthority(caseId), is(prosecutingAuthority.name()));
    }

    @Test
    public void shouldSchemaValidationFailWhenEmailIsInvalid() {
        final CreateCase.CreateCasePayloadBuilder createCase = createMultiOffenceCase(randomUUID(), TFL, newArrayList("CA03010"));
        final String email = "   ";
        final String email2 = "@b.co";
        createCase.getDefendantBuilder().getContactDetailsBuilder().withEmail(email).withEmail2(email2);

        final String response = createCaseForPayloadBuilder(createCase, BAD_REQUEST);

        JsonObject responseJson = responseToJsonObject(response);
        JsonValue validationErrors = responseJson.get("validationErrors");
        String validationTrace = validationErrors.toString();

        with(validationTrace)
                .assertEquals("$.message", "#/defendant: #: only 1 subschema matches out of 2");
        assertThat(validationTrace, containsString(format("#/defendant/contactDetails/email2: string [%s] does not match pattern", email2)));
        assertThat(validationTrace, containsString(format("#/defendant/contactDetails/email: string [%s] does not match pattern", email)));
    }

    @Test
    public void shouldSchemaValidationFailWhenPostCodeNoSpaceIsInvalid() {
        final CreateCase.CreateCasePayloadBuilder createCase = createMultiOffenceCase(randomUUID(), TFL, newArrayList("CA03010"));
        final String postCode = "ML91NQ";

        createCase.getDefendantBuilder().getAddressBuilder().withPostcode(postCode);

        final String response = createCaseForPayloadBuilder(createCase, BAD_REQUEST);

        JsonObject responseJson = responseToJsonObject(response);
        JsonValue validationErrors = responseJson.get("validationErrors");
        String validationTrace = validationErrors.toString();
        assertThat(validationTrace, containsString("#/defendant/address/postcode: string [ML91NQ] does not match pattern ^(([gG][iI][rR] {0,}0[aA]{2})|(([aA][sS][cC][nN]|[sS][tT][hH][lL]|[tT][dD][cC][uU]|[bB][bB][nN][dD]|[bB][iI][qQ][qQ]|[fF][iI][qQ][qQ]|[pP][cC][rR][nN]|[sS][iI][qQ][qQ]|[iT][kK][cC][aA]) {0,}1[zZ]{2})|((([a-pr-uwyzA-PR-UWYZ][a-hk-yxA-HK-XY]?[0-9][0-9]?)|(([a-pr-uwyzA-PR-UWYZ][0-9][a-hjkstuwA-HJKSTUW])|([a-pr-uwyzA-PR-UWYZ][a-hk-yA-HK-Y][0-9][abehmnprv-yABEHMNPRV-Y]))) [0-9][abd-hjlnp-uw-zABD-HJLNP-UW-Z]{2}))$"));

    }

    @Test
    public void shouldMultiOffenceCaseBeCreatedWithDvlaAttributes() {
        final UUID caseId = randomUUID();
        final ProsecutingAuthority prosecutingAuthority = DVLA;
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());

        final LocalDate backDutyFromDate = now().minusMonths(6);
        final LocalDate backDutyToDate = now().minusMonths(3);

        final CreateCase.CreateCasePayloadBuilder createCase = createMultiOffenceCaseWithDvlaAttributes(caseId, prosecutingAuthority,
                backDutyFromDate, backDutyToDate,
                newArrayList(offenceCode1, offenceCode2));

        stubEnforcementAreaByPostcode(createCase.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, DEFENDANT_REGION);

        createCaseForPayloadBuilder(createCase);

        final JsonPath jsonResponse = pollUntilCaseByIdIsOk(caseId, JsonPathMatchers.withJsonPath("$.status", equalTo(CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION.name())));
        assertThat(jsonResponse.get("id"), equalTo(caseId.toString()));
        assertOffenceDvlaData(jsonResponse, backDutyFromDate, backDutyToDate, 0);
    }

    @Test
    public void shouldCaseBeCreatedWithoutDefendantTitle() {
        final UUID caseId = randomUUID();
        final ProsecutingAuthority prosecutingAuthority = TFL;
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());

        final CreateCase.CreateCasePayloadBuilder createCase = createMultiOffenceCase(caseId, prosecutingAuthority,
                newArrayList(offenceCode1, offenceCode2));

        final CreateCase.DefendantBuilder defendant = createCase.getDefendantBuilder();
        defendant.withTitle(null);
        stubEnforcementAreaByPostcode(defendant.getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, DEFENDANT_REGION);

        createCaseForPayloadBuilder(createCase);

        final JsonPath jsonResponse = pollUntilCaseByIdIsOk(caseId, JsonPathMatchers.withJsonPath("$.status", equalTo(CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION.name())));
        assertThat(jsonResponse.get("id"), equalTo(caseId.toString()));
        assertThat(jsonResponse.get("urn"), equalTo(createCase.getUrn()));
        assertThat(jsonResponse.get("prosecutingAuthorityName"), equalTo(TFL.getFullName()));
        assertThat(jsonResponse.get("defendant.personalDetails.title"), blankOrNullString());
    }

    @Test
    public void shouldCaseBeCreatedWithoutPostcode() {
        final UUID caseId = randomUUID();
        final ProsecutingAuthority prosecutingAuthority = TFL;
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());

        final CreateCase.CreateCasePayloadBuilder createCase = createMultiOffenceCase(caseId, prosecutingAuthority,
                newArrayList(offenceCode1, offenceCode2));

        final CreateCase.DefendantBuilder defendant = createCase.getDefendantBuilder();
        defendant.withTitle(null);
        AddressBuilder defendantAddress = defendant.getAddressBuilder();
        defendantAddress.withPostcode(null);

        createCaseForPayloadBuilder(createCase);

        final JsonPath jsonResponse = pollUntilCaseByIdIsOk(caseId, JsonPathMatchers.withJsonPath("$.status", equalTo(CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION.name())));
        assertThat(jsonResponse.get("id"), equalTo(caseId.toString()));
        assertThat(jsonResponse.get("urn"), equalTo(createCase.getUrn()));
        assertThat(jsonResponse.get("prosecutingAuthorityName"), equalTo(TFL.getFullName()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.postcode"), blankOrNullString());
    }

    @Test
    public void shouldCaseBeCreatedWithAsnAndPncIdentifier() {
        final UUID caseId = randomUUID();
        final ProsecutingAuthority prosecutingAuthority = TFL;
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());

        final CreateCase.CreateCasePayloadBuilder createCase = createMultiOffenceCase(caseId, prosecutingAuthority,
                newArrayList(offenceCode1, offenceCode2));

        final CreateCase.DefendantBuilder defendant = createCase.getDefendantBuilder();
        defendant.withAsn("12345");
        defendant.withPncIdentifier("6789");

        createCaseForPayloadBuilder(createCase);

        final JsonPath jsonResponse = pollUntilCaseByIdIsOk(caseId, JsonPathMatchers.withJsonPath("$.status", equalTo(CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION.name())));
        assertThat(jsonResponse.get("id"), equalTo(caseId.toString()));
        assertThat(jsonResponse.get("urn"), equalTo(createCase.getUrn()));
        assertThat(jsonResponse.get("prosecutingAuthorityName"), equalTo(TFL.getFullName()));
        assertThat(jsonResponse.get("defendant.asn"), equalTo(defendant.getAsn()));
        assertThat(jsonResponse.get("defendant.pcqId"), is(notNullValue()));
        assertThat(jsonResponse.get("defendant.pncIdentifier"), equalTo(defendant.getPncIdentifier()));
    }

    @Test
    public void shouldCaseBeCreatedWithEndorsableOffences() {
        final UUID caseId = randomUUID();
        final ProsecutingAuthority prosecutingAuthority = TVL;
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());

        final CreateCase.CreateCasePayloadBuilder createCase = createMultiOffenceCase(caseId, prosecutingAuthority,
                newArrayList(offenceCode1, offenceCode2));

        final CreateCase.DefendantBuilder defendant = createCase.getDefendantBuilder();

        createCase.getOffenceBuilders().forEach(offenceBuilder -> offenceBuilder.withEndorsable(true));
        stubEnforcementAreaByPostcode(defendant.getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, DEFENDANT_REGION);

        final Optional<JsonEnvelope> caseReceivedEvent = new EventListener()
                .subscribe(CaseReceived.EVENT_NAME)
                .run(() -> createCaseForPayloadBuilder(createCase))
                .popEvent(CaseReceived.EVENT_NAME);

        assertTrue(caseReceivedEvent.isPresent());

        final JsonPath jsonResponse = pollUntilCaseByIdIsOk(caseId, JsonPathMatchers.withJsonPath("$.status", equalTo(CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION.name())));
        assertThat(jsonResponse.get("id"), equalTo(caseId.toString()));
        assertThat(jsonResponse.get("urn"), equalTo(createCase.getUrn()));
        assertThat(jsonResponse.get("prosecutingAuthorityName"), equalTo(TVL.getFullName()));
        assertThat(jsonResponse.get("defendant.pcqId"), is(notNullValue()));
        assertThat(jsonResponse.get("defendant.offences[0].endorsable"), equalTo(true));
        assertThat(jsonResponse.get("defendant.offences[1].endorsable"), equalTo(true));
    }

    @Test
    public void shouldListenToCaseEligibleForAOCPPrivateEvent() {
        final UUID caseId = randomUUID();
        final ProsecutingAuthority prosecutingAuthority = TFL;
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());

        final CreateCase.CreateCasePayloadBuilder createCase = createMultiOffenceCase(caseId, prosecutingAuthority,
                newArrayList(offenceCode1, offenceCode2));

        final CreateCase.DefendantBuilder defendant = createCase.getDefendantBuilder();
        stubEnforcementAreaByPostcode(defendant.getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, DEFENDANT_REGION);

        final Optional<JsonEnvelope> event = new EventListener()
                .subscribe(CaseEligibleForAOCP.EVENT_NAME)
                .run(() -> createCaseForPayloadBuilder(createCase))
                .popEvent(CaseEligibleForAOCP.EVENT_NAME);

        assertTrue(event.isPresent());
        final JsonObject payload = event.get().payloadAsJsonObject();
        assertThat(payload.getString("caseId"), is(caseId.toString()));
        assertThat(payload.getInt("victimSurcharge"), is(80));
        assertThat(payload.getJsonNumber("aocpTotalCost").doubleValue(), is(285.91));
        assertThat(payload.getJsonObject("defendant").getString("id"), is(defendant.getId().toString()));
        final JsonArray offences = payload.getJsonObject("defendant").getJsonArray("offences");
        assertThat(offences.getJsonObject(0).getInt("aocpStandardPenaltyAmount"), is(100));

    }

    private void assertOffenceData(final JsonPath jsonResponse, final CreateCase.OffenceBuilder offence, final String offenceCode, final JsonObject offenceDefinition, boolean outOfTime, boolean notInEffect, final int index) {
        assertThat(jsonResponse.get(format("defendant.offences[%d].offenceSequenceNumber", index)), equalTo(index + 1));
        assertThat(jsonResponse.get(format("defendant.offences[%d].wording", index)), equalTo(offence.getOffenceWording()));
        assertThat(jsonResponse.get(format("defendant.offences[%d].wordingWelsh", index)), equalTo(offence.getOffenceWordingWelsh()));
        assertThat(jsonResponse.get(format("defendant.offences[%d].chargeDate", index)), equalTo(offence.getChargeDate().toString()));
        assertThat(jsonResponse.get(format("defendant.offences[%d].startDate", index)), equalTo(offence.getOffenceCommittedDate().toString()));
        assertThat(jsonResponse.get(format("defendant.offences[%d].offenceCode", index)), equalTo(offenceCode));
        assertThat(jsonResponse.get(format("defendant.offences[%d].titleWelsh", index)), equalTo(JsonObjects.getString(offenceDefinition, "details", "document", "welsh", "welshoffencetitle").orElse(null)));
        assertThat(jsonResponse.get(format("defendant.offences[%d].legislation", index)), equalTo(offenceDefinition.getString("legislation")));
        assertThat(jsonResponse.get(format("defendant.offences[%d].legislationWelsh", index)), equalTo(JsonObjects.getString(offenceDefinition, "details", "document", "welsh", "welshlegislation").orElse(null)));
        assertThat(jsonResponse.get(format("defendant.offences[%d].outOfTime", index)), equalTo(outOfTime));
        assertThat(jsonResponse.get(format("defendant.offences[%d].notInEffect", index)), equalTo(notInEffect));
    }

    private void assertOffenceDvlaData(final JsonPath jsonResponse, final LocalDate backDutyFrom, LocalDate backDutyTo, final int index) {
        assertThat(jsonResponse.get(format("defendant.offences[%d].offenceSequenceNumber", index)), equalTo(index + 1));

        assertThat(jsonResponse.get(format("defendant.offences[%d].backDuty", index)), equalTo(340.30F));
        assertThat(jsonResponse.get(format("defendant.offences[%d].backDutyDateFrom", index)), equalTo(backDutyFrom.toString()));
        assertThat(jsonResponse.get(format("defendant.offences[%d].backDutyDateTo", index)), equalTo(backDutyTo.toString()));
        assertThat(jsonResponse.get(format("defendant.offences[%d].vehicleMake", index)), equalTo("FORD"));
        assertThat(jsonResponse.get(format("defendant.offences[%d].vehicleRegistrationMark", index)), equalTo("FG59 4FD"));
    }

    private CreateCase.CreateCasePayloadBuilder createMultiOffenceCase(final UUID caseId, final ProsecutingAuthority prosecutingAuthority,
                                                                       final List<String> offenceCodes) {
        return defaultCaseBuilder()
                .withId(caseId)
                .withProsecutingAuthority(prosecutingAuthority)
                .withOffenceBuilders(offenceCodes.stream()
                        .map(offenceCode -> defaultOffenceBuilder()
                                .withId(randomUUID())
                                .withLibraOffenceCode(offenceCode)
                                .withOffenceCommittedDate(now().minusMonths(4))
                                .withOffenceChargeDate(now())
                                .withProsecutorOfferAOCP(true)
                        ).collect(toList()))
                .withDefendantId(randomUUID());
    }

    private CreateCase.CreateCasePayloadBuilder createMultiOffenceCaseWithDvlaAttributes(final UUID caseId, final ProsecutingAuthority prosecutingAuthority,
                                                                                         LocalDate backDutyFromDate, LocalDate backDutyToDate, final List<String> offenceCodes) {
        return defaultCaseBuilder()
                .withId(caseId)
                .withProsecutingAuthority(prosecutingAuthority)
                .withOffenceBuilders(offenceCodes.stream()
                        .map(offenceCode -> defaultOffenceBuilder()
                                .withId(randomUUID())
                                .withLibraOffenceCode(offenceCode)
                                .withOffenceCommittedDate(now().minusMonths(4))
                                .withOffenceChargeDate(now())
                                .withBackDuty(BigDecimal.valueOf(340.30))
                                .withBackDutyDateFrom(backDutyFromDate)
                                .withBackDutyDateTo(backDutyToDate)
                                .withVehicleMake("FORD")
                                .withVehicleRegistrationMark("FG59 4FD")
                        ).collect(toList()))
                .withDefendantId(randomUUID());
    }

    private JsonObject responseToJsonObject(String response) {
        return createReader(new StringReader(response)).readObject();
    }

    private static void verifyOffenceCode(final UUID caseId, final String offenceCode2) {
        await().atMost(TIMEOUT_IN_SECONDS, SECONDS).until(() -> {
            final JsonPath updateCase = pollUntilCaseByIdIsOk(caseId);
            assertThat(updateCase.get("prosecutingAuthorityName"), equalTo(METLI.getFullName()));
            return offenceCode2.equalsIgnoreCase(updateCase.get("defendant.offences[0].offenceCode"));
        });
    }
}
