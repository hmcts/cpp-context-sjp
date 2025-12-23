package uk.gov.moj.cpp.sjp.command.api;

import static java.util.Collections.singletonMap;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getRequestDeleteDocsGroups;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
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
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RequestDeleteDocsApiTest extends BaseDroolsAccessControlTest {

    public static final String SJP_REQUEST_DELETE_DOCS = "sjp.request-delete-docs";
    @Mock
    private Sender sender;

    @InjectMocks
    private RequestDeleteDocsApi requestDeleteDocsApi;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public RequestDeleteDocsApiTest() {
        super("COMMAND_API_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @Test
    public void shouldAllowAuthorisedUserToUploadCaseDocument() {
        final Action action = createActionFor(SJP_REQUEST_DELETE_DOCS);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getRequestDeleteDocsGroups()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldHandleRequestDeleteDocsCommands() {
        assertThat(RequestDeleteDocsApi.class, isHandlerClass(COMMAND_API)
                .with(method("requestDeleteDocs")
                        .thatHandles(SJP_REQUEST_DELETE_DOCS)));
    }

    @Test
    public void shouldRequestDeleteDocs() {
        final JsonEnvelope commandEnvelope = envelope().
                with(metadataWithRandomUUID(SJP_REQUEST_DELETE_DOCS))
                .withPayloadFrom(createObjectBuilder()
                        .add("caseId", randomUUID().toString())
                        .build()
                )
                .build();

        requestDeleteDocsApi.requestDeleteDocs(commandEnvelope);

        verify(sender).send(envelopeCaptor.capture());
        final JsonEnvelope sentCommandEnvelope = envelopeCaptor.getValue();
        assertThat(sentCommandEnvelope.metadata().name(), is("sjp.command.request-delete-docs"));
        assertThat(commandEnvelope.payloadAsJsonObject(), equalTo(sentCommandEnvelope.payloadAsJsonObject()));
    }

}
