package uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SystemDocGeneratorTest {

    @Mock
    private Sender sender;
    @InjectMocks
    private SystemDocGenerator systemDocGenerator;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> argumentCaptor;

    @Test
    public void originatingSourceShouldBeSjp() {
        final String sourceCorrelationId = randomUUID().toString();
        final UUID payloadFileServiceId = randomUUID();
        final DocumentGenerationRequest request = new DocumentGenerationRequest(
                TemplateIdentifier.NOTIFICATION_TO_DVLA_TO_REMOVE_ENDORSEMENT,
                ConversionFormat.PDF,
                sourceCorrelationId,
                payloadFileServiceId
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
    }

    private JsonEnvelope envelope() {
        return envelopeFrom(metadataWithRandomUUID("test envelope"), createObjectBuilder().build());
    }
}