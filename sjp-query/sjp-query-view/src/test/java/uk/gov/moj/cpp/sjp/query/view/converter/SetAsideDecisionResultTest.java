package uk.gov.moj.cpp.sjp.query.view.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.sjp.query.view.util.results.ResultsMatchers.D45;
import static uk.gov.moj.cpp.sjp.query.view.util.results.ResultsMatchers.DPR;
import static uk.gov.moj.cpp.sjp.query.view.util.results.ResultsMatchers.SETASIDE;
import static uk.gov.moj.cpp.sjp.query.view.util.results.ResultsMatchers.hasResults;
import static uk.gov.moj.cpp.sjp.query.view.util.results.ResultsMatchers.isOffenceDecision;

import uk.gov.moj.cpp.sjp.query.view.util.builders.OffenceDecisionBuilder;
import uk.gov.moj.cpp.sjp.query.view.util.fakes.FakeCachedReferenceData;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

public class SetAsideDecisionResultTest {

    private FakeCachedReferenceData referenceData = new FakeCachedReferenceData();

    @Test
    public void shouldConvertSetAsideResults() {
        final OffenceDecisionBuilder offenceDecision = OffenceDecisionBuilder.offenceDecision().setAside();

        final JsonObject actual = new SetAsideDecisionResult(offenceDecision.build(), referenceData, null).toJsonObjectBuilder().build();

        assertThat(actual.toString(), isOffenceDecision(offenceDecision.getOffenceId()));
        assertThat(actual.toString(), hasResults(SETASIDE()));
    }

    @Test
    public void shouldConvertPressRestrictionsResults() {
        final OffenceDecisionBuilder offenceDecision = OffenceDecisionBuilder.offenceDecision()
                .setAside()
                .withPressRestriction("Mamilsom");

        final JsonObject actual = new SetAsideDecisionResult(offenceDecision.build(), referenceData, null).toJsonObjectBuilder().build();

        assertThat(actual.toString(), hasResults(SETASIDE(), D45("Mamilsom")));
    }

    @Test
    public void shouldConvertPressRestrictionRevokedResults() {
        final JsonObject offenceDecision = OffenceDecisionBuilder.offenceDecision()
                .setAside()
                .withPressRestrictionRevoked()
                .build();
        final String resultedOn = "2020-06-22";

        final JsonObject actual = new SetAsideDecisionResult(offenceDecision, referenceData, resultedOn).toJsonObjectBuilder().build();

        assertThat(actual.toString(), hasResults(SETASIDE(), DPR(resultedOn)));
    }
}