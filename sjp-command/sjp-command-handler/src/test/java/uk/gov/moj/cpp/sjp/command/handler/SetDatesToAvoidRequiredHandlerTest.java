package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createObjectBuilder;
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
import uk.gov.moj.cpp.sjp.event.DatesToAvoidRequired;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SetDatesToAvoidRequiredHandlerTest {

    private final UUID caseId = UUID.randomUUID();

    @Mock
    private EventSource eventSource;

    @Spy
    private Enveloper enveloper = createEnveloperWithEvents(DatesToAvoidRequired.class);

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventStream eventStream;

    @Mock
    private CaseAggregate caseAggregate;

    @InjectMocks
    private SetDatesToAvoidRequiredHandler setDatesToAvoidRequiredHandler;

    @Test
    public void shouldCreateSetDateToAvoidRequired() throws EventStreamException {
        // given
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("sjp.command.set-dates-to-avoid-required"),
                createObjectBuilder().add("caseId", caseId.toString()));

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        final LocalDate futureDate = LocalDate.now().plusDays(10);
        final DatesToAvoidRequired datesToAvoidRequired = new DatesToAvoidRequired(caseId, futureDate);
        when(caseAggregate.setDatesToAvoidRequired()).thenReturn(Stream.of(datesToAvoidRequired));

        // when
        setDatesToAvoidRequiredHandler.setDatesToAvoidRequired(jsonEnvelope);

        // then
        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(jsonEnvelope(withMetadataEnvelopedFrom(jsonEnvelope)
                                .withName(DatesToAvoidRequired.EVENT_NAME),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.datesToAvoidExpirationDate", equalTo(futureDate.toString()))
                        ))))));
    }

}