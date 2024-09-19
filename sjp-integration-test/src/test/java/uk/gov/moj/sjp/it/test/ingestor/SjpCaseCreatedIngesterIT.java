package uk.gov.moj.sjp.it.test.ingestor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.Constants.SJP_EVENT;
import static uk.gov.moj.sjp.it.test.ingestor.helper.ElasticSearchQueryHelper.getCaseFromElasticSearch;
import static uk.gov.moj.sjp.it.test.ingestor.helper.IngesterHelper.buildEnvelope;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;
import uk.gov.moj.sjp.it.framework.util.ViewStoreCleaner;
import uk.gov.moj.sjp.it.test.BaseIntegrationTest;

import java.io.IOException;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SjpCaseCreatedIngesterIT extends BaseIntegrationTest {
    private static final String EVENT_NAME = "sjp.events.sjp-case-created";
    private static final String PAYLOAD_PATH = "stub-data/sjp.events.sjp-case-created.json";
    public static final String CASE_ID = "9f9f843e-d639-40b3-8611-8015f3a18958";

    private final MessageProducerClient privateEventsProducer = new MessageProducerClient();

    private final ViewStoreCleaner viewStoreCleaner = new ViewStoreCleaner();

    @BeforeEach
    public void setUp() throws IOException {
        privateEventsProducer.startProducer(SJP_EVENT);
        new ElasticSearchIndexRemoverUtil().deleteAndCreateCaseIndex();
    }

    @AfterEach
    public void tearDown() {
        viewStoreCleaner.cleanDataInViewStore(UUID.fromString(CASE_ID));
        privateEventsProducer.close();
    }

    @Test
    public void shouldIngestSjpCaseCreatedEvent() {
        publishSjpCaseCreatedEvent();

        final JsonObject actualCase = getCaseFromElasticSearch(CASE_ID);

        verifyCase(actualCase);
    }

    private void verifyCase(final JsonObject actualCase) {
        assertThat(actualCase.getString("caseId"), is(CASE_ID));
        assertThat(actualCase.getString("caseReference"), is("22C22222222"));
        assertThat(actualCase.getString("sjpNoticeServed"), is("2055-12-02"));
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
