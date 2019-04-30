package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

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
public class DecisionProcessorTest {

    @Mock
    private Sender sender;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @InjectMocks
    private DecisionProcessor caseDecisionListener;

    @Test
    public void shouldCompleteCase() {
        final UUID caseId = randomUUID();

        final JsonEnvelope event = createEnvelope("public.resulting.referenced-decisions-saved",
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("resultedOn", now().toString())
                        .add("sjpSessionId", randomUUID().toString())
                        .build());

        caseDecisionListener.handelDecisionsSaved(event);

        verify(sender).send(argThat(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(event).withName("sjp.command.complete-case"),
                        payloadIsJson(
                                withJsonPath("$.caseId", equalTo(caseId.toString()))
                        ))));
    }

    @Test
    public void shouldHandleDecisionSavedEvents() {
        assertThat(DecisionProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(method("handelDecisionsSaved").thatHandles("public.resulting.referenced-decisions-saved")));
    }
}
