package uk.gov.moj.cpp.sjp.command.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.command.api.RequestWithdrawalAllOffencesApi;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RequestWithdrawalAllOffencesApiTest {

    private static final String COMMAND_NAME = "sjp.request-withdrawal-all-offences";
    private static final String NEW_COMMAND_NAME = "sjp.command.request-withdrawal-all-offences";

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @InjectMocks
    private RequestWithdrawalAllOffencesApi requestWithdrawalAllOffencesApi;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void shouldHandleCommand() {
        assertThat(RequestWithdrawalAllOffencesApi.class, isHandlerClass(COMMAND_API)
                .with(method("requestWithdrawalAllOffences").thatHandles(COMMAND_NAME)));
    }

    @Test
    public void shouldRenameCommand() {
        final JsonEnvelope command = envelope().with(metadataWithRandomUUID(COMMAND_NAME)).build();

        requestWithdrawalAllOffencesApi.requestWithdrawalAllOffences(command);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata(), withMetadataEnvelopedFrom(command).withName(NEW_COMMAND_NAME));
        assertThat(newCommand.payloadAsJsonObject(), equalTo(command.payloadAsJsonObject()));
    }
}
