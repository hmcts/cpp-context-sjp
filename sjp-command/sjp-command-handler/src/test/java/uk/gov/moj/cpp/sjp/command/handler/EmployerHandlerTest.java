package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZonedDateTime.now;
import static java.util.Optional.of;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.UpdateEmployer.updateEmployer;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.Address;
import uk.gov.moj.cpp.sjp.Employer;
import uk.gov.moj.cpp.sjp.UpdateEmployer;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.event.EmployerDeleted;
import uk.gov.moj.cpp.sjp.event.EmployerUpdated;
import uk.gov.moj.cpp.sjp.event.EmploymentStatusUpdated;

import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EmployerHandlerTest {

    @InjectMocks
    private EmployerHandler employerHandler;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(EmployerUpdated.class,
            EmploymentStatusUpdated.class, EmployerDeleted.class);

    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> argumentCaptor;

    @Test
    public void shouldUpdateEmployer() throws EventStreamException {
        final CaseAggregate caseAggregate = new CaseAggregate();
        final Case aCase = CaseBuilder.aDefaultSjpCase().build();
        caseAggregate.receiveCase(aCase, now());

        // Note that the defendantId created by the builder is overwritten by the aggregate
        final UUID defendantId = caseAggregate.getOffenceIdsByDefendantId().keySet().iterator().next();

        final Address address = Address.address()
                .withAddress1(of("123 High St"))
                .withAddress2(of(""))
                .withAddress3(of("London"))
                .withAddress4(of("Croydon"))
                .withPostcode(of("CR01XG"))
                .build();
        final Employer employer = Employer.employer()
                .withName(of("Nando's"))
                .withEmployeeReference(of("123"))
                .withPhone(of("0208123123"))
                .withAddress(of(address))
                .build();

        final UpdateEmployer payload = updateEmployer()
                .withCaseId(aCase.getId())
                .withDefendantId(defendantId)
                .withEmployer(employer)
                .build();

        when(eventSource.getStreamById(aCase.getId())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        final Envelope<UpdateEmployer> envelope = envelopeFrom(metadataWithRandomUUID("sjp.command.update-employer"), payload);
        employerHandler.updateEmployer(envelope);

        verify(eventStream).append(argumentCaptor.capture());

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        final Stream<JsonEnvelope> value = argumentCaptor.getValue();
        assertThat(value, is(streamContaining(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(jsonEnvelope)
                                .withName("sjp.events.employer-updated"),
                        payloadIsJson(allOf(
                                withJsonPath("$.defendantId", equalTo(defendantId.toString())),
                                withJsonPath("$.name", equalTo("Nando's")),
                                withJsonPath("$.employeeReference", equalTo("123")),
                                withJsonPath("$.phone", equalTo("0208123123")),
                                withJsonPath("$.address.address1", equalTo("123 High St")),
                                withJsonPath("$.address.address2", equalTo("")),
                                withJsonPath("$.address.address3", equalTo("London")),
                                withJsonPath("$.address.address4", equalTo("Croydon")),
                                withJsonPath("$.address.postcode", equalTo("CR01XG"))

                        )))
                        .thatMatchesSchema(),
                jsonEnvelope(
                        withMetadataEnvelopedFrom(jsonEnvelope)
                                .withName("sjp.events.employment-status-updated"),
                        payloadIsJson(allOf(
                                withJsonPath("$.defendantId", equalTo(defendantId.toString())),
                                withJsonPath("$.employmentStatus", equalTo("EMPLOYED"))

                        )))
                        .thatMatchesSchema()
        )));
    }

    @Test
    public void shouldDeleteEmployer() throws EventStreamException {
        final CaseAggregate caseAggregate = new CaseAggregate();
        final Case aCase = CaseBuilder.aDefaultSjpCase().build();
        caseAggregate.receiveCase(aCase, now());

        // Note that the defendantId created by the builder is overwritten by the aggregate
        final UUID defendantId = caseAggregate.getOffenceIdsByDefendantId().keySet().iterator().next();

        caseAggregate.apply(new EmploymentStatusUpdated(defendantId, "EMPLOYED"));

        final JsonObject payload = createObjectBuilder()
                .add("caseId", aCase.getId().toString())
                .add("defendantId", defendantId.toString())
                .build();

        when(eventSource.getStreamById(aCase.getId())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        final JsonEnvelope envelope = EnvelopeFactory.createEnvelope("sjp.command.delete-employer", payload);
        employerHandler.deleteEmployer(envelope);

        verify(eventStream).append(argumentCaptor.capture());

        final Stream<JsonEnvelope> value = argumentCaptor.getValue();
        assertThat(value, is(streamContaining(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope)
                                .withName("sjp.events.employer-deleted"),
                        payloadIsJson(
                                withJsonPath("$.defendantId", equalTo(defendantId.toString()))

                        ))
                        .thatMatchesSchema()
        )));
    }
}