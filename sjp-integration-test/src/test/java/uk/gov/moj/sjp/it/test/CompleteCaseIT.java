package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertFalse;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
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

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

public class CompleteCaseIT extends BaseIntegrationTest {

    private static final UUID DISMISSED_RESULT_ID = UUID.fromString("14d66587-8fbe-424f-a369-b1144f1684e3");
    private static final UUID PAY_COSTS_RESULT_ID = UUID.fromString("b786ce8a-ce7a-4fa1-94ce-a3d9777574e4");
    private final UUID caseId = UUID.randomUUID();
    private UUID defendantId;
    private UUID offenceId;

    @Before
    public void setUp() {
        stubRemoveAssignmentCommand();

        final CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults().withId(caseId);
        stubQueryOffenceById(createCasePayloadBuilder.getOffenceBuilder().getId());
        defendantId = createCasePayloadBuilder.getDefendantBuilder().getId();
        offenceId = createCasePayloadBuilder.getOffenceBuilder().getId();

        final EventListener eventListener = new EventListener();
        eventListener
                .subscribe(CaseMarkedReadyForDecision.EVENT_NAME)
                .run(() -> CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder));

        final Optional<JsonEnvelope> jsonEnvelope = eventListener.popEvent(CaseMarkedReadyForDecision.EVENT_NAME);

        assertThat(jsonEnvelope.isPresent(), equalTo(true));//this is to ensure the subscriber didn't time out
    }

    @Test
    public void shouldCompleteCase() {
        final CompleteCaseProducer completeCaseProducer = new CompleteCaseProducer(caseId, defendantId, offenceId);
        new EventListener()
                .subscribe(CaseCompleted.EVENT_NAME)
                .run(completeCaseProducer::completeCase);

        completeCaseProducer.assertCaseCompleted();
    }

}
