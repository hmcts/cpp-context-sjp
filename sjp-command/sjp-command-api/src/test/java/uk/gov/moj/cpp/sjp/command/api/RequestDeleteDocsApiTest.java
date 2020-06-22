package uk.gov.moj.cpp.sjp.command.api;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RequestDeleteDocsApiTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private RequestDeleteDocsApi requestDeleteDocsApi;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void shouldHandleRequestDeleteDocsCommands() {
        assertThat(RequestDeleteDocsApi.class, isHandlerClass(COMMAND_API)
                .with(method("requestDeleteDocs")
                .thatHandles("sjp.request-delete-docs")));
    }

    @Test
    public void shouldRequestDeleteDocs() {
        final JsonEnvelope commandEnvelope = envelope().
                with(metadataWithRandomUUID("sjp.request-delete-docs"))
                .withPayloadFrom(createObjectBuilder()
                        .add("caseId", randomUUID().toString())
                        .build()
                )
                .build();

        requestDeleteDocsApi.requestDeleteDocs(commandEnvelope);

        verify(sender).send(envelopeCaptor.capture());
        final JsonEnvelope sentCommandEnvelope = envelopeCaptor.getValue();
        assertThat(sentCommandEnvelope.metadata().name(), is("sjp.command.request-delete-docs"));
        assertThat(commandEnvelope.payloadAsJsonObject(), equalTo(sentCommandEnvelope.payloadAsJsonObject()));
    }

}
