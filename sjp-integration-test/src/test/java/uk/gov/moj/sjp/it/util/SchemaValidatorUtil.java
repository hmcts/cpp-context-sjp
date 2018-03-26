package uk.gov.moj.sjp.it.util;

import static org.junit.Assert.fail;

import java.io.InputStream;
import java.net.URL;

import com.google.common.io.Resources;
import com.jayway.restassured.path.json.JsonPath;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaValidatorUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaValidatorUtil.class);

    /**
     * This method will validate JSON schema against the service response
     *
     * @param schemaFileName Name of the JSON schema file to be used for validation
     * @param type           Type of JSON e.g. if its JSONObject then type=JSONObject.class, if its JSONArray then type=JSONArray.class
     * @param jsonResponse   JSON response
     */
    public static void validateAgainstSchema(String schemaFileName, Class<?> type, JsonPath jsonResponse) {
        validateAgainstSchema(schemaFileName, type, jsonResponse.prettify());
    }

    /**
     * This method will validate JSON schema against the service response
     *
     * @param schemaFileName Name of the JSON schema file to be used for validation
     * @param type           Type of JSON e.g. if its JSONObject then type=JSONObject.class, if its JSONArray then type=JSONArray.class
     * @param jsonString     Service response
     */
    public static void validateAgainstSchema(String schemaFileName, Class<?> type, String jsonString) {
        final URL resource = Resources.getResource("raml/json/schema/" + schemaFileName);
        try (InputStream inputStream = resource.openStream()) {
            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));

            Schema schema = SchemaLoader.load(rawSchema);
            if (type == JSONArray.class) {
                schema.validate(new JSONArray(jsonString));
            } else {
                schema.validate(new JSONObject(jsonString));
            }
        } catch (Exception e) {
            LOGGER.error("Error validating payload against schema " + resource.toString(), e);
            fail("Error validating payload against schema");
        }
    }
}
