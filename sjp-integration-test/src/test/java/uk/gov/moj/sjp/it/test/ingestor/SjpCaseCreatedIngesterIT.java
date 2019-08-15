package uk.gov.moj.sjp.it.test.ingestor;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getJsonArray;
import static uk.gov.moj.sjp.it.test.ingestor.helper.IngesterHelper.buildEnvelope;
import static uk.gov.moj.sjp.it.test.ingestor.helper.IngesterHelper.jsonFromString;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchClient;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexFinderUtil;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;
import uk.gov.moj.sjp.it.test.BaseIntegrationTest;

import java.io.IOException;
import java.util.Optional;

import javax.json.JsonObject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SjpCaseCreatedIngesterIT extends BaseIntegrationTest {
    private static final String EVENT_NAME = "sjp.events.sjp-case-created";
    private static final String PAYLOAD_PATH = "stub-data/sjp.events.sjp-case-created.json";
    private static final String SJP_EVENT = "sjp.event";

    private final MessageProducerClient privateEventsProducer = new MessageProducerClient();

    private ElasticSearchIndexFinderUtil elasticSearchIndexFinderUtil;
    private final Poller poller = new Poller(1200, 1000L);

    @Before
    public void setUp() throws IOException {
        privateEventsProducer.startProducer(SJP_EVENT);
        final ElasticSearchClient elasticSearchClient = new ElasticSearchClient();
        elasticSearchIndexFinderUtil = new ElasticSearchIndexFinderUtil(elasticSearchClient);
        new ElasticSearchIndexRemoverUtil().deleteAndCreateCaseIndex();
    }

    @After
    public void tearDown() {
        privateEventsProducer.close();
    }

    @Test
    public void shouldIngestSjpCaseCreatedEvent() {
        publishSjpCaseCreatedEvent();
        final Optional<JsonObject> caseCreatedResponseObject = poller.pollUntilFound(() -> {
            try {
                final JsonObject jsonObject = elasticSearchIndexFinderUtil.findAll("crime_case_index");
                if (jsonObject.getInt("totalResults") == 1) {
                    return of(jsonObject);
                }
            } catch (final IOException e) {
                fail();
            }
            return empty();
        });

        final JsonObject actualCase = jsonFromString(getJsonArray(caseCreatedResponseObject.get(), "index").get().getString(0));

        verifyCase(actualCase);
    }

    private void verifyCase(final JsonObject actualCase) {
        assertThat(actualCase.getString("caseId"), is("7e2f843e-d639-40b3-8611-8015f3a18958"));
        assertThat(actualCase.getString("caseReference"), is("22C22222222"));
        assertThat(actualCase.getString("sjpNoticeServed"), is("2015-12-02"));
        assertThat(actualCase.getString("prosecutingAuthority"), is("DVLA"));
        assertThat(actualCase.getString("caseStatus"), is("NO_PLEA_RECEIVED"));
        assertThat(actualCase.getString("_case_type"), is("PROSECUTION"));
        assertThat(actualCase.getBoolean("_is_sjp"), is(true));
        assertThat(actualCase.getBoolean("_is_magistrates"), is(false));
        assertThat(actualCase.getBoolean("_is_crown"), is(false));
        assertThat(actualCase.getBoolean("_is_charging"), is(false));
    }

    private void publishSjpCaseCreatedEvent() {
        final String payload = getPayload(PAYLOAD_PATH);
        final JsonEnvelope jsonEnvelope = buildEnvelope(payload, EVENT_NAME);
        privateEventsProducer.sendMessage(EVENT_NAME, jsonEnvelope);
    }
}
