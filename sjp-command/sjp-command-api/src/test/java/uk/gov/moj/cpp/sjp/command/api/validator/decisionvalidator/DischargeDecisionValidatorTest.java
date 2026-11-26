package uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.command.api.validator.decisionvalidator.DischargeDecisionValidator.validateDischargeDecision;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

public class DischargeDecisionValidatorTest {

    private static final JsonObject DISCHARGED_FOR = createObjectBuilder().build();
    private static final String CONDITIONAL = DischargeType.CONDITIONAL.name();
    private static final String ABSOLUTE = DischargeType.ABSOLUTE.name();

    @Test
    public void shouldThrowErrorWhen_ConditionalDischarge_With_DischargedFor_NotPresent() {
        // assume validation is ignored/passes
        validateDischargeDecision(createObjectBuilder().build());
        validateDischargeDecision(createObjectBuilder().addNull("dischargeType").build());
        validateDischargeDecision(createObjectBuilder().addNull("dischargedFor").build());
        validateDischargeDecision(createObjectBuilder().add("dischargeType", CONDITIONAL).add("dischargedFor", DISCHARGED_FOR).build());
        validateDischargeDecision(createObjectBuilder().add("dischargeType", ABSOLUTE).add("dischargedFor", DISCHARGED_FOR).build());
        validateDischargeDecision(createObjectBuilder().add("dischargeType", ABSOLUTE).build());

        JsonObject dischargeType = createObjectBuilder().add("dischargeType", CONDITIONAL).build();
        var e = assertThrows(BadRequestException.class, () -> validateDischargeDecision(dischargeType));
        assertThat(e.getMessage(), is("dischargedFor is required for conditional discharge"));
    }

    @Test
    public void shouldThrowErrorWhen_ConditionalDischarge_With_DischargedFor_IsNull() {
        JsonObject discharge = createObjectBuilder().add("dischargeType", CONDITIONAL).addNull("dischargedFor").build();
        var e = assertThrows(BadRequestException.class, () -> validateDischargeDecision(discharge));
        assertThat(e.getMessage(), is("dischargedFor is required for conditional discharge"));
    }

}
