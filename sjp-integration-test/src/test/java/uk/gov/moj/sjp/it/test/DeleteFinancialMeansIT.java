package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singleton;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.IncomeFrequency.MONTHLY;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.DeleteFinancialMeansMatcherHelper.getExpectedFinancialMeanDataAfterDeletionMatcher;
import static uk.gov.moj.sjp.it.helper.DeleteFinancialMeansMatcherHelper.getExpectedFinancialMeanDataBeforeDeletionMatcher;
import static uk.gov.moj.sjp.it.helper.DeleteFinancialMeansMatcherHelper.getSavedOnlinePleaPayloadContentMatcher;
import static uk.gov.moj.sjp.it.helper.EmployerHelper.getEmployerUpdatedPayloadMatcher;
import static uk.gov.moj.sjp.it.helper.FinancialMeansHelper.getOnlinePleaData;
import static uk.gov.moj.sjp.it.stub.MaterialStub.stubAddCaseMaterial;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubAllIndividualProsecutorsQueries;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubCountryByPostcodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryOffencesByCode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;

import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseDocumentHelper;
import uk.gov.moj.sjp.it.helper.EmployerHelper;
import uk.gov.moj.sjp.it.helper.FinancialMeansHelper;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;
import uk.gov.moj.sjp.it.stub.MaterialStub;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.core.Response;

import org.hamcrest.Matcher;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DeleteFinancialMeansIT extends BaseIntegrationTest {

    private FinancialMeansHelper financialMeansHelper;
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;
    private static final String TEMPLATE_PLEA_NOT_GUILTY_PAYLOAD = "raml/json/sjp.command.plead-online__not-guilty.json";
    //To Query OnlinePlea
    private static final Set<UUID> DEFAULT_STUBBED_USER_ID = singleton(USER_ID);
    private static final String NATIONAL_COURT_CODE = "1080";


    @BeforeEach
    public void setUp() {

        //Following 3 stubs require to post online plead successfully
        stubCountryByPostcodeQuery("W1T 1JY", "England");
        stubQueryOffencesByCode("PS00001");
        stubAllIndividualProsecutorsQueries();
        stubForUserDetails(UUID.fromString("7242d476-9ca3-454a-93ee-78bf148602bf"), "ALL");

        financialMeansHelper = new FinancialMeansHelper();
        this.createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, "TestRegion");
        createCaseForPayloadBuilder(this.createCasePayloadBuilder);
    }

    @AfterEach
    public void tearDown() {
        financialMeansHelper.close();
    }

    @Test
    void shouldDeleteFinancialMeansData() {

        final UUID caseId = createCasePayloadBuilder.getId();
        final String defendantId = CasePoller.pollUntilCaseByIdIsOk(caseId).getString("defendant.id");
        final Income originalIncome = new Income(MONTHLY, BigDecimal.valueOf(1000.50));
        final String employmentStatus = "EMPLOYED";

        //Step 1: Create Financial Means Data
        createDefendantsFinancialMeansData(caseId, defendantId, originalIncome, employmentStatus);

        //Step 2: Update Financial Means Data by calling Online Plea
        final JSONObject pleaPayload = getOnlinePleaPayload();
        defendantRaisesOnlinePlea(caseId, defendantId, pleaPayload);

        //Step 3: upload Case Documents of FINANCIAL_MEANS documentType
        uploadFinancialMeansCaseDocument(caseId);

        //Step 4: Delete Financial Means Data
        financialMeansHelper.deleteFinancialMeans(caseId, defendantId, createObjectBuilder().build());

        //Step 5: Verify the deletion of financial Mean data from
        // a) financial-mean
        // b) online-plea
        // c) case document entry containing finacial means reference data
        // d) verify employer data deletion
        verifyFinancialMeansDataDeletion(defendantId);
        verifyOnlinePleaDataDeletion(pleaPayload, caseId, defendantId);
        verifyCaseDocumentReferenceDataDeletion(caseId);
        verifyEmployerDataDeletion(defendantId);
    }


    @Test
    public void testDeleteFinancialMeansWithMaterials() {

        //Create a Case...
        final UUID caseId = createCasePayloadBuilder.getId();
        final String defendantId = CasePoller.pollUntilCaseByIdIsOk(caseId).getString("defendant.id");
        final CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(caseId);

        final List<String> fmiMaterials = financialMeansHelper.associateCaseWithFinancialMeansDocuments(caseId, USER_ID, caseDocumentHelper);

        final JsonObject payload = createObjectBuilder().build();
        financialMeansHelper.deleteFinancialMeans(caseId, defendantId, payload);

        FinancialMeansHelper.assertDocumentDeleted(caseDocumentHelper, USER_ID, fmiMaterials);
        fmiMaterials.forEach(MaterialStub::assertMaterialDeleteFMICommandInvoked);
        fmiMaterials.clear();
        financialMeansHelper.getEventFromPublicTopic(
                isJson(
                        withJsonPath("$.defendantId", is(defendantId)))
        );
    }

    private void uploadFinancialMeansCaseDocument(final UUID caseId) {
        stubAddCaseMaterial();
        try (final CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(caseId)) {
            caseDocumentHelper.uploadDocument("FINANCIAL_MEANS");
            final UUID documentId = caseDocumentHelper.verifyCaseDocumentUploadedEventRaised();
            final UUID materialId = MaterialStub.processMaterialAddedCommand(documentId);
            CaseDocumentHelper.assertDocumentAdded(USER_ID, caseId, materialId, documentId, "FINANCIAL_MEANS");
        }
    }

    private void verifyEmployerDataDeletion(final String defendantId) {
        final Response response = EmployerHelper.pollForEmployerForDefendant(defendantId);
        assertThat(response.readEntity(String.class), is("{}"));
    }

    private void verifyCaseDocumentReferenceDataDeletion(final UUID caseId) {
        try (final CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(caseId)) {
            caseDocumentHelper.assertDocumentNotExist(USER_ID, caseId);
        }
    }

    private void verifyOnlinePleaDataDeletion(final JSONObject pleaPayload, final UUID caseId, final String defendantId) {
        final Matcher<Object> expectedOnlinePleaResultAfterDelete = getSavedOnlinePleaPayloadContentMatcher(pleaPayload, caseId.toString(), defendantId, true);
        DEFAULT_STUBBED_USER_ID.forEach(userId -> getOnlinePleaData(caseId.toString(), expectedOnlinePleaResultAfterDelete, userId, defendantId));
    }

    private void verifyFinancialMeansDataDeletion(final String defendantId) {
        financialMeansHelper.getFinancialMeans(defendantId, getExpectedFinancialMeanDataAfterDeletionMatcher());
    }

    private void defendantRaisesOnlinePlea(final UUID caseId, final String defendantId, final JSONObject pleaPayload) {
        pleadOnline(pleaPayload.toString(), caseId, defendantId);
        final Matcher<Object> expectedOnlinePleaResultBeforeDelete = getSavedOnlinePleaPayloadContentMatcher(pleaPayload, caseId.toString(), defendantId, false);

        DEFAULT_STUBBED_USER_ID.forEach(userId -> getOnlinePleaData(caseId.toString(), expectedOnlinePleaResultBeforeDelete, userId, defendantId));
        verifyEmployerDataExist(caseId, defendantId, pleaPayload);
    }

    private void verifyEmployerDataExist(final UUID caseId, final String defendantId, final JSONObject pleaPayload) {

        final JSONObject jsonObject = pleaPayload.getJSONObject("employer");
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonObject.toString()))) {
            JsonObject employer1 = jsonReader.readObject();
            EmployerHelper.pollForEmployerForDefendant(defendantId, getEmployerUpdatedPayloadMatcher(employer1));
        }
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

    private void pleadOnline(final String payload, final UUID caseId, final String defendantId) {
        final String writeUrl = String.format("/cases/%s/defendants/%s/plead-online", caseId, defendantId);
        makePostCall(writeUrl, "application/vnd.sjp.plead-online+json", payload, Response.Status.ACCEPTED);
    }


}
