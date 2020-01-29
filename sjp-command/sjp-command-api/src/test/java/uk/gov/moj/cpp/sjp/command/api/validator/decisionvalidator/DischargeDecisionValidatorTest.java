package uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType;

import javax.json.JsonObject;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator.DischargeDecisionValidator.validateDischargeDecision;

public class DischargeDecisionValidatorTest {

    private static final JsonObject DISCHARGED_FOR = createObjectBuilder().build();
    private static final String CONDITIONAL = DischargeType.CONDITIONAL.name();
    private static final String ABSOLUTE = DischargeType.ABSOLUTE.name();
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldThrowErrorWhen_ConditionalDischarge_With_DischargedFor_NotPresent() {
        // assume validation is ignored/passes
        validateDischargeDecision(createObjectBuilder().build());
        validateDischargeDecision(createObjectBuilder().addNull("dischargeType").build());
        validateDischargeDecision(createObjectBuilder().addNull("dischargedFor").build());
        validateDischargeDecision(createObjectBuilder().add("dischargeType", CONDITIONAL).add("dischargedFor", DISCHARGED_FOR).build());
        validateDischargeDecision(createObjectBuilder().add("dischargeType", ABSOLUTE).add("dischargedFor", DISCHARGED_FOR).build());
        validateDischargeDecision(createObjectBuilder().add("dischargeType", ABSOLUTE).build());

        thrown.expect(BadRequestException.class);
        thrown.expectMessage("dischargedFor is required for conditional discharge");

        validateDischargeDecision(createObjectBuilder().add("dischargeType", CONDITIONAL).build());
    }

    @Test
    public void shouldThrowErrorWhen_ConditionalDischarge_With_DischargedFor_IsNull() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("dischargedFor is required for conditional discharge");

        validateDischargeDecision(createObjectBuilder().add("dischargeType", CONDITIONAL).addNull("dischargedFor").build());
    }

}
