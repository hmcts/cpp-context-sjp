package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.event.CaseAdjournedToLaterSjpHearingRecorded.caseAdjournedToLaterSjpHearingRecorded;
import static uk.gov.moj.cpp.sjp.event.CaseAdjournmentToLaterSjpHearingElapsed.caseAdjournmentToLaterSjpHearingElapsed;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.event.CaseAdjournedToLaterSjpHearingRecorded;
import uk.gov.moj.cpp.sjp.event.CaseAdjournmentToLaterSjpHearingElapsed;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseAdjournmentHandlerTest {

    private final UUID caseId = randomUUID();

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventStream caseEventStream;

    @Mock
    private CaseAggregate caseAggregate;

    @Spy
    private Clock clock = new StoppedClock(now(UTC));

    @Spy
    private Enveloper enveloper = createEnveloperWithEvents(
            CaseAdjournedToLaterSjpHearingRecorded.class,
            CaseAdjournmentToLaterSjpHearingElapsed.class);

    @InjectMocks
    private CaseAdjournmentHandler caseAdjournmentHandler;

    @Before
    public void init() {
        when(eventSource.getStreamById(caseId)).thenReturn(caseEventStream);
        when(aggregateService.get(caseEventStream, CaseAggregate.class)).thenReturn(caseAggregate);
    }

    @Test
    public void shouldRecordCaseAdjournedToLaterSjpHearing() throws EventStreamException {
        final UUID sessionId = randomUUID();
        final LocalDate adjournedTo = LocalDate.now();

        final JsonEnvelope command = envelopeFrom(metadataWithRandomUUID("sjp.command.record-case-adjourned-to-later-sjp-hearing"),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("sjpSessionId", sessionId.toString())
                        .add("adjournedTo", adjournedTo.toString())
                        .build());

        final CaseAdjournedToLaterSjpHearingRecorded event = caseAdjournedToLaterSjpHearingRecorded()
                .withCaseId(caseId)
                .withSessionId(sessionId)
                .withAdjournedTo(adjournedTo)
                .build();

        when(caseAggregate.recordCaseAdjournedToLaterSjpHearing(caseId, sessionId, adjournedTo)).thenReturn(Stream.of(event));

        caseAdjournmentHandler.recordCaseAdjournedToLaterSjpHearing(command);

        assertThat(caseEventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.case-adjourned-to-later-sjp-hearing-recorded"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                                        withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                                        withJsonPath("$.adjournedTo", equalTo(adjournedTo.toString()))
                                ))))));
    }

    @Test
    public void shouldRecordCaseAdjournmentToLaterSjpHearingElapsed() throws EventStreamException {
        final ZonedDateTime elapsedAt = clock.now();

        final JsonEnvelope command = envelopeFrom(metadataWithRandomUUID("sjp.command.record-case-adjournment-to-later-sjp-hearing-elapsed"),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .build());

        final CaseAdjournmentToLaterSjpHearingElapsed event = caseAdjournmentToLaterSjpHearingElapsed()
                .withCaseId(caseId)
                .withElapsedAt(elapsedAt)
                .build();

        when(caseAggregate.recordCaseAdjournmentToLaterSjpHearingElapsed(caseId, elapsedAt)).thenReturn(Stream.of(event));

        caseAdjournmentHandler.recordCaseAdjournmentToLaterSjpHearingElapsed(command);

        assertThat(caseEventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.case-adjournment-to-later-sjp-hearing-elapsed"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                                        withJsonPath("$.elapsedAt", equalTo(elapsedAt.toString()))
                                ))))));
    }

    @Test
    public void shouldHandleSessionCommands() {
        assertThat(CaseAdjournmentHandler.class, isHandlerClass(COMMAND_HANDLER)
                .with(allOf(
                        method("recordCaseAdjournedToLaterSjpHearing").thatHandles("sjp.command.record-case-adjourned-to-later-sjp-hearing"),
                        method("recordCaseAdjournmentToLaterSjpHearingElapsed").thatHandles("sjp.command.record-case-adjournment-to-later-sjp-hearing-elapsed")
                )));
    }
}

