package uk.gov.moj.cpp.sjp.command.api;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AocpResponseTimeExpiredApiTest {

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope> dispatchedCommand;

    @InjectMocks
    private AocpResponseTimerExpiredApi aocpResponseTimerExpiredApi;

    @Test
    public void shouldHandleAocpResponseTimerExpiredRequested() {
        assertThat(AocpResponseTimerExpiredApi.class, isHandlerClass(COMMAND_API)
                .with(method("aocpResponseTimerExpiredRequested").thatHandles("sjp.expire-defendant-aocp-response-timer")));
    }

    @Test
    public void shouldHandleAndDispatchCommand() {
        final JsonEnvelope command = createPaylod();

        final JsonObject payload = command.payloadAsJsonObject();

        aocpResponseTimerExpiredApi.aocpResponseTimerExpiredRequested(command);

        verify(sender).send(dispatchedCommand.capture());
        final Envelope dispatchedEnvelope = dispatchedCommand.getValue();
        assertThat(dispatchedEnvelope.metadata().name(),
                is("sjp.command.controller.expire-defendant-aocp-response-timer"));

       final JsonObject dispatchedPayload =  (JsonObject) dispatchedEnvelope.payload();
       assertThat(dispatchedPayload.getString("caseId"), is(payload.getString("caseId")));

    }

    private JsonEnvelope createPaylod() {
        final JsonObject payload = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .build();
        return envelopeFrom(
                metadataWithRandomUUID("sjp.expire-defendant-aocp-response-timer"),
                payload);
    }

}
