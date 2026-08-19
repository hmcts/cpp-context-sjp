package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.converter.JsonObjectToObjectConverterFactory.createJsonObjectToObjectConverter;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.event.processor.CaseReceivedProcessor.CASE_STARTED_PUBLIC_EVENT_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.CaseReceivedProcessor.RESOLVE_CASE_AOCP_ELIGIBILITY;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.DEFENDANT;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.DEFENDANT_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.EXPECTED_DATE_READY;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.POSTING_DATE;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.PROSECUTING_AUTHORITY;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.URN;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.processor.service.AzureFunctionService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.timers.TimerService;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseReceivedProcessorTest {

    @InjectMocks
    private CaseReceivedProcessor caseReceivedProcessor;

    @Mock
    private AzureFunctionService azureFunctionService;

    @Mock
    protected Sender sender;

    @Mock
    private TimerService timerService;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Mock
    ReferenceDataService referenceDataService;

    @Spy
    @SuppressWarnings("unused")
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = createJsonObjectToObjectConverter();

    @Test
    public void shouldResolveAOCPEligibilityWhenRefDataNotFound() throws IOException {
        final UUID caseId = randomUUID();
        final String urn = randomUUID().toString().replace("-", "").toUpperCase().substring(0, 13);
        final LocalDate postingDate = now();
        final LocalDate expectedDateReady = now().plusDays(28);

        final JsonEnvelope privateEvent = createPayload(caseId, urn, postingDate, expectedDateReady, null, null);

        when(referenceDataService.getProsecutor(any(), any())).thenReturn(java.util.Optional.ofNullable((createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("aocpApproved", true)
                .build())));
        when(referenceDataService.getVictimSurcharges(any(), any(), eq("Fine"), eq("Adult"))).thenReturn(asList());

        caseReceivedProcessor.handleCaseReceivedEvent(privateEvent);

        verify(sender, times(2)).send(envelopeCaptor.capture());

        final JsonEnvelope firstCommandEvent = this.envelopeCaptor.getAllValues().get(0);
        assertThat(firstCommandEvent.metadata().name(), is(RESOLVE_CASE_AOCP_ELIGIBILITY));
        final JsonObject commandPayload = (JsonObject) firstCommandEvent.payload();
        assertThat(commandPayload.getString("caseId"), is(caseId.toString()));
        assertThat(commandPayload.getBoolean("isProsecutorAOCPApproved"), is(true));
        assertThat(commandPayload.getJsonNumber("surchargeAmount").bigDecimalValue(), is(BigDecimal.ZERO));
        assertThat(commandPayload.containsKey("surchargeAmountMax"), is(false));
        assertThat(commandPayload.containsKey("surchargeAmountMin"), is(false));
        assertThat(commandPayload.containsKey("surchargeFinePercentage"), is(false));

    }


    @Test
    public void shouldUpdateCaseStateAndResolveCaseAOCPEligibility() throws IOException {
        final UUID caseId = randomUUID();
        final String urn = randomUUID().toString().replace("-", "").toUpperCase().substring(0, 13);
        final LocalDate postingDate = now();
        final LocalDate expectedDateReady = now().plusDays(28);

        final JsonEnvelope privateEvent = createPayload(caseId, urn, postingDate, expectedDateReady, null, null);

        when(referenceDataService.getProsecutor(any(), any())).thenReturn(java.util.Optional.ofNullable((createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("aocpApproved", true)
                .build())));
        final BigDecimal surchargeAmount = BigDecimal.valueOf(10);
        when(referenceDataService.getVictimSurcharges(any(), any(), eq("Fine"), eq("Adult"))).thenReturn(
                asList(createObjectBuilder()
                        .add("surchargeAmount", surchargeAmount)
                        .add("surchargeAmountMin", JsonValue.NULL)
                        .add("surchargeAmountMax", JsonValue.NULL)
                        .add("surchargeFinePercentage", JsonValue.NULL)
                        .build())
        );

        caseReceivedProcessor.handleCaseReceivedEvent(privateEvent);

        verify(sender, times(2)).send(envelopeCaptor.capture());

        final JsonEnvelope firstCommandEvent = this.envelopeCaptor.getAllValues().get(0);
        assertThat(firstCommandEvent.metadata().name(), is(RESOLVE_CASE_AOCP_ELIGIBILITY));
        final JsonObject commandPayload = (JsonObject) firstCommandEvent.payload();
        assertThat(commandPayload.getString("caseId"), is(caseId.toString()));
        assertThat(commandPayload.getBoolean("isProsecutorAOCPApproved"), is(true));
        assertThat(commandPayload.getJsonNumber("surchargeAmount").bigDecimalValue(), is(surchargeAmount));
        assertThat(commandPayload.containsKey("surchargeAmountMax"), is(false));
        assertThat(commandPayload.containsKey("surchargeAmountMin"), is(false));
        assertThat(commandPayload.containsKey("surchargeFinePercentage"), is(false));

        final JsonEnvelope secondCommandEvent = this.envelopeCaptor.getAllValues().get(1);
        assertThat(secondCommandEvent.metadata().name(), is(CASE_STARTED_PUBLIC_EVENT_NAME));
        assertThat(secondCommandEvent.payload(), payloadIsJson(allOf(
                withJsonPath("$.id", equalTo(caseId.toString())),
                withJsonPath("$.postingDate", equalTo(postingDate.toString())))));

        final JsonObjectBuilder payloadBuilder = createObjectBuilder();
        payloadBuilder.add("CaseReference", urn);
        verify(azureFunctionService).relayCaseOnCPP(payloadBuilder.build().toString());
        verify(timerService).startTimerForDefendantResponse(caseId, expectedDateReady, privateEvent.metadata());
    }

    @Test
    public void shouldUpdateCaseStateAndResolveCaseAOCPEligibilityWhenSurchangeAmountIsNull() throws IOException {
        final UUID caseId = randomUUID();
        final String urn = randomUUID().toString().replace("-", "").toUpperCase().substring(0, 13);
        final LocalDate postingDate = now();
        final LocalDate expectedDateReady = now().plusDays(28);

        final JsonEnvelope privateEvent = createPayload(caseId, urn, postingDate, expectedDateReady, null, null);

        when(referenceDataService.getProsecutor(any(), any())).thenReturn(java.util.Optional.ofNullable((createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("aocpApproved", true)
                .build())));
        final BigDecimal surchargeAmount = BigDecimal.valueOf(10);
        when(referenceDataService.getVictimSurcharges(any(), any(), eq("Fine"), eq("Adult"))).thenReturn(
                asList(createObjectBuilder()
                        .add("surchargeAmountMin", JsonValue.NULL)
                        .add("surchargeAmountMax", JsonValue.NULL)
                        .add("surchargeFinePercentage", JsonValue.NULL)
                        .build())
        );

        caseReceivedProcessor.handleCaseReceivedEvent(privateEvent);

        verify(sender, times(2)).send(envelopeCaptor.capture());

        final JsonEnvelope firstCommandEvent = this.envelopeCaptor.getAllValues().get(0);
        assertThat(firstCommandEvent.metadata().name(), is(RESOLVE_CASE_AOCP_ELIGIBILITY));
        final JsonObject commandPayload = (JsonObject) firstCommandEvent.payload();
        assertThat(commandPayload.getString("caseId"), is(caseId.toString()));
        assertThat(commandPayload.getBoolean("isProsecutorAOCPApproved"), is(true));
        assertThat(commandPayload.containsKey("surchargeAmountMax"), is(false));
        assertThat(commandPayload.containsKey("surchargeAmountMin"), is(false));
        assertThat(commandPayload.containsKey("surchargeFinePercentage"), is(false));

        final JsonEnvelope secondCommandEvent = this.envelopeCaptor.getAllValues().get(1);
        assertThat(secondCommandEvent.metadata().name(), is(CASE_STARTED_PUBLIC_EVENT_NAME));
        assertThat(secondCommandEvent.payload(), payloadIsJson(allOf(
                withJsonPath("$.id", equalTo(caseId.toString())),
                withJsonPath("$.postingDate", equalTo(postingDate.toString())))));

        final JsonObjectBuilder payloadBuilder = createObjectBuilder();
        payloadBuilder.add("CaseReference", urn);
        verify(azureFunctionService).relayCaseOnCPP(payloadBuilder.build().toString());
        verify(timerService).startTimerForDefendantResponse(caseId, expectedDateReady, privateEvent.metadata());
    }

    @Test
    public void shouldUpdateCaseStateAndResolveCaseAOCPEligibilityWhenMinAndMaxVictimSurchargeIsNull() throws IOException {
        final UUID caseId = randomUUID();
        final String urn = randomUUID().toString().replace("-", "").toUpperCase().substring(0, 13);
        final LocalDate postingDate = now();
        final LocalDate expectedDateReady = now().plusDays(28);

        final JsonEnvelope privateEvent = createPayload(caseId, urn, postingDate, expectedDateReady, null, null);

        when(referenceDataService.getProsecutor(any(), any())).thenReturn(java.util.Optional.ofNullable((createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("aocpApproved", true)
                .build())));
        final BigDecimal surchargeAmountMax = BigDecimal.valueOf(100);
        final BigDecimal surchargeFinePercentage = BigDecimal.valueOf(40);
        when(referenceDataService.getVictimSurcharges(any(), any(), eq("Fine"), eq("Adult"))).thenReturn(
                asList(createObjectBuilder()
                        .add("surchargeAmount", JsonValue.NULL)
                        .add("surchargeAmountMin", JsonValue.NULL)
                        .add("surchargeAmountMax", JsonValue.NULL)
                        .add("surchargeFinePercentage", surchargeFinePercentage)
                        .build())
        );

        caseReceivedProcessor.handleCaseReceivedEvent(privateEvent);

        verify(sender, times(2)).send(envelopeCaptor.capture());

        final JsonEnvelope firstCommandEvent = this.envelopeCaptor.getAllValues().get(0);
        assertThat(firstCommandEvent.metadata().name(), is(RESOLVE_CASE_AOCP_ELIGIBILITY));
        final JsonObject commandPayload = (JsonObject) firstCommandEvent.payload();
        assertThat(commandPayload.getString("caseId"), is(caseId.toString()));
        assertThat(commandPayload.getBoolean("isProsecutorAOCPApproved"), is(true));
        assertThat(commandPayload.getJsonNumber("surchargeFinePercentage").bigDecimalValue(), is(surchargeFinePercentage));
        assertThat(commandPayload.containsKey("surchargeAmountMin"), is(false));
        assertThat(commandPayload.containsKey("surchargeAmountMax"), is(false));
        assertThat(commandPayload.containsKey("surchargeAmount"), is(false));

        final JsonEnvelope secondCommandEvent = this.envelopeCaptor.getAllValues().get(1);
        assertThat(secondCommandEvent.metadata().name(), is(CASE_STARTED_PUBLIC_EVENT_NAME));
        assertThat(secondCommandEvent.payload(), payloadIsJson(allOf(
                withJsonPath("$.id", equalTo(caseId.toString())),
                withJsonPath("$.postingDate", equalTo(postingDate.toString())))));

        final JsonObjectBuilder payloadBuilder = createObjectBuilder();
        payloadBuilder.add("CaseReference", urn);
        verify(azureFunctionService).relayCaseOnCPP(payloadBuilder.build().toString());
        verify(timerService).startTimerForDefendantResponse(caseId, expectedDateReady, privateEvent.metadata());
    }

    @Test
    public void shouldResolveCaseAOCPEligibilityWhenDefendantIsOlderThan18YearsOld() {
        final UUID caseId = randomUUID();
        final String urn = randomUUID().toString().replace("-", "").toUpperCase().substring(0, 13);
        final LocalDate postingDate = now();
        final LocalDate expectedDateReady = now().plusDays(28);

        final JsonEnvelope privateEvent = createPayload(caseId, urn, postingDate, expectedDateReady, LocalDate.now().minusYears(18).minusDays(1), null);

        when(referenceDataService.getProsecutor(any(), any())).thenReturn(java.util.Optional.ofNullable((createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("aocpApproved", true)
                .build())));
        final BigDecimal surchargeAmountMin = BigDecimal.ONE;
        final BigDecimal surchargeAmountMax = BigDecimal.valueOf(2000);
        final BigDecimal surchargeFinePercentage = BigDecimal.valueOf(40);
        when(referenceDataService.getVictimSurcharges(any(), any(), eq("Fine"), eq("Adult"))).thenReturn(
                asList(createObjectBuilder()
                        .add("surchargeAmountMin", surchargeAmountMin)
                        .add("surchargeAmountMax", surchargeAmountMax)
                        .add("surchargeFinePercentage", surchargeFinePercentage)
                        .add("surchargeAmount", JsonValue.NULL)
                        .build())
        );

        caseReceivedProcessor.handleCaseReceivedEvent(privateEvent);

        verify(sender, times(2)).send(envelopeCaptor.capture());

        final JsonEnvelope firstCommandEvent = this.envelopeCaptor.getAllValues().get(0);
        assertThat(firstCommandEvent.metadata().name(), is(RESOLVE_CASE_AOCP_ELIGIBILITY));
        final JsonObject commandPayload = (JsonObject) firstCommandEvent.payload();
        assertThat(commandPayload.getString("caseId"), is(caseId.toString()));
        assertThat(commandPayload.getBoolean("isProsecutorAOCPApproved"), is(true));
        assertThat(commandPayload.getJsonNumber("surchargeAmountMin").bigDecimalValue(), is(surchargeAmountMin));
        assertThat(commandPayload.getJsonNumber("surchargeAmountMax").bigDecimalValue(), is(surchargeAmountMax));
        assertThat(commandPayload.getJsonNumber("surchargeFinePercentage").bigDecimalValue(), is(surchargeFinePercentage));
        assertThat(commandPayload.containsKey("surchargeAmount"), is(false));

    }

    @Test
    public void shouldResolveCaseAOCPEligibilityWhenDefendantIsYoungerThan18YearsOld() {
        final UUID caseId = randomUUID();
        final String urn = randomUUID().toString().replace("-", "").toUpperCase().substring(0, 13);
        final LocalDate postingDate = now();
        final LocalDate expectedDateReady = now().plusDays(28);

        final JsonEnvelope privateEvent = createPayload(caseId, urn, postingDate, expectedDateReady, LocalDate.now().minusYears(18).plusDays(1), "");

        when(referenceDataService.getProsecutor(any(), any())).thenReturn(java.util.Optional.ofNullable((createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("aocpApproved", true)
                .build())));

        final BigDecimal surchargeAmount = BigDecimal.valueOf(40);
        when(referenceDataService.getVictimSurcharges(any(), any(), eq("Fine"), eq("Youth"))).thenReturn(
                asList(createObjectBuilder()
                        .add("surchargeAmount", surchargeAmount)
                        .add("surchargeAmountMin", JsonValue.NULL)
                        .add("surchargeAmountMax", JsonValue.NULL)
                        .add("surchargeFinePercentage", JsonValue.NULL)
                        .build())
        );

        caseReceivedProcessor.handleCaseReceivedEvent(privateEvent);

        verify(sender, times(2)).send(envelopeCaptor.capture());

        final JsonEnvelope firstCommandEvent = this.envelopeCaptor.getAllValues().get(0);
        assertThat(firstCommandEvent.metadata().name(), is(RESOLVE_CASE_AOCP_ELIGIBILITY));
        final JsonObject commandPayload = (JsonObject) firstCommandEvent.payload();
        assertThat(commandPayload.getString("caseId"), is(caseId.toString()));
        assertThat(commandPayload.getBoolean("isProsecutorAOCPApproved"), is(true));
        assertThat(commandPayload.getJsonNumber("surchargeAmount").bigDecimalValue(), is(surchargeAmount));
        assertThat(commandPayload.containsKey("surchargeAmountMax"), is(false));
        assertThat(commandPayload.containsKey("surchargeAmountMin"), is(false));
        assertThat(commandPayload.containsKey("surchargeFinePercentage"), is(false));

    }

    @Test
    public void shouldResolveCaseAOCPEligibilityWhenDefendantIs18YearsOld() {
        final UUID caseId = randomUUID();
        final String urn = randomUUID().toString().replace("-", "").toUpperCase().substring(0, 13);
        final LocalDate postingDate = now();
        final LocalDate expectedDateReady = now().plusDays(28);

        final JsonEnvelope privateEvent = createPayload(caseId, urn, postingDate, expectedDateReady, LocalDate.now().minusYears(18), "");

        when(referenceDataService.getProsecutor(any(), any())).thenReturn(java.util.Optional.ofNullable((createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("aocpApproved", true)
                .build())));

        final BigDecimal surchargeAmountMin = BigDecimal.ONE;
        final BigDecimal surchargeAmountMax = BigDecimal.valueOf(2000);
        final BigDecimal surchargeFinePercentage = BigDecimal.valueOf(40);
        when(referenceDataService.getVictimSurcharges(any(), any(), eq("Fine"), eq("Adult"))).thenReturn(
                asList(createObjectBuilder()
                        .add("surchargeAmountMin", surchargeAmountMin)
                        .add("surchargeAmountMax", surchargeAmountMax)
                        .add("surchargeFinePercentage", surchargeFinePercentage)
                        .add("surchargeAmount", JsonValue.NULL)
                        .build())
        );

        caseReceivedProcessor.handleCaseReceivedEvent(privateEvent);

        verify(sender, times(2)).send(envelopeCaptor.capture());

        final JsonEnvelope firstCommandEvent = this.envelopeCaptor.getAllValues().get(0);
        assertThat(firstCommandEvent.metadata().name(), is(RESOLVE_CASE_AOCP_ELIGIBILITY));
        final JsonObject commandPayload = (JsonObject) firstCommandEvent.payload();
        assertThat(commandPayload.getString("caseId"), is(caseId.toString()));
        assertThat(commandPayload.getBoolean("isProsecutorAOCPApproved"), is(true));
        assertThat(commandPayload.getJsonNumber("surchargeAmountMin").bigDecimalValue(), is(surchargeAmountMin));
        assertThat(commandPayload.getJsonNumber("surchargeAmountMax").bigDecimalValue(), is(surchargeAmountMax));
        assertThat(commandPayload.getJsonNumber("surchargeFinePercentage").bigDecimalValue(), is(surchargeFinePercentage));
        assertThat(commandPayload.containsKey("surchargeAmount"), is(false));

    }

    @Test
    public void shouldResolveCaseAOCPEligibilityWhenDefendantIsOrganization() {
        final UUID caseId = randomUUID();
        final String urn = randomUUID().toString().replace("-", "").toUpperCase().substring(0, 13);
        final LocalDate postingDate = now();
        final LocalDate expectedDateReady = now().plusDays(28);

        final JsonEnvelope privateEvent = createPayload(caseId, urn, postingDate, expectedDateReady, LocalDate.now().minusYears(18).plusDays(1), "any legal name");

        when(referenceDataService.getProsecutor(any(), any())).thenReturn(java.util.Optional.ofNullable((createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("aocpApproved", true)
                .build())));
        final BigDecimal surchargeAmountMin = BigDecimal.ONE;
        final BigDecimal surchargeAmountMax = BigDecimal.valueOf(2000);
        final BigDecimal surchargeFinePercentage = BigDecimal.valueOf(40);
        when(referenceDataService.getVictimSurcharges(any(), any(), eq("Fine"), eq("Organisation"))).thenReturn(
                asList(createObjectBuilder()
                        .add("surchargeAmountMin", surchargeAmountMin)
                        .add("surchargeAmountMax", surchargeAmountMax)
                        .add("surchargeFinePercentage", surchargeFinePercentage)
                        .add("surchargeAmount", JsonValue.NULL)
                        .build())
        );

        caseReceivedProcessor.handleCaseReceivedEvent(privateEvent);

        verify(sender, times(2)).send(envelopeCaptor.capture());

        final JsonEnvelope firstCommandEvent = this.envelopeCaptor.getAllValues().get(0);
        assertThat(firstCommandEvent.metadata().name(), is(RESOLVE_CASE_AOCP_ELIGIBILITY));
        final JsonObject commandPayload = (JsonObject) firstCommandEvent.payload();
        assertThat(commandPayload.getString("caseId"), is(caseId.toString()));
        assertThat(commandPayload.getBoolean("isProsecutorAOCPApproved"), is(true));
        assertThat(commandPayload.getJsonNumber("surchargeAmountMin").bigDecimalValue(), is(surchargeAmountMin));
        assertThat(commandPayload.getJsonNumber("surchargeAmountMax").bigDecimalValue(), is(surchargeAmountMax));
        assertThat(commandPayload.getJsonNumber("surchargeFinePercentage").bigDecimalValue(), is(surchargeFinePercentage));
        assertThat(commandPayload.containsKey("surchargeAmount"), is(false));

    }


    @Test
    public void shouldHandleCaseReceivedEvent() {
        assertThat(CaseReceivedProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(method("handleCaseReceivedEvent").thatHandles(CaseReceived.EVENT_NAME)));
    }

    private JsonEnvelope createPayload(final UUID caseId, final String urn, final LocalDate postingDate, final LocalDate expectedDateReady,
                                       final LocalDate dateOfBirth, final String legalEntityName) {
        final JsonObjectBuilder defendantBuilder = createObjectBuilder().add(DEFENDANT_ID, randomUUID().toString());
        if (nonNull(dateOfBirth)) {
            defendantBuilder.add("dateOfBirth", dateOfBirth.toString());
        }
        if (nonNull(legalEntityName)) {
            defendantBuilder.add("legalEntityName", legalEntityName);
        }

        return createEnvelope(CaseReceived.EVENT_NAME,
                createObjectBuilder()
                        .add(CASE_ID, caseId.toString())
                        .add(URN, urn)
                        .add(POSTING_DATE, postingDate.toString())
                        .add(EXPECTED_DATE_READY, expectedDateReady.toString())
                        .add(PROSECUTING_AUTHORITY, "TVL")
                        .add(DEFENDANT, defendantBuilder.build())
                        .build());
    }
}
