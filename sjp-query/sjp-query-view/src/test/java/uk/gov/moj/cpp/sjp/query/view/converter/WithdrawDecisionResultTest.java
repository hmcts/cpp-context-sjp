package uk.gov.moj.cpp.sjp.query.view.converter;

import static java.util.UUID.fromString;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.sjp.query.view.util.results.ResultsMatchers.D45;
import static uk.gov.moj.cpp.sjp.query.view.util.results.ResultsMatchers.DPR;
import static uk.gov.moj.cpp.sjp.query.view.util.results.ResultsMatchers.WDRNNOT;
import static uk.gov.moj.cpp.sjp.query.view.util.results.ResultsMatchers.hasResults;
import static uk.gov.moj.cpp.sjp.query.view.util.results.ResultsMatchers.isOffenceDecision;

import uk.gov.moj.cpp.sjp.query.view.util.builders.OffenceDecisionBuilder;
import uk.gov.moj.cpp.sjp.query.view.util.fakes.FakeCachedReferenceData;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WithdrawDecisionResultTest {

    private static final UUID WITHDRAW_REASON_ID = fromString("1dbf0960-51e3-4d90-803d-d54cd8ea7d3e");
    private static final String WITHDRAW_REASON = "A random reason";

    private FakeCachedReferenceData referenceData = new FakeCachedReferenceData();

    @BeforeEach
    public void setUp() {
        referenceData.addWithdrawalReason(WITHDRAW_REASON_ID, WITHDRAW_REASON);
    }

    @Test
    public void shouldConvertWithdrawDecisionResults() {
        final OffenceDecisionBuilder offenceDecision = OffenceDecisionBuilder.offenceDecision()
                .withWithdrawalReasonId(WITHDRAW_REASON_ID);

        final JsonObject actual = new WithdrawDecisionResult(offenceDecision.build(), referenceData, null).toJsonObjectBuilder().build();

        assertThat(actual.toString(), isOffenceDecision(offenceDecision.getOffenceId(), offenceDecision.getVerdict()));
        assertThat(actual.toString(), hasResults(WDRNNOT(WITHDRAW_REASON)));
    }

    @Test
    public void shouldConvertPressRestrictionAppliedResults() {
        final OffenceDecisionBuilder offenceDecision = OffenceDecisionBuilder.offenceDecision()
                .withWithdrawalReasonId(WITHDRAW_REASON_ID)
                .withPressRestriction("A Name");

        final JsonObject actual = new WithdrawDecisionResult(offenceDecision.build(), referenceData, null).toJsonObjectBuilder().build();

        assertThat(actual.toString(), hasResults(WDRNNOT(WITHDRAW_REASON), D45("A Name")));
    }

    @Test
    public void shouldConvertPressRestrictionRevokedResults() {
        final OffenceDecisionBuilder offenceDecision = OffenceDecisionBuilder.offenceDecision()
                .withWithdrawalReasonId(WITHDRAW_REASON_ID)
                .withPressRestrictionRevoked();
        final String resultedOn = "2020-06-10";

        final JsonObject actual = new WithdrawDecisionResult(offenceDecision.build(), referenceData, resultedOn).toJsonObjectBuilder().build();

        assertThat(actual.toString(), hasResults(WDRNNOT(WITHDRAW_REASON), DPR(resultedOn)));
    }
}
