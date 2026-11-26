package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityAccess;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityProvider;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregateBaseTest;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdatesAcknowledged;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatterBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AcknowledgeDefendantDetailsUpdatesHandlerTest extends CaseAggregateBaseTest {

    private static final ZonedDateTime NOW = ZonedDateTime.now(UTC);
    private static final String ACKNOWLEDGE_DEFENDANT_UPDATES_COMMAND_NAME = "sjp.command.acknowledge-defendant-details-updates";

    @Spy
    private Clock clock = new UtcClock();

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            DefendantDetailsUpdatesAcknowledged.class);

    @InjectMocks
    private AcknowledgeDefendantDetailsUpdatesHandler acknowledgeDefendantDetailsUpdatesHandler;

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventStream eventStream;

    @Mock
    private ProsecutingAuthorityProvider prosecutingAuthorityProvider;

    @Test
    public void shouldAcknowledgeDefendantDetailsUpdates() throws EventStreamException {
        final JsonEnvelope command = envelopeFrom(
                metadataWithRandomUUID(ACKNOWLEDGE_DEFENDANT_UPDATES_COMMAND_NAME),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("defendantId", defendantId.toString()));

        when(prosecutingAuthorityProvider.getCurrentUsersProsecutingAuthorityAccess(command)).thenReturn(ProsecutingAuthorityAccess.ALL);
        when(clock.now()).thenReturn(NOW);
        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        acknowledgeDefendantDetailsUpdatesHandler.acknowledgeDefendantDetailsUpdates(command);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-details-updates-acknowledged"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(caseReceivedEvent.getCaseId().toString())),
                                        withJsonPath("$.defendantId", equalTo(defendantId.toString())),
                                        withJsonPath("$.acknowledgedAt", equalTo(NOW.format(new DateTimeFormatterBuilder()
                                                .appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                                                .appendZoneId()
                                                .toFormatter())))
                                )))
                )));
    }
}
