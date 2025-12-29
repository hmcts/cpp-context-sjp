package uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;
import static java.math.BigDecimal.ZERO;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator.FinancialImpositionValidator.validateFinancialImposition;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DecisionName.DISCHARGE;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DecisionName.FINANCIAL_PENALTY;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.CONDITIONAL;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType;

import java.time.LocalDate;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.rules.ExpectedException;

public class FinancialImpositionValidatorTest {

    @Test
    public void shouldNotRequireFinancialImpositionWhenCostsAreEmpty() {
        final JsonArrayBuilder excisePenaltyOffences = createArrayBuilder()
                .add(createObjectBuilder()
                        .add("type", FINANCIAL_PENALTY)
                        .add("excisePenalty", ZERO)
                        .add("backDuty", ZERO)
                        .add("fine", ZERO)
                        .add("compensation", ZERO))
                .add(createObjectBuilder()
                        .add("type", DISCHARGE)
                        .add("backDuty", ZERO)
                        .add("compensation", ZERO));

        final JsonObject excisePenaltyDecision = createObjectBuilder()
                .add("offenceDecisions", excisePenaltyOffences)
                .build();

        validateFinancialImposition(excisePenaltyDecision, true);
    }

    @Test
    public void shouldNotCheckVictimSurchargeWhenAllOffencesAreExcisePenalty() {
        final JsonArrayBuilder excisePenaltyOffences = createArrayBuilder()
                .add(createObjectBuilder()
                        .add("type", FINANCIAL_PENALTY)
                        .add("excisePenalty", ONE))
                .add(createObjectBuilder()
                        .add("type", FINANCIAL_PENALTY)
                        .add("excisePenalty", ONE));

        final JsonObject excisePenaltyDecision = createObjectBuilder()
                .add("financialImposition", createFinancialImposition())
                .add("offenceDecisions", excisePenaltyOffences)
                .build();

        validateFinancialImposition(excisePenaltyDecision, true);

        final JsonArrayBuilder mixOffences = createArrayBuilder()
                .add(createObjectBuilder()
                        .add("type", FINANCIAL_PENALTY)
                        .add("excisePenalty", ONE))
                .add(createObjectBuilder()
                        .add("type", DISCHARGE)
                        .add("dischargeType", CONDITIONAL.toString()));

        final JsonObject mixDecision = createObjectBuilder()
                .add("financialImposition", createFinancialImposition())
                .add("offenceDecisions", mixOffences)
                .build();

        var e = assertThrows(BadRequestException.class, () -> validateFinancialImposition(mixDecision, false));
        assertThat(e.getMessage(), is("reasonForNoVictimSurcharge is required"));
    }

    private JsonObject createFinancialImposition() {
        return createObjectBuilder()
                .add("costsAndSurcharge", createObjectBuilder().add("costs", TEN).add("victimSurcharge", ZERO))
                .add("payment", createObjectBuilder()
                        .add("paymentType", PaymentType.ATTACH_TO_EARNINGS.toString())
                        .add("paymentTerms", createObjectBuilder()
                                .add("lumpSum", createObjectBuilder()
                                        .add("withinDays", 1)
                                        .add("payByDate", LocalDate.now().plusDays(2).toString()))))
                .build();
    }
}
