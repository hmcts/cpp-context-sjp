package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.DefendantRepository;

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
public class FinancialImpositionExportListenerTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID CORRELATION_ID = randomUUID();
    private static final String ACCOUNT_NUMBER = "123456780";

    @Mock
    private DefendantRepository defendantRepository;

    @Spy
    private DefendantDetail defendantDetail;

    @InjectMocks
    private FinancialImpositionExportListener listener;

    @Before
    public void setUp() {
        when(defendantRepository.findBy(DEFENDANT_ID)).thenReturn(defendantDetail);
    }

    @Test
    public void shouldUpdatedDefendantWhenFinancialImpositionCorrelationIdAdded() {
        final JsonEnvelope envelope = createFinancialImpositionCorrelationIdAddedEvent();
        listener.financialImpositionCorrelationIdAdded(envelope);
        verify(defendantRepository).save(defendantDetail);
        assertEquals(CORRELATION_ID, defendantDetail.getCorrelationId());
    }

    @Test
    public void shouldUpdateDefendantAccountNumberWhenFinancialImpositionAccountNumberAdded() {
        final JsonEnvelope envelope = createFinancialImpositionAccountNumberAddedEvent();
        listener.financialImpositionAccountNumberAdded(envelope);
        verify(defendantRepository).save(defendantDetail);
        assertEquals(ACCOUNT_NUMBER, defendantDetail.getAccountNumber());
    }

    @Test
    public void shouldUpdateDefendantAccountNumberWhenFinancialImpositionAccountNumberAddedFromBdf() {
        final JsonEnvelope envelope = createFinancialImpositionAccountNumberAddedBdfEvent();
        listener.financialImpositionAccountNumberAddedBdf(envelope);
        verify(defendantRepository).save(defendantDetail);
        assertEquals(CORRELATION_ID, defendantDetail.getCorrelationId());
        assertEquals(ACCOUNT_NUMBER, defendantDetail.getAccountNumber());
    }

    private JsonEnvelope createFinancialImpositionAccountNumberAddedEvent() {
        final JsonObject payload = createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .add("defendantId", DEFENDANT_ID.toString())
                .add("accountNumber", ACCOUNT_NUMBER)
                .build();

        return envelopeFrom(
                metadataWithRandomUUID("sjp.events.financial-imposition-account-number-added"),
                payload);
    }

    private JsonEnvelope createFinancialImpositionAccountNumberAddedBdfEvent() {
        final JsonObject payload = createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .add("defendantId", DEFENDANT_ID.toString())
                .add("correlationId", CORRELATION_ID.toString())
                .add("accountNumber", ACCOUNT_NUMBER)
                .build();

        return envelopeFrom(
                metadataWithRandomUUID("sjp.events.financial-imposition-account-number-added-bdf"),
                payload);
    }

    private JsonEnvelope createFinancialImpositionCorrelationIdAddedEvent() {
        final JsonObject payload = createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .add("defendantId", DEFENDANT_ID.toString())
                .add("correlationId", CORRELATION_ID.toString())
                .build();

        return envelopeFrom(
                metadataWithRandomUUID("sjp.events.financial-imposition-correlation-id-added"),
                payload);
    }

}
