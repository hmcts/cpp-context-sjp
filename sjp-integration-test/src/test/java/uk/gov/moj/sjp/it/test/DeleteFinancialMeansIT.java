package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singleton;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static uk.gov.moj.cpp.sjp.domain.IncomeFrequency.MONTHLY;
import static uk.gov.moj.sjp.it.helper.DeleteFinancialMeansMatcherHelper.getExpectedFinancialMeanDataAfterDeletionMatcher;
import static uk.gov.moj.sjp.it.helper.DeleteFinancialMeansMatcherHelper.getExpectedFinancialMeanDataBeforeDeletionMatcher;
import static uk.gov.moj.sjp.it.helper.DeleteFinancialMeansMatcherHelper.getSavedOnlinePleaPayloadContentMatcher;
import static uk.gov.moj.sjp.it.helper.FinancialMeansHelper.getOnlinePleaData;
import static uk.gov.moj.sjp.it.stub.MaterialStub.stubAddCaseMaterial;
import static uk.gov.moj.sjp.it.stub.NotifyStub.stubNotifications;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubCountryByPostcodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryOffenceById;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;

import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseDocumentHelper;
import uk.gov.moj.sjp.it.helper.FinancialMeansHelper;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;
import uk.gov.moj.sjp.it.stub.UsersGroupsStub;
import uk.gov.moj.sjp.it.stub.MaterialStub;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DeleteFinancialMeansIT extends BaseIntegrationTest {

    private FinancialMeansHelper financialMeansHelper;
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;
    private static final String TEMPLATE_PLEA_NOT_GUILTY_PAYLOAD = "raml/json/sjp.command.plead-online__not-guilty.json";
    //To add Case Document
    private static final String PROSECUTING_AUTHORITY_ACCESS_ALL = "ALL";
    //To Query OnlinePlea
    private static final Set<UUID> DEFAULT_STUBBED_USER_ID = singleton(USER_ID);


    @Before
    public void setUp() {

        //Following 3 stubs require to post online plead successfully
        stubCountryByPostcodeQuery("W1T 1JY", "England");
        stubNotifications();
        stubQueryOffenceById(randomUUID());

        financialMeansHelper = new FinancialMeansHelper();
        this.createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        CreateCase.createCaseForPayloadBuilder(this.createCasePayloadBuilder);
    }

    @After
    public void tearDown() throws Exception {
        financialMeansHelper.close();
    }

    @Test
    public void shouldDeleteFinancialMeansData() throws InterruptedException {

        final UUID caseId = createCasePayloadBuilder.getId();
        final String defendantId = CasePoller.pollUntilCaseByIdIsOk(caseId).getString("defendant.id");
        final Income originalIncome = new Income(MONTHLY, BigDecimal.valueOf(1000.50));
        final String employmentStatus = "EMPLOYED";

        //Step 1: Create Financial Means Data
        createDefendantsFinancialMeansData(caseId, defendantId, originalIncome, employmentStatus);

        //Step 2: Update Financial Means Data by calling Online Plea
        final JSONObject pleaPayload = getOnlinePleaPayload();
        defendantRaisesOnlinePlea(caseId, defendantId, pleaPayload);

        //Step 3: Add Case Documents of FINANCIAL_MEANS documentType
        final UUID legalAdviserId = randomUUID();
        addFinancialMeansCaseDocument(caseId,defendantId,legalAdviserId);

        //Step 4: Delete Financial Means Data
        financialMeansHelper.deleteFinancialMeans(caseId, defendantId, createObjectBuilder().build());

        //Step 5: Verify the deletion of financial Mean data from
        // a) financial-mean
        // b) online-plea
        // c) case document entry containing finacial means reference data
        verifyFinancialMeansDataDeletion(defendantId);
        verifyOnlinePleaDataDeletion(pleaPayload, caseId, defendantId);
        verifyFinancialMeansCaseDocumentDeletion(caseId,legalAdviserId);
    }

    @Test
    public void testDeleteFinancialMeansWithMaterials() {

        //Create a Case...
        final UUID caseId = createCasePayloadBuilder.getId();
        final String defendantId = CasePoller.pollUntilCaseByIdIsOk(caseId).getString("defendant.id");
        final CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(caseId);

        List<String> fmiMaterials = financialMeansHelper.associateCaseWithFinancialMeansDocuments(caseId,USER_ID,caseDocumentHelper);

        final JsonObject payload = createObjectBuilder().build();
        financialMeansHelper.deleteFinancialMeans(caseId, defendantId, payload);

        FinancialMeansHelper.assertDocumentDeleted(caseDocumentHelper,USER_ID,fmiMaterials);
        fmiMaterials.forEach(materialId -> MaterialStub.assertMaterialDeleteFMICommandInvoked(materialId));
        fmiMaterials.clear();
        financialMeansHelper.getEventFromPublicTopic(
                isJson(
                        withJsonPath("$.defendantId", is(defendantId)))
        );

    }

    private void verifyFinancialMeansCaseDocumentDeletion(final UUID caseId, final UUID legalAdviserId) {
        try (final CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(caseId)) {
            caseDocumentHelper.assertDocumentNotExist(legalAdviserId, 0, "FINANCIAL_MEANS", 1);
        }
    }

    private void addFinancialMeansCaseDocument(final UUID caseId, final String defendantId, final UUID legalAdviserId) {

        UsersGroupsStub.stubGroupForUser(legalAdviserId, UsersGroupsStub.LEGAL_ADVISERS_GROUP);
        UsersGroupsStub.stubForUserDetails(legalAdviserId, PROSECUTING_AUTHORITY_ACCESS_ALL);

        stubAddCaseMaterial();

        try (final CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(caseId)) {
            caseDocumentHelper.addCaseDocumentWithDocumentType(legalAdviserId, "FINANCIAL_MEANS");
            caseDocumentHelper.findDocument(legalAdviserId, 0, "FINANCIAL_MEANS", 1);
        }

    }

    private void verifyOnlinePleaDataDeletion(final JSONObject pleaPayload, final UUID caseId, final String defendantId) throws InterruptedException {
        final Matcher<Object> expectedOnlinePleaResultAfterDelete = getSavedOnlinePleaPayloadContentMatcher(pleaPayload, caseId.toString(), defendantId, true);
        DEFAULT_STUBBED_USER_ID.forEach(userId -> getOnlinePleaData(caseId.toString(), expectedOnlinePleaResultAfterDelete, userId));
    }

    private void verifyFinancialMeansDataDeletion(final String defendantId) {
        financialMeansHelper.getFinancialMeans(defendantId, getExpectedFinancialMeanDataAfterDeletionMatcher());
    }

    private void defendantRaisesOnlinePlea(final UUID caseId, final String defendantId, final JSONObject pleaPayload) throws InterruptedException {
        financialMeansHelper.pleadOnline(pleaPayload.toString(), caseId, defendantId);
        final Matcher<Object> expectedOnlinePleaResultBeforeDelete = getSavedOnlinePleaPayloadContentMatcher(pleaPayload, caseId.toString(), defendantId, false);

        DEFAULT_STUBBED_USER_ID.forEach(userId -> getOnlinePleaData(caseId.toString(), expectedOnlinePleaResultBeforeDelete, userId));
    }

    private void createDefendantsFinancialMeansData(final UUID caseId, final String defendantId, final Income originalIncome, final String employmentStatus) {

        final JsonObject original = createObjectBuilder()
                .add("income", createObjectBuilder()
                        .add("frequency", originalIncome.getFrequency().name())
                        .add("amount", originalIncome.getAmount()))
                .add("benefits", createObjectBuilder())
                .add("employmentStatus", employmentStatus)
                .build();

        financialMeansHelper.updateFinancialMeans(caseId, defendantId, original);
        financialMeansHelper.getFinancialMeans(defendantId, getExpectedFinancialMeanDataBeforeDeletionMatcher(originalIncome, employmentStatus));
    }

    private JSONObject getOnlinePleaPayload() {
        String templateRequest = getPayload(TEMPLATE_PLEA_NOT_GUILTY_PAYLOAD);
        final JSONObject jsonObject = new JSONObject(templateRequest);
        //set offence.id to match setup data
        jsonObject.getJSONArray("offences").getJSONObject(0).put("id", createCasePayloadBuilder.getOffenceId().toString());
        return jsonObject;
    }
}
