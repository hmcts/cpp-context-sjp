package uk.gov.moj.cpp.sjp.command.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.domain.IncomeFrequency;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;

@ExtendWith(MockitoExtension.class)
public class FinancialMeansHandlerTest extends CaseCommandHandlerTest {

    @InjectMocks
    private FinancialMeansHandler financialMeansHandler;

    @Test
    public void shouldUpdateFinancialMeans() throws EventStreamException {
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonObject.getString(CaseCommandHandler.STREAM_ID)).thenReturn(CASE_ID.toString());
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(enveloper.withMetadataFrom(jsonEnvelope)).thenReturn(function);
        when(events.map(function)).thenReturn(jsonEvents);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonEnvelope.metadata()).thenReturn(metadata);
        when(metadata.userId()).thenReturn(Optional.of(userId.toString()));

        final Income income = new Income(IncomeFrequency.MONTHLY, BigDecimal.valueOf(1000.50));
        final Benefits benefits = new Benefits(true, "Benefits type");
        final FinancialMeans financialMeans = new FinancialMeans(UUID.randomUUID(), income, benefits, "EMPLOYED", null, null, null, null);

        when(converter.convert(jsonObject, FinancialMeans.class)).thenReturn(financialMeans);
        when(caseAggregate.updateFinancialMeans(userId, financialMeans)).thenReturn(events);

        financialMeansHandler.updateFinancialMeans(jsonEnvelope);

        verify(converter).convert(jsonObject, FinancialMeans.class);
        verify(caseAggregate).updateFinancialMeans(userId, financialMeans);
    }


    @Test
    public void shouldRaiseFinancialMeansDeletedEvent() throws EventStreamException {
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonObject.getString(CaseCommandHandler.STREAM_ID)).thenReturn(CASE_ID.toString());
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(enveloper.withMetadataFrom(jsonEnvelope)).thenReturn(function);
        when(events.map(function)).thenReturn(jsonEvents);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);

        final String defendantId = "c43946f7-8f39-4814-82d6-7bee1e34d07f";
        when(jsonObject.getString("defendantId")).thenReturn(defendantId);
        when(caseAggregate.deleteFinancialMeans(UUID.fromString(defendantId))).thenReturn(events);

        financialMeansHandler.deleteFinancialMeans(jsonEnvelope);

        verify(jsonObject, atLeast(1)).getString("defendantId");
        verify(caseAggregate).deleteFinancialMeans(any());
    }

}