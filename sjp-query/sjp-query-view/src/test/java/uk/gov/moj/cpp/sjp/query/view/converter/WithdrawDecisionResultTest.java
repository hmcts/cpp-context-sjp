package uk.gov.moj.cpp.sjp.query.view.converter;

import static java.util.UUID.fromString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.query.view.converter.ResultCode.D45;
import static uk.gov.moj.cpp.sjp.query.view.converter.ResultCode.DPR;
import static uk.gov.moj.cpp.sjp.query.view.converter.ResultCode.WDRNNOT;
import static uk.gov.moj.cpp.sjp.query.view.util.results.ResultsMatchers.D45;
import static uk.gov.moj.cpp.sjp.query.view.util.results.ResultsMatchers.DPR;
import static uk.gov.moj.cpp.sjp.query.view.util.results.ResultsMatchers.WDRNNOT;
import static uk.gov.moj.cpp.sjp.query.view.util.results.ResultsMatchers.hasResults;
import static uk.gov.moj.cpp.sjp.query.view.util.results.ResultsMatchers.isOffenceDecision;

import uk.gov.moj.cpp.sjp.query.view.service.CachedReferenceData;
import uk.gov.moj.cpp.sjp.query.view.util.builders.OffenceDecisionBuilder;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WithdrawDecisionResultTest {

    private static final UUID WITHDRAW_REASON_ID = fromString("1dbf0960-51e3-4d90-803d-d54cd8ea7d3e");
    private static final String WITHDRAW_REASON = "A random reason";

    @Mock
    private CachedReferenceData referenceData;

    @Before
    public void setUp() {
        when(referenceData.getWithdrawalReason(WITHDRAW_REASON_ID)).thenReturn(WITHDRAW_REASON);
        when(referenceData.getResultId(WDRNNOT.name())).thenReturn(WDRNNOT.getResultDefinitionId());
        when(referenceData.getResultId(D45.name())).thenReturn(D45.getResultDefinitionId());
        when(referenceData.getResultId(DPR.name())).thenReturn(DPR.getResultDefinitionId());
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
