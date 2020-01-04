package uk.gov.moj.sjp.it.test.ingestor;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.saveDefaultDecision;
import static uk.gov.moj.sjp.it.test.ingestor.helper.ElasticSearchQueryHelper.getCaseFromElasticSearch;

import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseReopenedInLibraHelper;
import uk.gov.moj.sjp.it.helper.CaseReopenedInLibraHelper.MarkCaseReopenedInLibraHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.test.BaseIntegrationTest;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CaseStatusChangedIT extends BaseIntegrationTest {

    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();

    @Before
    public void setUp() throws Exception {
        cleanDb();
        new ElasticSearchIndexRemoverUtil().deleteAndCreateCaseIndex();

        new EventListener()
                .subscribe(CaseMarkedReadyForDecision.EVENT_NAME)
                .run(() -> CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder));

        saveDefaultDecision(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceIds());
    }

    @After
    public void cleanDatabase() {
        cleanDb();
    }

    @Test
    public void shouldChangeCaseStatus() {
        final UUID caseId= createCasePayloadBuilder.getId();
        JsonObject outputCase = getCaseFromElasticSearch("caseStatus", "NO_PLEA_RECEIVED_READY_FOR_DECISION");
        assertThat(caseId.toString(), is(outputCase.getString("caseId")));
        assertThat(outputCase.getString("_case_type"), is("PROSECUTION"));

        try (final CaseReopenedInLibraHelper mark = new MarkCaseReopenedInLibraHelper(caseId)) {
            test(mark);
        }

        outputCase = getCaseFromElasticSearch("caseStatus", "REOPENED_IN_LIBRA");
        assertThat(caseId.toString(), is(outputCase.getString("caseId")));
        assertThat(outputCase.getString("_case_type"), is("PROSECUTION"));
    }

    private void test(CaseReopenedInLibraHelper caseReopenedInLibraHelper) {
        caseReopenedInLibraHelper.call();
        caseReopenedInLibraHelper.assertCaseReopenedDetailsSet();
        caseReopenedInLibraHelper.verifyEventInActiveMQ();
        caseReopenedInLibraHelper.verifyEventInPublicTopic();
    }
}
