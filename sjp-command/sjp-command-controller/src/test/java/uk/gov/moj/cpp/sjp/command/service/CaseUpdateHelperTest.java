package uk.gov.moj.cpp.sjp.command.service;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.moj.cpp.sjp.command.service.CaseUpdateHelper.RejectReason.CASE_ASSIGNED;
import static uk.gov.moj.cpp.sjp.command.service.CaseUpdateHelper.RejectReason.CASE_COMPLETED;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.command.service.AssignmentQueryService;
import uk.gov.moj.cpp.sjp.command.service.CaseUpdateHelper;
import uk.gov.moj.cpp.sjp.command.service.ResultingQueryService;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArrayBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)

public class CaseUpdateHelperTest {

    private static final String CASE_ID_KEY = "caseId";

    @InjectMocks
    private CaseUpdateHelper caseUpdateHelper;
    @Mock
    private ResultingQueryService resultingQueryService;
    @Mock
    private AssignmentQueryService assignmentQueryService;
    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    private JsonEnvelope inputCommand;
    private UUID caseId, userId;

    @Before
    public void init() {
        caseId = randomUUID();
        userId = randomUUID();
        inputCommand = getInputCommand(userId);
    }

    @Test
    public void shouldNotRejectCaseUpdateIfCaseNotAssignedAndNotCompleted() {
        //Given
        final JsonEnvelope caseDecision = getCaseDecision();
        final JsonEnvelope assignments = getAssignments();
        //When
        when(resultingQueryService.findCaseDecision(inputCommand)).thenReturn(caseDecision);
        when(assignmentQueryService.findAssignmentDetails(inputCommand)).thenReturn(assignments);

        final Optional<JsonEnvelope> rejectCommand = caseUpdateHelper.checkForCaseUpdateRejectReasons(inputCommand);

        //Then
        assertThat(rejectCommand.isPresent(), is(false));
    }

    @Test
    public void shouldRejectCaseUpdateIfCaseCompleted() {
        //Given
        final JsonEnvelope caseDecision = getCaseDecision(randomUUID());
        final JsonEnvelope expectedRejectCommand = getRejectCommand(CASE_COMPLETED);

        //When
        when(resultingQueryService.findCaseDecision(inputCommand)).thenReturn(caseDecision);

        final Optional<JsonEnvelope> rejectCommand = caseUpdateHelper.checkForCaseUpdateRejectReasons(inputCommand);

        //Then
        assertThat(rejectCommand.isPresent(), is(true));
        assertThat(rejectCommand.get().metadata().name(), is(expectedRejectCommand.metadata().name()));
        assertThat(rejectCommand.get().payloadAsJsonObject(), is(expectedRejectCommand.payloadAsJsonObject()));
    }

    @Test
    public void shouldRejectCaseUpdateIfCaseAssignedToSomebodyElse() {
        //Given
        final JsonEnvelope caseDecision = getCaseDecision();
        final JsonEnvelope assignments = getAssignments(randomUUID());
        final JsonEnvelope expectedRejectCommand = getRejectCommand(CASE_ASSIGNED);

        //When
        when(resultingQueryService.findCaseDecision(inputCommand)).thenReturn(caseDecision);
        when(assignmentQueryService.findAssignmentDetails(inputCommand)).thenReturn(assignments);

        final Optional<JsonEnvelope> rejectCommand = caseUpdateHelper.checkForCaseUpdateRejectReasons(inputCommand);

        //Then
        assertThat(rejectCommand.isPresent(), is(true));
        assertThat(rejectCommand.get().metadata().name(), is(expectedRejectCommand.metadata().name()));
        assertThat(rejectCommand.get().payloadAsJsonObject(), is(expectedRejectCommand.payloadAsJsonObject()));
    }

    @Test
    public void shouldNotRejectCaseUpdateIfCaseAssignedToCaller() {
        //Given
        final JsonEnvelope caseDecision = getCaseDecision();
        final JsonEnvelope assignments = getAssignments(userId);

        //When
        when(resultingQueryService.findCaseDecision(inputCommand)).thenReturn(caseDecision);
        when(assignmentQueryService.findAssignmentDetails(inputCommand)).thenReturn(assignments);

        final Optional<JsonEnvelope> rejectCommand = caseUpdateHelper.checkForCaseUpdateRejectReasons(inputCommand);

        //Then
        assertThat(rejectCommand.isPresent(), is(false));
    }

    private JsonEnvelope getCaseDecision(final UUID... decisionIds) {
        final JsonArrayBuilder caseDecisionBuilder = createArrayBuilder();
        for (final UUID decisionId : decisionIds) {
            caseDecisionBuilder.add(createObjectBuilder().add("id", decisionId.toString()));
        }

        return createEnvelope("resulting.query.case-decisions",
                createObjectBuilder().add("caseDecisions", caseDecisionBuilder).build());
    }


    private JsonEnvelope getAssignments(final UUID... userIds) {
        final JsonArrayBuilder assignmentsBuilder = createArrayBuilder();
        for (final UUID userId : userIds) {
            assignmentsBuilder.add(createObjectBuilder()
                    .add("id", randomUUID().toString())
                    .add("domainObjectId", caseId.toString())
                    .add("assignee", userId.toString()));
        }

        return createEnvelope("assignment.query.assignments",
                createObjectBuilder().add("assignments", assignmentsBuilder).build());
    }

    private JsonEnvelope getInputCommand(final UUID userId) {
        return envelope()
                .with(metadataWithRandomUUIDAndName().withUserId(userId.toString()))
                .withPayloadOf(caseId.toString(), CASE_ID_KEY)
                .build();
    }

    private JsonEnvelope getRejectCommand(final CaseUpdateHelper.RejectReason rejectReason) {
        return envelope()
                .with(metadataWithRandomUUID("sjp.command.case-update-rejected"))
                .withPayloadOf(caseId.toString(), CASE_ID_KEY)
                .withPayloadOf(rejectReason.name(), "reason")
                .build();
    }

}