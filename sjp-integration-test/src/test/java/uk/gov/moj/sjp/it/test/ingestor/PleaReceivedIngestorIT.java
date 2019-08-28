package uk.gov.moj.sjp.it.test.ingestor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubCountryByPostcodeQuery;
import static uk.gov.moj.sjp.it.test.ingestor.helper.ElasticSearchQueryHelper.getCaseFromElasticSearch;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;

import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;
import uk.gov.moj.sjp.it.framework.util.ViewStoreCleaner;
import uk.gov.moj.sjp.it.helper.PleadOnlineHelper;
import uk.gov.moj.sjp.it.helper.UpdatePleaHelper;
import uk.gov.moj.sjp.it.test.BaseIntegrationTest;

import java.io.IOException;
import java.util.UUID;

import javax.json.JsonObject;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PleaReceivedIngestorIT extends BaseIntegrationTest {
    private static final String ONLINE_PLEA_PAYLOAD = "raml/json/sjp.command.plead-online__not-guilty_for_ingester.json";


    private CreateCasePayloadBuilder casePayloadBuilder;

    private final UUID caseIdOne = randomUUID();
    private final ViewStoreCleaner viewStoreCleaner = new ViewStoreCleaner();

    @After
    public void cleanDatabase() {
        viewStoreCleaner.cleanDataInViewStore(caseIdOne);
    }

    @Before
    public void setUp() throws IOException {
        casePayloadBuilder = CreateCasePayloadBuilder.withDefaults().withId(caseIdOne);
        createCaseForPayloadBuilder(this.casePayloadBuilder);

        pollUntilCaseByIdIsOk(casePayloadBuilder.getId());

        new ElasticSearchIndexRemoverUtil().deleteAndCreateCaseIndex();

        stubCountryByPostcodeQuery("W1T 1JY", "England");
    }

    @Test
    public void shouldIngestCaseReceivedEvent() {
        pleadOnline();

        final JsonObject outputCase = getCaseFromElasticSearch();

        verifyElasticSearchResponse(outputCase);
    }

    private void verifyElasticSearchResponse(final JsonObject casePayload) {
        assertThat(casePayload.toString(),
                isJson(allOf(
                        withJsonPath("caseId", equalTo(casePayloadBuilder.getId().toString())),
                        withJsonPath("parties[0].partyId", equalTo(casePayloadBuilder.getDefendantBuilder().getId().toString())),
                        withJsonPath("parties[0].firstName", equalTo("Testy")),
                        withJsonPath("parties[0].lastName", equalTo("Testerson")),
                        withJsonPath("parties[0].dateOfBirth", equalTo("1990-09-09")),
                        withJsonPath("parties[0].addressLines", equalTo("15 Harvey Avenue Barking Essex Wales Bhirmingham")),
                        withJsonPath("parties[0].postCode", equalTo("W1T 1JY"))
                )));
    }

    private JSONObject pleadOnline() {

        final JSONObject pleaPayload = getOnlinePleaPayload();

        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper()) {
            final PleadOnlineHelper pleadOnlineHelper = new PleadOnlineHelper(casePayloadBuilder.getId());

            pleadOnlineHelper.pleadOnline(pleaPayload.toString());

            updatePleaHelper.verifyPleaUpdated(casePayloadBuilder.getId(), PleaType.NOT_GUILTY, PleaMethod.ONLINE);
        }

        return pleaPayload;
    }

    private JSONObject getOnlinePleaPayload() {
        final String templateRequest = getPayload(ONLINE_PLEA_PAYLOAD);

        final JSONObject jsonObject = new JSONObject(templateRequest);
        jsonObject.getJSONArray("offences")
                .getJSONObject(0)
                .put("id", casePayloadBuilder.getOffenceId().toString());

        return jsonObject;
    }
}
