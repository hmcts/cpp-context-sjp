package uk.gov.moj.cpp.sjp.command.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.command.api.UpdatePleaApi;
import uk.gov.moj.cpp.sjp.command.api.validator.UpdatePleaModel;
import uk.gov.moj.cpp.sjp.command.api.validator.UpdatePleaValidator;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.BadRequestException;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpdatePleaApiTest {

    private static final String UPDATE_PLEA_COMMAND_NAME = "sjp.update-plea";
    private static final String CONTROLLER_UPDATE_PLEA_COMMAND_NAME = "sjp.command.update-plea";

    private static final String CANCEL_PLEA_COMMAND_NAME = "sjp.cancel-plea";
    private static final String CONTROLLER_CANCEL_PLEA_COMMAND_NAME = "sjp.command.cancel-plea";

    @Mock
    private Sender sender;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Spy
    private ObjectToJsonValueConverter objectToJsonValueConverter =
            new ObjectToJsonValueConverter(new ObjectMapperProducer().objectMapper());

    @Mock
    private UpdatePleaValidator updatePleaValidator;

    @InjectMocks
    private UpdatePleaApi updatePleaApi;


    @Test
    public void shouldUpdatePlea() {
        when(updatePleaValidator.validate(any())).thenReturn(Collections.emptyMap());
        final JsonEnvelope command = envelope().with(metadataWithRandomUUID(UPDATE_PLEA_COMMAND_NAME)).build();

        updatePleaApi.updatePlea(command);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata(), withMetadataEnvelopedFrom(command).withName(CONTROLLER_UPDATE_PLEA_COMMAND_NAME));
        assertThat(newCommand.payloadAsJsonObject(), equalTo(command.payloadAsJsonObject()));

    }

    @Test
    public void shouldCancelPlea() {
        final JsonEnvelope command = envelope().with(metadataWithRandomUUID(CANCEL_PLEA_COMMAND_NAME)).build();

        updatePleaApi.cancelPlea(command);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata(), withMetadataEnvelopedFrom(command).withName(CONTROLLER_CANCEL_PLEA_COMMAND_NAME));
        assertThat(newCommand.payloadAsJsonObject(), equalTo(command.payloadAsJsonObject()));
    }

    @Test
    public void shouldFail_whenInterpreterRequiredButNoLanguageSet() {
        final JsonEnvelope command = envelope().with(metadataWithRandomUUID(CANCEL_PLEA_COMMAND_NAME)).build();
        final Map<String, List<String>> map = new HashMap<>();
        map.put("interpreter", Lists.newArrayList("Interpreter cannot be empty"));
        when(updatePleaValidator.validate(any(UpdatePleaModel.class))).thenReturn(map);

        try {
            updatePleaApi.updatePlea(command);
            fail();
        } catch (final BadRequestException e) {
            assertEquals("{\"interpreter\":[\"Interpreter cannot be empty\"]}", e.getMessage());
        }
    }
}
