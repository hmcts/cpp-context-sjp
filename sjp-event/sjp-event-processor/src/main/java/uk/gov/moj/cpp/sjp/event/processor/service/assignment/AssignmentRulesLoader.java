package uk.gov.moj.cpp.sjp.event.processor.service.assignment;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static uk.gov.moj.cpp.sjp.domain.AssignmentRuleType.ALLOW;
import static uk.gov.moj.cpp.sjp.domain.AssignmentRuleType.DISALLOW;

import uk.gov.moj.cpp.sjp.domain.AssignmentRuleType;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Collection;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;

import org.apache.commons.io.IOUtils;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

public class AssignmentRulesLoader {

    private static final String ASSIGNMENT_RULES_FILE = "assignments_rules.json";
    private static final String ASSIGNMENT_RULES_SCHEMA_FILE = "assignments_rules_schema.json";

    public AssignmentRules load() throws IOException {
        return load(ASSIGNMENT_RULES_FILE);
    }

    public AssignmentRules load(final String configurationFilename) throws IOException {
        try (final InputStream configurationStream = getClass().getClassLoader().getResourceAsStream(configurationFilename)) {
            final String configuration = IOUtils.toString(configurationStream);

            validateConfiguration(configuration);

            try (final JsonReader reader = Json.createReader(new StringReader(configuration))) {
                final List<AssignmentRule> assignmentRulesByOuCode = reader.readObject()
                        .getJsonArray("assignmentRules")
                        .getValuesAs(JsonObject.class)
                        .stream()
                        .map(assignmentRule -> {
                                    final String ouCodePrefix = assignmentRule.getString("courtHouseCodePrefix");
                                    final AssignmentRuleType assignmentRuleType = assignmentRule.containsKey("allowedProsecutingAuthorities") ? ALLOW : DISALLOW;
                                    final String prosecutingAuthorityProperty = assignmentRuleType.equals(ALLOW) ? "allowedProsecutingAuthorities" : "disallowedProsecutingAuthorities";
                                    final Collection<String> prosecutingAuthorities = assignmentRule.getJsonArray(prosecutingAuthorityProperty)
                                            .getValuesAs(JsonString.class)
                                            .stream()
                                            .map(JsonString::getString)
                                            .collect(toSet());
                                    return new AssignmentRule(ouCodePrefix, prosecutingAuthorities, assignmentRuleType);
                                }
                        )
                        .collect(toList());

                return new AssignmentRules(assignmentRulesByOuCode);
            }
        }
    }

    private void validateConfiguration(final String configuration) throws IOException {
        try (final InputStream inputStream = getClass().getClassLoader().getResourceAsStream(ASSIGNMENT_RULES_SCHEMA_FILE)) {
            final Schema schema = SchemaLoader.load(new JSONObject(new JSONTokener(inputStream)));
            schema.validate(new JSONObject(configuration));
        }
    }

}
