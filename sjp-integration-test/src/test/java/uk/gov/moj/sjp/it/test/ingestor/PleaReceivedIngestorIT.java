package uk.gov.moj.sjp.it.test.ingestor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getJsonArray;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubCountryByPostcodeQuery;
import static uk.gov.moj.sjp.it.test.ingestor.helper.IngesterHelper.jsonFromString;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;

import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchClient;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexFinderUtil;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;
import uk.gov.moj.sjp.it.helper.PleadOnlineHelper;
import uk.gov.moj.sjp.it.helper.UpdatePleaHelper;
import uk.gov.moj.sjp.it.test.BaseIntegrationTest;

import java.io.IOException;
import java.util.Optional;

import javax.json.JsonObject;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class PleaReceivedIngestorIT extends BaseIntegrationTest {
    private static final String ONLINE_PLEA_PAYLOAD = "raml/json/sjp.command.plead-online__not-guilty_for_ingester.json";

    private ElasticSearchIndexFinderUtil elasticSearch;

    private CreateCasePayloadBuilder casePayloadBuilder;

    private final Poller poller = new Poller(1200, 1000L);

    @Before
    public void setUp() throws IOException {
        casePayloadBuilder = CreateCasePayloadBuilder.withDefaults();

        createCaseForPayloadBuilder(this.casePayloadBuilder);

        pollUntilCaseByIdIsOk(casePayloadBuilder.getId());

        elasticSearch = new ElasticSearchIndexFinderUtil(new ElasticSearchClient());
        new ElasticSearchIndexRemoverUtil().deleteAndCreateCaseIndex();

        stubCountryByPostcodeQuery("W1T 1JY", "England");
    }

    @Test
    public void shouldIngestCaseReceivedEvent() {
        pleadOnline();

        final Optional<JsonObject> searchResponse = getElasticSearchResponse();

        final JsonObject outputCase = jsonFromString(getJsonArray(searchResponse.get(), "index").get().getString(0));

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

    private Optional<JsonObject> getElasticSearchResponse() {
        return poller.pollUntilFound(() -> {
            try {
                final JsonObject jsonObject = elasticSearch.findAll("crime_case_index");
                if (jsonObject.getInt("totalResults") == 1) {
                    return of(jsonObject);
                }
            } catch (final IOException e) {
                fail();
            }
            return empty();
        });
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
