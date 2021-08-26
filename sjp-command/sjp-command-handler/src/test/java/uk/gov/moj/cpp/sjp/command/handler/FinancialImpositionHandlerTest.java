package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.FinancialImpositionExportDetails;
import uk.gov.moj.cpp.sjp.event.FinancialImpositionAccountNumberAdded;
import uk.gov.moj.cpp.sjp.event.FinancialImpositionAccountNumberAddedBdf;
import uk.gov.moj.cpp.sjp.event.FinancialImpositionCorrelationIdAdded;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FinancialImpositionHandlerTest {

    @Spy
    private CaseAggregate caseAggregate;

    @Mock
    private EventStream eventStream;

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private FinancialImpositionHandler financialImpositionHandler;

    @Spy
    private Enveloper enveloper = createEnveloperWithEvents(
            FinancialImpositionCorrelationIdAdded.class,
            FinancialImpositionAccountNumberAdded.class,
            FinancialImpositionAccountNumberAddedBdf.class

    );

    private static final UUID CASE_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID CORRELATION_ID = randomUUID();
    private static final String ACCOUNT_NUMBER = "123456780";


    @Before
    public void setUp() {
        when(aggregateService.get(any(), any())).thenReturn(caseAggregate);
        caseAggregate.getState().setCaseId(CASE_ID);
        caseAggregate.getState().setDefendantId(DEFENDANT_ID);
    }

    @Test
    public void shouldHandleAddFinancialImpositionCorrelationId() throws EventStreamException {
        final JsonEnvelope command = createAddCorrelationIdCommand();
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        financialImpositionHandler.addFinancialImpositionCorrelationId(command);
        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.financial-imposition-correlation-id-added"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(CASE_ID.toString())),
                                        withJsonPath("$.defendantId", equalTo(DEFENDANT_ID.toString())),
                                        withJsonPath("$.correlationId", equalTo(CORRELATION_ID.toString()))

                                )))
                )));
    }

    @Test
    public void shouldHandleAddFinancialImpositionAccountNumber() throws EventStreamException {
        final JsonEnvelope command = createAddAccountNumberCommand();
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        final FinancialImpositionExportDetails exportDetails = new FinancialImpositionExportDetails();
        exportDetails.setCorrelationId(CORRELATION_ID);
        caseAggregate.getState().addFinancialImpositionExportDetails(DEFENDANT_ID, exportDetails);

        financialImpositionHandler.addFinancialImpositionAccountNumber(command);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.financial-imposition-account-number-added"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(CASE_ID.toString())),
                                        withJsonPath("$.defendantId", equalTo(DEFENDANT_ID.toString())),
                                        withJsonPath("$.accountNumber", equalTo(ACCOUNT_NUMBER))
                                )))
                )));
    }

    @Test
    public void shouldHandleAddFinancialImpositionAccountNumberBdf() throws EventStreamException {
        final JsonEnvelope command = createAddAccountNumberCommandBdf();
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        final FinancialImpositionExportDetails exportDetails = new FinancialImpositionExportDetails();
        exportDetails.setCorrelationId(CORRELATION_ID);
        caseAggregate.getState().addFinancialImpositionExportDetails(DEFENDANT_ID, exportDetails);

        financialImpositionHandler.addFinancialImpositionAccountNumberBdf(command);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.financial-imposition-account-number-added-bdf"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(CASE_ID.toString())),
                                        withJsonPath("$.defendantId", equalTo(DEFENDANT_ID.toString())),
                                        withJsonPath("$.correlationId", equalTo(CORRELATION_ID.toString())),
                                        withJsonPath("$.accountNumber", equalTo(ACCOUNT_NUMBER))
                                )))
                )));
    }

    private JsonEnvelope createAddCorrelationIdCommand() {
        final JsonObject payload = createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .add("defendantId", DEFENDANT_ID.toString())
                .add("correlationId", CORRELATION_ID.toString())
                .build();

        return envelopeFrom(
                metadataWithRandomUUID("sjp.command.add-financial-imposition-correlation-id"),
                payload);
    }

    private JsonEnvelope createAddAccountNumberCommand() {
        final JsonObject payload = createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .add("correlationId", CORRELATION_ID.toString())
                .add("accountNumber", ACCOUNT_NUMBER)
                .build();

        return envelopeFrom(
                metadataWithRandomUUID("sjp.command.add-financial-imposition-account-number"),
                payload);
    }

    private JsonEnvelope createAddAccountNumberCommandBdf() {
        final JsonObject payload = createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .add("defendantId", DEFENDANT_ID.toString())
                .add("correlationId", CORRELATION_ID.toString())
                .add("accountNumber", ACCOUNT_NUMBER)
                .build();

        return envelopeFrom(
                metadataWithRandomUUID("sjp.command.add-financial-imposition-account-number-bdf"),
                payload);
    }

}
