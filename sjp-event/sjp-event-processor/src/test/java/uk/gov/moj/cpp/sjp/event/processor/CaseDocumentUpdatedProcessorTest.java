package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_DOCUMENT;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.DOCUMENT_TYPE;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.MATERIAL_ID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import java.util.Optional;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseDocumentUpdatedProcessorTest {
    private static final String PUBLIC_CASE_DOCUMENT_ADDED_PUBLIC_EVENT = "public.sjp.case-document-added";
    private static final String PUBLIC_CASE_DOCUMENT_ALREADY_EXISTS_PUBLIC_EVENT = "public.sjp.case-document-already-exists";

    private static final String VALUE_CASE_ID = UUID.randomUUID().toString();
    private static final String VALUE_MATERIAL_ID = UUID.randomUUID().toString();
    private static final String VALUE_CASE_DOCUMENT_ID = UUID.randomUUID().toString();
    private static final String VALUE_USER_ID = UUID.randomUUID().toString();
    private static final String VALUE_DOCUMENT_TYPE = "SJPN";

    @InjectMocks
    private CaseDocumentUpdatedProcessor caseDocumentUpdatedListener;

    @Mock
    private Sender sender;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(JsonObject.class);

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void shouldRaisePublicEventForCaseDocumentAddedWithoutDocumentType() {
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
    public void shouldRaisePublicEventForCaseDocumentAddedWithDocumentType() {
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
    public void shouldRaisePublicEventForCaseDocumentAlreadyAdded() {
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
        final JsonObjectBuilder documentBuilder = JsonObjects.createObjectBuilder()
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
