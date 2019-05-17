package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.HashMap;
import java.util.Map;

import javax.json.JsonObject;

import org.junit.Test;


public class CreateCaseTest extends BaseDroolsAccessControlTest {

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return new HashMap<>();
    }

    @Test
    public void shouldAllowCreateSjpCaseCommand() {
        final JsonObject inputPayload = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .build();
        final JsonEnvelope envelope = JsonEnvelopeBuilder.envelopeFrom(metadataWithRandomUUID("sjp.command.create-sjp-case").build(), inputPayload);
        final Action action = new Action(envelope);
        assertSuccessfulOutcome(executeRulesWith(action));
    }
}
