package uk.gov.moj.sjp.it.test;

import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseReopenedInLibraHelper;
import uk.gov.moj.sjp.it.helper.CaseReopenedInLibraHelper.MarkCaseReopenedInLibraHelper;
import uk.gov.moj.sjp.it.helper.CaseReopenedInLibraHelper.UndoCaseReopenedInLibraHelper;
import uk.gov.moj.sjp.it.helper.CaseReopenedInLibraHelper.UpdateCaseReopenedInLibraHelper;
import uk.gov.moj.sjp.it.helper.DecisionHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import org.junit.Before;
import org.junit.Test;

public class CaseReopenedInLibraIT extends BaseIntegrationTest {

    private SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();

    @Before
    public void setUp() throws Exception {
        databaseCleaner.cleanAll();

        new EventListener()
                .subscribe(CaseMarkedReadyForDecision.EVENT_NAME)
                .run(() -> CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder));

        DecisionHelper.saveDefaultDecision(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceIds());
    }

    @Test
    public void shouldMarkCaseReopenedInLibra() {
        try (final CaseReopenedInLibraHelper mark = new MarkCaseReopenedInLibraHelper(createCasePayloadBuilder.getId())) {
            test(mark);
        }
    }

    @Test
    public void shouldUpdateCaseReopenedInLibra() {
        shouldMarkCaseReopenedInLibra();

        try (final CaseReopenedInLibraHelper update = new UpdateCaseReopenedInLibraHelper(createCasePayloadBuilder.getId())) {
            test(update);
        }
    }

    @Test
    public void shouldUndoCaseReopenedInLibra() {
        shouldMarkCaseReopenedInLibra();

        try (final CaseReopenedInLibraHelper undo = new UndoCaseReopenedInLibraHelper(createCasePayloadBuilder.getId())) {
            test(undo);
        }
    }

    private void test(CaseReopenedInLibraHelper caseReopenedInLibraHelper) {
        caseReopenedInLibraHelper.call();

        caseReopenedInLibraHelper.assertCaseReopenedDetailsSet();
        caseReopenedInLibraHelper.verifyEventInActiveMQ();
        caseReopenedInLibraHelper.verifyEventInPublicTopic();
    }
}
