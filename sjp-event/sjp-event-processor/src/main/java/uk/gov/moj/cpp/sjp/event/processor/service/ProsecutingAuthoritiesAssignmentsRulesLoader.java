package uk.gov.moj.cpp.sjp.event.processor.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;

import org.apache.commons.io.IOUtils;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

public class ProsecutingAuthoritiesAssignmentsRulesLoader {

    private final static String DEFAULT_CONF_FILE = "prosecuting_authorities_assignments.json";
    private final static String SCHEMA_FILE = "prosecuting_authorities_assignments_schema.json";

    public ProsecutingAuthoritiesAssignmentsRules load() throws IOException {
        return load(DEFAULT_CONF_FILE);
    }

    public ProsecutingAuthoritiesAssignmentsRules load(final String configurationFilename) throws IOException {
        try (final InputStream configurationStream = getClass().getClassLoader().getResourceAsStream(configurationFilename)) {
            final String configuration = IOUtils.toString(configurationStream);

            validateConfiguration(configuration);

            try (final JsonReader reader = Json.createReader(new StringReader(configuration))) {
                final Map<String, Set<String>> courtCodesByProsecutor = new HashMap<>();
                reader.readObject().getJsonArray("assignmentRules").getValuesAs(JsonObject.class).forEach(
                        assignmentRule -> courtCodesByProsecutor.computeIfAbsent(assignmentRule.getString("prosecutingAuthority"), key -> new HashSet())
                                .addAll(assignmentRule.getJsonArray("exclusiveCourtCodes").getValuesAs(JsonString.class)
                                        .stream()
                                        .map(JsonString::getString)
                                        .collect(Collectors.toSet())));
                return new ProsecutingAuthoritiesAssignmentsRules(courtCodesByProsecutor);
            }
        }
    }

    private void validateConfiguration(final String payload) throws IOException {
        try (final InputStream inputStream = getClass().getClassLoader().getResourceAsStream(SCHEMA_FILE)) {
            final Schema schema = SchemaLoader.load(new JSONObject(new JSONTokener(inputStream)));
            schema.validate(new JSONObject(payload));
        }
    }

}
