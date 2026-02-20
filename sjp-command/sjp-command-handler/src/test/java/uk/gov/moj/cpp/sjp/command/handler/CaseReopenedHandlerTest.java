package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.NO_PLEA_RECEIVED;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.REOPENED_IN_LIBRA;
import static uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder.aDefaultSjpCase;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.CASE_ID;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.REOPEN_DATE;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.REOPEN_LIBRA_NUMBER;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.REOPEN_REASON;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.REOPEN_UPDATE_DATE;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.REOPEN_UPDATE_LIBRA_NUMBER;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.REOPEN_UPDATE_REASON;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.sjp.command.handler.builder.CaseReopenDetailsBuilder;
import uk.gov.moj.cpp.sjp.command.handler.common.EventNamesHolder;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.event.CaseReopened;
import uk.gov.moj.cpp.sjp.event.CaseReopenedUndone;
import uk.gov.moj.cpp.sjp.event.CaseReopenedUpdated;
import uk.gov.moj.cpp.sjp.event.CaseStatusChanged;

import java.util.stream.Stream;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseReopenedHandlerTest {

    private static final CaseReopenDetailsBuilder CASE_REOPEN_DETAILS =
            CaseReopenDetailsBuilder.defaultCaseReopenDetails();

    private static final CaseReopenDetailsBuilder CASE_REOPEN_DETAILS_UPDATED =
            CaseReopenDetailsBuilder.defaultCaseReopenUpdatedDetails();

    private final CaseAggregate caseAggregate = new CaseAggregate();
    private final Case aCase = aDefaultSjpCase().build();

    @InjectMocks
    private CaseReopenedHandler caseReopenedHandler;

    @Mock
    private EventSource eventSource;
    @Mock
    private EventStream eventStream;
    @Mock
    private AggregateService aggregateService;
    @Spy
    private Clock clock = new UtcClock();
    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CaseReopened.class, CaseReopenedUpdated.class, CaseReopenedUndone.class, CaseStatusChanged.class);
    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> argumentCaptor;

    @BeforeEach
    public void setup() {
        when(eventSource.getStreamById(eq(CASE_ID))).thenReturn(eventStream);
        when(aggregateService.get(any(EventStream.class), eq(CaseAggregate.class))).thenReturn(caseAggregate);

        caseAggregate.receiveCase(aCase, clock.now());
    }

    @Test
    public void shouldMarkCaseReopenedInLibra() throws EventStreamException {
        caseReopenedHandler.markCaseReopenedInLibra(CASE_REOPEN_DETAILS.buildJsonEnvelope(EventNamesHolder.CASE_REOPENED));

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), streamContaining(
                jsonEnvelope(metadata().withName(EventNamesHolder.CASE_REOPENED),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(CASE_ID.toString())),
                                withJsonPath("$.reopenedDate", equalTo(REOPEN_DATE.toString())),
                                withJsonPath("$.libraCaseNumber", equalTo(REOPEN_LIBRA_NUMBER)),
                                withJsonPath("$.reason", equalTo(REOPEN_REASON))))),
                jsonEnvelope(metadata().withName(EventNamesHolder.CASE_STATUS_CHANGED),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(CASE_ID.toString())),
                                withJsonPath("$.caseStatus", equalTo(REOPENED_IN_LIBRA.toString()))
                        ))
                )));
    }

    @Test
    public void shouldUpdateCaseReopenedInLibra() throws EventStreamException {
        caseAggregate.markCaseReopened(CASE_REOPEN_DETAILS.getCaseReopenDetails());

        caseReopenedHandler.updateCaseReopenedInLibra(CASE_REOPEN_DETAILS_UPDATED.buildJsonEnvelope(EventNamesHolder.CASE_REOPENED_UPDATED));

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), streamContaining(
                jsonEnvelope(metadata().withName(EventNamesHolder.CASE_REOPENED_UPDATED),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(CASE_ID.toString())),
                                withJsonPath("$.reopenedDate", equalTo(REOPEN_UPDATE_DATE.toString())),
                                withJsonPath("$.libraCaseNumber", equalTo(REOPEN_UPDATE_LIBRA_NUMBER)),
                                withJsonPath("$.reason", equalTo(REOPEN_UPDATE_REASON)))))));
    }

    @Test
    public void shouldUndoCaseReopenedInLibra() throws EventStreamException {
        // given
        caseAggregate.markCaseReopened(CASE_REOPEN_DETAILS.getCaseReopenDetails());
        caseAggregate.updateCaseReopened(CASE_REOPEN_DETAILS_UPDATED.getCaseReopenDetails());

        // when
        JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataOf(CASE_ID, EventNamesHolder.CASE_REOPENED_UNDONE).build(),
                Json.createObjectBuilder().add("caseId", CASE_ID.toString()).build());

        caseReopenedHandler.undoCaseReopenedInLibra(jsonEnvelope);

        verify(eventStream).append(argumentCaptor.capture());

        // then

        assertThat(argumentCaptor.getValue(), streamContaining(
                jsonEnvelope(metadata().withName(EventNamesHolder.CASE_REOPENED_UNDONE),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(CASE_ID.toString())),
                                withJsonPath("$.oldReopenedDate", equalTo(REOPEN_UPDATE_DATE.toString()))
                        ))
                ),
                jsonEnvelope(metadata().withName(EventNamesHolder.CASE_STATUS_CHANGED),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(CASE_ID.toString())),
                                withJsonPath("$.caseStatus", equalTo(NO_PLEA_RECEIVED.toString()))
                        ))
                )
        ));
    }
}