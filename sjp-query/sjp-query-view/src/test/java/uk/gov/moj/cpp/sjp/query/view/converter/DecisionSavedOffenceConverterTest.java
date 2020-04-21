package uk.gov.moj.cpp.sjp.query.view.converter;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.WITHDRAW;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;

import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.persistence.entity.DischargeOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialPenaltyOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.WithdrawOffenceDecision;
import uk.gov.moj.cpp.sjp.query.view.response.OffenceDecisionView;

import java.math.BigDecimal;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class DecisionSavedOffenceConverterTest {

    private final UUID OFFENCE_ID = randomUUID();
    private final UUID WITHDRAWAL_REASON_ID = randomUUID();
    private final UUID CASE_DECISION_ID = randomUUID();
    private final BigDecimal BACK_DUTY = new BigDecimal("2.0");
    private final BigDecimal EXCISE_PENALTY = new BigDecimal("3.0");

    @InjectMocks
    private DecisionSavedOffenceConverter decisionSavedOffenceConverter;

    @Test
    public void shouldConvertToDecisionSavedEvent() {
        OffenceDecision offenceDecision = new WithdrawOffenceDecision(OFFENCE_ID, CASE_DECISION_ID, WITHDRAWAL_REASON_ID, NO_VERDICT);

        final JsonObject decisionSavedPayload = decisionSavedOffenceConverter.convertOffenceDecision(new OffenceDecisionView(offenceDecision));
        assertThat(decisionSavedPayload,
                payloadIsJson(allOf(
                        withJsonPath("type", is(WITHDRAW.toString())),
                        withJsonPath("withdrawalReasonId", is(WITHDRAWAL_REASON_ID.toString())),
                        withJsonPath("offenceDecisionInformation[0].offenceId", is(OFFENCE_ID.toString())),
                        withJsonPath("offenceDecisionInformation[0].verdict", is(NO_VERDICT.toString()))
                )));
    }

    @Test
    public void shouldConvertBackDutyForDischargeOffence() {
        final DischargeOffenceDecision dischargeOffenceDecision = mock(DischargeOffenceDecision.class);
        when(dischargeOffenceDecision.getBackDuty()).thenReturn(BACK_DUTY);
        when(dischargeOffenceDecision.getDecisionType()).thenReturn(DecisionType.DISCHARGE);
        when(dischargeOffenceDecision.getDisqualificationPeriodValue()).thenReturn(null);

        final JsonObject decisionSavedPayload = decisionSavedOffenceConverter.convertOffenceDecision(new OffenceDecisionView(dischargeOffenceDecision));

        assertThat(decisionSavedPayload,
                payloadIsJson(allOf(
                        withJsonPath("backDuty", is(BACK_DUTY.doubleValue())))));
    }
    @Test
    public void shouldConvertBackDutyAndExcisePenaltyForFinancialPenaltyOffense() {
        final FinancialPenaltyOffenceDecision financialPenaltyOffenceDecision = mock(FinancialPenaltyOffenceDecision.class);
        when(financialPenaltyOffenceDecision.getBackDuty()).thenReturn(BACK_DUTY);
        when(financialPenaltyOffenceDecision.getExcisePenalty()).thenReturn(EXCISE_PENALTY);
        when(financialPenaltyOffenceDecision.getDecisionType()).thenReturn(DecisionType.FINANCIAL_PENALTY);
        when(financialPenaltyOffenceDecision.getDisqualificationPeriodValue()).thenReturn(null);

        final JsonObject financialPenaltySavedPayload = decisionSavedOffenceConverter.convertOffenceDecision(new OffenceDecisionView(financialPenaltyOffenceDecision));

        assertThat(financialPenaltySavedPayload,
                payloadIsJson(allOf(
                        withJsonPath("backDuty", is(BACK_DUTY.doubleValue())),
                        withJsonPath("excisePenalty", is(EXCISE_PENALTY.doubleValue()))
                )));
    }

    //TODO add tests for licence endorsement and disqualification attributes



}
