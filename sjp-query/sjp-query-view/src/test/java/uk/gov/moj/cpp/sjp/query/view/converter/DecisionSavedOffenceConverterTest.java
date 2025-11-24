package uk.gov.moj.cpp.sjp.query.view.converter;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.WITHDRAW;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;

import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.persistence.entity.DischargeOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.WithdrawOffenceDecision;
import uk.gov.moj.cpp.sjp.query.view.response.OffenceDecisionView;
import uk.gov.moj.cpp.sjp.query.view.util.builders.FinancialPenaltyOffenceDecisionBuilder;

import java.math.BigDecimal;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class DecisionSavedOffenceConverterTest {

    private final UUID OFFENCE_ID = randomUUID();
    private final UUID WITHDRAWAL_REASON_ID = randomUUID();
    private final UUID CASE_DECISION_ID = randomUUID();
    private final BigDecimal BACK_DUTY = new BigDecimal("2.0");
    private final BigDecimal EXCISE_PENALTY = new BigDecimal("3.0");

    @InjectMocks
    private DecisionSavedOffenceConverter converter;

    @Test
    public void shouldConvertToDecisionSavedEvent() {
        final OffenceDecision offenceDecision = new WithdrawOffenceDecision(OFFENCE_ID, CASE_DECISION_ID, WITHDRAWAL_REASON_ID, NO_VERDICT, null);

        final JsonObject actual = converter.convertOffenceDecision(new OffenceDecisionView(offenceDecision));

        assertThat(actual, payloadIsJson(allOf(
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

        final JsonObject decisionSavedPayload = converter.convertOffenceDecision(new OffenceDecisionView(dischargeOffenceDecision));

        assertThat(decisionSavedPayload, payloadIsJson(withJsonPath("backDuty", is(BACK_DUTY.doubleValue()))));
    }
    @Test
    public void shouldConvertBackDutyAndExcisePenaltyForFinancialPenaltyOffense() {
        final OffenceDecision offenceDecision = FinancialPenaltyOffenceDecisionBuilder.withDefaults()
                .withBackDuty(BACK_DUTY)
                .withExcisePenalty(EXCISE_PENALTY)
                .build();

        final JsonObject actual = converter.convertOffenceDecision(new OffenceDecisionView(offenceDecision));

        assertThat(actual, payloadIsJson(allOf(
                withJsonPath("backDuty", is(BACK_DUTY.doubleValue())),
                withJsonPath("excisePenalty", is(EXCISE_PENALTY.doubleValue())),
                withoutJsonPath("pressRestriction")
        )));
    }

    @Test
    public void shouldConvertPressRestrictionApplied() {
        final OffenceDecision offenceDecision = FinancialPenaltyOffenceDecisionBuilder.withDefaults()
                .pressRestrictionApplied("Baby Boy")
                .build();

        final JsonObject actual = converter.convertOffenceDecision(new OffenceDecisionView(offenceDecision));

        assertThat(actual.toString(), isJson(allOf(
                withJsonPath("pressRestriction.name", equalTo("Baby Boy")),
                withJsonPath("pressRestriction.requested", equalTo(true))
        )));
    }

    @Test
    public void shouldConvertPressRestrictionRevoked() {
        final OffenceDecision offenceDecision = FinancialPenaltyOffenceDecisionBuilder.withDefaults()
                .pressRestrictionRevoked()
                .build();

        final JsonObject actual = converter.convertOffenceDecision(new OffenceDecisionView(offenceDecision));

        assertThat(actual.toString(), isJson(allOf(
                withJsonPath("pressRestriction.name", nullValue()),
                withJsonPath("pressRestriction.requested", equalTo(false))
        )));
    }


    //TODO add tests for licence endorsement and disqualification attributes


}
