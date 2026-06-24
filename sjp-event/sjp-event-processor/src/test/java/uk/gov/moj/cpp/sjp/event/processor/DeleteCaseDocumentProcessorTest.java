package uk.gov.moj.cpp.sjp.event.processor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;

import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeleteCaseDocumentProcessorTest {
    @Mock
    private Sender sender;

    @Mock
    private Enveloper enveloper;

    @InjectMocks
    private DeleteCaseDocumentProcessor deleteCaseDocumentProcessor;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @Test
    public void shouldPublishPublicEventWhenDeleteCaseDocumentRequestRejected() {
        final UUID caseId = UUID.randomUUID();
        final UUID materialId = UUID.randomUUID();
        final JsonEnvelope privateEvent = envelope().with(metadataWithRandomUUID("sjp.events.delete-case-document-request-rejected"))
                .withPayloadOf(caseId.toString(), "caseId")
                .withPayloadOf(materialId.toString(), "materialId")
                .withPayloadOf("CASE_IN_SESSION", "reason")
                .build();

        deleteCaseDocumentProcessor.deleteCaseDocumentRejected(privateEvent);

        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope publicEvent = envelopeCaptor.getValue();
        assertThat(publicEvent.metadata().name(), is("public.sjp.delete-case-document-request-rejected"));
        assertThat(publicEvent.payload(), equalTo(privateEvent.payloadAsJsonObject()));
    }

    @Test
    public void shouldPublishPublicEventWhenDeleteCaseDocumentRequestAccepted() {
        final UUID caseId = UUID.randomUUID();
        final UUID materialId = UUID.randomUUID();
        final UUID documentId = UUID.randomUUID();
        final JsonEnvelope privateEvent = envelope().with(metadataWithRandomUUID("sjp.events.case-document-deleted"))
                .withPayloadOf(caseId.toString(), "caseId")
                .withPayloadOf(createObjectBuilder()
                        .add("id", documentId.toString())
                        .add("materialId", materialId.toString()).build(), "caseDocument")
                .build();

        final JsonObject expectedPublicEvent = createObjectBuilder().add("caseId", caseId.toString()).add("documentId", documentId.toString()).build();

        deleteCaseDocumentProcessor.processCaseDocumentDeleted(privateEvent);

        verify(sender, times(2)).send(envelopeCaptor.capture());

        List<DefaultEnvelope> values = envelopeCaptor.getAllValues();
        assertThat(values.get(0).metadata().name(), is("material.command.delete-material"));
        assertThat(stringToJsonObjectConverter.convert(values.get(0).payload().toString()).getString("materialId"), equalTo(materialId.toString()));

        assertThat(values.get(1).metadata().name(), is("public.sjp.delete-case-document-request-accepted"));
        assertThat(values.get(1).payload(), equalTo(expectedPublicEvent));
    }

}