package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assume.assumeThat;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.sjp.it.EventSelector.PUBLIC_SJP_CASE_UPDATE_REJECTED;
import static uk.gov.moj.sjp.it.EventSelector.SJP_EVENTS_CASE_UPDATE_REJECTED;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetEmptyAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.NotifyStub.stubNotifications;
import static uk.gov.moj.sjp.it.stub.NotifyStub.verifyNotification;
import static uk.gov.moj.sjp.it.stub.ResultingStub.stubGetCaseDecisionsWithDecision;
import static uk.gov.moj.sjp.it.stub.ResultingStub.stubGetCaseDecisionsWithNoDecision;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.COURT_ADMINISTRATORS_GROUP;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.LEGAL_ADVISERS_GROUP;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.SJP_PROSECUTORS_GROUP;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;

import uk.gov.moj.cpp.sjp.domain.PleaType;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.ContactDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseSearchResultHelper;
import uk.gov.moj.sjp.it.helper.CaseUpdateRejectedHelper;
import uk.gov.moj.sjp.it.helper.EmployerHelper;
import uk.gov.moj.sjp.it.helper.FinancialMeansHelper;
import uk.gov.moj.sjp.it.helper.PleadOnlineHelper;
import uk.gov.moj.sjp.it.helper.UpdatePleaHelper;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;
import uk.gov.moj.sjp.it.stub.UsersGroupsStub;
import uk.gov.moj.sjp.it.verifier.PersonInfoVerifier;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hamcrest.Matcher;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PleadOnlineIT extends BaseIntegrationTest {

    private EmployerHelper employerHelper;
    private FinancialMeansHelper financialMeansHelper;
    private PersonInfoVerifier personInfoVerifier;
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    private static final String TEMPLATE_PLEA_NOT_GUILTY_PAYLOAD = "raml/json/sjp.command.plead-online__not-guilty.json";
    private static final String TEMPLATE_PLEA_GUILTY_PAYLOAD = "raml/json/sjp.command.plead-online__guilty.json";
    private static final String TEMPLATE_PLEA_GUILTY_REQUEST_HEARING_PAYLOAD = "raml/json/sjp.command.plead-online__guilty_request_hearing.json";

    private static final Set<String> DEFAULT_STUBBED_USER_ID = singleton(USER_ID);

    @Before
    public void setUp() {
        this.createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        CreateCase.createCaseForPayloadBuilder(this.createCasePayloadBuilder);
        employerHelper = new EmployerHelper();
        financialMeansHelper = new FinancialMeansHelper();
        personInfoVerifier = PersonInfoVerifier.personInfoVerifierForCasePayload(createCasePayloadBuilder);

        stubGetCaseDecisionsWithNoDecision(createCasePayloadBuilder.getId());
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
        final String postcode = address.getString("postcode");

        //fields that we do not override
        final String title = personInfoVerifier.getPersonalDetails().getTitle();
        final String gender = personInfoVerifier.getPersonalDetails().getGender();

        return new PersonalDetails(title, firstName, lastName, LocalDate.parse(dateOfBirth), gender, nationalInsuranceNumber,
                new Address(address1, address2, address3, address4, postcode),
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
                                              final Collection<String> userIds, final boolean expectToHaveFinances) {
        assumeThat(userIds, not(empty()));

        final PleaMethod pleaMethod = PleaMethod.ONLINE;
        final String defendantId = CasePoller.pollUntilCaseByIdIsOk(createCasePayloadBuilder.getId()).getString("defendant.id");

        //checks person-info before plead-online
        personInfoVerifier.verifyPersonInfo();

        //runs plea-online
        pleadOnlineHelper.verifyOnlinePleaReceivedAndUpdatedCaseDetailsFlag(createCasePayloadBuilder.getId().toString(), false);
        final JSONObject pleaPayload = getOnlinePleaPayload(pleaType);
        pleadOnlineHelper.pleadOnline(pleaPayload.toString());

        //verify plea
        updatePleaHelper.verifyInPublicTopic(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId(), pleaType, null);
        updatePleaHelper.verifyPleaUpdated(createCasePayloadBuilder.getId(), pleaType, pleaMethod);
        caseSearchResultHelper.verifyPleaReceivedDate();

        //verify employer
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
        userIds.forEach(userId -> pleadOnlineHelper.getOnlinePlea(createCasePayloadBuilder.getId().toString(), expectedResult, userId));
        pleadOnlineHelper.verifyOnlinePleaReceivedAndUpdatedCaseDetailsFlag(createCasePayloadBuilder.getId().toString(), true);

        verifyNotification("criminal@gmail.com", createCasePayloadBuilder.getUrn());
    }

    @Test
    public void shouldPleadNotGuiltyOnlineThenFailWithSecondPleadAttemptAsNotAllowedTwoPleasAgainstSameOffence() {
        stubGetEmptyAssignmentsByDomainObjectId(createCasePayloadBuilder.getId());
        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper();
             final CaseUpdateRejectedHelper caseUpdateRejectedHelper = new CaseUpdateRejectedHelper(createCasePayloadBuilder.getId(),
                     SJP_EVENTS_CASE_UPDATE_REJECTED, PUBLIC_SJP_CASE_UPDATE_REJECTED)) {
            final PleadOnlineHelper pleadOnlineHelper = new PleadOnlineHelper(createCasePayloadBuilder.getId());
            final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(createCasePayloadBuilder.getId(),
                    createCasePayloadBuilder.getUrn(),
                    createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                    createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());

            //1) First plea should be successful
            pleadOnlineAndConfirmSuccess(PleaType.NOT_GUILTY, pleadOnlineHelper, updatePleaHelper, caseSearchResultHelper);

            //2) Second plea should fail as cannot plea twice
            final JSONObject pleaPayload = getOnlinePleaPayload(PleaType.NOT_GUILTY);
            pleadOnlineHelper.pleadOnline(pleaPayload.toString());
            caseUpdateRejectedHelper.verifyCaseUpdateRejectedPrivateInActiveMQ(CaseUpdateRejected.RejectReason.PLEA_ALREADY_SUBMITTED.name());
        }
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

    private void verifyGroupsCanSeeDefendantFinances(boolean expectToHaveFinances, Collection<String> financeProsecutors) {
        List<String> mockedUserId = financeProsecutors.stream()
                .map(userGroup -> {
                    String userId = UUID.randomUUID().toString();
                    UsersGroupsStub.stubGroupForUser(userId, userGroup);

                    return userId;
                }).collect(toList());

        pleadGuiltyOnlineWithUserAndExpectedFinances(mockedUserId, expectToHaveFinances);
    }


    @Test
    public void shouldPleadGuiltyRequestHearingOnline() {
        stubGetEmptyAssignmentsByDomainObjectId(createCasePayloadBuilder.getId());
        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper()) {
            final PleadOnlineHelper pleadOnlineHelper = new PleadOnlineHelper(createCasePayloadBuilder.getId());
            final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(createCasePayloadBuilder.getId(),
                    createCasePayloadBuilder.getUrn(),
                    createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                    createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());
            pleadOnlineAndConfirmSuccess(PleaType.GUILTY_REQUEST_HEARING, pleadOnlineHelper, updatePleaHelper, caseSearchResultHelper);
        }
    }

    @Test
    public void shouldRejectPleadOnlineWhenCaseAssigned() {
        stubGetAssignmentsByDomainObjectId(createCasePayloadBuilder.getId(), randomUUID());
        try (final CaseUpdateRejectedHelper caseUpdateRejectedHelper = new CaseUpdateRejectedHelper(createCasePayloadBuilder.getId(),
                SJP_EVENTS_CASE_UPDATE_REJECTED, PUBLIC_SJP_CASE_UPDATE_REJECTED)) {
            final PleadOnlineHelper pleadOnlineHelper = new PleadOnlineHelper(createCasePayloadBuilder.getId());
            final JSONObject onlinePleaPayload = getOnlinePleaPayload(PleaType.NOT_GUILTY);
            pleadOnlineHelper.pleadOnline(onlinePleaPayload.toString());

            caseUpdateRejectedHelper.verifyCaseUpdateRejectedPrivateInActiveMQ(CaseUpdateRejected.RejectReason.CASE_ASSIGNED.name());
            caseUpdateRejectedHelper.verifyCaseUpdateRejectedPublicInActiveMQ(CaseUpdateRejected.RejectReason.CASE_ASSIGNED.name());
        }
    }

    /*
     * Do twice to check serialization works correctly
     */
    @Test
    public void shouldRejectPleadOnlineWhenCaseCompletedTwice() {
        stubGetEmptyAssignmentsByDomainObjectId(createCasePayloadBuilder.getId());
        stubGetCaseDecisionsWithDecision(createCasePayloadBuilder.getId());

        try (final CaseUpdateRejectedHelper caseUpdateRejectedHelper = new CaseUpdateRejectedHelper(createCasePayloadBuilder.getId(),
                SJP_EVENTS_CASE_UPDATE_REJECTED, PUBLIC_SJP_CASE_UPDATE_REJECTED)) {
            final PleadOnlineHelper pleadOnlineHelper = new PleadOnlineHelper(createCasePayloadBuilder.getId());
            final JSONObject onlinePleaPayload = getOnlinePleaPayload(PleaType.NOT_GUILTY);
            pleadOnlineHelper.pleadOnline(onlinePleaPayload.toString());

            caseUpdateRejectedHelper.verifyCaseUpdateRejectedPrivateInActiveMQ(CaseUpdateRejected.RejectReason.CASE_COMPLETED.name());
            caseUpdateRejectedHelper.verifyCaseUpdateRejectedPublicInActiveMQ(CaseUpdateRejected.RejectReason.CASE_COMPLETED.name());
        }

        try (final CaseUpdateRejectedHelper caseUpdateRejectedHelper = new CaseUpdateRejectedHelper(createCasePayloadBuilder.getId(),
                SJP_EVENTS_CASE_UPDATE_REJECTED, PUBLIC_SJP_CASE_UPDATE_REJECTED)) {
            final PleadOnlineHelper pleadOnlineHelper = new PleadOnlineHelper(createCasePayloadBuilder.getId());
            final JSONObject onlinePleaPayload = getOnlinePleaPayload(PleaType.NOT_GUILTY);
            pleadOnlineHelper.pleadOnline(onlinePleaPayload.toString());

            caseUpdateRejectedHelper.verifyCaseUpdateRejectedPrivateInActiveMQ(CaseUpdateRejected.RejectReason.CASE_COMPLETED.name());
            caseUpdateRejectedHelper.verifyCaseUpdateRejectedPublicInActiveMQ(CaseUpdateRejected.RejectReason.CASE_COMPLETED.name());
        }
    }

    private void pleadGuiltyOnlineWithUserAndExpectedFinances(Collection<String> userIds, boolean expectToHaveFinances) {
        stubGetEmptyAssignmentsByDomainObjectId(createCasePayloadBuilder.getId());
        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper()) {
            final PleadOnlineHelper pleadOnlineHelper = new PleadOnlineHelper(createCasePayloadBuilder.getId());
            final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(createCasePayloadBuilder.getId(),
                    createCasePayloadBuilder.getUrn(),
                    createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                    createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());
            pleadOnlineAndConfirmSuccess(PleaType.GUILTY, pleadOnlineHelper, updatePleaHelper, caseSearchResultHelper, userIds, expectToHaveFinances);
        }
    }

    private JSONObject getOnlinePleaPayload(PleaType pleaType) {
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

    private Matcher getEmployerUpdatedPayloadMatcher(final JSONObject employer) {
        return isJson(getEmployerUpdatedPayloadContentMatcher(employer));
    }

    private Matcher getEmployerUpdatedPublicEventMatcher(final JSONObject employer) {
        final Matcher payloadContentMatcher = getEmployerUpdatedPayloadContentMatcher(employer);
        return jsonEnvelope()
                .withMetadataOf(metadata().withName("public.sjp.employer-updated"))
                .withPayloadOf(payloadIsJson(payloadContentMatcher));
    }

    private Matcher getEmployerUpdatedPayloadContentMatcher(final JSONObject onlinePleaPayload) {
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
                withJsonPath("$.address.postcode", equalTo(address.getString("postcode")))
        );
    }

    private Matcher getFinancialMeansUpdatedPayloadContentMatcher(final JSONObject onlinePleaPayload, final String defendantId) {
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
        List<Matcher> fieldMatchers = getCommonFieldMatchers(onlinePleaPayload, caseId, defendantId, expectToHaveFinances);
        List<Matcher> extraMatchers;
        if (PleaType.NOT_GUILTY.equals(pleaType)) {
            extraMatchers = getNotGuiltyMatchers(onlinePleaPayload);
        } else if (PleaType.GUILTY.equals(pleaType)) {
            extraMatchers = getGuiltyMatchers(onlinePleaPayload, expectToHaveFinances);
        } else {
            extraMatchers = getGuiltyRequestHearingMatchers(onlinePleaPayload);
        }
        fieldMatchers.addAll(extraMatchers);

        return isJson(allOf(
                fieldMatchers.toArray(new Matcher[fieldMatchers.size()])
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

        List<Matcher> matchers = new ArrayList<>(asList(
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
        List<Matcher> mitigation = new ArrayList<>(asList(
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
                withJsonPath("$.pleaDetails.interpreterLanguage", equalTo(onlinePleaPayload.getString("interpreterLanguage"))),
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
