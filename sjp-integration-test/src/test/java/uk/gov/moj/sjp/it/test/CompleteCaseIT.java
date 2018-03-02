package uk.gov.moj.sjp.it.test;

import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CompleteCaseHelper;

import org.junit.Before;
import org.junit.Test;

/**
 * Integration test for complete case.
 */
public class CompleteCaseIT extends BaseIntegrationTest {

    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    @Before
    public void setUp()  {
        createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);
    }

    @Test
    public void completeCase() {
        try (final CompleteCaseHelper completeCaseHelper = new CompleteCaseHelper(createCasePayloadBuilder.getId())) {
            completeCaseHelper.completeCase();
            completeCaseHelper.verifyInActiveMQ();
            completeCaseHelper.assertCaseCompleted();
        }
    }
}
