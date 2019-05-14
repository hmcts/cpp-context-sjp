package uk.gov.moj.sjp.it.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubRemoveAssignmentCommand;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryOffenceById;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.producer.CompleteCaseProducer;

import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class CompleteCaseIT extends BaseIntegrationTest {

    private UUID caseId = UUID.randomUUID();
    private UUID offenceId = UUID.randomUUID();

    @Before
    public void setUp() {
        stubRemoveAssignmentCommand();
        stubQueryOffenceById(offenceId);
        final CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults().withId(caseId);

        final EventListener eventListener = new EventListener();
        eventListener
                .subscribe(CaseMarkedReadyForDecision.EVENT_NAME)
                .run(() -> CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder));

        final Optional<JsonEnvelope> jsonEnvelope = eventListener.popEvent(CaseMarkedReadyForDecision.EVENT_NAME);

        assertThat(jsonEnvelope.isPresent(), equalTo(true));//this is to ensure the subscriber didn't time out
    }

    @Test
    public void shouldCompleteCase() {
        final CompleteCaseProducer completeCaseProducer = new CompleteCaseProducer(caseId);
        new EventListener()
                .subscribe(CaseCompleted.EVENT_NAME)
                .run(completeCaseProducer::completeCase);

        completeCaseProducer.assertCaseCompleted();
    }

    @Test
    public void shouldCompleteCaseAndGenerateResultsEvent() {
        final CompleteCaseProducer completeCaseProducer = new CompleteCaseProducer(caseId);
        new EventListener()
                .subscribe("public.sjp.case-resulted")
                .run(completeCaseProducer::completeCaseResults);

        completeCaseProducer.assertCaseResults();
    }

}
