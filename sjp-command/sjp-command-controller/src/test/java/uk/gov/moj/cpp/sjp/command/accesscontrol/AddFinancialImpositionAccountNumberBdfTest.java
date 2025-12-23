package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class AddFinancialImpositionAccountNumberBdfTest extends BaseDroolsAccessControlTest {
    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return new HashMap<>();
    }

    public AddFinancialImpositionAccountNumberBdfTest() {
        super("COMMAND_CONTROLLER_SESSION");
    }

    @Test
    public void shouldAllowAddFinancialImpositionAccountNumber() {
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.command.add-financial-imposition-account-number-bdf").build(), createObjectBuilder().build());
        final Action action = new Action(envelope);
        assertSuccessfulOutcome(executeRulesWith(action));
    }
}
