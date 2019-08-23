package uk.gov.moj.sjp.it.test.ingestor;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.jmx.system.command.client.connection.JmxParametersBuilder.jmxParameters;
import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getJsonArray;
import static uk.gov.moj.sjp.it.test.ingestor.helper.ElasticSearchQueryHelper.getElasticSearchResponse;
import static uk.gov.moj.sjp.it.test.ingestor.helper.IngesterHelper.buildEnvelope;
import static uk.gov.moj.sjp.it.test.ingestor.helper.IngesterHelper.jsonFromString;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;

import uk.gov.justice.services.jmx.api.command.IndexerCatchupCommand;
import uk.gov.justice.services.jmx.system.command.client.SystemCommanderClient;
import uk.gov.justice.services.jmx.system.command.client.TestSystemCommanderClientFactory;
import uk.gov.justice.services.jmx.system.command.client.connection.JmxParameters;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;
import uk.gov.moj.sjp.it.framework.util.ViewStoreCleaner;
import uk.gov.moj.sjp.it.test.BaseIntegrationTest;

import java.io.IOException;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class IndexerCatchupIT extends BaseIntegrationTest {
    private static final String HOST = getHost();
    private static final int JMX_PORT = 9990;
    private static final String CONTEXT = "sjp";
    public static final String CASE_ID = "7e2f843e-d639-40b3-8611-8015f3a18958";
    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
    private final TestSystemCommanderClientFactory testSystemCommanderClientFactory = new TestSystemCommanderClientFactory();
    private static final String EVENT_NAME = "sjp.events.sjp-case-created";
    private static final String PAYLOAD_PATH = "stub-data/sjp.events.sjp-case-created.json";
    private static final String SJP_EVENT = "sjp.event";

    private final ViewStoreCleaner viewStoreCleaner = new ViewStoreCleaner();
    private final MessageProducerClient privateEventsProducer = new MessageProducerClient();
    private ElasticSearchIndexRemoverUtil elasticSearchIndexRemoverUtil = new ElasticSearchIndexRemoverUtil();

    @Before
    public void before() throws IOException {
        databaseCleaner.cleanEventStoreTables(CONTEXT);
        databaseCleaner.cleanSystemTables(CONTEXT);
        databaseCleaner.cleanViewStoreTables(CONTEXT, "processed_event", "stream_status", "stream_buffer");

        privateEventsProducer.startProducer(SJP_EVENT);
        elasticSearchIndexRemoverUtil.deleteAndCreateCaseIndex();
    }

    @After
    public void tearDown() {
        viewStoreCleaner.cleanDataInViewStore(UUID.fromString(CASE_ID));
        privateEventsProducer.close();
    }

    @Test
    public void shouldRunIndexerCatchupAndDataShouldBeInElasticSearch() throws Exception {

        publishSjpCaseCreatedEvent();
        final JsonObject actualCase = jsonFromString(getJsonArray(getElasticSearchResponse().get(), "index").get().getString(0));
        assertThat(actualCase.getString("caseId"), is(CASE_ID));

        /* Clear the data again so that we can do catchup */
        elasticSearchIndexRemoverUtil.deleteAndCreateCaseIndex();
        databaseCleaner.cleanViewStoreTables(CONTEXT, "processed_event", "stream_status", "stream_buffer");

        runIndexerCatchup();

        final JsonObject actualCaseAfterCatchup = jsonFromString(getJsonArray(getElasticSearchResponse().get(), "index").get().getString(0));
        assertThat(actualCaseAfterCatchup.getString("caseId"), is(CASE_ID));
    }

    private void runIndexerCatchup() {
        final String contextName = "sjp-service";
        final JmxParameters jmxParameters = jmxParameters()
                .withContextName(contextName)
                .withHost(HOST)
                .withPort(JMX_PORT)
                .withUsername("admin")
                .withPassword("admin")
                .build();

        try (final SystemCommanderClient systemCommanderClient = testSystemCommanderClientFactory.create(jmxParameters)) {
            systemCommanderClient.getRemote(contextName).call(new IndexerCatchupCommand());
        }
    }

    private void publishSjpCaseCreatedEvent() {
        final String payload = getPayload(PAYLOAD_PATH);
        final JsonEnvelope jsonEnvelope = buildEnvelope(payload, EVENT_NAME);
        privateEventsProducer.sendMessage(EVENT_NAME, jsonEnvelope);
    }
}