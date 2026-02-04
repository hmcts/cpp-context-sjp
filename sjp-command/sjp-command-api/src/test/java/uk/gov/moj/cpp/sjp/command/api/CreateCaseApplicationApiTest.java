package uk.gov.moj.cpp.sjp.command.api;

import static java.util.Collections.singletonMap;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getCreateCaseApplicationGroups;

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
public class CreateCaseApplicationApiTest extends BaseDroolsAccessControlTest {

    public static final String SJP_CREATE_CASE_APPLICATION = "sjp.create-case-application";
    @Mock
    private Sender sender;

    @InjectMocks
    private CreateCaseApplicationApi createCaseApplicationApi;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public CreateCaseApplicationApiTest() {
        super("COMMAND_API_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @Test
    public void shouldAllowAuthorisedUserToCreateCaseApplication() {
        final Action action = createActionFor(SJP_CREATE_CASE_APPLICATION);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getCreateCaseApplicationGroups()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldHandleCreateCaseApplicationCommands() {
        assertThat(CreateCaseApplicationApi.class, isHandlerClass(COMMAND_API)
                .with(method("createCaseApplication")
                        .thatHandles(SJP_CREATE_CASE_APPLICATION)));
    }

    @Test
    public void shouldDispatchApplicationCreationCommand() {
        final JsonEnvelope commandEnvelope = envelope().
                with(metadataWithRandomUUID(SJP_CREATE_CASE_APPLICATION))
                .withPayloadFrom(createApplicationPayload())
                .build();


        createCaseApplicationApi.createCaseApplication(commandEnvelope);

        verify(sender).send(envelopeCaptor.capture());
        final JsonEnvelope sentCommandEnvelope = envelopeCaptor.getValue();
        assertThat(sentCommandEnvelope.metadata().name(), is("sjp.command.controller.create-case-application"));
        assertThat(commandEnvelope.payloadAsJsonObject(), equalTo(sentCommandEnvelope.payloadAsJsonObject()));
    }

    private JsonObject createApplicationPayload() {
        return createObjectBuilder()
                .add("courtApplication", createObjectBuilder()
                        .add("id", randomUUID().toString())
                        .add("type", createObjectBuilder()
                                .add("id", randomUUID().toString())
                                .add("type", "a")
                                .add("categoryCode", "a")
                                .add("linkType", "STANDALONE")
                                .add("jurisdiction", "MAGISTRATES")
                                .add("summonsTemplateType", "NOT_APPLICABLE")
                                .add("breachType", "NOT_APPLICABLE")
                                .add("appealFlag", false)
                                .add("applicantAppellantFlag", false)
                                .add("pleaApplicableFlag", false)
                                .add("commrOfOathFlag", false)
                                .add("courtOfAppealFlag", false)
                                .add("courtExtractAvlFlag", false)
                                .add("prosecutorThirdPartyFlag", false)
                                .add("spiOutApplicableFlag", false)
                                .add("offenceActiveOrder", "COURT_ORDER")
                        )
                        .add("applicationReceivedDate", "2020-07-03")
                        .add("applicationStatus", "DRAFT")
                        .add("subject", createObjectBuilder()
                                .add("id", randomUUID().toString())
                                .add("summonsRequired", false)
                                .add("notificationRequired", false)
                        )
                        .add("applicant", createObjectBuilder()
                                .add("id", randomUUID().toString())
                                .add("summonsRequired", false)
                                .add("notificationRequired", false)
                        )
                        .add("courtApplicationCases",
                                createArrayBuilder().add(createObjectBuilder()
                                        .add("prosecutionCaseId", randomUUID().toString())
                                        .add("prosecutionCaseIdentifier", createArrayBuilder().add(
                                                createObjectBuilder().add("prosecutionAuthorityId", randomUUID().toString())
                                                        .add("prosecutionAuthorityCode", "TFL")
                                                        .add("prosecutionAuthorityReference", "TFL12345-ABC").build()
                                        ).build())
                                        .add("isSJP", true)).build()
                        )
                )
                .build();
    }
}