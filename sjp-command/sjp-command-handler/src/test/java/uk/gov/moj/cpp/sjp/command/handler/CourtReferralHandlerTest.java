package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomUtils.nextInt;
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
import static uk.gov.moj.cpp.sjp.ReferCaseForCourtHearing.referCaseForCourtHearing;
import static uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing.caseReferredForCourtHearing;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.ReferCaseForCourtHearing;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;

import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
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
    private Enveloper enveloper = createEnveloperWithEvents(CaseReferredForCourtHearing.class);

    @InjectMocks
    private CourtReferralHandler courtReferralHandler;

    @Test
    public void shouldReferCaseForCourtHearing() throws EventStreamException {

        final ReferCaseForCourtHearing referCaseForCourtHearing = referCaseForCourtHearing()
                .withCaseId(randomUUID())
                .withSessionId(randomUUID())
                .withReferralReasonId(randomUUID())
                .withHearingTypeId(randomUUID())
                .withEstimatedHearingDuration(nextInt(0, 999))
                .withListingNotes(randomAlphanumeric(100))
                .withRequestedAt(now(UTC))
                .build();

        final CaseReferredForCourtHearing caseReferredForCourtHearing = caseReferredForCourtHearing()
                .withCaseId(referCaseForCourtHearing.getCaseId())
                .withSessionId(referCaseForCourtHearing.getSessionId())
                .withReferralReasonId(referCaseForCourtHearing.getReferralReasonId())
                .withHearingTypeId(referCaseForCourtHearing.getHearingTypeId())
                .withEstimatedHearingDuration(referCaseForCourtHearing.getEstimatedHearingDuration())
                .withListingNotes(referCaseForCourtHearing.getListingNotes())
                .withReferredAt(referCaseForCourtHearing.getRequestedAt())
                .build();

        when(caseAggregate.referCaseForCourtHearing(
                referCaseForCourtHearing.getCaseId(),
                referCaseForCourtHearing.getSessionId(),
                referCaseForCourtHearing.getReferralReasonId(),
                referCaseForCourtHearing.getHearingTypeId(),
                referCaseForCourtHearing.getEstimatedHearingDuration(),
                referCaseForCourtHearing.getListingNotes(),
                referCaseForCourtHearing.getRequestedAt()
        )).thenReturn(Stream.of(caseReferredForCourtHearing));

        when(eventSource.getStreamById(referCaseForCourtHearing.getCaseId())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        final Envelope<ReferCaseForCourtHearing> referCaseForCourtHearingCommand = envelopeFrom(metadataWithRandomUUID("sjp.command.refer-case-for-court-hearing"), referCaseForCourtHearing);

        courtReferralHandler.referCaseForCourtHearing(referCaseForCourtHearingCommand);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                metadata().envelopedWith(referCaseForCourtHearingCommand.metadata()).withName(CaseReferredForCourtHearing.class.getAnnotation(Event.class).value()),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(caseReferredForCourtHearing.getCaseId().toString())),
                                        withJsonPath("$.sessionId", equalTo(caseReferredForCourtHearing.getSessionId().toString())),
                                        withJsonPath("$.referralReasonId", equalTo(caseReferredForCourtHearing.getReferralReasonId().toString())),
                                        withJsonPath("$.hearingTypeId", equalTo(caseReferredForCourtHearing.getHearingTypeId().toString())),
                                        withJsonPath("$.estimatedHearingDuration", equalTo(caseReferredForCourtHearing.getEstimatedHearingDuration())),
                                        withJsonPath("$.listingNotes", equalTo(caseReferredForCourtHearing.getListingNotes())),
                                        withJsonPath("$.referredAt", equalTo(caseReferredForCourtHearing.getReferredAt().toString()))
                                ))))));
    }

    @Test
    public void shouldHandleCourtReferralRelatedCommands() {
        assertThat(CourtReferralHandler.class, isHandlerClass(COMMAND_HANDLER)
                .with(method("referCaseForCourtHearing").thatHandles("sjp.command.refer-case-for-court-hearing")));
    }

}