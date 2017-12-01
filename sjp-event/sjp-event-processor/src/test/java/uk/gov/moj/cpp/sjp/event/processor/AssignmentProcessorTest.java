package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.event.processor.AssignmentProcessor.ASSIGNMENT_CONTEXT_ASSIGNMENT_CREATED;
import static uk.gov.moj.cpp.sjp.event.processor.AssignmentProcessor.ASSIGNMENT_CONTEXT_ASSIGNMENT_DELETED;
import static uk.gov.moj.cpp.sjp.event.processor.AssignmentProcessor.SJP_COMMAND_HANDLER_ASSIGNMENT_CREATED;
import static uk.gov.moj.cpp.sjp.event.processor.AssignmentProcessor.SJP_COMMAND_HANDLER_ASSIGNMENT_DELETED;
import static uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants.ASSIGNMENT_DOMAIN_OBJECT_ID;
import static uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants.ASSIGNMENT_NATURE_TYPE;
import static uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants.CASE_ASSIGNMENT_TYPE;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;
import uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants;

import java.util.UUID;
import java.util.function.Consumer;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AssignmentProcessorTest {

    private static final String CASE_ID = UUID.randomUUID().toString();
    private static final CaseAssignmentType TYPE = CaseAssignmentType.MAGISTRATE_DECISION;

    @InjectMocks
    private AssignmentProcessor assignmentProcessor;
    @Mock
    private Sender sender;
    @Mock
    private JsonObject jsonObject;
    @Mock
    private JsonEnvelope messageToPublish;
    @Captor
    private ArgumentCaptor<JsonEnvelope> captor;
    @Spy
    private Enveloper envelopers = createEnveloper();

    @Test
    public void shouldHandleCaseAssignmentCreated() throws Exception {
        shouldHandleCaseAssignment(
                ASSIGNMENT_CONTEXT_ASSIGNMENT_CREATED,
                SJP_COMMAND_HANDLER_ASSIGNMENT_CREATED,
                assignmentProcessor::handleAssignmentCreated);
    }

    @Test
    public void shouldHandleCaseAssignmentDeleted() throws Exception {
        shouldHandleCaseAssignment(
                ASSIGNMENT_CONTEXT_ASSIGNMENT_DELETED,
                SJP_COMMAND_HANDLER_ASSIGNMENT_DELETED,
                assignmentProcessor::handleAssignmentDeleted);
    }

    private void shouldHandleCaseAssignment(final String assignmentEventName, final String structureHandlerName, final Consumer<JsonEnvelope> consumer) {
        // given
        final JsonEnvelope event = EnvelopeFactory.createEnvelope(
                assignmentEventName,
                Json.createObjectBuilder()
                        .add(ASSIGNMENT_DOMAIN_OBJECT_ID, CASE_ID)
                        .add(ASSIGNMENT_NATURE_TYPE, TYPE.toString()).build());

        // when
        consumer.accept(event);

        // then
        verify(sender).send(captor.capture());
        assertThat(captor.getValue(), jsonEnvelope(
                metadata().withName(structureHandlerName),
                payloadIsJson(allOf(
                        withJsonPath("$." + EventProcessorConstants.CASE_ID, equalTo(CASE_ID)),
                        withJsonPath("$." + CASE_ASSIGNMENT_TYPE, equalTo(TYPE.toString()))))
        ));
    }

    @Test
    public void shouldIgnoreCaseAssignmentCreatedWithOtherType() throws Exception {
        shouldIgnoreCaseAssignment(
                ASSIGNMENT_CONTEXT_ASSIGNMENT_CREATED,
                assignmentProcessor::handleAssignmentCreated);
    }

    @Test
    public void shouldIgnoreCaseAssignmentDeletedWithOtherType() throws Exception {
        shouldIgnoreCaseAssignment(
                ASSIGNMENT_CONTEXT_ASSIGNMENT_DELETED,
                assignmentProcessor::handleAssignmentDeleted);
    }

    private void shouldIgnoreCaseAssignment(final String assignmentEventName, final Consumer<JsonEnvelope> consumer) {
        // given
        final JsonEnvelope event = EnvelopeFactory.createEnvelope(
                assignmentEventName,
                Json.createObjectBuilder()
                        .add(ASSIGNMENT_DOMAIN_OBJECT_ID, CASE_ID)
                        .add(ASSIGNMENT_NATURE_TYPE, "some random type").build());

        // when
        consumer.accept(event);

        // then
        verifyZeroInteractions(sender);
    }
}