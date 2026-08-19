package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.HashMap;
import java.util.Map;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

public class AddCaseDocumentTest extends BaseDroolsAccessControlTest {

    public AddCaseDocumentTest() {
        super("COMMAND_CONTROLLER_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return new HashMap<>();
    }

    @Test
    public void shouldAllowAddCaseDocument() {
        final JsonObject inputPayload = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .build();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.command.add-case-document").build(), inputPayload);
        final Action action = new Action(envelope);
        assertSuccessfulOutcome(executeRulesWith(action));
    }
}