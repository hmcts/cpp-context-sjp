package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

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
public class EnterpriseIdAssociatedProcessorTest {

    private static final String PUBLIC_EVENT_NAME = "public.sjp.enterprise-id-associated";
    private static final String PRIVATE_EVENT_NAME = "sjp.events.enterprise-id-associated";

    @Mock
    private Sender sender;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @InjectMocks
    private EnterpriseIdAssociatedProcessor listener;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void shouldSendPublicMessage() {
        final String caseId = randomUUID().toString();
        final String enterpriseId = randomUUID().toString();

        final JsonEnvelope event = buildEnveloper(caseId, enterpriseId);

        listener.enterpriseIdAssociated(event);

        verify(enveloper).withMetadataFrom(eq(event), eq(PUBLIC_EVENT_NAME));
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final JsonEnvelope actualEvent = envelopeCaptor.getValue();
        final JsonObject actualPayload = actualEvent.payloadAsJsonObject();

        assertThat(actualEvent.metadata().name(), is(PUBLIC_EVENT_NAME));
        assertThat(actualPayload.getString("caseId"), is(caseId));
        assertThat(actualPayload.getString("enterpriseId"), is(enterpriseId));
    }

    private JsonEnvelope buildEnveloper(final String caseId, final String enterpriseId) {
        final JsonObject payload = createObjectBuilder()
                .add("caseId", caseId)
                .add("enterpriseId", enterpriseId)
                .build();

        return envelopeFrom(metadataWithRandomUUID(PRIVATE_EVENT_NAME), payload);
    }
}