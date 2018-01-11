package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.sjp.it.EventSelector.PUBLIC_STRUCTURE_CASE_UPDATE_REJECTED;
import static uk.gov.moj.sjp.it.EventSelector.STRUCTURE_EVENTS_CASE_UPDATE_REJECTED;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetEmptyAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.ResultingStub.stubGetCaseDecisionsWithDecision;
import static uk.gov.moj.sjp.it.stub.ResultingStub.stubGetCaseDecisionsWithNoDecision;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;

import uk.gov.moj.cpp.sjp.domain.PleaType;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;
import uk.gov.moj.sjp.it.helper.CaseSearchResultHelper;
import uk.gov.moj.sjp.it.helper.CaseSjpHelper;
import uk.gov.moj.sjp.it.helper.CaseUpdateRejectedHelper;
import uk.gov.moj.sjp.it.helper.EmployerHelper;
import uk.gov.moj.sjp.it.helper.FinancialMeansHelper;
import uk.gov.moj.sjp.it.helper.PleaOnlineHelper;
import uk.gov.moj.sjp.it.helper.UpdatePleaHelper;

import org.hamcrest.Matcher;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PleaOnlineIT extends BaseIntegrationTest {

    private CaseSjpHelper caseSjpHelper;
    private EmployerHelper employerHelper;
    private FinancialMeansHelper financialMeansHelper;

    private static final String TEMPLATE_PLEA_ONLINE_PAYLOAD = "raml/json/sjp.command.plea-online__not-guilty.json";

    @Before
    public void setUp() {
        caseSjpHelper = new CaseSjpHelper();
        caseSjpHelper.createCase();
        caseSjpHelper.verifyCaseCreatedUsingId();
        employerHelper = new EmployerHelper();
        financialMeansHelper = new FinancialMeansHelper();

        stubGetCaseDecisionsWithNoDecision(caseSjpHelper.getCaseId());
    }

    @After
    public void tearDown() throws Exception {
        caseSjpHelper.close();
        employerHelper.close();
        financialMeansHelper.close();
    }

    private void pleaOnlineAndConfirmSuccess(final PleaOnlineHelper pleaOnlineHelper, final UpdatePleaHelper updatePleaHelper,
                                             final CaseSearchResultHelper caseSearchResultHelper) {
        final String pleaMethod = "ONLINE";
        final String defendantId = caseSjpHelper.getSingleDefendantId();
        final JSONObject onlinePleaPayload = getOnlinePleaPayload(PleaType.NOT_GUILTY);
        pleaOnlineHelper.pleaOnline(onlinePleaPayload.toString());

        //verify plea
        updatePleaHelper.verifyInPublicTopic(PleaType.NOT_GUILTY.name(), null);
        updatePleaHelper.verifyPleaUpdated(PleaType.NOT_GUILTY.name(), pleaMethod);
        caseSearchResultHelper.verifyPleaReceivedDate();

        //verify employer
        employerHelper.getEmployer(defendantId, getEmployerUpdatedPayloadMatcher(onlinePleaPayload));
        assertThat(employerHelper.getEventFromPublicTopic(), getEmployerUpdatedPublicEventMatcher(onlinePleaPayload));

        //verify financial-means
        financialMeansHelper.getFinancialMeans(defendantId, getFinancialMeansUpdatedPayloadContentMatcher(onlinePleaPayload, defendantId));
        financialMeansHelper.getEventFromPublicTopic(getFinancialMeansUpdatedPayloadContentMatcher(onlinePleaPayload, defendantId));

        //TODO: for ATCM-616, add to pleaOnlineHelper:
        //TODO: 1) check all data stored in new repository using new query
        //TODO: 2) check public topic to see if public events exist for new TrialRequested event
    }

    @Test
    public void shouldPleaOnlineThenFailWithSecondPleaAsNotAllowedToPleaAgainOnSameOffence() {
        stubGetEmptyAssignmentsByDomainObjectId(caseSjpHelper.getCaseId());
        try (final PleaOnlineHelper pleaOnlineHelper = new PleaOnlineHelper(caseSjpHelper);
             final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper(caseSjpHelper);
             final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(caseSjpHelper);
             final CaseUpdateRejectedHelper caseUpdateRejectedHelper = new CaseUpdateRejectedHelper(caseSjpHelper,
                     STRUCTURE_EVENTS_CASE_UPDATE_REJECTED, PUBLIC_STRUCTURE_CASE_UPDATE_REJECTED)) {

            //1) First plea should be successful
            pleaOnlineAndConfirmSuccess(pleaOnlineHelper, updatePleaHelper, caseSearchResultHelper);

            //2) Second plea should fail as cannot plea twice
            final JSONObject onlinePleaPayload = getOnlinePleaPayload(PleaType.NOT_GUILTY);
            pleaOnlineHelper.pleaOnline(onlinePleaPayload.toString());
            caseUpdateRejectedHelper.verifyCaseUpdateRejectedPrivateInActiveMQ(CaseUpdateRejected.RejectReason.PLEA_ALREADY_SUBMITTED.name());
        }
    }

    @Test
    public void shouldRejectPleaOnlineWhenCaseAssigned() {
        stubGetAssignmentsByDomainObjectId(caseSjpHelper.getCaseId(), randomUUID());
        try (final PleaOnlineHelper pleaOnlineHelper = new PleaOnlineHelper(caseSjpHelper);
             final CaseUpdateRejectedHelper caseUpdateRejectedHelper = new CaseUpdateRejectedHelper(caseSjpHelper,
                     STRUCTURE_EVENTS_CASE_UPDATE_REJECTED, PUBLIC_STRUCTURE_CASE_UPDATE_REJECTED)) {

            final JSONObject onlinePleaPayload = getOnlinePleaPayload(PleaType.NOT_GUILTY);
            pleaOnlineHelper.pleaOnline(onlinePleaPayload.toString());

            caseUpdateRejectedHelper.verifyCaseUpdateRejectedPrivateInActiveMQ(CaseUpdateRejected.RejectReason.CASE_ASSIGNED.name());
            caseUpdateRejectedHelper.verifyCaseUpdateRejectedPublicInActiveMQ(CaseUpdateRejected.RejectReason.CASE_ASSIGNED.name());
        }
    }

    /*
     * Do twice to check serialization works correctly
     */
    @Test
    public void shouldRejectPleaOnlineWhenCaseCompletedTwice() {
        stubGetEmptyAssignmentsByDomainObjectId(caseSjpHelper.getCaseId());
        stubGetCaseDecisionsWithDecision(caseSjpHelper.getCaseId());

        try (final PleaOnlineHelper pleaOnlineHelper = new PleaOnlineHelper(caseSjpHelper);
             final CaseUpdateRejectedHelper caseUpdateRejectedHelper = new CaseUpdateRejectedHelper(caseSjpHelper,
                     STRUCTURE_EVENTS_CASE_UPDATE_REJECTED, PUBLIC_STRUCTURE_CASE_UPDATE_REJECTED)) {

            final JSONObject onlinePleaPayload = getOnlinePleaPayload(PleaType.NOT_GUILTY);
            pleaOnlineHelper.pleaOnline(onlinePleaPayload.toString());

            caseUpdateRejectedHelper.verifyCaseUpdateRejectedPrivateInActiveMQ(CaseUpdateRejected.RejectReason.CASE_COMPLETED.name());
            caseUpdateRejectedHelper.verifyCaseUpdateRejectedPublicInActiveMQ(CaseUpdateRejected.RejectReason.CASE_COMPLETED.name());
        }

        try (final PleaOnlineHelper pleaOnlineHelper = new PleaOnlineHelper(caseSjpHelper);
             final CaseUpdateRejectedHelper caseUpdateRejectedHelper = new CaseUpdateRejectedHelper(caseSjpHelper,
                     STRUCTURE_EVENTS_CASE_UPDATE_REJECTED, PUBLIC_STRUCTURE_CASE_UPDATE_REJECTED)) {

            final JSONObject onlinePleaPayload = getOnlinePleaPayload(PleaType.NOT_GUILTY);
            pleaOnlineHelper.pleaOnline(onlinePleaPayload.toString());

            caseUpdateRejectedHelper.verifyCaseUpdateRejectedPrivateInActiveMQ(CaseUpdateRejected.RejectReason.CASE_COMPLETED.name());
            caseUpdateRejectedHelper.verifyCaseUpdateRejectedPublicInActiveMQ(CaseUpdateRejected.RejectReason.CASE_COMPLETED.name());
        }
    }

    private JSONObject getOnlinePleaPayload(PleaType pleaType) {
        String templateRequest = null;
        if (pleaType.equals(PleaType.NOT_GUILTY)) {
            templateRequest = getPayload(TEMPLATE_PLEA_ONLINE_PAYLOAD);
        }
        final JSONObject jsonObject = new JSONObject(templateRequest);
        //set offence.id to match setup data
        jsonObject.getJSONArray("offences").getJSONObject(0).put("id", caseSjpHelper.getSingleOffenceId());
        return jsonObject;
    }

    private Matcher getEmployerUpdatedPayloadMatcher(final JSONObject employer) {
        return isJson(getEmployerUpdatedPayloadContentMatcher(employer));
    }

    private Matcher getEmployerUpdatedPublicEventMatcher(final JSONObject employer) {
        final Matcher payloadContentMatcher = getEmployerUpdatedPayloadContentMatcher(employer);
        return jsonEnvelope()
                .withMetadataOf(metadata().withName("public.structure.employer-updated"))
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
}
