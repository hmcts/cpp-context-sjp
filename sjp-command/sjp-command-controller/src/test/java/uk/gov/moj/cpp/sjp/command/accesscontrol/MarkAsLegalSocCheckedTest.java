package uk.gov.moj.cpp.sjp.command.accesscontrol;

import org.junit.Test;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import javax.json.JsonObject;
import java.util.HashMap;
import java.util.Map;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

public class MarkAsLegalSocCheckedTest extends BaseDroolsAccessControlTest {

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return new HashMap<>();
    }

    @Test
    public void shouldAllowMarkAsLegalSocChecked() {
        final JsonObject inputPayload = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .build();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.command.mark-as-legal-soc-checked").build(), inputPayload);
        final Action action = new Action(envelope);
        assertSuccessfulOutcome(executeRulesWith(action));
    }
}