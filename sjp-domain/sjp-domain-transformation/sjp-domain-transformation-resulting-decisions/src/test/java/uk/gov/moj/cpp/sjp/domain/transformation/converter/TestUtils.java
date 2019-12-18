package uk.gov.moj.cpp.sjp.domain.transformation.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.io.IOException;
import java.io.InputStream;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.lang.String.format;

public class TestUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();

    public static <T> T readJson(final String jsonFilePath, final Class<T> clazz, final Object... placeholders) {
        try (final InputStream systemResourceAsStream = getSystemResourceAsStream(jsonFilePath)) {
            return OBJECT_MAPPER.readValue(format(IOUtils.toString(systemResourceAsStream), placeholders), clazz);
        } catch (IOException e) {
            throw new RuntimeException("Resource " + jsonFilePath + " inaccessible ", e);
        }
    }

}
