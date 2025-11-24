package uk.gov.moj.cpp.sjp.query.view.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.sjp.query.view.util.results.ResultsMatchers.D;
import static uk.gov.moj.cpp.sjp.query.view.util.results.ResultsMatchers.D45;
import static uk.gov.moj.cpp.sjp.query.view.util.results.ResultsMatchers.DPR;
import static uk.gov.moj.cpp.sjp.query.view.util.results.ResultsMatchers.hasResults;
import static uk.gov.moj.cpp.sjp.query.view.util.results.ResultsMatchers.isOffenceDecision;

import uk.gov.moj.cpp.sjp.query.view.util.builders.OffenceDecisionBuilder;
import uk.gov.moj.cpp.sjp.query.view.util.fakes.FakeCachedReferenceData;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

public class DismissDecisionResultTest {

    private FakeCachedReferenceData referenceData = new FakeCachedReferenceData();

    @Test
    public void shouldConvertDismissDecisionResults() {
        final OffenceDecisionBuilder offenceDecision = OffenceDecisionBuilder.offenceDecision();

        final JsonObject actual = new DismissDecisionResult(offenceDecision.build(), referenceData, null).toJsonObjectBuilder().build();

        assertThat(actual.toString(), isOffenceDecision(offenceDecision.getOffenceId(), offenceDecision.getVerdict()));
        assertThat(actual.toString(), hasResults(D()));
    }

    @Test
    public void shouldConvertPressRestriction() {
        final JsonObject offenceDecision = OffenceDecisionBuilder.offenceDecision()
                .withPressRestriction("A Name")
                .build();

        final JsonObject actual = new DismissDecisionResult(offenceDecision, referenceData, null).toJsonObjectBuilder().build();

        assertThat(actual.toString(), hasResults(D(), D45("A Name")));
    }

    @Test
    public void shouldConvertPressRestrictionRevokedResults() {
        final JsonObject offenceDecision = OffenceDecisionBuilder.offenceDecision()
                .withPressRestrictionRevoked()
                .build();
        final String resultedOn = "2020-06-22";

        final JsonObject actual = new DismissDecisionResult(offenceDecision, referenceData, resultedOn).toJsonObjectBuilder().build();

        assertThat(actual.toString(), hasResults(D(), DPR(resultedOn)));
    }
}
