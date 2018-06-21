package uk.gov.moj.sjp.it.util;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

    public static String getPayload(String path) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(path),
                    Charset.defaultCharset()
            );
        }
        catch (Exception e) {
            LOGGER.error("Error consuming file from location {}", path, e);
            fail("Error consuming file from location " + path);
        }
        return request;
    }

    public static JsonObject givenPayload(String filePath) throws IOException {
        try (InputStream inputStream = FileUtil.class.getResourceAsStream(filePath)) {
            JsonReader jsonReader = Json.createReader(inputStream);
            return jsonReader.readObject();
        }
    }
}
