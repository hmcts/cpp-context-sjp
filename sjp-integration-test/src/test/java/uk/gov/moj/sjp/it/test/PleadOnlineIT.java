package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static javax.json.Json.createReader;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static uk.gov.justice.json.schemas.domains.sjp.User.user;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.REFERRED_FOR_COURT_HEARING;
import static uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds.NO_DISABILITY_NEEDS;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.sjp.it.Constants.NOTICE_PERIOD_IN_DAYS;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.helper.PleadOnlineHelper.getOnlinePlea;
import static uk.gov.moj.sjp.it.helper.PleadOnlineHelper.verifyOnlinePleaReceivedAndUpdatedCaseDetailsFlag;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;
import static uk.gov.moj.sjp.it.helper.SetPleasHelper.createCase;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;
import static uk.gov.moj.sjp.it.stub.NotifyStub.stubNotifications;
import static uk.gov.moj.sjp.it.stub.NotifyStub.verifyNotification;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubAllReferenceData;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubCountryByPostcodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubHearingTypesQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubReferralReason;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubReferralReasonsQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubStartSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.COURT_ADMINISTRATORS_GROUP;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.LEGAL_ADVISERS_GROUP;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.SJP_PROSECUTORS_GROUP;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;

import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtInterpreter;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.decision.ReferForCourtHearing;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.ContactDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseSearchResultHelper;
import uk.gov.moj.sjp.it.helper.DecisionHelper;
import uk.gov.moj.sjp.it.helper.EmployerHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.FinancialMeansHelper;
import uk.gov.moj.sjp.it.helper.PleadOnlineHelper;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;
import uk.gov.moj.sjp.it.stub.UsersGroupsStub;
import uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper;
import uk.gov.moj.sjp.it.util.Defaults;
import uk.gov.moj.sjp.it.util.HttpClientUtil;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;
import uk.gov.moj.sjp.it.verifier.PersonInfoVerifier;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.ws.rs.core.Response;

import com.google.common.collect.Sets;
import com.jayway.jsonpath.ReadContext;
import com.jayway.restassured.path.json.JsonPath;
import org.apache.commons.lang.text.StrSubstitutor;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
public class PleadOnlineIT extends BaseIntegrationTest {
    private static final String TEMPLATE_PLEA_NOT_GUILTY_PAYLOAD = "raml/json/sjp.command.plead-online__not-guilty.json";
    private static final String TEMPLATE_PLEA_MULTI_OFFENCE_PAYLOAD = "raml/json/sjp.command.plead-online__multi_offence.json";
    private static final String TEMPLATE_PLEA_MULTI_OFFENCE_CUSTOM_V2 = "raml/json/sjp.command.plead-online__multi_offence_v2.json";
    private static final String TEMPLATE_PLEA_GUILTY_PAYLOAD = "raml/json/sjp.command.plead-online__guilty.json";
    private static final String TEMPLATE_PLEA_GUILTY_PAYLOAD_WITH_DRIVER_LICENCE_DETAILS = "raml/json/sjp.command.plead-online__guilty_with_driver_licence_details.json";
    private static final String TEMPLATE_PLEA_GUILTY_PAYLOAD_WITH_DRIVER_NUMBER = "raml/json/sjp.command.plead-online__guilty_with_driver_number.json";
    private static final String TEMPLATE_PLEA_GUILTY_REQUEST_HEARING_PAYLOAD = "raml/json/sjp.command.plead-online__guilty_request_hearing.json";
    private static final String TEMPLATE_PLEA_MULTI_OFFENCE_NO_CHANGE_PERSONAL_DETAILS_PAYLOAD = "raml/json/sjp.command.plead-online__david-lloyd.json";
    private static final String TEMPLATE_PLEA_GUILTY_WITH_FINANCIAL_MEANS_RESPONSE = "raml/json/sjp.command.plead-online__guilty_with_finances_response.json";
    private static final String TEMPLATE_PLEA_GUILTY_WITH_DRIVER_LICENCE_DETAILS_RESPONSE = "raml/json/sjp.command.plead-online__guilty_with_driver_licence_details_response.json";
    private static final String TEMPLATE_PLEA_GUILTY_WITH_FINANCIAL_MEANS_CASE_RESPONSE = "raml/json/sjp.command.plead-online__guilty_with_finances_case_response.json";
    private static final String TEMPLATE_PLEA_GUILTY_WITH_DRIVER_LICENCE_DETAILS_CASE_RESPONSE = "raml/json/sjp.command.plead-online__guilty_with_driver_licence_details_case_response.json";
    private static final String TEMPLATE_PLEA_NOT_GUILTY_WITHOUT_FINANCIAL_MEANS_RESPONSE = "raml/json/sjp.command.plead-online__not-guilty_without_finances_response.json";
    private static final String TEMPLATE_PLEA_NOT_GUILTY_WITHOUT_FINANCIAL_MEANS_AND_OUTGOINGS_RESPONSE = "raml/json/sjp.command.plead-online__not-guilty_without_finances_and_outgoings_response.json";
    private static final String TEMPLATE_PLEA_NOT_GUILTY_WITHOUT_OUTGOINGS_RESPONSE = "raml/json/sjp.command.plead-online__not-guilty_without_outgoings_response.json";
    private static final String TEMPLATE_PLEA_NOT_GUILTY_WITH_CHANGED_DETAILS_RESPONSE = "raml/json/sjp.command.plead-online__not-guilty_with_changed_details.json";
    private static final String TEMPLATE_PLEA_NOT_GUILTY_WITHOUT_FINANCIAL_MEANS_CASE_RESPONSE = "raml/json/sjp.command.plead-online__not-guilty_without_finances_case_response.json";
    private static final String TEMPLATE_PLEA_NOT_GUILTY_WITH_CHANGED_DETAILS_CASE_RESPONSE = "raml/json/sjp.command.plead-online__not-guilty_with_changed_details_case_response.json";
    private static final String DEFENDANT_DETAIL_UPDATES_CONTENT_TYPE = "application/vnd.sjp.query.defendant-details-updates+json";
    private static final String ENGLISH_TEMPLATE_ID = "07d1f043-6052-4d18-adce-58678d0e7018";
    private static final Set<UUID> DEFAULT_STUBBED_USER_ID = singleton(USER_ID);
    private static final SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();
    private static final UUID REFERRAL_REASON_ID = randomUUID();
    private static final String HEARING_CODE = "PLE";
    private static final String REFERRAL_REASON = "referral reason";
    private static final UUID HEARING_TYPE_ID = fromString("06b0c2bf-3f98-46ed-ab7e-56efaf9ecced");
    private static final String HEARING_DESCRIPTION = "Plea & Trial Preparation";
    private static final String DEFENDANT_REGION = "croydon";
    private static final String NATIONAL_COURT_CODE = "1080";
    private final EventListener eventListener = new EventListener();
    private EmployerHelper employerHelper;
    private FinancialMeansHelper financialMeansHelper;
    private PersonInfoVerifier personInfoVerifier;
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;
    private UUID offenceId;
    final User legalAdviser = user()
            .withUserId(UUID.fromString("1ac91935-4f82-4a4f-bd17-fb50397e42dd"))
            .withFirstName("John")
            .withLastName("Smith")
            .build();
    @Before
    @SuppressWarnings("squid:S2925")
    public void setUp() throws Exception {
        offenceId = randomUUID();
        databaseCleaner.cleanViewStore();
        CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));
        this.createCasePayloadBuilder =
                CreateCase.CreateCasePayloadBuilder
                        .withDefaults()
                        .withOffenceId(offenceId);
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, DEFENDANT_REGION);
        CreateCase.createCaseForPayloadBuilder(this.createCasePayloadBuilder);
        final ProsecutingAuthority prosecutingAuthority = createCasePayloadBuilder.getProsecutingAuthority();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
        stubForUserDetails(legalAdviser, "ALL");
        pollUntilCaseByIdIsOk(createCasePayloadBuilder.getId());
        employerHelper = new EmployerHelper();
        financialMeansHelper = new FinancialMeansHelper();
        personInfoVerifier = PersonInfoVerifier.personInfoVerifierForCasePayload(createCasePayloadBuilder);
        stubCountryByPostcodeQuery("W1T 1JY", "England");
        stubNotifications();
        stubAllReferenceData();
    }
    @After
    public void tearDown() throws Exception {
        if (null != employerHelper) {
            employerHelper.close();
        }
        if (null != financialMeansHelper) {
            financialMeansHelper.close();
        }
    }
    private PersonalDetails generateExpectedPersonDetails(final JSONObject payload) {
        final JSONObject person = payload.getJSONObject("personalDetails");
        final String firstName = person.getString("firstName");
        final String lastName = person.getString("lastName");
        final String nationalInsuranceNumber = person.getString("nationalInsuranceNumber");
        final String driverNumber = person.optString("driverNumber", null);
        final String driverLicenceDetails = person.optString("driverLicenceDetails", null);
        final String dateOfBirth = person.getString("dateOfBirth");
        final JSONObject contactDetails = person.getJSONObject("contactDetails");
        final String homeNumber = contactDetails.getString("home");
        final String mobileNumber = contactDetails.getString("mobile");
        final String email = contactDetails.getString("email");
        final JSONObject address = person.getJSONObject("address");
        final String address1 = address.getString("address1");
        final String address2 = address.getString("address2");
        final String address3 = address.getString("address3");
        final String address4 = address.getString("address4");
        final String address5 = address.getString("address5");
        final String postcode = address.getString("postcode");
        //fields that we do not override
        final String title = personInfoVerifier.getPersonalDetails().getTitle();
        final Gender gender = personInfoVerifier.getPersonalDetails().getGender();
        return new PersonalDetails(title, firstName, lastName, LocalDate.parse(dateOfBirth), gender, nationalInsuranceNumber, driverNumber, driverLicenceDetails,
                new Address(address1, address2, address3, address4, address5, postcode),
                new ContactDetails(email, homeNumber, mobileNumber),
                null
        );
    }
    private void pleadOnlineAndConfirmSuccess(final PleaType pleaType, final PleadOnlineHelper pleadOnlineHelper, final CaseSearchResultHelper caseSearchResultHelper) {
        pleadOnlineAndConfirmSuccess(pleaType, pleadOnlineHelper, caseSearchResultHelper,
                DEFAULT_STUBBED_USER_ID, true);
    }
    private void pleadOnlineAndConfirmSuccess(final PleaType pleaType, final PleadOnlineHelper pleadOnlineHelper, final CaseSearchResultHelper caseSearchResultHelper,
                                              final Collection<UUID> userIds, final boolean expectToHaveFinances) {
        assumeThat(userIds, not(empty()));
        //checks person-info before plead-online
        personInfoVerifier.verifyPersonInfo();
        //runs plea-online
        verifyOnlinePleaReceivedAndUpdatedCaseDetailsFlag(createCasePayloadBuilder.getId(), false);
        final JSONObject pleaPayload = getOnlinePleaPayload(pleaType);
        pleadOnlineHelper.pleadOnline(pleaPayload.toString());
        //verify plea
        pleadOnlineHelper.verifyInPublicTopic(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId(), pleaType, null);
        pleadOnlineHelper.verifyPleaUpdated(createCasePayloadBuilder.getId(), pleaType, PleaMethod.ONLINE);
        caseSearchResultHelper.verifyPleaReceivedDate();
        //verify employer
        final String defendantId = pleadOnlineHelper.getCaseDefendantId().toString();
        employerHelper.getEmployer(defendantId, getEmployerUpdatedPayloadMatcher(pleaPayload));
        assertThat(employerHelper.getEventFromPublicTopic(), getEmployerUpdatedPublicEventMatcher(pleaPayload));
        //verify financial-means
        financialMeansHelper.getFinancialMeans(defendantId, getFinancialMeansUpdatedPayloadContentMatcher(pleaPayload, defendantId));
        financialMeansHelper.getEventFromPublicTopic(getFinancialMeansUpdatedPayloadContentMatcher(pleaPayload, defendantId));
        //verifies person-info has changed
        final PersonalDetails expectedPersonalDetails = generateExpectedPersonDetails(pleaPayload);
        PersonInfoVerifier.personInfoVerifierForPersonalDetails(createCasePayloadBuilder.getId(), expectedPersonalDetails)
                .verifyPersonInfo(true);
        //verify online-plea
        final Matcher expectedResult = getSavedOnlinePleaPayloadContentMatcher(pleaType, pleaPayload, createCasePayloadBuilder.getId().toString(), defendantId, expectToHaveFinances);
        userIds.forEach(userId -> getOnlinePlea(createCasePayloadBuilder.getId().toString(),
                createCasePayloadBuilder.getDefendantBuilder().getId().toString(),
                expectedResult, userId));
        verifyOnlinePleaReceivedAndUpdatedCaseDetailsFlag(createCasePayloadBuilder.getId(), true);
        verifyNotification("criminal@gmail.com", createCasePayloadBuilder.getUrn(), ENGLISH_TEMPLATE_ID);
    }

    @Test
    public void shouldPleadOnlineMultiOffences() {
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();
        final PleaType pleaType1 = GUILTY;
        final PleaType pleaType2 = NOT_GUILTY;
        final PleaType pleaType3 = GUILTY;
        final UUID caseId = randomUUID();
        final CreateCase.DefendantBuilder defendantBuilder = CreateCase.DefendantBuilder.withDefaults();
        final LocalDate postingDate = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);
        this.createCasePayloadBuilder = createCase(caseId, defendantBuilder, offenceId1, offenceId2, offenceId3, postingDate);
        try (final PleadOnlineHelper pleadOnlineHelper = new PleadOnlineHelper(caseId, defendantBuilder.getId())) {
            final Map<String, String> values = new HashMap<>();
            values.put("offenceId1", offenceId1.toString());
            values.put("offenceId2", offenceId2.toString());
            values.put("offenceId3", offenceId3.toString());
            values.put("plea1", pleaType1.name());
            values.put("plea2", pleaType2.name());
            values.put("plea3", pleaType3.name());
            values.put("mitigation", "I was drunk at the time");
            values.put("notGuiltyBecause", "I was forced to do it");
            //runs plea-online
            verifyOnlinePleaReceivedAndUpdatedCaseDetailsFlag(createCasePayloadBuilder.getId(), false);
            final JsonPath pleadOnlinePayload = JsonPath.from(new StrSubstitutor(values).replace(getPayload(TEMPLATE_PLEA_MULTI_OFFENCE_PAYLOAD)));
            pleadOnlineHelper.pleadOnline(pleadOnlinePayload.prettify());
            //verify plea
            pleadOnlineHelper.verifyInPublicTopic(createCasePayloadBuilder.getId(), offenceId1, pleaType1, null);
            pleadOnlineHelper.verifyPleaUpdated(createCasePayloadBuilder.getId(), pleaType1, PleaMethod.ONLINE, 0);
            pleadOnlineHelper.verifyPleaUpdated(createCasePayloadBuilder.getId(), pleaType2, PleaMethod.ONLINE, 1);
            pleadOnlineHelper.verifyPleaUpdated(createCasePayloadBuilder.getId(), pleaType3, PleaMethod.ONLINE, 2);
            final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(
                    createCasePayloadBuilder.getUrn(),
                    createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                    createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());
            caseSearchResultHelper.verifyPleaReceivedDate();
            //verify online-plea
            final Response response = getOnlinePlea(caseId.toString(), defendantBuilder.getId().toString(), USER_ID);
            if (response.getStatus() != OK.getStatusCode()) {
                fail("Polling interrupted, please fix the error before continue. Status code: " + response.getStatus());
            }
            verifyOnlinePleaReceivedAndUpdatedCaseDetailsFlag(createCasePayloadBuilder.getId(), true);
            final JSONObject defendantsPlea = new JSONObject(response.readEntity(String.class));
            assertThat(defendantsPlea.getJSONArray("onlinePleaDetails").getJSONObject(0).get("offenceId"), equalTo(offenceId1.toString()));
            assertThat(defendantsPlea.getJSONArray("onlinePleaDetails").getJSONObject(0).get("plea"), equalTo(pleaType1.name()));
            assertThat(defendantsPlea.getJSONArray("onlinePleaDetails").getJSONObject(0).get("mitigation"), equalTo("I was drunk at the time"));
            assertThat(defendantsPlea.getJSONArray("onlinePleaDetails").getJSONObject(1).get("offenceId"), equalTo(offenceId2.toString()));
            assertThat(defendantsPlea.getJSONArray("onlinePleaDetails").getJSONObject(1).get("plea"), equalTo(pleaType2.name()));
            assertThat(defendantsPlea.getJSONArray("onlinePleaDetails").getJSONObject(1).get("notGuiltyBecause"), equalTo("I was forced to do it"));
            assertThat(defendantsPlea.getJSONArray("onlinePleaDetails").getJSONObject(2).get("offenceId"), equalTo(offenceId3.toString()));
            assertThat(defendantsPlea.getJSONArray("onlinePleaDetails").getJSONObject(2).get("plea"), equalTo(pleaType3.name()));
            assertFalse(defendantsPlea.getJSONArray("onlinePleaDetails").getJSONObject(2).has("mitigation"));
            assertThat(defendantsPlea.getJSONObject("personalDetails").get("firstName"), equalTo("Testy"));
            assertThat(defendantsPlea.getJSONObject("personalDetails").get("lastName"), equalTo("LLOYD"));
            assertThat(defendantsPlea.getJSONObject("personalDetails").getJSONObject("address").get("address1"), equalTo("14 Tottenham Court Road"));
            assertThat(defendantsPlea.getJSONObject("personalDetails").getJSONObject("address").get("address2"), equalTo("London"));
            assertThat(defendantsPlea.getJSONObject("personalDetails").getJSONObject("address").get("address3"), equalTo("England"));
            assertThat(defendantsPlea.getJSONObject("personalDetails").getJSONObject("address").get("address4"), equalTo("UK"));
            assertThat(defendantsPlea.getJSONObject("personalDetails").getJSONObject("address").get("address5"), equalTo("Greater London"));
            assertThat(defendantsPlea.getJSONObject("personalDetails").getJSONObject("address").get("postcode"), equalTo("W1T 1JY"));
            assertTrue(defendantsPlea.getJSONObject("pleaDetails").getBoolean("outstandingFines"));
            assertThat(defendantsPlea.getJSONObject("pleaDetails").getJSONObject("disabilityNeeds").getString("disabilityNeeds"), equalTo("Hearing aid"));
            final JsonObject updatedDefendantDetails = getUpdatedDefendantDetails();
            final boolean resultsContainUpdatedFirstNameValue = updatedDefendantDetails
                    .getJsonArray("defendantDetailsUpdates")
                    .getValuesAs(JsonObject.class)
                    .stream()
                    .anyMatch(e -> e.getString("firstName").equalsIgnoreCase("Testy"));
            assertThat(resultsContainUpdatedFirstNameValue, is(true));
        }
    }

    @Test
    public void shouldPleadOnlineWithUnchangedPersonalDetails() {
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();
        final PleaType pleaType1 = GUILTY;
        final PleaType pleaType2 = NOT_GUILTY;
        final PleaType pleaType3 = GUILTY;
        final UUID caseId = randomUUID();
        final CreateCase.DefendantBuilder defendantBuilder = CreateCase.DefendantBuilder.withDefaults();
        final LocalDate postingDate = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);
        this.createCasePayloadBuilder = createCase(caseId, defendantBuilder, offenceId1, offenceId2, offenceId3, postingDate);
        try (final PleadOnlineHelper pleadOnlineHelper = new PleadOnlineHelper(caseId, defendantBuilder.getId())) {
            final Map<String, String> values = new HashMap<>();
            values.put("offenceId1", offenceId1.toString());
            values.put("offenceId2", offenceId2.toString());
            values.put("offenceId3", offenceId3.toString());
            values.put("plea1", pleaType1.name());
            values.put("plea2", pleaType2.name());
            values.put("plea3", pleaType3.name());
            values.put("mitigation", "I was drunk at the time");
            values.put("notGuiltyBecause", "I was forced to do it");
            //runs plea-online
            verifyOnlinePleaReceivedAndUpdatedCaseDetailsFlag(createCasePayloadBuilder.getId(), false);
            final JsonPath pleadOnlinePayload = JsonPath.from(new StrSubstitutor(values).replace(getPayload(TEMPLATE_PLEA_MULTI_OFFENCE_NO_CHANGE_PERSONAL_DETAILS_PAYLOAD)));
            pleadOnlineHelper.pleadOnline(pleadOnlinePayload.prettify());
            //verify plea
            pleadOnlineHelper.verifyInPublicTopic(createCasePayloadBuilder.getId(), offenceId1, pleaType1, null);
            pleadOnlineHelper.verifyPleaUpdated(createCasePayloadBuilder.getId(), pleaType1, PleaMethod.ONLINE, 0);
            pleadOnlineHelper.verifyPleaUpdated(createCasePayloadBuilder.getId(), pleaType2, PleaMethod.ONLINE, 1);
            pleadOnlineHelper.verifyPleaUpdated(createCasePayloadBuilder.getId(), pleaType3, PleaMethod.ONLINE, 2);
            final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(
                    createCasePayloadBuilder.getUrn(),
                    createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                    createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());
            caseSearchResultHelper.verifyPleaReceivedDate();
            //verify online-plea
            final Response response = getOnlinePlea(caseId.toString(), defendantBuilder.getId().toString(), USER_ID);
            if (response.getStatus() != OK.getStatusCode()) {
                fail("Polling interrupted, please fix the error before continue. Status code: " + response.getStatus());
            }
            verifyOnlinePleaReceivedAndUpdatedCaseDetailsFlag(createCasePayloadBuilder.getId(), true);
            final JSONObject defendantsPlea = new JSONObject(response.readEntity(String.class));
            assertThat(defendantsPlea.getJSONArray("onlinePleaDetails").getJSONObject(0).get("offenceId"), equalTo(offenceId1.toString()));
            assertThat(defendantsPlea.getJSONArray("onlinePleaDetails").getJSONObject(0).get("plea"), equalTo(pleaType1.name()));
            assertThat(defendantsPlea.getJSONArray("onlinePleaDetails").getJSONObject(0).get("mitigation"), equalTo("I was drunk at the time"));
            assertThat(defendantsPlea.getJSONArray("onlinePleaDetails").getJSONObject(1).get("offenceId"), equalTo(offenceId2.toString()));
            assertThat(defendantsPlea.getJSONArray("onlinePleaDetails").getJSONObject(1).get("plea"), equalTo(pleaType2.name()));
            assertThat(defendantsPlea.getJSONArray("onlinePleaDetails").getJSONObject(1).get("notGuiltyBecause"), equalTo("I was forced to do it"));
            assertThat(defendantsPlea.getJSONArray("onlinePleaDetails").getJSONObject(2).get("offenceId"), equalTo(offenceId3.toString()));
            assertThat(defendantsPlea.getJSONArray("onlinePleaDetails").getJSONObject(2).get("plea"), equalTo(pleaType3.name()));
            assertFalse(defendantsPlea.getJSONArray("onlinePleaDetails").getJSONObject(2).has("mitigation"));
            assertThat(defendantsPlea.getJSONObject("personalDetails").get("firstName"), equalTo("David"));
            assertThat(defendantsPlea.getJSONObject("personalDetails").get("lastName"), equalTo("LLOYD"));
            assertThat(defendantsPlea.getJSONObject("personalDetails").getJSONObject("address").get("address1"), equalTo("14 Tottenham Court Road"));
            assertThat(defendantsPlea.getJSONObject("personalDetails").getJSONObject("address").get("address2"), equalTo("London"));
            assertThat(defendantsPlea.getJSONObject("personalDetails").getJSONObject("address").get("address3"), equalTo("England"));
            assertThat(defendantsPlea.getJSONObject("personalDetails").getJSONObject("address").get("address4"), equalTo("UK"));
            assertThat(defendantsPlea.getJSONObject("personalDetails").getJSONObject("address").get("address5"), equalTo("Greater London"));
            assertThat(defendantsPlea.getJSONObject("personalDetails").getJSONObject("address").get("postcode"), equalTo("W1T 1JY"));
            assertTrue(defendantsPlea.getJSONObject("pleaDetails").getBoolean("outstandingFines"));
            final JsonObject updatedDefendantDetails = getUpdatedDefendantDetails();
            final boolean resultsContainUpdatedFirstNameValue = updatedDefendantDetails
                    .getJsonArray("defendantDetailsUpdates")
                    .getValuesAs(JsonObject.class)
                    .stream()
                    .anyMatch(e -> e.getString("firstName").equalsIgnoreCase("Testy"));
            assertThat(resultsContainUpdatedFirstNameValue, is(false));
        }
    }
    @Test
    public void shouldUpdateDefendantsCurrentFirstName() {
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();
        final UUID caseId = randomUUID();
        final CreateCase.DefendantBuilder defendantBuilder = CreateCase.DefendantBuilder.withDefaults();
        final LocalDate postingDate = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);
        this.createCasePayloadBuilder = createCase(caseId, defendantBuilder, offenceId1, offenceId2, offenceId3, postingDate);
        try (final PleadOnlineHelper pleadOnlineHelper = new PleadOnlineHelper(caseId, defendantBuilder.getId())) {
            final Map<String, String> values = new HashMap<>();
            values.put("offenceId1", offenceId1.toString());
            values.put("offenceId2", offenceId2.toString());
            values.put("offenceId3", offenceId3.toString());
            values.put("plea1", GUILTY.name());
            values.put("plea2", NOT_GUILTY.name());
            values.put("plea3", GUILTY.name());
            values.put("mitigation", "I was drunk at the time");
            values.put("notGuiltyBecause", "I was forced to do it");
            values.put("firstName", "Anewname");
            values.put("email", "anotheremail@test.com");
            verifyOnlinePleaReceivedAndUpdatedCaseDetailsFlag(createCasePayloadBuilder.getId(), false);
            final JsonPath pleadOnlinePayload = JsonPath.from(new StrSubstitutor(values).replace(getPayload(TEMPLATE_PLEA_MULTI_OFFENCE_CUSTOM_V2)));
            pleadOnlineHelper.pleadOnline(pleadOnlinePayload.prettify());
            final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(
                    createCasePayloadBuilder.getUrn(),
                    createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                    createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());
            caseSearchResultHelper.verifyPleaReceivedDate();
            caseSearchResultHelper.verify(createCasePayloadBuilder.getUrn(), allOf(
                    withJsonPath("$.results[*]", hasItem(isJson(allOf(
                            withJsonPath("defendant.firstName", equalTo("Anewname")),
                            withJsonPath("defendant.lastName", equalTo(defendantBuilder.getLastName()))
                    ))))));
        }
    }

    @Test
    public void shouldPleadOnlineWithChangedDriverDetails() {
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();
        final PleaType pleaType1 = GUILTY;
        final PleaType pleaType2 = NOT_GUILTY;
        final PleaType pleaType3 = GUILTY;
        final UUID caseId = randomUUID();
        final CreateCase.DefendantBuilder defendantBuilder = CreateCase.DefendantBuilder.withDefaults();
        final LocalDate postingDate = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);

        this.createCasePayloadBuilder = createCase(caseId, defendantBuilder, offenceId1, offenceId2, offenceId3, postingDate);

        try (final PleadOnlineHelper pleadOnlineHelper = new PleadOnlineHelper(caseId, defendantBuilder.getId())) {
            final Map<String, String> values = new HashMap<>();
            values.put("offenceId1", offenceId1.toString());
            values.put("offenceId2", offenceId2.toString());
            values.put("offenceId3", offenceId3.toString());
            values.put("plea1", pleaType1.name());
            values.put("plea2", pleaType2.name());
            values.put("plea3", pleaType3.name());
            values.put("mitigation", "I was drunk at the time");
            values.put("notGuiltyBecause", "I was forced to do it");
            values.put("driverNumber", "MORGA753116SM9IV");
            values.put("driverLicenceDetails", "update_driver_licence_details");

            //runs plea-online
            verifyOnlinePleaReceivedAndUpdatedCaseDetailsFlag(createCasePayloadBuilder.getId(), false);

            final JsonPath pleadOnlinePayload = JsonPath.from(new StrSubstitutor(values).replace(getPayload(TEMPLATE_PLEA_GUILTY_PAYLOAD_WITH_DRIVER_NUMBER)));
            pleadOnlineHelper.pleadOnline(pleadOnlinePayload.prettify());

            //verify plea
            pleadOnlineHelper.verifyInPublicTopic(createCasePayloadBuilder.getId(), offenceId1, pleaType1, null);

            pleadOnlineHelper.verifyPleaUpdated(createCasePayloadBuilder.getId(), pleaType1, PleaMethod.ONLINE, 0);
            pleadOnlineHelper.verifyPleaUpdated(createCasePayloadBuilder.getId(), pleaType2, PleaMethod.ONLINE, 1);
            pleadOnlineHelper.verifyPleaUpdated(createCasePayloadBuilder.getId(), pleaType3, PleaMethod.ONLINE, 2);

            final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(
                    createCasePayloadBuilder.getUrn(),
                    createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                    createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());
            caseSearchResultHelper.verifyPleaReceivedDate();

            //verify online-plea
            final Response response = getOnlinePlea(caseId.toString(), defendantBuilder.getId().toString(), USER_ID);
            if (response.getStatus() != OK.getStatusCode()) {
                fail("Polling interrupted, please fix the error before continue. Status code: " + response.getStatus());
            }

            verifyOnlinePleaReceivedAndUpdatedCaseDetailsFlag(createCasePayloadBuilder.getId(), true);

            final JSONObject defendantsPlea = new JSONObject(response.readEntity(String.class));
            assertThat(defendantsPlea.getJSONObject("personalDetails").get("firstName"), equalTo("David"));
            assertThat(defendantsPlea.getJSONObject("personalDetails").get("lastName"), equalTo("LLOYD"));
            assertThat(defendantsPlea.getJSONObject("personalDetails").getJSONObject("address").get("address1"), equalTo("14 Tottenham Court Road"));
            assertThat(defendantsPlea.getJSONObject("personalDetails").getJSONObject("address").get("address2"), equalTo("London"));
            assertThat(defendantsPlea.getJSONObject("personalDetails").getJSONObject("address").get("address3"), equalTo("England"));
            assertThat(defendantsPlea.getJSONObject("personalDetails").getJSONObject("address").get("address4"), equalTo("UK"));
            assertThat(defendantsPlea.getJSONObject("personalDetails").getJSONObject("address").get("address5"), equalTo("Greater London"));
            assertThat(defendantsPlea.getJSONObject("personalDetails").getJSONObject("address").get("postcode"), equalTo("W1T 1JY"));
            assertTrue(defendantsPlea.getJSONObject("pleaDetails").getBoolean("outstandingFines"));
            assertThat(defendantsPlea.getJSONObject("personalDetails").get("driverNumber"), equalTo("MORGA753116SM9IV"));
            assertThat(defendantsPlea.getJSONObject("personalDetails").get("driverLicenceDetails"), equalTo("update_driver_licence_details"));
        }
    }



    @Test
    public void shouldPleadNotGuiltyOnlineThenFailWithSecondPleadAttemptAsNotAllowedTwoPleasAgainstSameOffence() {
        try (final PleadOnlineHelper pleadOnlineHelper = new PleadOnlineHelper(createCasePayloadBuilder.getId())) {
            final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(
                    createCasePayloadBuilder.getUrn(),
                    createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                    createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());
            //1) First plea should be successful
            pleadOnlineAndConfirmSuccess(PleaType.NOT_GUILTY, pleadOnlineHelper, caseSearchResultHelper);
            //2) Second plea should fail as cannot plea twice
            final JSONObject pleaPayload = getOnlinePleaPayload(PleaType.NOT_GUILTY);
            pleadOnlineHelper.pleadOnline(pleaPayload.toString(), Response.Status.BAD_REQUEST);
        }
    }
    @Test
    public void shouldPleaOnlineShouldRejectIfCaseIsInCompletedStatus() {
        DecisionHelper.saveDefaultDecision(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceIds());
        final PleadOnlineHelper pleadOnlineHelper = new PleadOnlineHelper(createCasePayloadBuilder.getId());
        final JSONObject pleaPayload = getOnlinePleaPayload(PleaType.NOT_GUILTY);
        pleadOnlineHelper.pleadOnline(pleaPayload.toString(), Response.Status.BAD_REQUEST);
    }
    @Test
    public void shouldRejectOnlinePleaIfCaseIsReferredForCourtHearing() {
        final UUID sjpSessionId = randomUUID();
        final UUID referralReasonId = randomUUID();
        final Integer estimatedHearingDuration = nextInt(1, 999);
        final String listingNotes = randomAlphanumeric(100);
        stubStartSjpSessionCommand();
        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubReferralReasonsQuery(REFERRAL_REASON_ID, HEARING_CODE, REFERRAL_REASON);
        stubReferralReason(referralReasonId.toString(), "stub-data/referencedata.referral-reason.json");
        stubHearingTypesQuery(HEARING_TYPE_ID.toString(), HEARING_CODE, HEARING_DESCRIPTION);
        startSession(sjpSessionId, legalAdviser.getUserId(), DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);
        requestCaseAssignment(sjpSessionId, legalAdviser.getUserId());
        final DefendantCourtOptions defendantCourtOptions = new DefendantCourtOptions(new DefendantCourtInterpreter("French", true), false, NO_DISABILITY_NEEDS);
        final ReferForCourtHearing referForCourtHearing = new ReferForCourtHearing(null, singletonList(new OffenceDecisionInformation(offenceId, VerdictType.NO_VERDICT)), referralReasonId, listingNotes, estimatedHearingDuration, defendantCourtOptions);
        final DecisionCommand decision = new DecisionCommand(sjpSessionId, createCasePayloadBuilder.getId(), null, legalAdviser, singletonList(referForCourtHearing), null);
        eventListener
                .subscribe(CaseReferredForCourtHearing.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decision))
                .popEvent(CaseReferredForCourtHearing.EVENT_NAME);
        CasePoller.pollUntilCaseHasStatus(createCasePayloadBuilder.getId(), REFERRED_FOR_COURT_HEARING);
        final PleadOnlineHelper pleadOnlineHelper = new PleadOnlineHelper(createCasePayloadBuilder.getId());
        final JSONObject pleaPayload = getOnlinePleaPayload(PleaType.NOT_GUILTY);
        pleadOnlineHelper.pleadOnline(pleaPayload.toString(), Response.Status.BAD_REQUEST);
    }
    @Test
    public void shouldPleadGuiltyOnline() {
        pleadGuiltyOnlineWithUserAndExpectedFinances(DEFAULT_STUBBED_USER_ID, true);
    }
    @Test
    public void shouldShowFinancesForProsecutors() {
        verifyGroupsCanSeeDefendantFinances(true, asList(LEGAL_ADVISERS_GROUP, COURT_ADMINISTRATORS_GROUP));
    }
    @Test
    public void shouldHideFinancesForProsecutors() {
        verifyGroupsCanSeeDefendantFinances(false, singleton(SJP_PROSECUTORS_GROUP));
    }
    @Test
    public void shouldPleaNotGuiltyWithoutFinancialMeans() {
        final PleaType notGuilty = PleaType.NOT_GUILTY;
        final JSONObject pleaPayload = getOnlinePleaPayload(notGuilty);
        pleaPayload.remove("financialMeans");
        assertThat(pleaPayload.has("financialMeans"), is(false));
        assertThat(pleaPayload.has("outgoings"), is(true));
        final JsonPath response = pleadOnline(pleaPayload);
        verifyResponseCase(response, TEMPLATE_PLEA_NOT_GUILTY_WITHOUT_FINANCIAL_MEANS_CASE_RESPONSE);
        verifyResponseOnlinePlea(TEMPLATE_PLEA_NOT_GUILTY_WITHOUT_FINANCIAL_MEANS_RESPONSE, notGuilty);
    }
    @Test
    public void shouldPleaNotGuiltyWithoutOutgoings() {
        final PleaType notGuilty = PleaType.NOT_GUILTY;
        final JSONObject pleaPayload = getOnlinePleaPayload(notGuilty);
        pleaPayload.remove("outgoings");
        assertThat(pleaPayload.has("financialMeans"), is(true));
        assertThat(pleaPayload.has("outgoings"), is(false));
        final JsonPath response = pleadOnline(pleaPayload);
        verifyResponseCase(response, TEMPLATE_PLEA_NOT_GUILTY_WITHOUT_FINANCIAL_MEANS_CASE_RESPONSE);
        verifyResponseOnlinePlea(TEMPLATE_PLEA_NOT_GUILTY_WITHOUT_OUTGOINGS_RESPONSE, notGuilty);
    }
    @Test
    public void shouldPleaNotGuiltyAndUpdateDefendantDetails() {
        final PleaType notGuilty = NOT_GUILTY;
        final JSONObject pleaPayload = getOnlinePleaPayload(notGuilty);
        pleaPayload.remove("outgoings");
        pleaPayload.getJSONObject("personalDetails").put("firstName", "Changy");
        pleaPayload.getJSONObject("personalDetails").put("nationalInsuranceNumber", "SR569876FD");
        assertThat(pleaPayload.has("financialMeans"), is(true));
        assertThat(pleaPayload.has("outgoings"), is(false));
        final JsonPath response = pleadOnline(pleaPayload);
        verifyResponseCase(response, TEMPLATE_PLEA_NOT_GUILTY_WITH_CHANGED_DETAILS_CASE_RESPONSE);
        verifyResponseOnlinePlea(TEMPLATE_PLEA_NOT_GUILTY_WITH_CHANGED_DETAILS_RESPONSE, notGuilty);
    }
    @Test
    public void shouldPleaNotGuiltyWithoutFinancialMeansAndOutgoings() {
        final PleaType notGuilty = PleaType.NOT_GUILTY;
        final JSONObject pleaPayload = getOnlinePleaPayload(notGuilty);
        pleaPayload.remove("financialMeans");
        pleaPayload.remove("outgoings");
        assertThat(pleaPayload.has("financialMeans"), is(false));
        assertThat(pleaPayload.has("outgoings"), is(false));
        final JsonPath response = pleadOnline(pleaPayload);
        verifyResponseCase(response, TEMPLATE_PLEA_NOT_GUILTY_WITHOUT_FINANCIAL_MEANS_CASE_RESPONSE);
        verifyResponseOnlinePlea(TEMPLATE_PLEA_NOT_GUILTY_WITHOUT_FINANCIAL_MEANS_AND_OUTGOINGS_RESPONSE, notGuilty);
    }
    private void verifyResponseOnlinePlea(final String nameFile, final PleaType pleaType) {
        final UUID caseId = createCasePayloadBuilder.getId();
        final CreateCase.DefendantBuilder defendantBuilder = createCasePayloadBuilder.getDefendantBuilder();
        final JsonPath response = JsonPath.from(
                getOnlinePlea(caseId.toString(), defendantBuilder.getId().toString(), isJson(allOf(
                        withJsonPath("$.defendantId"),
                        withJsonPath("$.onlinePleaDetails[0].plea", equalTo(pleaType.name())))
                ), USER_ID));
        final String submittedOn = response.getString("submittedOn");
        final String defendantId = response.getString("defendantId");
        final String onlinePleaDetailId = response.getString("onlinePleaDetails[0].id");
        final String offenceId = response.getString("onlinePleaDetails[0].offenceId");
        final Map<String, String> params = new HashMap<>();
        params.put("submittedOn", submittedOn);
        params.put("defendantId", defendantId);
        params.put("offenceId", offenceId);
        params.put("onlinePleaDetailId", onlinePleaDetailId);
        final JsonPath expectedResponse = fillTemplate(nameFile, params);
        assertEquals(expectedResponse.prettify(), response.prettify());
    }
    @Test
    public void shouldPleaNotGuiltyWithEmptyFinancialMeans() {
        final PleaType notGuilty = PleaType.NOT_GUILTY;
        final JSONObject pleaPayload = getOnlinePleaPayload(notGuilty);
        pleaPayload.put("financialMeans", createObjectBuilder().build());
        assertThat(pleaPayload.getJSONObject("financialMeans").keySet(), is(empty()));
        final JsonPath response = pleadOnline(pleaPayload);
        verifyResponseCase(response, TEMPLATE_PLEA_NOT_GUILTY_WITHOUT_FINANCIAL_MEANS_CASE_RESPONSE);
        verifyResponseOnlinePlea(TEMPLATE_PLEA_NOT_GUILTY_WITHOUT_FINANCIAL_MEANS_RESPONSE, notGuilty);
    }
    @Test
    public void shouldPleaGuiltyWithFinancialMeans() {
        final PleaType guilty = GUILTY;
        final JSONObject pleaPayload = getOnlinePleaPayload(guilty);
        assertThat(pleaPayload.has("financialMeans"), is(true));
        final JsonPath response = pleadOnline(pleaPayload);
        verifyResponseCase(response, TEMPLATE_PLEA_GUILTY_WITH_FINANCIAL_MEANS_CASE_RESPONSE);
        verifyResponseOnlinePlea(TEMPLATE_PLEA_GUILTY_WITH_FINANCIAL_MEANS_RESPONSE, guilty);
    }
    private void verifyResponseCase(final JsonPath response, final String templatePath) {
        final String dateTimeCreated = response.getString("dateTimeCreated");
        final String defendantId = response.getString("defendant.id");
        final String offenceId = response.getString("defendant.offences[0].id");
        final String pleaDate = response.getString("defendant.offences[0].pleaDate");
        final String caseStatus = response.getString("status");
        final Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put("dateTimeCreated", dateTimeCreated);
        expectedParams.put("offenceId", offenceId);
        expectedParams.put("defendantId", defendantId);
        expectedParams.put("pleaDate", pleaDate);
        expectedParams.put("caseStatus", caseStatus);
        expectedParams.put("speakWelsh", "false");
        final JsonPath expectedResponse = fillTemplate(templatePath, expectedParams);
        assertEquals(expectedResponse.prettify(), response.prettify());
    }
    private JsonPath fillTemplate(final String nameFile, final Map<String, String> values) {
        values.putIfAbsent("caseId", createCasePayloadBuilder.getId().toString());
        values.putIfAbsent("urn", createCasePayloadBuilder.getUrn());
        values.putIfAbsent("enterpriseId", createCasePayloadBuilder.getEnterpriseId());
        values.putIfAbsent("postConviction", "false");
        return JsonPath.from(new StrSubstitutor(values).replace(getPayload(nameFile)));
    }
    private JsonPath pleadOnline(final JSONObject pleaPayload) {
        final PleaType pleaType = PleaType.valueOf(pleaPayload.getJSONArray("offences").getJSONObject(0).getString("plea"));
        try (final PleadOnlineHelper pleadOnlineHelper = new PleadOnlineHelper(createCasePayloadBuilder.getId())) {
            pleadOnlineHelper.pleadOnline(pleaPayload.toString());
            return pleadOnlineHelper.verifyPleaUpdated(createCasePayloadBuilder.getId(), pleaType, PleaMethod.ONLINE);
        }
    }
    private void verifyGroupsCanSeeDefendantFinances(final boolean expectToHaveFinances, final Collection<String> financeProsecutors) {
        final List<UUID> mockedUserId = financeProsecutors.stream()
                .map(userGroup -> {
                    UUID userId = randomUUID();
                    UsersGroupsStub.stubGroupForUser(userId, userGroup);
                    return userId;
                }).collect(toList());
        pleadGuiltyOnlineWithUserAndExpectedFinances(mockedUserId, expectToHaveFinances);
    }
    @Test
    public void shouldSchemaValidationFailWhenEmailBlank() {
        shouldSchemaValidationFailWhenEmailInvalid("   ");
    }
    @Test
    public void shouldSchemaValidationFailWhenEmailInvalid() {
        shouldSchemaValidationFailWhenEmailInvalid("@b.co");
    }
    private void shouldSchemaValidationFailWhenEmailInvalid(final String email) {
        DecisionHelper.saveDefaultDecision(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceIds());
        final PleadOnlineHelper pleadOnlineHelper = new PleadOnlineHelper(createCasePayloadBuilder.getId());
        final JsonObject pleaPayload = toJsonObject(getOnlinePleaPayload(PleaType.NOT_GUILTY).toString());
        final JsonObject personalDetails = pleaPayload.getJsonObject("personalDetails");
        final JsonObject contactDetails = personalDetails.getJsonObject("contactDetails");
        final JsonObjectBuilder contactDetailsBuilder = createObjectBuilderWithFilter(contactDetails, field -> !field.equals("email"));
        final JsonObject contactDetailsNew = contactDetailsBuilder.add("email", email).build();
        final JsonObjectBuilder personDetailsBuilder = createObjectBuilderWithFilter(personalDetails, field -> !field.equals("contactDetails"));
        final JsonObject personDetailsNew = personDetailsBuilder.add("contactDetails", contactDetailsNew).build();
        final JsonObjectBuilder pleaPayloadBuilder = createObjectBuilderWithFilter(pleaPayload, field -> !field.equals("personalDetails"));
        final JsonObject payloadNew = pleaPayloadBuilder.add("personalDetails", personDetailsNew).build();
        final String response = pleadOnlineHelper.pleadOnline(payloadNew.toString(), Response.Status.BAD_REQUEST);
        JsonObject responseJson = toJsonObject(response);
        JsonValue validationErrors = responseJson.get("validationErrors");
        String validationTrace = validationErrors.toString();
        assertThat(validationTrace, containsString(String.format("#/personalDetails/contactDetails/email: string [%s] does not match pattern ", email)));
    }
    @Test
    public void shouldPleadGuiltyRequestHearingOnline() {
        try (final PleadOnlineHelper pleadOnlineHelper = new PleadOnlineHelper(createCasePayloadBuilder.getId())) {
            final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(
                    createCasePayloadBuilder.getUrn(),
                    createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                    createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());
            pleadOnlineAndConfirmSuccess(PleaType.GUILTY_REQUEST_HEARING, pleadOnlineHelper, caseSearchResultHelper);
        }
    }
    private void pleadGuiltyOnlineWithUserAndExpectedFinances(final Collection<UUID> userIds, final boolean expectToHaveFinances) {
        try (final PleadOnlineHelper pleadOnlineHelper = new PleadOnlineHelper(createCasePayloadBuilder.getId())) {
            final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(
                    createCasePayloadBuilder.getUrn(),
                    createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                    createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());
            pleadOnlineAndConfirmSuccess(GUILTY, pleadOnlineHelper, caseSearchResultHelper, userIds, expectToHaveFinances);
        }
    }
    private JSONObject getOnlinePleaPayload(final PleaType pleaType) {
        String templateRequest = null;
        if (pleaType.equals(PleaType.NOT_GUILTY)) {
            templateRequest = getPayload(TEMPLATE_PLEA_NOT_GUILTY_PAYLOAD);
        } else if (pleaType.equals(GUILTY)) {
            templateRequest = getPayload(TEMPLATE_PLEA_GUILTY_PAYLOAD);
        } else if (pleaType.equals(PleaType.GUILTY_REQUEST_HEARING)) {
            templateRequest = getPayload(TEMPLATE_PLEA_GUILTY_REQUEST_HEARING_PAYLOAD);
        }
        final JSONObject jsonObject = new JSONObject(templateRequest);
        //set offence.id to match setup data
        jsonObject.getJSONArray("offences").getJSONObject(0).put("id", createCasePayloadBuilder.getOffenceId().toString());
        return jsonObject;
    }
    private Matcher<Object> getEmployerUpdatedPayloadMatcher(final JSONObject employer) {
        return isJson(getEmployerUpdatedPayloadContentMatcher(employer));
    }
    private Matcher<JsonEnvelope> getEmployerUpdatedPublicEventMatcher(final JSONObject employer) {
        final Matcher<ReadContext> payloadContentMatcher = getEmployerUpdatedPayloadContentMatcher(employer);
        return jsonEnvelope()
                .withMetadataOf(metadata().withName("public.sjp.employer-updated"))
                .withPayloadOf(payloadIsJson(payloadContentMatcher));
    }
    private Matcher<ReadContext> getEmployerUpdatedPayloadContentMatcher(final JSONObject onlinePleaPayload) {
        final JSONObject employer = onlinePleaPayload.getJSONObject("employer");
        final JSONObject address = employer.getJSONObject("address");
        return allOf(
                withJsonPath("$.name", equalTo(employer.getString("name"))),
                withJsonPath("$.employeeReference", equalTo(employer.getString("employeeReference"))),
                withJsonPath("$.phone", equalTo(employer.getString("phone"))),
                withJsonPath("$.address.address1", equalTo(address.getString("address1"))),
                withJsonPath("$.address.address2", equalTo(address.getString("address2"))),
                withJsonPath("$.address.address3", equalTo(address.getString("address3"))),
                withJsonPath("$.address.address4", equalTo(address.getString("address4"))),
                withJsonPath("$.address.address5", equalTo(address.getString("address5"))),
                withJsonPath("$.address.postcode", equalTo(address.getString("postcode")))
        );
    }
    private Matcher<Object> getFinancialMeansUpdatedPayloadContentMatcher(final JSONObject onlinePleaPayload, final String defendantId) {
        final JSONObject financialMeans = onlinePleaPayload.getJSONObject("financialMeans");
        return isJson(allOf(
                withJsonPath("$.defendantId", equalTo(defendantId)),
                withJsonPath("$.income.frequency", equalTo(financialMeans.getJSONObject("income").getString("frequency"))),
                withJsonPath("$.income.amount", equalTo(financialMeans.getJSONObject("income").getDouble("amount"))),
                withJsonPath("$.benefits.claimed", equalTo(financialMeans.getJSONObject("benefits").getBoolean("claimed"))),
                withJsonPath("$.benefits.type", equalTo(financialMeans.getJSONObject("benefits").getString("type")))
        ));
    }
    private Matcher getSavedOnlinePleaPayloadContentMatcher(final PleaType pleaType, final JSONObject onlinePleaPayload, final String caseId, final String defendantId, final boolean expectToHaveFinances) {
        final List<Matcher> fieldMatchers = getCommonFieldMatchers(onlinePleaPayload, caseId, defendantId, expectToHaveFinances);
        final List<Matcher> extraMatchers;
        if (PleaType.NOT_GUILTY.equals(pleaType)) {
            extraMatchers = getNotGuiltyMatchers(onlinePleaPayload);
        } else if (GUILTY.equals(pleaType)) {
            extraMatchers = getGuiltyMatchers(onlinePleaPayload, expectToHaveFinances);
        } else {
            extraMatchers = getGuiltyRequestHearingMatchers(onlinePleaPayload);
        }
        fieldMatchers.addAll(extraMatchers);
        return isJson(allOf(
                fieldMatchers.<Matcher<ReadContext>>toArray(new Matcher[fieldMatchers.size()])
        ));
    }
    private List<Matcher> getCommonFieldMatchers(final JSONObject onlinePleaPayload, final String caseId, final String defendantId, final boolean expectToHaveFinances) {
        final JSONObject person = onlinePleaPayload.getJSONObject("personalDetails");
        final JSONObject personAddress = person.getJSONObject("address");
        final JSONObject personContactDetails = person.getJSONObject("contactDetails");
        final JSONObject financialMeans = onlinePleaPayload.getJSONObject("financialMeans");
        final JSONObject employer = onlinePleaPayload.getJSONObject("employer");
        final JSONObject employerAddress = employer.getJSONObject("address");
        final JSONObject accommodationOutgoing = onlinePleaPayload.getJSONArray("outgoings").getJSONObject(0);
        final JSONObject councilTaxOutgoing = onlinePleaPayload.getJSONArray("outgoings").getJSONObject(1);
        final JSONObject householdBillsOutgoing = onlinePleaPayload.getJSONArray("outgoings").getJSONObject(2);
        final JSONObject travelExpensesOutgoing = onlinePleaPayload.getJSONArray("outgoings").getJSONObject(3);
        final JSONObject childMaintenanceOutgoing = onlinePleaPayload.getJSONArray("outgoings").getJSONObject(4);
        final JSONObject otherOutgoing = onlinePleaPayload.getJSONArray("outgoings").getJSONObject(5);
        final List<Matcher> matchers = new ArrayList<>(asList(
                withJsonPath("$.caseId", equalTo(caseId)),
                withJsonPath("$.defendantId", equalTo(defendantId)),
                //personal details
                withJsonPath("$.personalDetails.firstName", equalTo(person.getString("firstName"))),
                withJsonPath("$.personalDetails.lastName", equalTo(person.getString("lastName"))),
                withJsonPath("$.personalDetails.homeTelephone", equalTo(personContactDetails.getString("home"))),
                withJsonPath("$.personalDetails.mobile", equalTo(personContactDetails.getString("mobile"))),
                withJsonPath("$.personalDetails.email", equalTo(personContactDetails.getString("email"))),
                withJsonPath("$.personalDetails.dateOfBirth", equalTo(person.getString("dateOfBirth"))),
                withJsonPath("$.personalDetails.nationalInsuranceNumber", equalTo(person.getString("nationalInsuranceNumber")))
        ));
        if (expectToHaveFinances) {
            matchers.addAll(asList(
                    //financial-means
                    withJsonPath("$.employment.incomePaymentFrequency", equalTo(financialMeans.getJSONObject("income").getString("frequency"))),
                    withJsonPath("$.employment.incomePaymentAmount", equalTo(financialMeans.getJSONObject("income").getDouble("amount"))),
                    withJsonPath("$.employment.benefitsClaimed", equalTo(financialMeans.getJSONObject("benefits").getBoolean("claimed"))),
                    withJsonPath("$.employment.benefitsType", equalTo(financialMeans.getJSONObject("benefits").getString("type"))),
                    withJsonPath("$.employment.benefitsDeductPenaltyPreference", equalTo(financialMeans.getJSONObject("benefits").getBoolean("deductPenaltyPreference"))),
                    //employer
                    withJsonPath("$.employer.name", equalTo(employer.getString("name"))),
                    withJsonPath("$.employer.employeeReference", equalTo(employer.getString("employeeReference"))),
                    withJsonPath("$.employer.phone", equalTo(employer.getString("phone"))),
                    withJsonPath("$.employer.address.address1", equalTo(employerAddress.getString("address1"))),
                    withJsonPath("$.employer.address.address2", equalTo(employerAddress.getString("address2"))),
                    withJsonPath("$.employer.address.address3", equalTo(employerAddress.getString("address3"))),
                    withJsonPath("$.employer.address.address4", equalTo(employerAddress.getString("address4"))),
                    withJsonPath("$.employer.address.address5", equalTo(employerAddress.getString("address5"))),
                    withJsonPath("$.employer.address.postcode", equalTo(employerAddress.getString("postcode"))),
                    //outgoings
                    withJsonPath("$.outgoings.accommodationAmount", equalTo(accommodationOutgoing.getDouble("amount"))),
                    withJsonPath("$.outgoings.councilTaxAmount", equalTo(councilTaxOutgoing.getDouble("amount"))),
                    withJsonPath("$.outgoings.householdBillsAmount", equalTo(householdBillsOutgoing.getDouble("amount"))),
                    withJsonPath("$.outgoings.travelExpensesAmount", equalTo(travelExpensesOutgoing.getDouble("amount"))),
                    withJsonPath("$.outgoings.childMaintenanceAmount", equalTo(childMaintenanceOutgoing.getDouble("amount"))),
                    withJsonPath("$.outgoings.otherDescription", equalTo(otherOutgoing.getString("description"))),
                    withJsonPath("$.outgoings.otherAmount", equalTo(otherOutgoing.getDouble("amount"))),
                    withJsonPath("$.outgoings.monthlyAmount", equalTo(1772.3))
            ));
        } else {
            matchers.addAll(asList(
                    withoutJsonPath("$.employment"),
                    withoutJsonPath("$.employer"),
                    withoutJsonPath("$.outgoings")
            ));
        }
        return matchers;
    }
    private List<Matcher> getNotGuiltyMatchers(final JSONObject onlinePleaPayload) {
        final JSONObject offence = onlinePleaPayload.getJSONArray("offences").getJSONObject(0);
        return asList(
                //plea-details
                withJsonPath("$.onlinePleaDetails[0].plea", equalTo(PleaType.NOT_GUILTY.name())),
                withJsonPath("$.pleaDetails.comeToCourt", equalTo(true)),
                withJsonPath("$.onlinePleaDetails[0].notGuiltyBecause", equalTo(offence.getString("notGuiltyBecause"))),
                withJsonPath("$.pleaDetails.witnessDispute", equalTo(onlinePleaPayload.getString("witnessDispute"))),
                withJsonPath("$.pleaDetails.witnessDetails", equalTo(onlinePleaPayload.getString("witnessDetails"))),
                withJsonPath("$.pleaDetails.speakWelsh", equalTo(onlinePleaPayload.getBoolean("speakWelsh"))),
                withJsonPath("$.pleaDetails.interpreterLanguage", equalTo(onlinePleaPayload.getString("interpreterLanguage"))),
                withJsonPath("$.pleaDetails.interpreterRequired", equalTo(true)),
                withJsonPath("$.pleaDetails.unavailability", equalTo(onlinePleaPayload.getString("unavailability"))),
                //employment status
                withJsonPath("$.employment.employmentStatus", equalTo("OTHER")),
                withJsonPath("$.employment.employmentStatusDetails", equalTo("my employment status"))
        );
    }
    private List<Matcher> getGuiltyMatchers(final JSONObject onlinePleaPayload, final boolean expectToHaveFinances) {
        final JSONObject offence = onlinePleaPayload.getJSONArray("offences").getJSONObject(0);
        final List<Matcher> mitigation = new ArrayList<>(asList(
                //plea-details
                withJsonPath("$.onlinePleaDetails[0].plea", equalTo(GUILTY.name())),
                withJsonPath("$.pleaDetails.comeToCourt", equalTo(false)),
                withJsonPath("$.onlinePleaDetails[0].mitigation", equalTo(offence.getString("mitigation"))),
                withoutJsonPath("$.employment.employmentStatusDetails")));
        if (expectToHaveFinances) {
            //employment status
            mitigation.add(withJsonPath("$.employment.employmentStatus", equalTo("EMPLOYED")));
        }
        return mitigation;
    }
    private List<Matcher> getGuiltyRequestHearingMatchers(final JSONObject onlinePleaPayload) {
        final JSONObject offence = onlinePleaPayload.getJSONArray("offences").getJSONObject(0);
        return asList(
                onlinePleaPayload.has("speakWelsh") ? withJsonPath("$.pleaDetails.speakWelsh", equalTo(onlinePleaPayload.getBoolean("speakWelsh"))) : withoutJsonPath("speakWelsh"),
                onlinePleaPayload.has("interpreterLanguage") ? withJsonPath("$.pleaDetails.interpreterLanguage", equalTo(onlinePleaPayload.getString("interpreterLanguage"))) : withoutJsonPath("interpreterLanguage"),
                withJsonPath("$.pleaDetails.interpreterRequired", equalTo(true)),
                withJsonPath("$.onlinePleaDetails[0].plea", equalTo(PleaType.GUILTY_REQUEST_HEARING.name())),
                withJsonPath("$.pleaDetails.comeToCourt", equalTo(true)),
                withJsonPath("$.onlinePleaDetails[0].mitigation", equalTo(offence.getString("mitigation"))),
                //employment status
                withJsonPath("$.employment.employmentStatus", equalTo("UNEMPLOYED")),
                withoutJsonPath("$.employment.employmentStatusDetails")
        );
    }
    private JsonObject toJsonObject(String response) {
        return createReader(new StringReader(response)).readObject();
    }
    private JsonObject getUpdatedDefendantDetails() {
        final Response response = HttpClientUtil.makeGetCall(
                "/defendant-details-updates?limit=" + Integer.MAX_VALUE,
                DEFENDANT_DETAIL_UPDATES_CONTENT_TYPE,
                Defaults.DEFAULT_USER_ID);
        assertThat(response.getStatus(), Matchers.equalTo(Response.Status.OK.getStatusCode()));
        return createReader(new StringReader(response.readEntity(String.class))).readObject();
    }
}