package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregateBaseTest;
import uk.gov.moj.cpp.sjp.event.CaseListedInCriminalCourtsUpdated;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UpdateCaseListedInCriminalCourtsHandlerTest extends CaseAggregateBaseTest {

    @InjectMocks
    private UpdateCaseListedInCriminalCourtsHandler updateCaseListedInCriminalCourtsHandler;
    @Mock
    private EventStream eventStream;
    @Mock
    private EventSource eventSource;
    @Mock
    private AggregateService aggregateService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(CaseListedInCriminalCourtsUpdated.class);

    @Test
    public void shouldHandleUpdateCaseListedInCriminalCourtsCommand() {
        assertThat(UpdateCaseListedInCriminalCourtsHandler.class, isHandlerClass(COMMAND_HANDLER)
                .with(method("updateCaseListedInCriminalCourts").thatHandles(UpdateCaseListedInCriminalCourtsHandler.COMMAND_NAME)));
    }

    @Test
    public void shouldUpdateCaseApplicationListedInCC() throws EventStreamException {
        final UUID caseId = randomUUID();
        final JsonEnvelope originalEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(),
                createObjectBuilder().add("caseId", caseId.toString()).add("listedInCriminalCourts",true).build());
        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        updateCaseListedInCriminalCourtsHandler.updateCaseApplicationListedInCC(originalEnvelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(jsonEnvelope(withMetadataEnvelopedFrom(originalEnvelope)
                                .withName("sjp.events.case-listed-in-criminal-courts-updated"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.listedInCriminalCourts", equalTo(true))
                        ))))));

    }

}
