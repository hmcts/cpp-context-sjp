package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.json.schemas.domains.sjp.AddCaseAssignmentRestriction;
import uk.gov.justice.json.schemas.domains.sjp.events.CaseAssignmentRestrictionAdded;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAssignmentRestrictionAggregate;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;


import javax.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseAssignmentRestrictionHandlerTest {
    private static final String PROSECUTING_AUTHORITY = "TVL";
    private static final List<String> INCLUDE_ONLY = singletonList("1234");
    private static final List<String> EXCLUDE = singletonList("9876");
    private static final ZonedDateTime DATE_TIME_CREATED = ZonedDateTime.of(2021, 2, 23, 19, 2, 12, 0, UTC);
    @InjectMocks
    private CaseAssignmentRestrictionHandler caseAssignmentRestrictionHandler;

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventStream eventStream;

    @Mock
    private CaseAssignmentRestrictionAggregate caseAssignmentRestrictionAggregate;


    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> argumentCaptor;


    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(CaseAssignmentRestrictionAdded.class);

    @Spy
    private Clock clock = new StoppedClock(ZonedDateTime.of(2021, 2, 23, 19, 2, 11, 0, UTC));

    @Test
    public void shouldHandleCaseAssignmentRestrictionCommands() {
        assertThat(CaseAssignmentRestrictionHandler.class, isHandlerClass(COMMAND_HANDLER)
                .with(method("addCaseAssignmentRestriction").thatHandles("sjp.command.add-case-assignment-restriction")));
    }

    @Test
    public void shouldHandleAddCaseAssignmentRestriction() throws EventStreamException {

        final CaseAssignmentRestrictionAdded caseAssignmentRestrictionAdded = new CaseAssignmentRestrictionAdded(DATE_TIME_CREATED.toString(), EXCLUDE, INCLUDE_ONLY, PROSECUTING_AUTHORITY);
        when(eventSource.getStreamById(CaseAssignmentRestrictionHandler.CASE_ASSIGNMENT_RESTRICTION_STREAM_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAssignmentRestrictionAggregate.class)).thenReturn(caseAssignmentRestrictionAggregate);
        when(caseAssignmentRestrictionAggregate.updateCaseAssignmentRestriction(PROSECUTING_AUTHORITY, INCLUDE_ONLY, EXCLUDE, DATE_TIME_CREATED.toString())).thenReturn(Stream.of(caseAssignmentRestrictionAdded));
        when(clock.now()).thenReturn(DATE_TIME_CREATED);
        final AddCaseAssignmentRestriction payload = new AddCaseAssignmentRestriction(EXCLUDE, INCLUDE_ONLY, PROSECUTING_AUTHORITY);
        final Envelope<AddCaseAssignmentRestriction> addCaseAssignmentRestrictionCommand = envelopeFrom(metadataWithRandomUUID("sjp.command.add-case-assignment-restriction"), payload);
        caseAssignmentRestrictionHandler.addCaseAssignmentRestriction(addCaseAssignmentRestrictionCommand);

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), is(streamContaining(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(JsonEnvelope.envelopeFrom(addCaseAssignmentRestrictionCommand.metadata(), JsonValue.NULL))
                                .withName("sjp.events.case-assignment-restriction-added"),
                        payloadIsJson(allOf(
                                withJsonPath("$.prosecutingAuthority", equalTo(PROSECUTING_AUTHORITY)),
                                withJsonPath("$.dateTimeCreated", equalTo("2021-02-23T19:02:12Z")),
                                withJsonPath("$.exclude[0]", equalTo("9876")),
                                withJsonPath("$.includeOnly[0]", equalTo("1234"))))))));
    }
}
