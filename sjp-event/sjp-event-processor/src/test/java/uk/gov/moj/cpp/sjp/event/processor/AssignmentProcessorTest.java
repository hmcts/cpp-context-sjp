package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.ASSIGNEE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.REASON;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.SESSION_ID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.domain.AssignmentCandidate;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseAssignmentTimeoutProcess;
import uk.gov.moj.cpp.sjp.event.processor.service.assignment.AssignmentService;
import uk.gov.moj.cpp.sjp.event.session.CaseAssigned;
import uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected;
import uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRequested;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AssignmentProcessorTest {

    private final UUID sessionId = randomUUID();
    private final UUID caseId = randomUUID();
    private final UUID assigneeId = randomUUID();
    private final String courtHouseCode = randomAlphanumeric(4);
    private final String localJusticeAreaNationalCourtCode = randomNumeric(4);

    @Mock
    private Sender sender;

    @Mock
    private AssignmentService assignmentService;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeCaptor;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private CaseAssignmentTimeoutProcess caseAssignmentTimeoutProcess;

    @InjectMocks
    private AssignmentProcessor assignmentProcessor;

    @Test
    public void shouldReturnListOfAssignmentCandidatesForMagistrateSession() {
        final JsonEnvelope caseAssignmentRequestedEvent = envelopeFrom(metadataWithRandomUUID(CaseAssignmentRequested.EVENT_NAME), createObjectBuilder()
                .add("session", createObjectBuilder()
                        .add("id", sessionId.toString())
                        .add("type", MAGISTRATE.name())
                        .add("userId", assigneeId.toString())
                        .add("courtHouseCode", courtHouseCode)
                ).build());


        final AssignmentCandidate assignmentCandidate1 = new AssignmentCandidate(randomUUID(), 1);
        final AssignmentCandidate assignmentCandidate2 = new AssignmentCandidate(randomUUID(), 2);

        final List<AssignmentCandidate> assignmentCandidates = Arrays.asList(assignmentCandidate1, assignmentCandidate2);

        when(assignmentService.getAssignmentCandidates(caseAssignmentRequestedEvent, assigneeId, courtHouseCode, MAGISTRATE)).thenReturn(assignmentCandidates);

        assignmentProcessor.handleCaseAssignmentRequestedEvent(caseAssignmentRequestedEvent);

        verify(sender).send(jsonEnvelopeCaptor.capture());
        assertThat(jsonEnvelopeCaptor.getValue(), jsonEnvelope(withMetadataEnvelopedFrom(caseAssignmentRequestedEvent)
                .withName("sjp.command.assign-case-from-candidates-list"), payload().isJson(allOf(
                withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                withJsonPath("$.assignmentCandidates[0].caseId", equalTo(assignmentCandidate1.getCaseId().toString())),
                withJsonPath("$.assignmentCandidates[0].caseStreamVersion", equalTo(assignmentCandidate1.getCaseStreamVersion())),
                withJsonPath("$.assignmentCandidates[1].caseId", equalTo(assignmentCandidate2.getCaseId().toString())),
                withJsonPath("$.assignmentCandidates[1].caseStreamVersion", equalTo(assignmentCandidate2.getCaseStreamVersion()))
        ))));
    }

    @Test
    public void shouldReturnListOfAssignmentCandidatesForDelegatedPowersSession() {
        final JsonEnvelope caseAssignmentRequestedEvent = envelopeFrom(metadataWithRandomUUID(CaseAssignmentRequested.EVENT_NAME), createObjectBuilder()
                .add("session", createObjectBuilder()
                        .add("id", sessionId.toString())
                        .add("type", DELEGATED_POWERS.name())
                        .add("userId", assigneeId.toString())
                        .add("courtHouseCode", courtHouseCode)
                ).build());

        final AssignmentCandidate assignmentCandidate1 = new AssignmentCandidate(randomUUID(), 1);
        final AssignmentCandidate assignmentCandidate2 = new AssignmentCandidate(randomUUID(), 2);

        final List<AssignmentCandidate> assignmentCandidates = Arrays.asList(assignmentCandidate1, assignmentCandidate2);

        when(assignmentService.getAssignmentCandidates(caseAssignmentRequestedEvent, assigneeId, courtHouseCode, DELEGATED_POWERS)).thenReturn(assignmentCandidates);

        assignmentProcessor.handleCaseAssignmentRequestedEvent(caseAssignmentRequestedEvent);

        verify(sender).send(jsonEnvelopeCaptor.capture());
        assertThat(jsonEnvelopeCaptor.getValue(), jsonEnvelope(withMetadataEnvelopedFrom(caseAssignmentRequestedEvent)
                .withName("sjp.command.assign-case-from-candidates-list"), payload().isJson(allOf(
                withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                withJsonPath("$.assignmentCandidates[0].caseId", equalTo(assignmentCandidate1.getCaseId().toString())),
                withJsonPath("$.assignmentCandidates[0].caseStreamVersion", equalTo(assignmentCandidate1.getCaseStreamVersion())),
                withJsonPath("$.assignmentCandidates[1].caseId", equalTo(assignmentCandidate2.getCaseId().toString())),
                withJsonPath("$.assignmentCandidates[1].caseStreamVersion", equalTo(assignmentCandidate2.getCaseStreamVersion()))
        ))));
    }

    @Test
    public void shouldEmitCaseNotAssignedPublicEvent() {
        final JsonEnvelope caseAssignmentRequestedEvent = envelopeFrom(metadataWithRandomUUID(CaseAssignmentRequested.EVENT_NAME), createObjectBuilder()
                .add("session", createObjectBuilder()
                        .add("id", sessionId.toString())
                        .add("type", DELEGATED_POWERS.name())
                        .add("userId", assigneeId.toString())
                        .add("courtHouseCode", courtHouseCode)
                ).build());

        when(assignmentService.getAssignmentCandidates(caseAssignmentRequestedEvent, assigneeId, courtHouseCode, DELEGATED_POWERS)).thenReturn(Collections.emptyList());

        assignmentProcessor.handleCaseAssignmentRequestedEvent(caseAssignmentRequestedEvent);

        verify(sender).send(jsonEnvelopeCaptor.capture());
        assertThat(jsonEnvelopeCaptor.getValue(), jsonEnvelope().withMetadataOf(withMetadataEnvelopedFrom(caseAssignmentRequestedEvent)
                .withName("public.sjp.case-not-assigned")));
    }

    @Test
    public void shouldEmitCaseAssignedPublicEventAndReplicateEventInAssignmentContextAndStartAssignmentTimer() {

        final JsonEnvelope caseAssignedEvent = envelopeFrom(metadataWithRandomUUID(CaseAssigned.EVENT_NAME), createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .add(ASSIGNEE_ID, assigneeId.toString())
                .add("caseAssignmentType", CaseAssignmentType.MAGISTRATE_DECISION.toString())
                .build());

        assignmentProcessor.handleCaseAssignedEvent(caseAssignedEvent);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(caseAssignedEvent).withName("public.sjp.case-assigned"),
                payloadIsJson(withJsonPath(CASE_ID, equalTo(caseId.toString())))
        )));

        //TODO remove (ATCM-3097)
        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(caseAssignedEvent).withName("assignment.command.add-assignment-to"),
                payloadIsJson(allOf(
                        withJsonPath("id"),
                        withJsonPath("version", equalTo(0)),
                        withJsonPath("domainObjectId", equalTo(caseId.toString())),
                        withJsonPath("assignmentNatureType", equalTo(CaseAssignmentType.MAGISTRATE_DECISION.toString())),
                        withJsonPath("assignee", equalTo(assigneeId.toString()))
                ))
        )));

        verify(caseAssignmentTimeoutProcess).startTimer(caseId, Duration.ofMinutes(60));
    }

    @Test
    public void shouldReplicateCaseUnassignedEventInAssignmentContextAndCancelAssignmentTimer() {
        final JsonEnvelope caseAssignedEvent = envelopeFrom(metadataWithRandomUUID(CaseUnassigned.EVENT_NAME), createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .build());

        assignmentProcessor.handleCaseUnassignedEvent(caseAssignedEvent);

        verify(caseAssignmentTimeoutProcess).cancelTimer(caseId);

        //TODO remove (ATCM-3097)
        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(caseAssignedEvent).withName("assignment.command.remove-assignment"),
                payloadIsJson(withJsonPath("domainObjectId", equalTo(caseId.toString())))
        )));

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(caseAssignedEvent).withName(AssignmentProcessor.PUBLIC_SJP_CASE_UNASSIGNED),
                payloadIsJson(withJsonPath(CASE_ID, equalTo(caseId.toString()))))));
    }

    @Test
    public void shouldEmitCaseAssignmentRejectedPublicEvent() {
        final CaseAssignmentRejected.RejectReason rejectionReason = CaseAssignmentRejected.RejectReason.SESSION_ENDED;

        final JsonEnvelope caseAssignmentRejectedEvent = envelopeFrom(metadataWithRandomUUID(CaseAssignmentRejected.EVENT_NAME), createObjectBuilder()
                .add(SESSION_ID, sessionId.toString())
                .add(REASON, rejectionReason.toString())
                .build());

        assignmentProcessor.handleCaseAssignmentRejectedEvent(caseAssignmentRejectedEvent);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(caseAssignmentRejectedEvent).withName("public.sjp.case-assignment-rejected"),
                payloadIsJson(withJsonPath(REASON, equalTo(rejectionReason.toString())))
        )));
    }

    @Test
    public void shouldHandleCaseAlreadyAssignedEvent() {
        final JsonEnvelope caseAlreadyAssignedEvent = envelopeFrom(metadataWithRandomUUID(CaseUnassigned.EVENT_NAME), createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .build());

        assignmentProcessor.handleCaseAlreadyAssignedEvent(caseAlreadyAssignedEvent);

        verify(caseAssignmentTimeoutProcess).resetTimer(caseId, Duration.ofMinutes(60));

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(caseAlreadyAssignedEvent).withName("public.sjp.case-assigned"),
                payloadIsJson(withJsonPath(CASE_ID, equalTo(caseId.toString())))
        )));
    }
}
