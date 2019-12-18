package uk.gov.moj.cpp.sjp.command.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.sjp.command.api.validator.SetPleasModel;
import uk.gov.moj.cpp.sjp.command.api.validator.SetPleasValidator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SetPleasApiTest {

    private static final String COMMAND_NAME = "sjp.set-pleas";
    private static final String NEW_COMMAND_NAME = "sjp.command.set-pleas";


    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @Mock
    private SetPleasValidator setPleasValidator;

    @Spy
    private ObjectToJsonValueConverter objectToJsonValueConverter =
            new ObjectToJsonValueConverter(new ObjectMapperProducer().objectMapper());

    @InjectMocks
    private SetPleasApi setPleasApi;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void shouldHandleCommand(){
        assertThat(SetPleasApi.class, isHandlerClass(COMMAND_API)
                .with(method("setPleas").thatHandles(COMMAND_NAME)));
    }

    @Test
    public void shouldRenameCommand(){
        final JsonEnvelope command = envelope().with(MetadataBuilderFactory.metadataWithRandomUUID(COMMAND_NAME)).build();

        setPleasApi.setPleas(command);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata(), withMetadataEnvelopedFrom(command).withName(NEW_COMMAND_NAME));
        assertThat(newCommand.payloadAsJsonObject(), equalTo(command.payloadAsJsonObject()));
    }

    @Test(expected = BadRequestException.class)
    public void shouldReturnBadRequestOnValidationErrors(){
        final JsonEnvelope command = envelope().with(MetadataBuilderFactory.metadataWithRandomUUID(COMMAND_NAME)).build();
        final Map<String, List<String>> validationErrors = new HashMap<>();
        final List<String> interpreterErrors = Arrays.asList("interpreterRequired", "interpreterLanguage");
        validationErrors.put("interpreter", interpreterErrors);

        when(setPleasValidator.validate(any(SetPleasModel.class))).
                thenReturn(validationErrors);

        setPleasApi.setPleas(command);
    }
}
