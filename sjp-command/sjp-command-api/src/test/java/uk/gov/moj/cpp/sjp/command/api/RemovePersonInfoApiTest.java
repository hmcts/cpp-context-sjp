package uk.gov.moj.cpp.sjp.command.api;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.function.Function;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RemovePersonInfoApiTest {

    @Mock
    private Enveloper enveloper;

    @Mock
    private Sender sender;

    @InjectMocks
    private RemovePersonInfoApi api;

    @Mock
    private JsonEnvelope commandEnvelope;

    @Mock
    private Function<Object, JsonEnvelope> function;

    @Test
    public void shouldRemovePersonInfo() {
        final JsonEnvelope command = mock(JsonEnvelope.class);

        when(enveloper.withMetadataFrom(command, "sjp.command.remove-person-info"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(commandEnvelope);
        api.removePersonInfo(command);
        verify(sender).send(commandEnvelope);
    }
}