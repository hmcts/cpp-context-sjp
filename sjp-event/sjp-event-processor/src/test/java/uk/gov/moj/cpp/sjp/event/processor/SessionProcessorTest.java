package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.domain.AssignmentCandidate;
import uk.gov.moj.cpp.sjp.event.processor.service.AssignmentService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SessionProcessorTest {

    private static final String EVENT_NAME = "sjp.events.session-started";

    @Mock
    private Sender sender;

    @Mock
    private AssignmentService assignmentService;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeCaptor;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @InjectMocks
    private SessionProcessor sessionProcessor;

    private UUID sessionId, assigneeId;
    private String courtCode;
    private LocalDateTime startedAt;

    @Before
    public void init() {
        sessionId = randomUUID();
        assigneeId = randomUUID();
        courtCode = RandomStringUtils.randomNumeric(4);
        startedAt = LocalDateTime.now();
    }

    @Test
    public void shouldReturnListOfAssignmentCandidatesForMagistrateSession() {

        final String magistrate = "Magistrate";

        final JsonEnvelope sessionStartedEvent = envelope().with(metadataWithRandomUUID(EVENT_NAME))
                .withPayloadOf(sessionId, "sessionId")
                .withPayloadOf(assigneeId, "legalAdviserId")
                .withPayloadOf(courtCode, "courtCode")
                .withPayloadOf(startedAt.toString(), "startedAt")
                .withPayloadOf(magistrate, "magistrate")
                .build();

        final AssignmentCandidate assignmentCandidate1 = new AssignmentCandidate(randomUUID(), 1);
        final AssignmentCandidate assignmentCandidate2 = new AssignmentCandidate(randomUUID(), 2);

        final List<AssignmentCandidate> assignmentCandidates = Arrays.asList(assignmentCandidate1, assignmentCandidate2);

        when(assignmentService.getAssignmentCandidates(sessionStartedEvent, assigneeId, courtCode, MAGISTRATE)).thenReturn(assignmentCandidates);

        sessionProcessor.magistrateSessionStarted(sessionStartedEvent);

        verify(sender).send(jsonEnvelopeCaptor.capture());
        assertThat(jsonEnvelopeCaptor.getValue(), jsonEnvelope(withMetadataEnvelopedFrom(sessionStartedEvent)
                .withName("sjp.command.assign-case"), payload().isJson(allOf(
                withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                withJsonPath("$.assignmentCandidates[0].caseId", equalTo(assignmentCandidate1.getCaseId().toString())),
                withJsonPath("$.assignmentCandidates[0].caseStreamVersion", equalTo(assignmentCandidate1.getCaseStreamVersion())),
                withJsonPath("$.assignmentCandidates[1].caseId", equalTo(assignmentCandidate2.getCaseId().toString())),
                withJsonPath("$.assignmentCandidates[1].caseStreamVersion", equalTo(assignmentCandidate2.getCaseStreamVersion()))
        ))).thatMatchesSchema());
    }

    @Test
    public void shouldReturnListOfAssignmentCandidatesForDelegatedPowersSession() {

        final JsonEnvelope sessionStartedEvent = envelope().with(metadataWithRandomUUID(EVENT_NAME))
                .withPayloadOf(sessionId, "sessionId")
                .withPayloadOf(assigneeId, "legalAdviserId")
                .withPayloadOf(courtCode, "courtCode")
                .withPayloadOf(startedAt.toString(), "startedAt")
                .build();

        final AssignmentCandidate assignmentCandidate1 = new AssignmentCandidate(randomUUID(), 1);
        final AssignmentCandidate assignmentCandidate2 = new AssignmentCandidate(randomUUID(), 2);

        final List<AssignmentCandidate> assignmentCandidates = Arrays.asList(assignmentCandidate1, assignmentCandidate2);

        when(assignmentService.getAssignmentCandidates(sessionStartedEvent, assigneeId, courtCode, DELEGATED_POWERS)).thenReturn(assignmentCandidates);

        sessionProcessor.delegatedPowersSessionStarted(sessionStartedEvent);

        verify(sender).send(jsonEnvelopeCaptor.capture());
        assertThat(jsonEnvelopeCaptor.getValue(), jsonEnvelope(withMetadataEnvelopedFrom(sessionStartedEvent)
                .withName("sjp.command.assign-case"), payload().isJson(allOf(
                withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                withJsonPath("$.assignmentCandidates[0].caseId", equalTo(assignmentCandidate1.getCaseId().toString())),
                withJsonPath("$.assignmentCandidates[0].caseStreamVersion", equalTo(assignmentCandidate1.getCaseStreamVersion())),
                withJsonPath("$.assignmentCandidates[1].caseId", equalTo(assignmentCandidate2.getCaseId().toString())),
                withJsonPath("$.assignmentCandidates[1].caseStreamVersion", equalTo(assignmentCandidate2.getCaseStreamVersion()))
        ))).thatMatchesSchema());
    }

    @Test
    public void shouldCreatePublicSessionCreatedEventWithoutAssignment() {

        final JsonEnvelope sessionStartedEvent = envelope().with(metadataWithRandomUUID(EVENT_NAME))
                .withPayloadOf(sessionId, "sessionId")
                .withPayloadOf(assigneeId, "legalAdviserId")
                .withPayloadOf(courtCode, "courtCode")
                .withPayloadOf(startedAt.toString(), "startedAt")
                .build();

        when(assignmentService.getAssignmentCandidates(sessionStartedEvent, assigneeId, courtCode, DELEGATED_POWERS)).thenReturn(Collections.emptyList());

        sessionProcessor.delegatedPowersSessionStarted(sessionStartedEvent);

        verify(sender).send(jsonEnvelopeCaptor.capture());
        assertThat(jsonEnvelopeCaptor.getValue(), jsonEnvelope(withMetadataEnvelopedFrom(sessionStartedEvent)
                .withName("public.sjp.session-started"), payload().isJson(allOf(
                withJsonPath("$.sessionId", equalTo(sessionId.toString()))
        ))).thatMatchesSchema());
    }
}
