package uk.gov.moj.cpp.sjp.command.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SessionApiTest {

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @InjectMocks
    private SessionApi sessionApi;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeCaptor;

    @Test
    public void shouldRenameAndForwardStartSessionCommand() {
        final JsonEnvelope startSessionCommand = envelope().with(metadataWithRandomUUID("sjp.start-session")).build();

        sessionApi.startSession(startSessionCommand);

        verify(sender).send(jsonEnvelopeCaptor.capture());

        final JsonEnvelope renamedStartSessionCommand = jsonEnvelopeCaptor.getValue();
        assertThat(renamedStartSessionCommand.metadata(), withMetadataEnvelopedFrom(startSessionCommand).withName("sjp.command.start-session"));
        assertThat(renamedStartSessionCommand.payloadAsJsonObject(), equalTo(startSessionCommand.payloadAsJsonObject()));
    }
}
