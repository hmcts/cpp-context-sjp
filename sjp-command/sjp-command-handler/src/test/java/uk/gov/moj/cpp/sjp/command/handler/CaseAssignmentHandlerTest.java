package uk.gov.moj.cpp.sjp.command.handler;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.event.CaseAssigned;
import uk.gov.moj.cpp.sjp.event.CaseAssignmentDeleted;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseAssignmentHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventStream eventStream;

    @Mock
    private CaseAggregate caseAggregate;

    @Spy
    private Enveloper enveloper = createEnveloperWithEvents(CaseAssigned.class, CaseAssignmentDeleted.class);

    @InjectMocks
    private CaseAssignmentHandler caseAssignmentHandler;

    @Test
    public void shouldHandleAssignmentCreatedCommand() throws Exception {
        final UUID caseId = UUID.randomUUID();
        final UUID assigneeId = UUID.randomUUID();
        final CaseAssignmentType caseAssignmentType = CaseAssignmentType.MAGISTRATE_DECISION;

        final JsonEnvelope command = envelope()
                .with(metadataWithRandomUUID("sjp.command.case-assignment-created"))
                .withPayloadOf(caseId.toString(), "caseId")
                .withPayloadOf(assigneeId.toString(), "assigneeId")
                .withPayloadOf(caseAssignmentType.toString(), "caseAssignmentType")
                .build();

        final CaseAssigned caseAssignmentCreated = new CaseAssigned(caseId, null, assigneeId, caseAssignmentType);
        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(caseAggregate.caseAssignmentCreated(caseId, assigneeId, caseAssignmentType)).thenReturn(Stream.of(caseAssignmentCreated));
        caseAssignmentHandler.assignmentCreated(command);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.case-assigned"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                                        withJsonPath("$.assigneeId", equalTo(assigneeId.toString())),
                                        withJsonPath("$.caseAssignmentType", equalTo(caseAssignmentType.toString())))))
                )));
    }

    @Test
    public void shouldHandleAssignmentDeletedCommand() throws Exception {
        final UUID caseId = UUID.randomUUID();
        final CaseAssignmentType caseAssignmentType = CaseAssignmentType.MAGISTRATE_DECISION;

        final JsonEnvelope command = envelope()
                .with(metadataWithRandomUUID("sjp.command.case-assignment-deleted"))
                .withPayloadOf(caseId.toString(), "caseId")
                .withPayloadOf(caseAssignmentType.toString(), "caseAssignmentType")
                .build();

        final CaseAssignmentDeleted caseAssignmentDeleted = new CaseAssignmentDeleted(caseId, caseAssignmentType);
        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(caseAggregate.caseAssignmentDeleted(caseId, caseAssignmentType)).thenReturn(Stream.of(caseAssignmentDeleted));
        caseAssignmentHandler.assignmentDeleted(command);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.case-assignment-deleted"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                                        withJsonPath("$.caseAssignmentType", equalTo(caseAssignmentType.toString())))))
                )));
    }
}
