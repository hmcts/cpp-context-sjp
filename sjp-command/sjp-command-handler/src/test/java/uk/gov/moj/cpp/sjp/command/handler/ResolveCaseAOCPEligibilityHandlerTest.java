package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static java.util.UUID.randomUUID;
import static java.math.BigDecimal.valueOf;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.event.CaseEligibleForAOCP;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Stream;
import javax.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ResolveCaseAOCPEligibilityHandlerTest {

    @Mock
    private CaseAggregate caseAggregate;

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventStream eventStream;

    @Spy
    private Enveloper enveloper = createEnveloperWithEvents(CaseEligibleForAOCP.class);

    @InjectMocks
    private ResolveCaseAOCPEligibilityHandler resolveCaseAOCPEligibilityHandler;

    private static final String COMMAND_RESOLVE_CASE_AOCP_ELIGIBILITY = "sjp.command.resolve-case-aocp-eligibility";
    private static final String HANDLER_METHOD = "handleResolveCaseAOCPEligibility";

    @Test
    public void shouldHandleResolveCaseAOCPEligibilityComand() {
        assertThat(resolveCaseAOCPEligibilityHandler, isHandler(COMMAND_HANDLER)
                .with(method(HANDLER_METHOD)
                        .thatHandles(COMMAND_RESOLVE_CASE_AOCP_ELIGIBILITY)
                ));
    }

    @Test
    public void shouldHandleResolveCaseAOCPEligibility() throws EventStreamException {
        final UUID caseId = randomUUID();
        final BigDecimal surchargeAmountMin = BigDecimal.ONE;
        final BigDecimal surchargeAmountMax = BigDecimal.valueOf(2000);
        final BigDecimal surchargeFinePercentage = BigDecimal.valueOf(40);

        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID(COMMAND_RESOLVE_CASE_AOCP_ELIGIBILITY),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("isProsecutorAOCPApproved", true)
                        .add("surchargeAmountMin", surchargeAmountMin)
                        .add("surchargeAmountMax",surchargeAmountMax)
                        .add("surchargeFinePercentage",surchargeFinePercentage)

                        .build());
        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(caseAggregate.resolveCaseAOCPEligibility(caseId, true, of(surchargeAmountMin), of(surchargeAmountMax), of(surchargeFinePercentage), empty()))
                .thenReturn(Stream.of(new CaseEligibleForAOCP(caseId, valueOf(30), valueOf(100), valueOf(200), null)));

        resolveCaseAOCPEligibilityHandler.handleResolveCaseAOCPEligibility(envelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(jsonEnvelope(withMetadataEnvelopedFrom(envelope)
                                .withName(CaseEligibleForAOCP.EVENT_NAME),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.costs", equalTo(30)),
                                withJsonPath("$.victimSurcharge", equalTo(100))
                        ))))));
    }

    @Test
    public void shouldHandleResolveCaseAOCPEligibilityWhenSurchargeAmountIsPopulated() throws EventStreamException {
        final UUID caseId = randomUUID();
        final BigDecimal surchargeAmount = BigDecimal.valueOf(40);

        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID(COMMAND_RESOLVE_CASE_AOCP_ELIGIBILITY),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("isProsecutorAOCPApproved", true)
                        .add("surchargeAmount", surchargeAmount)
                        .build());
        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(caseAggregate.resolveCaseAOCPEligibility(caseId, true, empty(), empty(), empty(), of(surchargeAmount)))
                .thenReturn(Stream.of(new CaseEligibleForAOCP(caseId, valueOf(30), valueOf(100), valueOf(200), null)));

        resolveCaseAOCPEligibilityHandler.handleResolveCaseAOCPEligibility(envelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(jsonEnvelope(withMetadataEnvelopedFrom(envelope)
                                .withName(CaseEligibleForAOCP.EVENT_NAME),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.costs", equalTo(30)),
                                withJsonPath("$.victimSurcharge", equalTo(100))
                        ))))));
    }

    @Test
    public void shouldHandleResolveCaseAOCPEligibilityWhenSurchargeSurchargeFinePercentageIsPopulated() throws EventStreamException {
        final UUID caseId = randomUUID();
        final BigDecimal surchargeFinePercentage = BigDecimal.valueOf(40);

        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID(COMMAND_RESOLVE_CASE_AOCP_ELIGIBILITY),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("isProsecutorAOCPApproved", true)
                        .add("surchargeFinePercentage", surchargeFinePercentage)
                        .add("surchargeAmountMin", JsonValue.NULL)
                        .add("surchargeAmountMax", JsonValue.NULL)
                        .build());
        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(caseAggregate.resolveCaseAOCPEligibility(caseId, true, empty(), empty(), of(surchargeFinePercentage), empty()))
                .thenReturn(Stream.of(new CaseEligibleForAOCP(caseId, valueOf(30), valueOf(100), valueOf(200), null)));

        resolveCaseAOCPEligibilityHandler.handleResolveCaseAOCPEligibility(envelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(jsonEnvelope(withMetadataEnvelopedFrom(envelope)
                                .withName(CaseEligibleForAOCP.EVENT_NAME),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.costs", equalTo(30)),
                                withJsonPath("$.victimSurcharge", equalTo(100))
                        ))))));
    }
}
