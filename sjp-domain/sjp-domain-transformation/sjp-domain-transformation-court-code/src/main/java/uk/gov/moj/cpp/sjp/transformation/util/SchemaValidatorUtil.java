package uk.gov.moj.cpp.sjp.transformation.util;

import static com.google.common.io.Resources.getResource;
import static org.everit.json.schema.loader.SchemaLoader.load;

import uk.gov.moj.cpp.sjp.transformation.exception.TransformationException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.everit.json.schema.Schema;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Defines a schema validation util.
 */
public class SchemaValidatorUtil {

    private SchemaValidatorUtil() {
    }

    /**
     * Validates a json against a schema file.
     *
     * @param schemaFileName the schema file location
     * @param jsonString     the stringified json
     */
    public static void validateAgainstSchema(final String schemaFileName, final String jsonString) {
        final URL resource = getResource(schemaFileName);

        try (final InputStream inputStream = resource.openStream()) {
            final JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));

            final Schema schema = load(rawSchema);

            schema.validate(new JSONObject(jsonString));
        } catch (IOException e) {
            throw new TransformationException("Error validating payload against schema", e);
        }
    }
}

