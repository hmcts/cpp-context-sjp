package uk.gov.moj.cpp.sjp.command.controller;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.json.Json.createArrayBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.sjp.command.service.ReadyCasesService;

import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class AssignmentControllerTest {

    @Mock
    private Sender sender;

    @Mock
    private ReadyCasesService readyCasesService;

    @Captor
    private ArgumentCaptor<Envelope<JsonValue>> envelopeCaptor;

    @InjectMocks
    private AssignmentController assignmentController;

    private final UUID caseToBeAssignedId = randomUUID();

    private final UUID userId = randomUUID();

    private final UUID readyCaseId = randomUUID();

    private final JsonObject readyCases = createObjectBuilder()
            .add("readyCases", createArrayBuilder()
                    .add(createObjectBuilder()
                            .add("caseId", caseToBeAssignedId.toString())
                            .add("reason", "PIA")
                            .add("assigneeId", userId.toString()))
                    .add(createObjectBuilder()
                            .add("caseId", readyCaseId.toString())
                            .add("reason", "WITHDRAWAL_REQUESTED")
                            .add("assigneeId", userId.toString()))
            )
            .build();

    @Test
    public void shouldHandleAssignCaseCommands() {
        assertThat(AssignmentController.class, isHandlerClass(COMMAND_CONTROLLER)
                .with(method("assignCase").thatHandles("sjp.command.controller.assign-case")));
    }

    @Test
    public void shouldEnrichCaseToBeAssignedWithCasesToBeUnassigned() {
        final JsonEnvelope assignCaseCommand = createAssignCaseCommand(userId);

        when(readyCasesService.getReadyCasesAssignedToUser(userId, assignCaseCommand)).thenReturn(readyCases);

        assignmentController.assignCase(assignCaseCommand);

        verifyAssignCaseCommandPassed(assignCaseCommand);
    }

    private JsonEnvelope createAssignCaseCommand(final UUID userId) {
        final JsonObjectBuilder readyCasePayloadBuilder = createObjectBuilder()
                .add("caseId", caseToBeAssignedId.toString())
                .add("userId", userId.toString());

        return envelopeFrom(metadataWithRandomUUID("sjp.command.controller.assign-case").withUserId(userId.toString()), readyCasePayloadBuilder.build());
    }

    private void verifyAssignCaseCommandPassed(final JsonEnvelope envelope) {
        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonValue> commandSent = envelopeCaptor.getValue();
        assertThat(commandSent.metadata(), JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom(envelope).withName("sjp.command.assign-case"));

        assertThat(commandSent.payload(), JsonEnvelopePayloadMatcher.payloadIsJson(allOf(
                withJsonPath("$.assignCase", equalTo(caseToBeAssignedId.toString())),
                withJsonPath("$.unassignCases", hasSize(1)),
                withJsonPath("$.unassignCases[0]", equalTo(readyCaseId.toString()))
        )));
    }
}