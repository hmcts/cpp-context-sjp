package uk.gov.moj.cpp.sjp.command.handler;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.domain.IncomeFrequency;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FinancialMeansHandlerTest extends CaseCommandHandlerTest {

    @InjectMocks
    private FinancialMeansHandler financialMeansHandler;

    @Test
    public void shouldUpdateFinancialMeans() throws EventStreamException {

        final Income income = new Income(IncomeFrequency.MONTHLY, BigDecimal.valueOf(1000.50));
        final Benefits benefits = new Benefits(true, "Benefits type");
        final FinancialMeans financialMeans = new FinancialMeans(UUID.randomUUID(), income, benefits, "EMPLOYED");

        when(converter.convert(jsonObject, FinancialMeans.class)).thenReturn(financialMeans);
        when(caseAggregate.updateFinancialMeans(userId, financialMeans)).thenReturn(events);

        financialMeansHandler.updateFinancialMeans(jsonEnvelope);

        verify(converter).convert(jsonObject, FinancialMeans.class);
        verify(caseAggregate).updateFinancialMeans(userId, financialMeans);
    }


    @Test
    public void shouldRaiseFinancialMeansDeletedEvent() throws EventStreamException {

        final String defendantId = "c43946f7-8f39-4814-82d6-7bee1e34d07f";
        when(jsonObject.getString("defendantId")).thenReturn(defendantId);
        when(caseAggregate.deleteFinancialMeans(UUID.fromString(defendantId))).thenReturn(events);

        financialMeansHandler.deleteFinancialMeans(jsonEnvelope);

        verify(jsonObject, atLeast(1)).getString("defendantId");
        verify(caseAggregate).deleteFinancialMeans(anyObject());
    }

}