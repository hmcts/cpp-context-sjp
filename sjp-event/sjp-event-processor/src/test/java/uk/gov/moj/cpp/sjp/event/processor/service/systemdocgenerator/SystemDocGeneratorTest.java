package uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.net.URI;
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
public class SystemDocGeneratorTest {

    @Mock
    private Sender sender;
    @InjectMocks
    private SystemDocGenerator systemDocGenerator;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> argumentCaptor;

    @Test
    public void shouldSendGenerateDocumentCommandWithPayloadSourceUri() {
        final String sourceCorrelationId = randomUUID().toString();
        final UUID payloadFileServiceId = randomUUID();
        final URI payloadSourceUri = URI.create("https://devstoreaccount1.blob.core.windows.net/sjp-files/published/sdg-payloads/" + payloadFileServiceId + "?sv=2021");
        final DocumentGenerationRequest request = new DocumentGenerationRequest(
                TemplateIdentifier.NOTIFICATION_TO_DVLA_TO_REMOVE_ENDORSEMENT,
                ConversionFormat.PDF,
                sourceCorrelationId,
                payloadFileServiceId,
                payloadSourceUri
        );

        systemDocGenerator.generateDocument(request, envelope());

        verify(sender).sendAsAdmin(argumentCaptor.capture());
        final Envelope<JsonObject> actual = argumentCaptor.getValue();
        assertThat(actual.metadata().name(), equalTo("systemdocgenerator.generate-document"));
        assertThat(actual.payload().getString("originatingSource"), equalTo("sjp"));
        assertThat(actual.payload().getString("templateIdentifier"), equalTo("NotificationToDvlaToRemoveEndorsement"));
        assertThat(actual.payload().getString("conversionFormat"), equalTo("pdf"));
        assertThat(actual.payload().getString("sourceCorrelationId"), equalTo(sourceCorrelationId));
        assertThat(actual.payload().getString("payloadFileServiceId"), equalTo(payloadFileServiceId.toString()));
        assertThat(actual.payload().getJsonArray("additionalInformation").getJsonObject(0).getString("propertyName"), equalTo("payloadSourceUri"));
        assertThat(actual.payload().getJsonArray("additionalInformation").getJsonObject(0).getString("propertyValue"), equalTo(payloadSourceUri.toString()));
    }

    private JsonEnvelope envelope() {
        return envelopeFrom(metadataWithRandomUUID("test envelope"), createObjectBuilder().build());
    }
}