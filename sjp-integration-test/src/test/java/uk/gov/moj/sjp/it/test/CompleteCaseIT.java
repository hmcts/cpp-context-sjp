package uk.gov.moj.sjp.it.test;

import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.producer.CompleteCaseProducer;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

/**
 * Integration test for complete case.
 */
public class CompleteCaseIT extends BaseIntegrationTest {

    private UUID caseId = UUID.randomUUID();

    @Before
    public void setUp() {
        final CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults().withId(caseId);

        new EventListener()
                .subscribe(CaseMarkedReadyForDecision.EVENT_NAME)
                .run(() -> CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder));
    }

    @Test
    public void completeCase() {
        final CompleteCaseProducer completeCaseProducer = new CompleteCaseProducer(caseId);
        new EventListener()
                .subscribe(CaseCompleted.EVENT_NAME)
                .run(() -> completeCaseProducer.completeCase());

        completeCaseProducer.assertCaseCompleted();
    }

}
