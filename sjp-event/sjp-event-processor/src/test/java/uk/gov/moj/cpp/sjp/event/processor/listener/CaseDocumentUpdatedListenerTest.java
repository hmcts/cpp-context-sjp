package uk.gov.moj.cpp.sjp.event.processor.listener;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.DefaultJsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants.CASE_DOCUMENT;
import static uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants.DOCUMENT_TYPE;
import static uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants.ID;
import static uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants.MATERIAL_ID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.authorisation.client.AuthorisationServiceClient;

import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseDocumentUpdatedListenerTest {
    private static final String PUBLIC_CASE_DOCUMENT_ADDED_PUBLIC_EVENT = "public.structure.case-document-added";
    private static final String PUBLIC_CASE_DOCUMENT_ALREADY_EXISTS_PUBLIC_EVENT = "public.structure.case-document-already-exists";

    private static final String VALUE_CASE_ID = UUID.randomUUID().toString();
    private static final String VALUE_MATERIAL_ID = UUID.randomUUID().toString();
    private static final String VALUE_CASE_DOCUMENT_ID = UUID.randomUUID().toString();
    private static final String VALUE_USER_ID = UUID.randomUUID().toString();
    private static final String VALUE_DOCUMENT_TYPE = "SJPN";

    @InjectMocks
    private CaseDocumentUpdatedListener caseDocumentUpdatedListener;

    @Mock
    private Sender sender;

    @Mock
    private AuthorisationServiceClient authorisationServiceClient;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(JsonObject.class);

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void shouldRaisePublicEventForCaseDocumentAddedWithoutDocumentType() throws Exception {
        final JsonEnvelope privateEnvelope = buildCaseDocumentAddedEvent(Optional.empty(), PUBLIC_CASE_DOCUMENT_ADDED_PUBLIC_EVENT);

        caseDocumentUpdatedListener.handleCaseDocumentAdded(privateEnvelope);

        verify(enveloper).withMetadataFrom(eq(privateEnvelope), eq(PUBLIC_CASE_DOCUMENT_ADDED_PUBLIC_EVENT));
        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope publicEvent = envelopeCaptor.getValue();
        assertThat(publicEvent.metadata(), withMetadataEnvelopedFrom(privateEnvelope).withName(PUBLIC_CASE_DOCUMENT_ADDED_PUBLIC_EVENT));

        final JsonObject payload = publicEvent.payloadAsJsonObject();
        assertThat(payload.getString(CASE_ID), is(VALUE_CASE_ID));
        assertThat(payload.getString(ID), is(VALUE_CASE_DOCUMENT_ID));
        assertThat(payload.getString(MATERIAL_ID), is(VALUE_MATERIAL_ID));
        assertFalse(payload.containsKey(DOCUMENT_TYPE));
    }

    @Test
    public void shouldRaisePublicEventForCaseDocumentAddedWithDocumentType() throws Exception {
        final JsonEnvelope privateEnvelope = buildCaseDocumentAddedEvent(Optional.of(VALUE_DOCUMENT_TYPE), PUBLIC_CASE_DOCUMENT_ADDED_PUBLIC_EVENT);

        caseDocumentUpdatedListener.handleCaseDocumentAdded(privateEnvelope);

        verify(enveloper).withMetadataFrom(eq(privateEnvelope), eq(PUBLIC_CASE_DOCUMENT_ADDED_PUBLIC_EVENT));
        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope publicEvent = envelopeCaptor.getValue();
        assertThat(publicEvent.metadata(), withMetadataEnvelopedFrom(privateEnvelope).withName(PUBLIC_CASE_DOCUMENT_ADDED_PUBLIC_EVENT));

        final JsonObject payload = publicEvent.payloadAsJsonObject();
        assertThat(payload.getString(CASE_ID), is(VALUE_CASE_ID));
        assertThat(payload.getString(ID), is(VALUE_CASE_DOCUMENT_ID));
        assertThat(payload.getString(DOCUMENT_TYPE), is(VALUE_DOCUMENT_TYPE));
    }

    @Test
    public void shouldRaisePublicEventForCaseDocumentAlreadyAdded() throws Exception {
        final JsonEnvelope privateEnvelope = buildCaseDocumentAddedEvent(Optional.empty(), PUBLIC_CASE_DOCUMENT_ALREADY_EXISTS_PUBLIC_EVENT);

        caseDocumentUpdatedListener.handleDuplicateCaseDocumentAddedEvent(privateEnvelope);

        verify(enveloper).withMetadataFrom(eq(privateEnvelope), eq(PUBLIC_CASE_DOCUMENT_ALREADY_EXISTS_PUBLIC_EVENT));
        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope publicEvent = envelopeCaptor.getValue();
        assertThat(publicEvent.metadata(), withMetadataEnvelopedFrom(privateEnvelope).withName(PUBLIC_CASE_DOCUMENT_ALREADY_EXISTS_PUBLIC_EVENT));

        final JsonObject payload = publicEvent.payloadAsJsonObject();
        assertThat(payload.getString(CASE_ID), is(VALUE_CASE_ID));
        assertThat(payload.getString(ID), is(VALUE_CASE_DOCUMENT_ID));
        assertThat(payload.getString(MATERIAL_ID), is(VALUE_MATERIAL_ID));
    }

    private JsonEnvelope buildCaseDocumentAddedEvent(final Optional<String> documentType, String eventName) {
        final JsonObjectBuilder documentBuilder = Json.createObjectBuilder()
                .add(ID, VALUE_CASE_DOCUMENT_ID)
                .add(MATERIAL_ID, VALUE_MATERIAL_ID);

        if (documentType.isPresent()) {
            documentBuilder.add(DOCUMENT_TYPE, VALUE_DOCUMENT_TYPE);
        }

        return envelopeFrom(
                metadataWithRandomUUID(eventName)
                        .withUserId(VALUE_USER_ID),
                createObjectBuilder()
                        .add(CASE_ID, VALUE_CASE_ID)
                        .add(CASE_DOCUMENT, documentBuilder.build())
                        .build());
    }
}
