package uk.gov.moj.sjp.it.test.ingestor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TFL;
import static uk.gov.moj.cpp.sjp.event.CaseReceived.EVENT_NAME;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.test.ingestor.helper.ElasticSearchQueryHelper.getCaseFromElasticSearch;

import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.framework.util.SystemCommandInvoker;
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
    private static final String CONTEXT = "sjp";
    public static final String CASE_ID = "7e2f843e-d639-40b3-8611-8015f3a18958";
    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
    private final SystemCommandInvoker systemCommandInvoker = new SystemCommandInvoker();
    private final ViewStoreCleaner viewStoreCleaner = new ViewStoreCleaner();
    private final MessageProducerClient privateEventsProducer = new MessageProducerClient();
    private ElasticSearchIndexRemoverUtil elasticSearchIndexRemoverUtil = new ElasticSearchIndexRemoverUtil();

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

        systemCommandInvoker.invokeIndexerCatchup();

        checkThatCaseIsinElasticSearch();
    }

    private void checkThatCaseIsinElasticSearch() {
        final JsonObject actualCase = getCaseFromElasticSearch();
        assertThat(actualCase.getString("caseId"), is(CASE_ID));
    }


    private void publishSjpCaseReceivedEvent() {
        final CreateCase.CreateCasePayloadBuilder createCase = CreateCase.CreateCasePayloadBuilder
                .withDefaults()
                .withId(UUID.fromString(CASE_ID))
                .withProsecutingAuthority(TFL)
                .withDefendantId(randomUUID());

        new EventListener()
                .subscribe(EVENT_NAME)
                .run(() -> createCaseForPayloadBuilder(createCase))
                .popEvent(EVENT_NAME);
    }
}