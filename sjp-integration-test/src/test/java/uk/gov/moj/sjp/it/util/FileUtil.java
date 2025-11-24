package uk.gov.moj.sjp.it.util;

import static java.nio.charset.Charset.defaultCharset;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.InputStream;
import java.util.Map;

import javax.json.JsonObject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

    public static String getPayload(final String path) {
        String fileContents = null;
        try (final InputStream inputStream = FileUtil.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(inputStream, notNullValue());
            fileContents = IOUtils.toString(inputStream, defaultCharset());
        } catch (final Exception e) {
            LOGGER.error("Error consuming file from location {}", path, e);
            fail("Error consuming file from location " + path);
        }
        return fileContents;
    }

    public static InputStream getPayloadAsInputStream(final String path) {
        final InputStream inputStream = FileUtil.class.getClassLoader().getResourceAsStream(path);
        assertThat(inputStream, notNullValue());
        return inputStream;
    }

    public static String getFileContent(final String path, final Map<String, Object> namedPlaceholders) {
        return new StrSubstitutor(namedPlaceholders).replace(getPayload(path));
    }

    public static JsonObject getFileContentAsJson(final String path, final Map<String, Object> namedPlaceholders) {
        return JsonHelper.getJsonObject(getFileContent(path, namedPlaceholders));
    }

    public static JsonObject getFileContentAsJson(final String path) {
        return JsonHelper.getJsonObject(getFileContent(path, emptyMap()));
    }
}
