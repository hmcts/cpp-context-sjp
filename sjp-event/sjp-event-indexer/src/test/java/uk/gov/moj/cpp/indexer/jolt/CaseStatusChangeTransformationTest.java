package uk.gov.moj.cpp.indexer.jolt;

import static com.jayway.jsonpath.JsonPath.parse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.indexer.jolt.helper.JoltInstanceHelper.initializeJolt;
import static uk.gov.moj.cpp.indexer.jolt.helper.JsonHelper.readJson;
import static uk.gov.moj.cpp.indexer.jolt.helper.JsonHelper.readJsonViaPath;

import uk.gov.justice.json.jolt.JoltTransformer;

import java.io.IOException;

import javax.json.JsonObject;
import javax.json.JsonString;

import com.jayway.jsonpath.DocumentContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CaseStatusChangeTransformationTest {


    private final JoltTransformer joltTransformer = new JoltTransformer();

    @BeforeEach
    public void setUp() {
        initializeJolt(joltTransformer);
    }

    @Test
    public void shouldTransformCaseReceivedEvent() throws IOException {

        final JsonObject specJson = readJsonViaPath("src/transformer/sjp.events.case-status-changed-spec.json");
        assertNotNull(specJson);

        final JsonObject inputJson = readJson("/sjp.events.case-status-changed.json");
        final DocumentContext inputCourtApplication = parse(inputJson);
        final JsonObject transformedJson = joltTransformer.transformWithJolt(specJson.toString(), inputJson);

        verifyCase(inputCourtApplication, transformedJson);
    }

    private void verifyCase(final DocumentContext inputCase, final JsonObject outputCase) {
        assertThat(outputCase.getString("_case_type"), is("PROSECUTION"));
        assertThat(((JsonString) inputCase.read("$.caseId")).getString(), is(outputCase.getString("caseId")));
        assertThat(outputCase.getString("caseStatus"), is("NO_PLEA_RECEIVED_READY_FOR_DECISION"));
    }

}
