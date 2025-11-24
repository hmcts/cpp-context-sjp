package uk.gov.moj.cpp.sjp.command.api;

import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getDeleteFinancialMeansGroups;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getUpdateFinancialMeansGroups;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

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
public class FinancialMeansApiTest extends BaseDroolsAccessControlTest {

    private static final String UPDATE_FINANCIAL_MEANS_COMMAND_NAME = "sjp.update-financial-means";
    private static final String UPDATE_FINANCIAL_MEANS_NEW_COMMAND_NAME = "sjp.command.update-financial-means";
    private static final String DELETE_FINANCIAL_MEANS_COMMAND_NAME = "sjp.delete-financial-means";
    private static final String DELETE_FINANCIAL_MEANS_NEW_COMMAND_NAME = "sjp.command.delete-defendant-financial-means-information";

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @InjectMocks
    private FinancialMeansApi financialMeansApi;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public FinancialMeansApiTest() {
        super("COMMAND_API_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @Test
    public void shouldAllowAuthorisedUserToUploadCaseDocument() {
        final Action action = createActionFor(UPDATE_FINANCIAL_MEANS_COMMAND_NAME);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getUpdateFinancialMeansGroups()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToDeleteDefendantFinancialMeans() {
        final Action action = createActionFor(DELETE_FINANCIAL_MEANS_COMMAND_NAME);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getDeleteFinancialMeansGroups()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldHandleUpdateFinancialMeansCommand() {
        assertThat(FinancialMeansApi.class, isHandlerClass(COMMAND_API)
                .with(method("updateFinancialMeans").thatHandles(UPDATE_FINANCIAL_MEANS_COMMAND_NAME)));
    }

    @Test
    public void shouldRenameUpdateFinancialMeansCommand() {
        final JsonEnvelope command = envelope().with(metadataWithRandomUUID(UPDATE_FINANCIAL_MEANS_COMMAND_NAME)).build();

        financialMeansApi.updateFinancialMeans(command);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata(), withMetadataEnvelopedFrom(command).withName(UPDATE_FINANCIAL_MEANS_NEW_COMMAND_NAME));
        assertThat(newCommand.payloadAsJsonObject(), equalTo(command.payloadAsJsonObject()));
    }

    @Test
    public void shouldHandleDeleteFinancialMeansCommand() {
        assertThat(FinancialMeansApi.class, isHandlerClass(COMMAND_API)
                .with(method("deleteDefendantFinancialMeans").thatHandles(DELETE_FINANCIAL_MEANS_COMMAND_NAME)));
    }

    @Test
    public void shouldRenameDeleteFinancialMeansCommand() {
        final JsonEnvelope command = envelope().with(metadataWithRandomUUID(DELETE_FINANCIAL_MEANS_COMMAND_NAME)).build();

        financialMeansApi.deleteDefendantFinancialMeans(command);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata(), withMetadataEnvelopedFrom(command).withName(DELETE_FINANCIAL_MEANS_NEW_COMMAND_NAME));
        assertThat(newCommand.payloadAsJsonObject(), equalTo(command.payloadAsJsonObject()));
    }
}
