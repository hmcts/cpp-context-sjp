package uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator;

import static java.math.BigDecimal.ONE;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator.FinancialPenaltyDecisionValidator.validateFinancialPenaltyDecision;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

public class FinancialPenaltyDecisionValidatorTest {

    private static final UUID CASE_ID = randomUUID();
    private static final BigDecimal MAX_FINE_VALUE= BigDecimal.valueOf(999999999.99);

    @Test
    public void shouldValidate_with_valid_attributes() {
        final JsonObject validOffence = createObjectBuilder().add("maxFineLevel", 2).add("maxFineValue", 2).build();

        validateFinancialPenaltyDecision(createObjectBuilder().add("compensation", 1).build(), empty());
        validateFinancialPenaltyDecision(createObjectBuilder().add("fine", 1).build(), of(validOffence));
        validateFinancialPenaltyDecision(createObjectBuilder().add("compensation", 0).add("fine", 1).build(), of(validOffence));
        validateFinancialPenaltyDecision(createObjectBuilder().add("compensation", 1).add("fine", 0).build(), empty());
        validateFinancialPenaltyDecision(createObjectBuilder().add("compensation", 1).add("fine", 1).build(), of(validOffence));
        validateFinancialPenaltyDecision(createObjectBuilder().add("backDuty", 1).add("compensation", 0).build(), empty());
        validateFinancialPenaltyDecision(createObjectBuilder().add("excisePenalty", 2).build(), empty());
    }

    @Test
    public void shouldThrowErrorWhen_both_compensation_and_fine_are_empty() {
        JsonObject financialPenaltyDecision = createObjectBuilder().add("compensation", 0).add("fine", 0).build();
        Optional<JsonObject> optionalJsonEmptyObject = empty();
        var e = assertThrows(BadRequestException.class, () -> validateFinancialPenaltyDecision(financialPenaltyDecision, optionalJsonEmptyObject));
        assertThat(e.getMessage(), is("Both compensation and fine cannot be empty"));
    }

    @Test
    public void shouldThrowErrorWhen_excise_penalty_greater_than_maxvalue() {
        JsonObject financialPenaltyJsonObject = createObjectBuilder().add("excisePenalty", MAX_FINE_VALUE.add(ONE)).build();
        Optional<JsonObject> optionalJsonObject = empty();
        var e = assertThrows(BadRequestException.class, () -> validateFinancialPenaltyDecision(financialPenaltyJsonObject, optionalJsonObject));
        assertThat(e.getMessage(), is("The maximum excise penalty for this offence is £" + MAX_FINE_VALUE + ""));
    }

    @Test
    public void shouldThrowErrorWhen_fine_greater_than_max_fine_value() {
        final JsonObject invalidOffence = createObjectBuilder().add("maxFineLevel", 2).add("maxFineValue", 2).build();

        JsonObject jsonObject = createObjectBuilder().add("compensation", 0).add("fine", 3).build();
        Optional<JsonObject> invalidOffenceObject = of(invalidOffence);
        var e = assertThrows(BadRequestException.class, () -> validateFinancialPenaltyDecision(jsonObject, invalidOffenceObject));
        assertThat(e.getMessage(), is("The maximum fine for this offence is £" + 2 + ""));
    }
}
