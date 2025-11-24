package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.RecordCaseReferralForCourtHearingRejection.recordCaseReferralForCourtHearingRejection;
import static uk.gov.moj.cpp.sjp.event.CaseReferralForCourtHearingRejectionRecorded.caseReferralForCourtHearingRejectionRecorded;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.RecordCaseReferralForCourtHearingRejection;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.event.CaseReferralForCourtHearingRejectionRecorded;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtReferralHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private CaseAggregate caseAggregate;

    @Spy
    private Clock clock = new UtcClock();

    @Spy
    private Enveloper enveloper = createEnveloperWithEvents(
            CaseReferredForCourtHearing.class,
            CaseReferralForCourtHearingRejectionRecorded.class);

    @InjectMocks
    private CourtReferralHandler courtReferralHandler;

    @Test
    public void shouldAcknowledgeCaseCourtHearingReferral() throws EventStreamException {
        String rejectionReason = "rejection reason";
        UUID caseId = randomUUID();
        ZonedDateTime rejectionTimestamp = ZonedDateTime.of(2019, 12, 3, 11, 23, 2, 0, UTC);

        final RecordCaseReferralForCourtHearingRejection recordCaseReferralForCourtHearingRejection =
                recordCaseReferralForCourtHearingRejection()
                        .withCaseId(caseId)
                        .withRejectionReason(rejectionReason)
                        .withRejectedAt(rejectionTimestamp)
                        .build();

        final CaseReferralForCourtHearingRejectionRecorded caseReferralForCourtHearingRejectionRecorded =
                caseReferralForCourtHearingRejectionRecorded()
                        .withCaseId(caseId)
                        .withRejectionReason(rejectionReason)
                        .withRejectedAt(rejectionTimestamp)
                        .build();

        when(caseAggregate.recordCaseReferralForCourtHearingRejection(
                caseId,
                rejectionReason,
                rejectionTimestamp
        )).thenReturn(Stream.of(caseReferralForCourtHearingRejectionRecorded));

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        final Envelope<RecordCaseReferralForCourtHearingRejection> commandEnvelope =
                envelopeFrom(
                        metadataWithRandomUUID("sjp.command.record-refer-case-for-court-hearing-rejection"),
                        recordCaseReferralForCourtHearingRejection);

        courtReferralHandler.recordCaseReferralForCourtHearingRejection(commandEnvelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                metadata().envelopedWith(commandEnvelope.metadata()).withName(CaseReferralForCourtHearingRejectionRecorded.class.getAnnotation(Event.class).value()),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                                        withJsonPath("$.rejectionReason", equalTo(rejectionReason)),
                                        withJsonPath("$.rejectedAt", equalTo("2019-12-03T11:23:02.000Z"))
                                ))))));
    }

    @Test
    public void shouldHandleCourtReferralRelatedCommands() {
        assertThat(CourtReferralHandler.class, isHandlerClass(COMMAND_HANDLER)
                .with(method("recordCaseReferralForCourtHearingRejection").thatHandles("sjp.command.record-case-referral-for-court-hearing-rejection")));
    }

}
