package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.UUID.randomUUID;
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
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Defendant;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregateBaseTest;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantNameUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantPendingChangesAccepted;

import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AcceptPendingDefendantChangesHandlerTest extends CaseAggregateBaseTest {
    private static final UUID defendantId = randomUUID();
    private static final UUID caseId = randomUUID();
    private static final String firstName = "test";
    private static final String lastName = "lastName";
    private static final String dateOfBirth = LocalDate.parse("1980-07-15").toString();
    private static final String ADDRESS_1 = "14 Tottenham Court Road";
    private static final String ADDRESS_2 = "London";
    private static final String ADDRESS_3 = "Surrey";
    private static final String ADDRESS_4 = "England";
    private static final String ADDRESS_5 = "United Kingdom";
    private static final String POSTCODE = "W1T 1JY";
    private static final Address ADDRESS = new Address(ADDRESS_1, ADDRESS_2, ADDRESS_3, ADDRESS_4, ADDRESS_5, POSTCODE);
    @Spy
    private Clock clock = new UtcClock();
    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            DefendantNameUpdated.class, DefendantDetailsUpdated.class, DefendantPendingChangesAccepted.class);
    @InjectMocks
    private AcceptPendingDefendantChangesHandler acceptPendingDefendantChangesHandler;
    @Mock
    private EventStream eventStream;
    @Mock
    private EventSource eventSource;
    @Mock
    private AggregateService aggregateService;

    private static JsonObject toJsonObject(final Address address) {
        return createObjectBuilder()
                .add("address1", address.getAddress1())
                .add("address2", address.getAddress2())
                .add("address3", address.getAddress3())
                .add("address4", address.getAddress4())
                .add("address5", address.getAddress5())
                .add("postcode", address.getPostcode())
                .build();
    }

    @Test
    public void shouldAcceptPendingDefendantChanges() throws EventStreamException {

        final Defendant defendant = caseReceivedEvent.getDefendant();
        final UUID defendantId = defendant.getId();
        final UUID caseId = caseReceivedEvent.getCaseId();

        final JsonEnvelope command = createAcceptPendingDefendantChangesCommand(randomUUID());

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        acceptPendingDefendantChangesHandler.acceptPendingDefendantChanges(command);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-name-updated"),
                                payloadIsJson(withJsonPath("$.newPersonalName.firstName", equalTo(firstName)))),
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-details-updated"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.defendantId", equalTo(defendantId.toString())),
                                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                                        withJsonPath("$.firstName", equalTo(firstName)),
                                        withJsonPath("$.lastName", equalTo(defendant.getLastName())),
                                        withJsonPath("$.dateOfBirth", equalTo(defendant.getDateOfBirth().format(ofPattern("YYY-MM-dd"))))))),
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.defendant-pending-changes-accepted"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                                        withJsonPath("$.defendantId", equalTo(defendantId.toString())))))
                )));
    }

    private JsonEnvelope createAcceptPendingDefendantChangesCommand(UUID userId) {
        final Defendant defendant = caseReceivedEvent.getDefendant();

        final JsonObjectBuilder payload = createObjectBuilder()
                .add("defendantId", defendant.getId().toString())
                .add("caseId", caseReceivedEvent.getCaseId().toString())
                .add("firstName", firstName)
                .add("lastName", defendant.getLastName())
                .add("dateOfBirth", defendant.getDateOfBirth().format(ofPattern("YYY-MM-dd")))
                .add("address", toJsonObject(defendant.getAddress()));

        return envelopeFrom(
                metadataOf(randomUUID(), "sjp.command.accept-pending-defendant-changes").withUserId(userId.toString()),
                payload.build());
    }

}
