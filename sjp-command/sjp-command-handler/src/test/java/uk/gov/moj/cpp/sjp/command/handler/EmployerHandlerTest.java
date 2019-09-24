package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZonedDateTime.now;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.UpdateEmployer.updateEmployer;

import uk.gov.justice.json.schemas.domains.sjp.Address;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.Employer;
import uk.gov.moj.cpp.sjp.UpdateEmployer;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.event.EmployerDeleted;
import uk.gov.moj.cpp.sjp.event.EmployerUpdated;
import uk.gov.moj.cpp.sjp.event.EmploymentStatusUpdated;

import java.util.List;
import java.util.stream.Collectors;
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

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(EmployerUpdated.class,
            EmploymentStatusUpdated.class, EmployerDeleted.class);
    @InjectMocks
    private EmployerHandler employerHandler;
    @Mock
    private EventSource eventSource;
    @Mock
    private EventStream eventStream;
    @Mock
    private AggregateService aggregateService;
    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> argumentCaptor;

    @Test
    public void shouldUpdateEmployer() throws EventStreamException, NoSuchFieldException, IllegalAccessException {
        final CaseAggregate caseAggregate = new CaseAggregate();
        final Case aCase = CaseBuilder.aDefaultSjpCase().build();

        caseAggregate.receiveCase(aCase, now());

        final Address address = Address.address()
                .withAddress1("123 High St")
                .withAddress2("")
                .withAddress3("London")
                .withAddress4("Croydon")
                .withAddress5("Greater London")
                .withPostcode("CR01XG")
                .build();
        final Employer employer = Employer.employer()
                .withName("Nando's")
                .withEmployeeReference("123")
                .withPhone("0208123123")
                .withAddress(address)
                .build();

        final UpdateEmployer payload = updateEmployer()
                .withCaseId(aCase.getId())
                .withDefendantId(aCase.getDefendant().getId())
                .withEmployer(employer)
                .build();

        when(eventSource.getStreamById(aCase.getId())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        final Envelope<UpdateEmployer> envelope = envelopeFrom(metadataWithRandomUUID("sjp.command.update-employer"), payload);
        employerHandler.updateEmployer(envelope);

        verify(eventStream).append(argumentCaptor.capture());

        final JsonEnvelope jsonEnvelope = envelopeFrom(envelope.metadata(), JsonValue.NULL);
        final Stream<JsonEnvelope> value = argumentCaptor.getValue();

        final List<JsonEnvelope> collect = value.collect(Collectors.toList());

        assertThat(collect.stream(), is(streamContaining(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(jsonEnvelope)
                                .withName("sjp.events.employer-updated"),
                        payloadIsJson(allOf(
                                withJsonPath("$.defendantId", equalTo(aCase.getDefendant().getId().toString())),
                                withJsonPath("$.name", equalTo("Nando's")),
                                withJsonPath("$.employeeReference", equalTo("123")),
                                withJsonPath("$.phone", equalTo("0208123123")),
                                withJsonPath("$.address.address1", equalTo("123 High St")),
                                withJsonPath("$.address.address2", equalTo("")),
                                withJsonPath("$.address.address3", equalTo("London")),
                                withJsonPath("$.address.address4", equalTo("Croydon")),
                                withJsonPath("$.address.address5", equalTo("Greater London")),
                                withJsonPath("$.address.postcode", equalTo("CR01XG"))

                        ))),
                jsonEnvelope(withMetadataEnvelopedFrom(jsonEnvelope)
                                .withName("sjp.events.employment-status-updated"),
                        payloadIsJson(allOf(
                                withJsonPath("$.defendantId", equalTo(aCase.getDefendant().getId().toString())),
                                withJsonPath("$.employmentStatus", equalTo("EMPLOYED"))

                        ))))
        ));
    }


    @Test
    public void shouldDeleteEmployer() throws EventStreamException {
        final CaseAggregate caseAggregate = new CaseAggregate();
        final Case aCase = CaseBuilder.aDefaultSjpCase().build();

        caseAggregate.receiveCase(aCase, now());

        caseAggregate.apply(new EmploymentStatusUpdated(aCase.getDefendant().getId(), "EMPLOYED"));

        final JsonObject payload = createObjectBuilder()
                .add("caseId", aCase.getId().toString())
                .add("defendantId", aCase.getDefendant().getId().toString())
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
                                withJsonPath("$.defendantId", equalTo(aCase.getDefendant().getId().toString()))

                        ))
        )));
    }
}