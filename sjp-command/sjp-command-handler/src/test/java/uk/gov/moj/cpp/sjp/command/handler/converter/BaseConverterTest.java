package uk.gov.moj.cpp.sjp.command.handler.converter;

import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.IOException;
import java.io.InputStream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public abstract class BaseConverterTest {
    @Mock
    protected JsonEnvelope command;

    @Mock
    protected JsonObject payload;

    protected void givenPayload(String filePath) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(filePath)) {
            JsonReader jsonReader = Json.createReader(inputStream);
            payload = jsonReader.readObject();
        }
        when(command.payloadAsJsonObject()).thenReturn(payload);
    }
}
