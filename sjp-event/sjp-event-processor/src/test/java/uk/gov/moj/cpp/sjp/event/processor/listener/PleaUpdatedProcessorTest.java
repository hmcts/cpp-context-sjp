package uk.gov.moj.cpp.sjp.event.processor.listener;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.DefaultJsonEnvelope.envelope;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PleaUpdatedProcessorTest {

    @Mock
    private Sender sender;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @InjectMocks
    private PleaUpdatedProcessor listener;

    @Test
    public void shouldSendPleaUpdatedPublicEvent() {
        final String caseId = UUID.randomUUID().toString();
        final String offenceId = UUID.randomUUID().toString();

        final String plea = "GUILTY";

        final JsonEnvelope command = envelope()
                .with(metadataWithRandomUUID("sjp.events.plea-updated"))
                .withPayloadOf(caseId, "caseId")
                .withPayloadOf(offenceId, "offenceId")
                .withPayloadOf(plea, "plea")
                .build();

        listener.updatePlea(command);

        verify(sender).send(argThat(
                jsonEnvelope(
                        metadata().withName("public.structure.plea-updated"),
                        payload().isJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId)),
                                withJsonPath("$.offenceId", equalTo(offenceId)),
                                withJsonPath("$.plea", equalTo(plea))
                        )))));
    }

    @Test
    public void shouldSendPleaCancelledPublicEvent() {
        final String caseId = UUID.randomUUID().toString();
        final String offenceId = UUID.randomUUID().toString();

        final JsonEnvelope command = envelope()
                .with(metadataWithRandomUUID("sjp.events.plea-cancelled"))
                .withPayloadOf(caseId, "caseId")
                .withPayloadOf(offenceId, "offenceId")
                .build();

        listener.cancelPlea(command);

        verify(sender).send(argThat(
                jsonEnvelope(
                        metadata().withName("public.structure.plea-cancelled"),
                        payload().isJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId)),
                                withJsonPath("$.offenceId", equalTo(offenceId))
                        )))));
    }

    @Test
    public void shouldHandlePleaUpdatedEvent() {
        assertThat(PleaUpdatedProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(method("updatePlea").thatHandles("sjp.events.plea-updated")));
    }

    @Test
    public void shouldHandlePleaCancelledEvent() {
        assertThat(PleaUpdatedProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(method("cancelPlea").thatHandles("sjp.events.plea-cancelled")));
    }
}
