package uk.gov.moj.sjp.it.test;

import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseReopenedInLibraHelper;
import uk.gov.moj.sjp.it.helper.CaseReopenedInLibraHelper.MarkCaseReopenedInLibraHelper;
import uk.gov.moj.sjp.it.helper.CaseReopenedInLibraHelper.UndoCaseReopenedInLibraHelper;
import uk.gov.moj.sjp.it.helper.CaseReopenedInLibraHelper.UpdateCaseReopenedInLibraHelper;

import org.junit.Before;
import org.junit.Test;

public class CaseReopenedInLibraIT extends BaseIntegrationTest {

    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    @Before
    public void setUp()  {
        createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);
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
