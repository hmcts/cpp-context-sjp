package uk.gov.moj.cpp.sjp.command.controller;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.command.service.UserService;

import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class CaseNoteControllerTest {

    @Mock
    private Sender sender;

    @Spy
    private Clock clock = new StoppedClock(now(UTC));

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private UserService userService;

    @InjectMocks
    private CaseNoteController caseNoteController;

    private final UUID userId = randomUUID();

    private final JsonObject userDetails = createObjectBuilder()
            .add("firstName", "John")
            .add("lastName", "Smith")
            .build();

    @Test
    public void shouldHandleCaseNoteCommands() {
        assertThat(CaseNoteController.class, isHandlerClass(COMMAND_CONTROLLER)
                .with(method("addCaseNote").thatHandles("sjp.command.controller.add-case-note")));
    }

    @Test
    public void shouldEnrichAddCaseNoteWithCommandWithDecisionIdWithNoteIdCreationTimeAndUserDetails() {
        final JsonEnvelope addCaseNoteCommand = createAddCaseNoteCommand(userId, randomUUID());

        when(userService.getCallingUserDetails(addCaseNoteCommand)).thenReturn(userDetails);

        caseNoteController.addCaseNote(addCaseNoteCommand);

        verifyCommandPassed(addCaseNoteCommand);
    }

    @Test
    public void shouldEnrichCaseTypeAddCaseNoteWithoutDecisionIdWithCommandWithNoteIdCreationTimeAndUserDetails() {
        final JsonEnvelope addCaseNoteCommand = createAddCaseNoteCommand(userId, null);

        when(userService.getCallingUserDetails(addCaseNoteCommand)).thenReturn(userDetails);

        caseNoteController.addCaseNote(addCaseNoteCommand);

        verifyCommandPassed(addCaseNoteCommand);
    }

    private void verifyCommandPassed(final JsonEnvelope addCaseNoteCommand) {
        final JsonObject addCaseNote = addCaseNoteCommand.payloadAsJsonObject();

        final String decisionId = addCaseNote.getString("decisionId", null);

        final Matcher<? super ReadContext> decisionIdMatcher = nonNull(decisionId) ? withJsonPath("$.decisionId", is(decisionId)) : withoutJsonPath("$.decisionId");

        verify(sender).send(argThat(jsonEnvelope(withMetadataEnvelopedFrom(addCaseNoteCommand)
                        .withName("sjp.command.add-case-note"),
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", equalTo(addCaseNoteCommand.payloadAsJsonObject().getString("caseId"))),
                        withJsonPath("$.note", isJson(allOf(
                                withJsonPath("id", notNullValue()),
                                withJsonPath("addedAt", is(clock.now().toString())),
                                withJsonPath("text", is(addCaseNote.getString("noteText"))),
                                withJsonPath("type", is(addCaseNote.getString("noteType")))
                        ))),
                        withJsonPath("$.author", isJson(allOf(
                                withJsonPath("userId", is(userId.toString())),
                                withJsonPath("firstName", is(userDetails.getString("firstName"))),
                                withJsonPath("lastName", is(userDetails.getString("lastName")))
                        ))),
                        decisionIdMatcher
                )))));
    }

    private static JsonEnvelope createAddCaseNoteCommand(final UUID userId, final UUID decisionId) {
        final JsonObjectBuilder notePayloadBuilder = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("noteType", "CASE")
                .add("noteText", "note");

        if (nonNull(decisionId)) {
            notePayloadBuilder.add("decisionId", decisionId.toString());
        }

        return envelopeFrom(metadataWithRandomUUID("sjp.controller.add-case-note").withUserId(userId.toString()), notePayloadBuilder.build());
    }
}
