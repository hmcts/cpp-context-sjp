package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class SaveApplicationDecisionTest extends BaseDroolsAccessControlTest {

    public SaveApplicationDecisionTest() {
        super("COMMAND_CONTROLLER_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return new HashMap<>();
    }

    @Test
    public void shouldAllowSaveApplicationDecision() {
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.command.controller.save-application-decision").build(), createObjectBuilder().build());
        final Action action = new Action(envelope);
        assertSuccessfulOutcome(executeRulesWith(action));
    }
}