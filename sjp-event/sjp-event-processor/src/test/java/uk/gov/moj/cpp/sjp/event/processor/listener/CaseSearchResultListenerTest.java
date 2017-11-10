package uk.gov.moj.cpp.sjp.event.processor.listener;

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
import uk.gov.moj.cpp.sjp.event.processor.listener.CaseSearchResultListener;

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
public class CaseSearchResultListenerTest {

    private static final String PUBLIC_EVENT_NAME = "public.structure.person-info-added";
    private static final String PRIVATE_EVENT_NAME = "structure.events.person-info-added";

    @Mock
    private Sender sender;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @InjectMocks
    private CaseSearchResultListener listener;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void shouldSendPublicMessage() {
        final String caseId = randomUUID().toString();
        final String personId = randomUUID().toString();

        final JsonEnvelope event = buildEnveloper(caseId, personId);

        listener.personInfoAdded(event);

        verify(enveloper).withMetadataFrom(eq(event), eq(PUBLIC_EVENT_NAME));
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final JsonEnvelope actualEvent = envelopeCaptor.getValue();
        final JsonObject actualPayload = actualEvent.payloadAsJsonObject();

        assertThat(actualEvent.metadata().name(), is(PUBLIC_EVENT_NAME));
        assertThat(actualPayload.getString("caseId"), is(caseId));
        assertThat(actualPayload.getString("personId"), is(personId));
    }

    private JsonEnvelope buildEnveloper(final String caseId, final String personId) {
        final JsonObject payload = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("caseId", caseId)
                .add("personId", personId)
                .add("lastName", "lastName")
                .build();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID(PRIVATE_EVENT_NAME), payload);

        return event;
    }
}