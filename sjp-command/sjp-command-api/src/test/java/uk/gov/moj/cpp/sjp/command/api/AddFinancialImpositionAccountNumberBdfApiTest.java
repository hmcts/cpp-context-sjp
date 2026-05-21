package uk.gov.moj.cpp.sjp.command.api;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonMap;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getAddFinancialImpositionAccountNumberBdfGroups;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AddFinancialImpositionAccountNumberBdfApiTest extends BaseDroolsAccessControlTest {

    public static final String SJP_ADD_FINANCIAL_IMPOSITION_ACCOUNT_NUMBER_BDF = "sjp.add-financial-imposition-account-number-bdf";
    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<JsonEnvelope> dispatchedCommand;

    @InjectMocks
    private AddFinancialImpositionAccountNumberBdfApi accountNumberApi;

    public AddFinancialImpositionAccountNumberBdfApiTest() {
        super("COMMAND_API_SESSION");
    }

    @Test
    public void shouldHandleAddFinancialImpositionAccountNumber() {
        assertThat(AddFinancialImpositionAccountNumberBdfApi.class, isHandlerClass(COMMAND_API)
                .with(method("addFinancialImpositionAccountNumber").thatHandles(SJP_ADD_FINANCIAL_IMPOSITION_ACCOUNT_NUMBER_BDF)));
    }

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @Test
    public void shouldAllowAuthorisedUserToAddFinancialImpositionAccountNumber() {
        final Action action = createActionFor(SJP_ADD_FINANCIAL_IMPOSITION_ACCOUNT_NUMBER_BDF);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getAddFinancialImpositionAccountNumberBdfGroups()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldHandleAndDispatchCommand() {
        final JsonEnvelope command = createAddAccountNumberCommand();

        final JsonObject payload = command.payloadAsJsonObject();

        accountNumberApi.addFinancialImpositionAccountNumber(command);

        verify(sender).send(dispatchedCommand.capture());
        final JsonEnvelope dispatchedEnvelope = dispatchedCommand.getValue();
        assertThat(dispatchedEnvelope.metadata().name(),
                is("sjp.command.add-financial-imposition-account-number-bdf"));

        assertThat(dispatchedEnvelope.payloadAsJsonObject(), payloadIsJson(allOf(
                withJsonPath("$.caseId", equalTo(payload.getString("caseId"))),
                withJsonPath("$.correlationId", equalTo(payload.getString("correlationId"))),
                withJsonPath("$.accountNumber", equalTo(payload.getString("accountNumber")))
        )));

    }

    private JsonEnvelope createAddAccountNumberCommand() {
        final JsonObject payload = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("correlationId", randomUUID().toString())
                .add("accountNumber", "88237111A")
                .build();

        return envelopeFrom(
                metadataWithRandomUUID(SJP_ADD_FINANCIAL_IMPOSITION_ACCOUNT_NUMBER_BDF),
                payload);
    }

}
