package uk.gov.moj.sjp.it.test.ingestor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.eventstore.management.commands.IndexerCatchupCommand.INDEXER_CATCHUP;
import static uk.gov.justice.services.jmx.system.command.client.connection.JmxParametersBuilder.jmxParameters;
import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.cpp.sjp.event.CaseReceived.EVENT_NAME;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.test.ingestor.helper.ElasticSearchQueryHelper.getCaseFromElasticSearch;

import uk.gov.justice.services.jmx.system.command.client.SystemCommanderClient;
import uk.gov.justice.services.jmx.system.command.client.TestSystemCommanderClientFactory;
import uk.gov.justice.services.jmx.system.command.client.connection.JmxParameters;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.framework.util.ViewStoreCleaner;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.test.BaseIntegrationTest;

import java.io.IOException;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IndexerCatchupIT extends BaseIntegrationTest {
    public static final String CASE_ID = "7e2f843e-d639-40b3-8611-8015f3a18958";
    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
    private final TestSystemCommanderClientFactory systemCommanderClientFactory = new TestSystemCommanderClientFactory();
    private final ViewStoreCleaner viewStoreCleaner = new ViewStoreCleaner();
    private final MessageProducerClient privateEventsProducer = new MessageProducerClient();
    private ElasticSearchIndexRemoverUtil elasticSearchIndexRemoverUtil = new ElasticSearchIndexRemoverUtil();
    private static final String NATIONAL_COURT_CODE = "1080";

    private static final String HOST = getHost();
    private static final int PORT = 9990;
    private static final String CONTEXT = "sjp";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";

    @Before
    public void before() throws IOException {
        databaseCleaner.cleanEventStoreTables(CONTEXT);
        databaseCleaner.cleanSystemTables(CONTEXT);
        databaseCleaner.cleanViewStoreTables(CONTEXT, "processed_event", "stream_status", "stream_buffer");

        elasticSearchIndexRemoverUtil.deleteAndCreateCaseIndex();
    }


    @After
    public void tearDown() {
        viewStoreCleaner.cleanDataInViewStore(UUID.fromString(CASE_ID));
        privateEventsProducer.close();
    }

    @Test
    public void shouldRunIndexerCatchupAndDataShouldBeInElasticSearch() throws Exception {

        publishSjpCaseReceivedEvent();

        checkThatCaseIsinElasticSearch();

        /* Clear the data again so that we can do catchup */
        elasticSearchIndexRemoverUtil.deleteAndCreateCaseIndex();
        databaseCleaner.cleanViewStoreTables(CONTEXT, "processed_event", "stream_status", "stream_buffer");

        runIndexerCatchup();

        checkThatCaseIsinElasticSearch();
    }

    private void runIndexerCatchup() {
        final JmxParameters jmxParameters = jmxParameters()
                .withContextName(CONTEXT)
                .withHost(HOST)
                .withPort(PORT)
                .withUsername(USERNAME)
                .withPassword(PASSWORD)
                .build();
        try (final SystemCommanderClient systemCommanderClient = systemCommanderClientFactory.create(jmxParameters)) {

            systemCommanderClient.getRemote(CONTEXT).call(INDEXER_CATCHUP);
        }
    }

    private void checkThatCaseIsinElasticSearch() {
        final JsonObject actualCase = getCaseFromElasticSearch(CASE_ID);
        assertThat(actualCase.getString("caseId"), is(CASE_ID));
    }

    private void publishSjpCaseReceivedEvent() {
        final CreateCase.CreateCasePayloadBuilder createCase = CreateCase.CreateCasePayloadBuilder
                .withDefaults()
                .withId(UUID.fromString(CASE_ID))
                .withProsecutingAuthority(TFL)
                .withDefendantId(randomUUID());
        stubEnforcementAreaByPostcode(createCase.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, "TestRegion");
        new EventListener()
                .subscribe(EVENT_NAME)
                .run(() -> createCaseForPayloadBuilder(createCase))
                .popEvent(EVENT_NAME);
    }
}