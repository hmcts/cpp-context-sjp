package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class UpdateAllFinancialMeansTest extends BaseDroolsAccessControlTest {

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return new HashMap<>();
    }

    @Test
    public void shouldAllowUpdateFinancialMeansCommand() {
        final JsonEnvelope envelope = createEnvelope("sjp.command.update-all-financial-means", createObjectBuilder().build());
        final Action action = new Action(envelope);
        assertSuccessfulOutcome(executeRulesWith(action));
    }
}