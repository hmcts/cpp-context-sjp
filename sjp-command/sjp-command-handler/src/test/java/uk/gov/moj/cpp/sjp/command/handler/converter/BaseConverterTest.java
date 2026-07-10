package uk.gov.moj.cpp.sjp.command.handler.converter;

import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.IOException;
import java.io.InputStream;

import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public abstract class BaseConverterTest {
    @Mock
    protected JsonEnvelope command;

    @Mock
    protected JsonObject payload;

    protected void givenPayload(String filePath) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(filePath)) {
            JsonReader jsonReader = createReader(inputStream);
            payload = jsonReader.readObject();
        }
        when(command.payloadAsJsonObject()).thenReturn(payload);
    }
}
