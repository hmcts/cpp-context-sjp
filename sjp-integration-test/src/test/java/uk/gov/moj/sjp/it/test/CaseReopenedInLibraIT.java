package uk.gov.moj.sjp.it.test;

import uk.gov.moj.sjp.it.helper.CaseReopenedInLibraHelper;
import uk.gov.moj.sjp.it.helper.CaseReopenedInLibraHelper.MarkCaseReopenedInLibraHelper;
import uk.gov.moj.sjp.it.helper.CaseReopenedInLibraHelper.UndoCaseReopenedInLibraHelper;
import uk.gov.moj.sjp.it.helper.CaseReopenedInLibraHelper.UpdateCaseReopenedInLibraHelper;
import uk.gov.moj.sjp.it.helper.CaseSjpHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CaseReopenedInLibraIT extends BaseIntegrationTest {

    private CaseSjpHelper caseSjpHelper;

    @Before
    public void setUp() throws Exception {
        caseSjpHelper = new CaseSjpHelper();
        caseSjpHelper.createCase();
        caseSjpHelper.verifyCaseCreatedUsingId();
    }

    @After
    public void tearDown() {
        caseSjpHelper.close();
    }

    @Test
    public void shouldMarkCaseReopenedInLibra() {
        try (final CaseReopenedInLibraHelper mark = new MarkCaseReopenedInLibraHelper(caseSjpHelper)) {
            test(mark);
        }
    }

    @Test
    public void shouldUpdateCaseReopenedInLibra() {
        shouldMarkCaseReopenedInLibra();

        try (final CaseReopenedInLibraHelper update = new UpdateCaseReopenedInLibraHelper(caseSjpHelper)) {
            test(update);
        }
    }

    @Test
    public void shouldUndoCaseReopenedInLibra() {
        shouldMarkCaseReopenedInLibra();

        try (final CaseReopenedInLibraHelper undo = new UndoCaseReopenedInLibraHelper(caseSjpHelper)) {
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
