package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class RequestDeleteDocsTest extends BaseDroolsAccessControlTest {
    @Override
    protected Map<Class, Object> getProviderMocks() {
        return new HashMap<>();
    }

    @Test
    public void shouldAllowRequestDeleteDocs() {
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.command.request-delete-docs").build(), createObjectBuilder().build());
        final Action action = new Action(envelope);
        assertSuccessfulOutcome(executeRulesWith(action));
    }
}
