package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.domain.Defendant;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregateBaseTest;
import uk.gov.moj.cpp.sjp.event.DefendantPendingChangesRejected;

import java.util.UUID;

import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RejectPendingDefendantChangesHandlerTest extends CaseAggregateBaseTest {
    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            DefendantPendingChangesRejected.class);
    @InjectMocks
    private RejectPendingDefendantChangesHandler rejectPendingDefendantChangesHandler;
    @Mock
    private EventStream eventStream;
    @Mock
    private EventSource eventSource;
    @Mock
    private AggregateService aggregateService;

    @Test
    public void shouldRejectPendingDefendantChanges() throws EventStreamException {

        final Defendant defendant = caseReceivedEvent.getDefendant();
        final UUID defendantId = defendant.getId();
        final UUID caseId = caseReceivedEvent.getCaseId();

        final JsonEnvelope command = createRejectPendingDefendantChangesCommand(randomUUID());

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        rejectPendingDefendantChangesHandler.rejectPendingDefendantChanges(command);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-pending-changes-rejected"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.defendantId", equalTo(defendantId.toString())),
                                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                                        withJsonPath("$.description", equalTo("Defendant pending changes rejected")))))
                )));
    }

    private JsonEnvelope createRejectPendingDefendantChangesCommand(UUID userId) {
        final Defendant defendant = caseReceivedEvent.getDefendant();

        final JsonObjectBuilder payload = createObjectBuilder()
                .add("defendantId", defendant.getId().toString())
                .add("caseId", caseReceivedEvent.getCaseId().toString());

        return envelopeFrom(
                metadataOf(randomUUID(), "sjp.command.reject-pending-defendant-changes").withUserId(userId.toString()),
                payload.build());
    }

}
