package uk.gov.moj.cpp.sjp.command.api;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.json.schemas.domains.sjp.NoteType.ADJOURNMENT;
import static uk.gov.justice.json.schemas.domains.sjp.NoteType.CASE;
import static uk.gov.justice.json.schemas.domains.sjp.NoteType.DECISION;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getAddCaseNoteGroups;

import uk.gov.justice.json.schemas.domains.sjp.NoteType;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;
import java.util.UUID;

import javax.json.JsonObjectBuilder;
import javax.ws.rs.BadRequestException;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseNoteApiTest extends BaseDroolsAccessControlTest {

    public static final String SJP_ADD_CASE_NOTE = "sjp.add-case-note";
    @Mock
    private Sender sender;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @InjectMocks
    private CaseNoteApi caseNoteApi;

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }

    @Test
    public void shouldAllowAuthorisedUserToAddCaseNote() {
        final Action action = createActionFor(SJP_ADD_CASE_NOTE);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getAddCaseNoteGroups()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldHandleCaseNoteCommands() {
        assertThat(CaseNoteApi.class, isHandlerClass(COMMAND_API)
                .with(method("addCaseNote").thatHandles(SJP_ADD_CASE_NOTE)));
    }

    @Test
    public void shouldRejectAddCaseNoteCommandWithDecisionTypeAndWithoutDecisionId() {
        final UUID decisionId = null;
        final JsonEnvelope command = createAddCaseNoteCommand(DECISION, decisionId);

        try {
            caseNoteApi.addCaseNote(command);
            fail("Bad request exception should be throws");
        } catch (final BadRequestException e) {
            assertThat(e.getMessage(), is("Field decisionId is required for DECISION note"));
        }
    }

    @Test
    public void shouldRejectAddCaseNoteCommandWithAdjournmentTypeAndWithoutDecisionId() {
        final UUID decisionId = null;
        final JsonEnvelope command = createAddCaseNoteCommand(ADJOURNMENT, decisionId);

        try {
            caseNoteApi.addCaseNote(command);
            fail("Bad request exception should be throws");
        } catch (final BadRequestException e) {
            assertThat(e.getMessage(), is("Field decisionId is required for ADJOURNMENT note"));
        }
    }

    @Test
    public void shouldRejectAddCaseNoteCommandWithCaseTypeAndWithDecisionId() {
        final UUID decisionId = randomUUID();
        final JsonEnvelope command = createAddCaseNoteCommand(CASE, decisionId);

        try {
            caseNoteApi.addCaseNote(command);
            fail("Bad request exception should be throws");
        } catch (final BadRequestException e) {
            assertThat(e.getMessage(), is("Field decisionId is not allowed for CASE note"));
        }
    }

    @Test
    public void shouldAcceptAddCaseNoteCommandWithDecisionTypeAndDecisionId() {
        final UUID decisionId = randomUUID();
        final JsonEnvelope command = createAddCaseNoteCommand(DECISION, decisionId);

        caseNoteApi.addCaseNote(command);

        verify(sender).send(argThat(jsonEnvelope(withMetadataEnvelopedFrom(command)
                        .withName("sjp.command.controller.add-case-note"),
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", equalTo(command.payloadAsJsonObject().getString("caseId"))),
                        withJsonPath("$.noteType", equalTo(command.payloadAsJsonObject().getString("noteType"))),
                        withJsonPath("$.noteText", equalTo(command.payloadAsJsonObject().getString("noteText"))),
                        withJsonPath("$.decisionId", equalTo(command.payloadAsJsonObject().getString("decisionId")))
                )))));
    }

    @Test
    public void shouldAcceptAddCaseNoteCommandWhenDecisionIsNotRequired() {
        final UUID decisionId = null;
        final JsonEnvelope command = createAddCaseNoteCommand(CASE, decisionId);

        caseNoteApi.addCaseNote(command);

        verify(sender).send(argThat(jsonEnvelope(withMetadataEnvelopedFrom(command)
                        .withName("sjp.command.controller.add-case-note"),
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", equalTo(command.payloadAsJsonObject().getString("caseId"))),
                        withJsonPath("$.noteType", equalTo(command.payloadAsJsonObject().getString("noteType"))),
                        withJsonPath("$.noteText", equalTo(command.payloadAsJsonObject().getString("noteText"))),
                        withoutJsonPath("$.decisionId")
                )))));
    }

    private static JsonEnvelope createAddCaseNoteCommand(final NoteType noteType, final UUID decisionId) {
        final JsonObjectBuilder notePayloadBuilder = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("noteType", noteType.name())
                .add("noteText", "note");

        if (nonNull(decisionId)) {
            notePayloadBuilder.add("decisionId", decisionId.toString());
        }

        return envelopeFrom(metadataWithRandomUUID("sjp.add-case-note"), notePayloadBuilder.build());
    }
}
