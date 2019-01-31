package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeThat;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.sjp.it.helper.PleadOnlineHelper.getOnlinePlea;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;
import static uk.gov.moj.sjp.it.stub.NotifyStub.stubNotifications;
import static uk.gov.moj.sjp.it.stub.NotifyStub.verifyNotification;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubCountryByPostcodeQuery;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.COURT_ADMINISTRATORS_GROUP;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.LEGAL_ADVISERS_GROUP;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.SJP_PROSECUTORS_GROUP;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.ContactDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseSearchResultHelper;
import uk.gov.moj.sjp.it.helper.EmployerHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.FinancialMeansHelper;
import uk.gov.moj.sjp.it.helper.PleadOnlineHelper;
import uk.gov.moj.sjp.it.helper.UpdatePleaHelper;
import uk.gov.moj.sjp.it.producer.CompleteCaseProducer;
import uk.gov.moj.sjp.it.producer.DecisionToReferCaseForCourtHearingSavedProducer;
import uk.gov.moj.sjp.it.stub.UsersGroupsStub;
import uk.gov.moj.sjp.it.verifier.PersonInfoVerifier;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.core.Response;

import com.jayway.jsonpath.ReadContext;
import com.jayway.restassured.path.json.JsonPath;
import org.apache.commons.lang.text.StrSubstitutor;
import org.hamcrest.Matcher;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PleadOnlineIT extends BaseIntegrationTest {

    private static final String TEMPLATE_PLEA_NOT_GUILTY_PAYLOAD = "raml/json/sjp.command.plead-online__not-guilty.json";
    private static final String TEMPLATE_PLEA_GUILTY_PAYLOAD = "raml/json/sjp.command.plead-online__guilty.json";
    private static final String TEMPLATE_PLEA_GUILTY_REQUEST_HEARING_PAYLOAD = "raml/json/sjp.command.plead-online__guilty_request_hearing.json";
    private static final String TEMPLATE_PLEA_GUILTY_WITH_FINANCIAL_MEANS_RESPONSE = "raml/json/sjp.command.plead-online__guilty_with_finances_response.json";
    private static final String TEMPLATE_PLEA_GUILTY_WITH_FINANCIAL_MEANS_CASE_RESPONSE = "raml/json/sjp.command.plead-online__guilty_with_finances_case_response.json";
    private static final String TEMPLATE_PLEA_NOT_GUILTY_WITHOUT_FINANCIAL_MEANS_RESPONSE = "raml/json/sjp.command.plead-online__not-guilty_without_finances_response.json";
    private static final String TEMPLATE_PLEA_NOT_GUILTY_WITHOUT_FINANCIAL_MEANS_AND_OUTGOINGS_RESPONSE = "raml/json/sjp.command.plead-online__not-guilty_without_finances_and_outgoings_response.json";
    private static final String TEMPLATE_PLEA_NOT_GUILTY_WITHOUT_OUTGOINGS_RESPONSE = "raml/json/sjp.command.plead-online__not-guilty_without_outgoings_response.json";
    private static final String TEMPLATE_PLEA_NOT_GUILTY_WITHOUT_FINANCIAL_MEANS_CASE_RESPONSE = "raml/json/sjp.command.plead-online__not-guilty_without_finances_case_response.json";
    private static final String ENGLISH_TEMPLATE_ID = "07d1f043-6052-4d18-adce-58678d0e7018";
    private static final Set<UUID> DEFAULT_STUBBED_USER_ID = singleton(USER_ID);
    private EmployerHelper employerHelper;
    private FinancialMeansHelper financialMeansHelper;
    private PersonInfoVerifier personInfoVerifier;
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    @Before
    public void setUp() throws UnsupportedEncodingException {
        this.createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        CreateCase.createCaseForPayloadBuilder(this.createCasePayloadBuilder);
        pollUntilCaseByIdIsOk(createCasePayloadBuilder.getId());

        employerHelper = new EmployerHelper();
        financialMeansHelper = new FinancialMeansHelper();
        personInfoVerifier = PersonInfoVerifier.personInfoVerifierForCasePayload(createCasePayloadBuilder);
        stubCountryByPostcodeQuery("W1T 1JY", "England");
        stubNotifications();
    }

    @After
    public void tearDown() throws Exception {
        employerHelper.close();
        financialMeansHelper.close();
    }

    private PersonalDetails generateExpectedPersonDetails(final JSONObject payload) {
        final JSONObject person = payload.getJSONObject("personalDetails");
        final String firstName = person.getString("firstName");
        final String lastName = person.getString("lastName");
        final String nationalInsuranceNumber = person.getString("nationalInsuranceNumber");
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

        return new PersonalDetails(title, firstName, lastName, LocalDate.parse(dateOfBirth), gender, nationalInsuranceNumber,
                new Address(address1, address2, address3, address4, address5, postcode),
                new ContactDetails(email, homeNumber, mobileNumber)
        );
    }

    private void pleadOnlineAndConfirmSuccess(final PleaType pleaType, final PleadOnlineHelper pleadOnlineHelper,
                                              final UpdatePleaHelper updatePleaHelper, final CaseSearchResultHelper caseSearchResultHelper) {
        pleadOnlineAndConfirmSuccess(pleaType, pleadOnlineHelper, updatePleaHelper, caseSearchResultHelper,
                DEFAULT_STUBBED_USER_ID, true);
    }

    private void pleadOnlineAndConfirmSuccess(final PleaType pleaType, final PleadOnlineHelper pleadOnlineHelper,
                                              final UpdatePleaHelper updatePleaHelper, final CaseSearchResultHelper caseSearchResultHelper,
                                              final Collection<UUID> userIds, final boolean expectToHaveFinances) {
        assumeThat(userIds, not(empty()));

        //checks person-info before plead-online
        personInfoVerifier.verifyPersonInfo();

        //runs plea-online
        pleadOnlineHelper.verifyOnlinePleaReceivedAndUpdatedCaseDetailsFlag(createCasePayloadBuilder.getId(), false);
        final JSONObject pleaPayload = getOnlinePleaPayload(pleaType);
        pleadOnlineHelper.pleadOnline(pleaPayload.toString());

        //verify plea
        updatePleaHelper.verifyInPublicTopic(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId(), pleaType, null);
        updatePleaHelper.verifyPleaUpdated(createCasePayloadBuilder.getId(), pleaType, PleaMethod.ONLINE);
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
        final Matcher<Object> expectedResult = getSavedOnlinePleaPayloadContentMatcher(pleaType, pleaPayload, createCasePayloadBuilder.getId().toString(), defendantId, expectToHaveFinances);
        userIds.forEach(userId -> getOnlinePlea(createCasePayloadBuilder.getId().toString(), expectedResult, userId));
        pleadOnlineHelper.verifyOnlinePleaReceivedAndUpdatedCaseDetailsFlag(createCasePayloadBuilder.getId(), true);

        verifyNotification("criminal@gmail.com", createCasePayloadBuilder.getUrn(), ENGLISH_TEMPLATE_ID);
    }

    @Test
    public void shouldPleadNotGuiltyOnlineThenFailWithSecondPleadAttemptAsNotAllowedTwoPleasAgainstSameOffence() {
        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper()) {
            final PleadOnlineHelper pleadOnlineHelper = new PleadOnlineHelper(createCasePayloadBuilder.getId());
            final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(createCasePayloadBuilder.getId(),
                    createCasePayloadBuilder.getUrn(),
                    createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                    createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());

            //1) First plea should be successful
            pleadOnlineAndConfirmSuccess(PleaType.NOT_GUILTY, pleadOnlineHelper, updatePleaHelper, caseSearchResultHelper);

            //2) Second plea should fail as cannot plea twice
            final JSONObject pleaPayload = getOnlinePleaPayload(PleaType.NOT_GUILTY);
            pleadOnlineHelper.pleadOnline(pleaPayload.toString(), Response.Status.BAD_REQUEST);
        }
    }


    @Test
    public void shouldPleaOnlineShouldRejectIfCaseIsInCompletedStatus() {
        final CompleteCaseProducer completeCaseProducer = new CompleteCaseProducer(createCasePayloadBuilder.getId());
        completeCaseProducer.completeCase();
        completeCaseProducer.assertCaseCompleted();

        final PleadOnlineHelper pleadOnlineHelper = new PleadOnlineHelper(createCasePayloadBuilder.getId());
        final JSONObject pleaPayload = getOnlinePleaPayload(PleaType.NOT_GUILTY);
        pleadOnlineHelper.pleadOnline(pleaPayload.toString(), Response.Status.BAD_REQUEST);
    }

    @Test
    public void shouldPleaOnlineShouldRejectIfCaseIsInReferredForCourtHearingStatus() {
        final UUID sjpSessionId = randomUUID();
        final ZonedDateTime resultedOn = now(UTC);
        final UUID referralReasonId = randomUUID();
        final UUID hearingTypeId = randomUUID();
        final Integer estimatedHearingDuration = nextInt(1, 999);
        final String listingNotes = randomAlphanumeric(100);

        final DecisionToReferCaseForCourtHearingSavedProducer decisionToReferCaseForCourtHearingSavedProducer = new DecisionToReferCaseForCourtHearingSavedProducer(createCasePayloadBuilder.getId(),
                sjpSessionId, referralReasonId, hearingTypeId, estimatedHearingDuration, listingNotes, resultedOn);

        new EventListener()
                .subscribe(CaseReferredForCourtHearing.class.getAnnotation(Event.class).value())
                .run(decisionToReferCaseForCourtHearingSavedProducer::saveDecisionToReferCaseForCourtHearing);

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

        final JsonPath response = JsonPath.from(
                getOnlinePlea(caseId.toString(), isJson(allOf(
                        withJsonPath("$.defendantId"),
                        withJsonPath("$.pleaDetails.plea", equalTo(pleaType.name())))
                ), USER_ID));

        final String submittedOn = response.getString("submittedOn");
        final String defendantId = response.getString("defendantId");

        final Map<String, String> params = new HashMap<>();
        params.put("submittedOn", submittedOn);
        params.put("defendantId", defendantId);

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
        final PleaType guilty = PleaType.GUILTY;
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

        final Map<String, String> params = new HashMap<>();
        params.put("dateTimeCreated", dateTimeCreated);
        params.put("offenceId", offenceId);
        params.put("defendantId", defendantId);
        params.put("pleaDate", pleaDate);
        params.put("caseStatus", caseStatus);

        final JsonPath expectedResponse = fillTemplate(templatePath, params);

        assertEquals(expectedResponse.prettify(), response.prettify());
    }

    private JsonPath fillTemplate(final String nameFile, final Map<String, String> values) {
        values.putIfAbsent("caseId", createCasePayloadBuilder.getId().toString());
        values.putIfAbsent("urn", createCasePayloadBuilder.getUrn());
        values.putIfAbsent("enterpriseId", createCasePayloadBuilder.getEnterpriseId());

        return JsonPath.from(new StrSubstitutor(values).replace(getPayload(nameFile)));
    }

    private JsonPath pleadOnline(final JSONObject pleaPayload) {
        final PleaType pleaType = PleaType.valueOf(pleaPayload.getJSONArray("offences").getJSONObject(0).getString("plea"));

        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper()) {
            final PleadOnlineHelper pleadOnlineHelper = new PleadOnlineHelper(createCasePayloadBuilder.getId());

            pleadOnlineHelper.pleadOnline(pleaPayload.toString());

            return updatePleaHelper.verifyPleaUpdated(createCasePayloadBuilder.getId(), pleaType, PleaMethod.ONLINE);
        }
    }

    private void verifyGroupsCanSeeDefendantFinances(final boolean expectToHaveFinances, final Collection<String> financeProsecutors) {
        final List<UUID> mockedUserId = financeProsecutors.stream()
                .map(userGroup -> {
                    UUID userId = UUID.randomUUID();
                    UsersGroupsStub.stubGroupForUser(userId, userGroup);

                    return userId;
                }).collect(toList());

        pleadGuiltyOnlineWithUserAndExpectedFinances(mockedUserId, expectToHaveFinances);
    }

    @Test
    public void shouldPleadGuiltyRequestHearingOnline() {
        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper()) {
            final PleadOnlineHelper pleadOnlineHelper = new PleadOnlineHelper(createCasePayloadBuilder.getId());
            final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(createCasePayloadBuilder.getId(),
                    createCasePayloadBuilder.getUrn(),
                    createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                    createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());
            pleadOnlineAndConfirmSuccess(PleaType.GUILTY_REQUEST_HEARING, pleadOnlineHelper, updatePleaHelper, caseSearchResultHelper);
        }
    }

    private void pleadGuiltyOnlineWithUserAndExpectedFinances(final Collection<UUID> userIds, final boolean expectToHaveFinances) {
        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper()) {
            final PleadOnlineHelper pleadOnlineHelper = new PleadOnlineHelper(createCasePayloadBuilder.getId());
            final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(createCasePayloadBuilder.getId(),
                    createCasePayloadBuilder.getUrn(),
                    createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                    createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());
            pleadOnlineAndConfirmSuccess(PleaType.GUILTY, pleadOnlineHelper, updatePleaHelper, caseSearchResultHelper, userIds, expectToHaveFinances);
        }
    }

    private JSONObject getOnlinePleaPayload(final PleaType pleaType) {
        String templateRequest = null;
        if (pleaType.equals(PleaType.NOT_GUILTY)) {
            templateRequest = getPayload(TEMPLATE_PLEA_NOT_GUILTY_PAYLOAD);
        } else if (pleaType.equals(PleaType.GUILTY)) {
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

    private Matcher<Object> getSavedOnlinePleaPayloadContentMatcher(final PleaType pleaType, final JSONObject onlinePleaPayload, final String caseId, final String defendantId, final boolean expectToHaveFinances) {
        final List<Matcher> fieldMatchers = getCommonFieldMatchers(onlinePleaPayload, caseId, defendantId, expectToHaveFinances);
        final List<Matcher> extraMatchers;
        if (PleaType.NOT_GUILTY.equals(pleaType)) {
            extraMatchers = getNotGuiltyMatchers(onlinePleaPayload);
        } else if (PleaType.GUILTY.equals(pleaType)) {
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
                withJsonPath("$.personalDetails.nationalInsuranceNumber", equalTo(person.getString("nationalInsuranceNumber"))),
                withJsonPath("$.personalDetails.address.address1", equalTo(personAddress.getString("address1"))),
                withJsonPath("$.personalDetails.address.address2", equalTo(personAddress.getString("address2"))),
                withJsonPath("$.personalDetails.address.address3", equalTo(personAddress.getString("address3"))),
                withJsonPath("$.personalDetails.address.address4", equalTo(personAddress.getString("address4"))),
                withJsonPath("$.personalDetails.address.address5", equalTo(personAddress.getString("address5"))),
                withJsonPath("$.personalDetails.address.postcode", equalTo(personAddress.getString("postcode")))
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
                withJsonPath("$.pleaDetails.plea", equalTo(PleaType.NOT_GUILTY.name())),
                withJsonPath("$.pleaDetails.comeToCourt", equalTo(true)),
                withJsonPath("$.pleaDetails.notGuiltyBecause", equalTo(offence.getString("notGuiltyBecause"))),
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
                withJsonPath("$.pleaDetails.plea", equalTo(PleaType.GUILTY.name())),
                withJsonPath("$.pleaDetails.comeToCourt", equalTo(false)),
                withJsonPath("$.pleaDetails.mitigation", equalTo(offence.getString("mitigation"))),
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
                withJsonPath("$.pleaDetails.plea", equalTo(PleaType.GUILTY_REQUEST_HEARING.name())),
                withJsonPath("$.pleaDetails.comeToCourt", equalTo(true)),
                withJsonPath("$.pleaDetails.mitigation", equalTo(offence.getString("mitigation"))),

                //employment status
                withJsonPath("$.employment.employmentStatus", equalTo("UNEMPLOYED")),
                withoutJsonPath("$.employment.employmentStatusDetails")
        );
    }

}
