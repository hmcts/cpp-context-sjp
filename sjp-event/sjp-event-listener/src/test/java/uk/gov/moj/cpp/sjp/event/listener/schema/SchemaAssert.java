package uk.gov.moj.cpp.sjp.event.listener.schema;

import com.google.common.base.Joiner;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class SchemaAssert {

    private Schema schema;

    public SchemaAssert(Schema schema) {
        this.schema = schema;
    }

    public static SchemaAssert loadSchema(String filePath) throws IOException {
        try (InputStream inputStream = SchemaAssert.class.getResourceAsStream(filePath)) {
            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
            Schema schema = SchemaLoader.load(rawSchema);
            return new SchemaAssert(schema);
        }
    }

    public void assertValid(String dirPath) throws URISyntaxException, IOException {
        doAssert(dirPath, true);
    }

    public void assertInvalid(String dirPath) throws URISyntaxException, IOException {
        doAssert(dirPath, false);
    }

    private void doAssert(String dirPath, Boolean expectedValidationResult) throws URISyntaxException,
            IOException {
        String[] files = new File(getClass().getResource(dirPath).toURI()).list();

        assertTrue(files.length > 0);

        List<String> failedFiles = new ArrayList<String>();

        for (String fileName : files) {
            String fileAbsolutePath = dirPath + "/" + fileName;

            try (InputStream inputStream = getClass().getResourceAsStream(fileAbsolutePath)) {
                JSONObject file = new JSONObject(new JSONTokener(inputStream));
                try {
                    schema.validate(file);
                    if (!expectedValidationResult) {
                        failedFiles.add(fileAbsolutePath);
                    }
                } catch (ValidationException e) {
                    if (expectedValidationResult) {
                        e.printStackTrace();
                        failedFiles.add(fileAbsolutePath);
                    }
                }
            }
        }

        assertTrue(Joiner.on('\n').join(failedFiles), failedFiles.isEmpty());
    }
}