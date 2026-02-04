package uk.gov.moj.cpp.sjp.command.api;

import static java.util.Collections.singletonMap;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getOffenceWithdrawalRequestActionGroups;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OffenceWithdrawalRequestApiTest extends BaseDroolsAccessControlTest {

    private static final String COMMAND_NAME = "sjp.set-offences-withdrawal-requests-status";
    private static final String NEW_COMMAND_NAME = "sjp.command.set-offences-withdrawal-requests-status";

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @InjectMocks
    private OffenceWithdrawalRequestApi offenceWithdrawalRequestApi;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public OffenceWithdrawalRequestApiTest() {
        super("COMMAND_API_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @Test
    public void shouldAllowAuthorisedUserToOffenceWithdrawalRequest() {
        final Action action = createActionFor(COMMAND_NAME);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getOffenceWithdrawalRequestActionGroups()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(OffenceWithdrawalRequestApi.class, isHandlerClass(COMMAND_API)
                .with(method("setOffenceWithdrawalRequestsStatus").thatHandles(COMMAND_NAME)));
    }

    @Test
    public void shouldSetNextCommandInTheChain() {
        final String userId = randomUUID().toString();
        final MetadataBuilder metadataBuilder = metadataWithRandomUUID(COMMAND_NAME).withUserId(userId);
        final JsonEnvelope command = JsonEnvelope.envelopeFrom(metadataBuilder.build(), buildPayload());
        offenceWithdrawalRequestApi.setOffenceWithdrawalRequestsStatus(command);

        verify(sender).send(envelopeCaptor.capture());
        final JsonEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata(), withMetadataEnvelopedFrom(command).withName(NEW_COMMAND_NAME).withUserId(userId));
        assertThat(newCommand.payloadAsJsonObject(), equalTo(command.payloadAsJsonObject()));
    }

    private JsonObject buildPayload() {
        final JsonArray jsonArray = createArrayBuilder()
                .add(createObjectBuilder()
                        .add("offenceId", randomUUID().toString())
                        .add("withdrawalRequestReasonId", randomUUID().toString()))
                .build();
        return createObjectBuilder().add("withdrawalRequestsStatus", jsonArray).build();
    }
}
