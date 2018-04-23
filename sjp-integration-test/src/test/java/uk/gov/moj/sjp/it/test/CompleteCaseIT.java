package uk.gov.moj.sjp.it.test;

import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.producer.CompleteCaseProducer;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

/**
 * Integration test for complete case.
 */
public class CompleteCaseIT extends BaseIntegrationTest {

    private UUID caseId;

    @Before
    public void setUp()  {
        CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);

        caseId = createCasePayloadBuilder.getId();
    }

    @Test
    public void completeCase() {
        CompleteCaseProducer completeCaseProducer = new CompleteCaseProducer(caseId);
        completeCaseProducer.completeCase();
        completeCaseProducer.verifyInActiveMQ();
        completeCaseProducer.assertCaseCompleted();
    }

}
