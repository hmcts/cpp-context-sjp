package uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator;

import static java.math.BigDecimal.ONE;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator.FinancialPenaltyDecisionValidator.validateFinancialPenaltyDecision;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.math.BigDecimal;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class FinancialPenaltyDecisionValidatorTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final UUID CASE_ID = randomUUID();
    private static final BigDecimal MAX_FINE_VALUE= BigDecimal.valueOf(999999999.99);

    final JsonEnvelope envelope = envelope().with(metadataWithRandomUUID("COMMAND_API")).withPayloadOf(CASE_ID, "caseId").build();

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
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Both compensation and fine cannot be empty");

        validateFinancialPenaltyDecision(createObjectBuilder().add("compensation", 0).add("fine", 0).build(), empty());
    }

    @Test
    public void shouldThrowErrorWhen_excise_penalty_greater_than_maxvalue() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("The maximum excise penalty for this offence is £" + MAX_FINE_VALUE + "");

        validateFinancialPenaltyDecision(createObjectBuilder().add("excisePenalty", MAX_FINE_VALUE.add(ONE)).build(), empty());
    }

    @Test
    public void shouldThrowErrorWhen_fine_greater_than_max_fine_value() {
        final JsonObject invalidOffence = createObjectBuilder().add("maxFineLevel", 2).add("maxFineValue", 2).build();
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("The maximum fine for this offence is £" + 2 + "");

        validateFinancialPenaltyDecision(createObjectBuilder().add("compensation", 0).add("fine", 3).build(), of(invalidOffence));
    }
}
