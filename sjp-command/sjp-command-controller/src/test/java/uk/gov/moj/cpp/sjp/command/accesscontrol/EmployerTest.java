package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.HashMap;
import java.util.Map;

import javax.json.JsonObject;

import org.junit.Test;

public class EmployerTest extends BaseDroolsAccessControlTest {

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return new HashMap<>();
    }

    @Test
    public void shouldAllowUpdateEmployerCommand() {
        final Action action = buildActionWithName("sjp.command.update-employer");
        assertSuccessfulOutcome(executeRulesWith(action));
    }

    @Test
    public void shouldAllowDeleteEmployerCommand() {
        final Action action = buildActionWithName("sjp.command.delete-employer");
        assertSuccessfulOutcome(executeRulesWith(action));
    }

    private Action buildActionWithName(final String name) {
        final JsonObject commandPayload = createObjectBuilder().add("caseId", randomUUID().toString()).build();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(name).build(), commandPayload);
        return new Action(envelope);
    }

}