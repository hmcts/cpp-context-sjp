package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.event.CaseStatusChanged;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class ResolveCaseStatusHandlerTest {

    private final UUID caseId = UUID.randomUUID();

    @Mock
    private EventSource eventSource;

    @Spy
    private Enveloper enveloper = createEnveloperWithEvents(CaseStatusChanged.class);

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventStream eventStream;

    @Mock
    private CaseAggregate caseAggregate;

    @InjectMocks
    private ResolveCaseStatusHandler resolveCaseStatusHandler;

    @Test
    public void shouldCreateCaseStatusChanged() throws EventStreamException {
        // given
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("sjp.command.resolve-case-status"),
                createObjectBuilder().add("caseId", caseId.toString()));

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        final CaseStatusChanged caseStatusChanged = new CaseStatusChanged(caseId, CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION);
        when(caseAggregate.resolveCaseStatus()).thenReturn(Stream.of(caseStatusChanged));

        // when
        resolveCaseStatusHandler.resolveCaseStatus(jsonEnvelope);

        // then
        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(jsonEnvelope(withMetadataEnvelopedFrom(jsonEnvelope)
                                .withName(CaseStatusChanged.EVENT_NAME),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.caseStatus", equalTo(CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION.toString()))
                        ))))));
    }

}