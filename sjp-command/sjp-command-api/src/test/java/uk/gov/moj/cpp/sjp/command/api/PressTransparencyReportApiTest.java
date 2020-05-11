package uk.gov.moj.cpp.sjp.command.api;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PressTransparencyReportApiTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private PressTransparencyReportApi pressTransparencyReportApi;

    @Captor
    private ArgumentCaptor<Envelope> envelopeCaptor;

    @Test
    public void shouldRequestPressTransparencyReport() {
        final JsonEnvelope command = envelope().with(metadataWithRandomUUID("sjp.request-press-transparency-report")).build();

        pressTransparencyReportApi.requestTransparencyReport(command);
        verify(sender).send(envelopeCaptor.capture());

        final Envelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata().name(), is("sjp.command.request-press-transparency-report"));
        assertThat(newCommand.metadata().id(), is(command.metadata().id()));
    }
}