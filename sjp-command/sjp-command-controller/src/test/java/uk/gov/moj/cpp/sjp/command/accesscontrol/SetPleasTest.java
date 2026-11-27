package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static java.util.Collections.singletonMap;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class SetPleasTest extends BaseDroolsAccessControlTest {

    private static final String SJP_COMMAND_SET_PLEAS = "sjp.command.set-pleas";

    public SetPleasTest() {
        super("COMMAND_CONTROLLER_SESSION");
    }

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Test
    public void shouldAllowUpdatePleaCommand() {
        final JsonObject inputPayload = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .build();

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUID(SJP_COMMAND_SET_PLEAS).build(), inputPayload);
        final Action action = new Action(envelope);
        assertSuccessfulOutcome(executeRulesWith(action));
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }
}

