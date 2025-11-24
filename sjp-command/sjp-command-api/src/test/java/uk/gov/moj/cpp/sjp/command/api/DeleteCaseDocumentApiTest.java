package uk.gov.moj.cpp.sjp.command.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeleteCaseDocumentApiTest {

    private static final String DELETE_COMMAND_NAME = "sjp.delete-case-document";
    private static final String DELETE_NEW_COMMAND_NAME = "sjp.command.delete-case-document";

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @InjectMocks
    private DeleteCaseDocumentApi deleteCaseDocumentApi;

    @Captor
    private ArgumentCaptor<Envelope> envelopeCaptor;

    @Test
    public void shouldHandleCommand() {
        assertThat(DeleteCaseDocumentApi.class, isHandlerClass(COMMAND_API).with(method("deleteCaseDocument").thatHandles(DELETE_COMMAND_NAME)));
    }

    @Test
    public void shouldHandleDeleteCaseDocumentCommand() {
        final JsonEnvelope command = envelope().with(metadataWithRandomUUID(DELETE_COMMAND_NAME)).build();
        deleteCaseDocumentApi.deleteCaseDocument(command);

        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope envelope = (DefaultEnvelope) envelopeCaptor.getValue();
        assertThat(envelope.metadata().name(), is(DELETE_NEW_COMMAND_NAME));
        assertThat(envelope.payload(), equalTo(command.payloadAsJsonObject()));
    }
}
