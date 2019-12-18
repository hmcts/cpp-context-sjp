package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.JsonValue.NULL;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.json.schemas.domains.sjp.Address;
import uk.gov.justice.json.schemas.domains.sjp.Benefits;
import uk.gov.justice.json.schemas.domains.sjp.Employment;
import uk.gov.justice.json.schemas.domains.sjp.Income;
import uk.gov.justice.json.schemas.domains.sjp.command.Frequency;
import uk.gov.justice.json.schemas.domains.sjp.command.UpdateAllFinancialMeans;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.Employer;
import uk.gov.moj.cpp.sjp.domain.EmploymentStatus;
import uk.gov.moj.cpp.sjp.domain.IncomeFrequency;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.event.EmployerUpdated;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpdateAllFinancialMeansCommandHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private CaseAggregate caseAggregate;

    @Captor
    private ArgumentCaptor<UUID> userIdArgumentCaptor;

    @Captor
    private ArgumentCaptor<UUID> defendantIdArgumentCaptor;

    @Captor
    private ArgumentCaptor<uk.gov.moj.cpp.sjp.domain.Income> incomeArgumentCaptor;

    @Captor
    private ArgumentCaptor<uk.gov.moj.cpp.sjp.domain.Benefits> benefitsArgumentCaptor;

    @Captor
    private ArgumentCaptor<uk.gov.moj.cpp.sjp.domain.Employer> employerArgumentCaptor;

    @Captor
    private ArgumentCaptor<String> empStatusArgumentCaptor;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(FinancialMeansUpdated.class, EmployerUpdated.class);

    @InjectMocks
    private UpdateAllFinancialMeansCommandHandler updateAllFinancialMeansCommandHandler;

    @Test
    public void shouldUpdateFinancialMeansWhenEmploymentStatusIsEmployed() throws EventStreamException {
        final UUID userId = randomUUID();
        final UUID caseId = randomUUID();
        final Income income = new Income(BigDecimal.valueOf(1000.50), Frequency.WEEKLY);
        final Benefits benefits = new Benefits(true, "Benefits type");
        final UUID defendantId = randomUUID();
        final String empStatus = EmploymentStatus.EMPLOYED.name();

        final UpdateAllFinancialMeans updateAllFinancialMeans = getUpdateAllFinancialMeansPayload(caseId, defendantId, income, benefits, EmploymentStatus.EMPLOYED, null);

        final uk.gov.moj.cpp.sjp.domain.Income transformedIncome = new uk.gov.moj.cpp.sjp.domain.Income(IncomeFrequency.valueOf(income.getFrequency().toString()), income.getAmount());
        final uk.gov.moj.cpp.sjp.domain.Benefits transformedBenefits = new uk.gov.moj.cpp.sjp.domain.Benefits(benefits.getClaimed(), benefits.getType());

        final FinancialMeansUpdated financialMeansUpdated = FinancialMeansUpdated
                .createEvent(defendantId, transformedIncome, transformedBenefits, empStatus);
        final EmployerUpdated employerUpdated = getEmployerUpdated(defendantId);

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(caseAggregate.updateAllFinancialMeans(any(UUID.class),
                any(UUID.class),
                any(uk.gov.moj.cpp.sjp.domain.Income.class),
                any(uk.gov.moj.cpp.sjp.domain.Benefits.class),
                any(uk.gov.moj.cpp.sjp.domain.Employer.class),
                anyString())).thenReturn(Stream.of(financialMeansUpdated, employerUpdated));

        final Envelope<UpdateAllFinancialMeans> command = Envelope.envelopeFrom(
                metadataWithRandomUUID("sjp.command.update-all-financial-means").
                        withUserId(userId.toString()).build(),
                updateAllFinancialMeans);

        updateAllFinancialMeansCommandHandler.updateAllFinancialMeans(command);

        verify(caseAggregate).updateAllFinancialMeans(
                userIdArgumentCaptor.capture(),
                defendantIdArgumentCaptor.capture(),
                incomeArgumentCaptor.capture(),
                benefitsArgumentCaptor.capture(),
                employerArgumentCaptor.capture(),
                empStatusArgumentCaptor.capture());

        assertThat(userIdArgumentCaptor.getValue(), is(userId));
        assertThat(defendantIdArgumentCaptor.getValue(), is(defendantId));
        assertThat(incomeArgumentCaptor.getValue().getAmount(), is(income.getAmount()));
        assertThat(incomeArgumentCaptor.getValue().getFrequency().name(), is(income.getFrequency().name()));
        assertThat(benefitsArgumentCaptor.getValue().getType(), is(benefits.getType()));
        assertThat(benefitsArgumentCaptor.getValue().getClaimed(), is(benefits.getClaimed()));
        assertThat(empStatusArgumentCaptor.getValue(), is(EmploymentStatus.EMPLOYED.name()));

        assertThat(employerArgumentCaptor.getValue().getAddress().getAddress1(), is("address1"));
        assertThat(employerArgumentCaptor.getValue().getAddress().getAddress2(), is("address2"));
        assertThat(employerArgumentCaptor.getValue().getAddress().getAddress3(), is("address3"));
        assertThat(employerArgumentCaptor.getValue().getAddress().getAddress4(), is("address4"));
        assertThat(employerArgumentCaptor.getValue().getAddress().getAddress5(), is("address5"));
        assertThat(employerArgumentCaptor.getValue().getEmployeeReference(), is("Mike"));
        assertThat(employerArgumentCaptor.getValue().getName(), is("ASDA"));
        assertThat(employerArgumentCaptor.getValue().getPhone(), is("07745454454"));

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(JsonEnvelope.envelopeFrom(command.metadata(), NULL))
                                        .withName("sjp.events.financial-means-updated"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.defendantId", equalTo(defendantId.toString())),
                                        withJsonPath("$.income.frequency", equalTo(income.getFrequency().toString())),
                                        withJsonPath("$.income.amount", equalTo(income.getAmount().doubleValue())),
                                        withJsonPath("$.benefits.claimed", equalTo(benefits.getClaimed())),
                                        withJsonPath("$.benefits.type", equalTo(benefits.getType())),
                                        withJsonPath("$.employmentStatus", equalTo(empStatus))
                                        )
                                )),
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(JsonEnvelope.envelopeFrom(command.metadata(), NULL))
                                        .withName("sjp.events.employer-updated"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.defendantId", equalTo(defendantId.toString())),
                                        withJsonPath("$.name", equalTo("ASDA")),
                                        withJsonPath("$.employeeReference", equalTo("Mike")),
                                        withJsonPath("$.address.address1", equalTo("address1")),
                                        withJsonPath("$.address.address2", equalTo("address2")),
                                        withJsonPath("$.address.address3", equalTo("address3")),
                                        withJsonPath("$.address.address4", equalTo("address4")),
                                        withJsonPath("$.address.address5", equalTo("address5")),
                                        withJsonPath("$.address.postcode", equalTo("SW19 8SW")),
                                        withJsonPath("$.phone", equalTo("07745454454"))
                                        )
                                ))

                )));

    }

    @Test
    public void shouldUpdateFinancialMeansWhenEmploymentStatusIsOther() throws EventStreamException {
        final UUID userId = randomUUID();
        final UUID caseId = randomUUID();
        final Income income = new Income(BigDecimal.valueOf(1000.50), Frequency.WEEKLY);
        final Benefits benefits = new Benefits(true, "Benefits type");
        final UUID defendantId = randomUUID();

        final UpdateAllFinancialMeans updateAllFinancialMeans = getUpdateAllFinancialMeansPayload(caseId, defendantId, income, benefits, EmploymentStatus.OTHER, "OddJobs");

        final uk.gov.moj.cpp.sjp.domain.Income transformedIncome = new uk.gov.moj.cpp.sjp.domain.Income(IncomeFrequency.valueOf(income.getFrequency().toString()), income.getAmount());
        final uk.gov.moj.cpp.sjp.domain.Benefits transformedBenefits = new uk.gov.moj.cpp.sjp.domain.Benefits(benefits.getClaimed(), benefits.getType());

        final FinancialMeansUpdated financialMeansUpdated = FinancialMeansUpdated
                .createEvent(defendantId, transformedIncome, transformedBenefits, "OddJobs");

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(caseAggregate.updateAllFinancialMeans(any(UUID.class),
                any(UUID.class),
                any(uk.gov.moj.cpp.sjp.domain.Income.class),
                any(uk.gov.moj.cpp.sjp.domain.Benefits.class),
                any(uk.gov.moj.cpp.sjp.domain.Employer.class),
                anyString())).thenReturn(Stream.of(financialMeansUpdated));

        final Envelope<UpdateAllFinancialMeans> command = Envelope.envelopeFrom(
                metadataWithRandomUUID("sjp.command.update-all-financial-means").
                        withUserId(userId.toString()).build(),
                updateAllFinancialMeans);

        updateAllFinancialMeansCommandHandler.updateAllFinancialMeans(command);

        verify(caseAggregate).updateAllFinancialMeans(
                userIdArgumentCaptor.capture(),
                defendantIdArgumentCaptor.capture(),
                incomeArgumentCaptor.capture(),
                benefitsArgumentCaptor.capture(),
                employerArgumentCaptor.capture(),
                empStatusArgumentCaptor.capture());

        assertThat(userIdArgumentCaptor.getValue(), is(userId));
        assertThat(defendantIdArgumentCaptor.getValue(), is(defendantId));

        assertThat(incomeArgumentCaptor.getValue().getAmount(), is(income.getAmount()));
        assertThat(incomeArgumentCaptor.getValue().getFrequency().name(), is(income.getFrequency().name()));

        assertThat(benefitsArgumentCaptor.getValue().getType(), is(benefits.getType()));
        assertThat(benefitsArgumentCaptor.getValue().getClaimed(), is(benefits.getClaimed()));

        assertThat(empStatusArgumentCaptor.getValue(), is("OddJobs"));
        assertThat(employerArgumentCaptor.getValue(), is(nullValue()));
    }

    private EmployerUpdated getEmployerUpdated(final UUID defendantId) {
        return EmployerUpdated.createEvent(defendantId, new uk.gov.moj.cpp.sjp.domain.Employer(defendantId,
                "ASDA",
                "Mike",
                "07745454454",
                new uk.gov.moj.cpp.sjp.domain.Address("address1",
                        "address2",
                        "address3",
                        "address4",
                        "address5",
                        "SW19 8SW")));
    }

    private UpdateAllFinancialMeans getUpdateAllFinancialMeansPayload(final UUID caseId,
                                                                      final UUID defendantId,
                                                                      final Income income,
                                                                      final Benefits benefits,
                                                                      final EmploymentStatus status,
                                                                      final String empDetails) {
        Employer employer = null;
        if(status == EmploymentStatus.EMPLOYED) {

            employer =  Employer.employer()
                    .withEmployeeReference("Mike")
                    .withName("ASDA")
                    .withAddress(Address.address()
                            .withAddress1("address1")
                            .withAddress2("address2")
                            .withAddress3("address3")
                            .withAddress4("address4")
                            .withAddress5("address5")
                            .withPostcode("SW19 8SW").build())
                    .withPhone("07745454454").build();
        }

        return UpdateAllFinancialMeans.updateAllFinancialMeans()
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .withIncome(income)
                .withBenefits(benefits)
                .withEmployer(employer)
                .withEmployment(Employment
                        .employment()
                        .withStatus(status.name())
                        .withDetails(empDetails)
                        .build())
                .build();
    }

}