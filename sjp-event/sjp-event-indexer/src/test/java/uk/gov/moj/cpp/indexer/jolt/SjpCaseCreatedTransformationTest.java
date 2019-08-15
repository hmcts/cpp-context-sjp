package uk.gov.moj.cpp.indexer.jolt;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.indexer.jolt.helper.JoltInstanceHelper.initializeJolt;
import static uk.gov.moj.cpp.indexer.jolt.helper.JsonHelper.readJson;
import static uk.gov.moj.cpp.indexer.jolt.helper.JsonHelper.readJsonViaPath;

import uk.gov.justice.json.jolt.JoltTransformer;

import java.io.IOException;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;

public class SjpCaseCreatedTransformationTest {


    private final JoltTransformer joltTransformer = new JoltTransformer();

    @Before
    public void setUp() {
        initializeJolt(joltTransformer);
    }

    @Test
    public void shouldTransformSjpCaseCreatedEvent() throws IOException {

        final JsonObject specJson = readJsonViaPath("src/transformer/sjp.events.sjp-case-created-spec.json");
        assertNotNull(specJson);

        final JsonObject inputJson = readJson("/sjp.events.sjp-case-created.json");
        final JsonObject transformedJson = joltTransformer.transformWithJolt(specJson.toString(), inputJson);

        verifyCase(transformedJson);
    }

    private void verifyCase(final JsonObject actualCase) {
        assertThat(actualCase.getString("caseId"), is("7e2f843e-d639-40b3-8611-8015f3a18958"));
        assertThat(actualCase.getString("caseReference"), is("22C22222222"));
        assertThat(actualCase.getString("sjpNoticeServed"), is("2015-12-02"));
        assertThat(actualCase.getString("prosecutingAuthority"), is("DVLA"));
        assertThat(actualCase.getString("caseStatus"), is("NO_PLEA_RECEIVED"));
        assertThat(actualCase.getString("_case_type"), is("PROSECUTION"));
        assertThat(actualCase.getBoolean("_is_sjp"), is(true));
        assertThat(actualCase.getBoolean("_is_magistrates"), is(false));
        assertThat(actualCase.getBoolean("_is_crown"), is(false));
        assertThat(actualCase.getBoolean("_is_charging"), is(false));
    }
}
