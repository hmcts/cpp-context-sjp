package uk.gov.moj.cpp.sjp.command.accesscontrol;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

public class DeleteFinancialMeansTest extends BaseDroolsAccessControlTest {

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return new HashMap<>();
    }

    @Test
    public void shouldAllowDeleteFinancialMeansCommand() {
        final JsonEnvelope jsonEnvelope = JsonEnvelopeBuilder.envelope().with(
                JsonEnvelope.metadataBuilder().withId(UUID.randomUUID())
                        .withName("sjp.command.delete-defendant-financial-means-information")
        ).build();

        final Action action = new Action(jsonEnvelope);
        assertSuccessfulOutcome(executeRulesWith(action));
    }
}
