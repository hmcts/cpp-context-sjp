package uk.gov.moj.cpp.sjp.query.view.util;

import static com.google.common.io.Resources.getResource;
import static org.apache.commons.io.FileUtils.readFileToString;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;

import javax.json.JsonObject;
import javax.json.JsonReader;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonHelper {

    private static ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    public static JsonObject readJsonFromFile(final String resource)  {
        final String fileContent = readResourceContent(resource);

        try (JsonReader reader = createReader(new StringReader(fileContent))) {
            return reader.readObject();
        }
    }

    private static String readResourceContent(final String resource)  {
        try {
            return readFileToString(new File(getResource(resource).toURI()));
        } catch (IOException e) {
            throw new RuntimeException("Cannot read from file", e);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Cannot convert to URI", e);
        }
    }
    public static <T> T readJsonFromFile(final String resource, Class<T> clazz) {
        try {
            return objectMapper.readValue(readResourceContent(resource), clazz);
        } catch (IOException e) {
            throw new RuntimeException("Cannot read from file", e);
        }
    }
}
