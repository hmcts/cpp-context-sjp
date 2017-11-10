package uk.gov.moj.sjp.it.test;

import uk.gov.moj.sjp.it.helper.CaseSjpHelper;
import uk.gov.moj.sjp.it.helper.CompleteCaseHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration test for complete case.
 */
public class CompleteCaseIT extends BaseIntegrationTest {

    private CaseSjpHelper caseHelper;

    @Before
    public void setUp() throws Exception {
        caseHelper = new CaseSjpHelper();
        caseHelper.createAndVerifyCase();
    }

    @After
    public void tearDown() {
        caseHelper.close();
    }

    @Test
    public void completeCase() {
        try (final CompleteCaseHelper completeCaseHelper = new CompleteCaseHelper(caseHelper)) {
            completeCaseHelper.completeCase();
            completeCaseHelper.verifyInActiveMQ();
            completeCaseHelper.assertCaseCompleted();
        }
    }
}
