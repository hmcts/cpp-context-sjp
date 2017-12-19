package uk.gov.moj.cpp.sjp.command.handler;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.domain.IncomeFrequency;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PleadOnline;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PleaOnlineHandlerTest extends CaseCommandHandlerTest {

    @InjectMocks
    private PleaOnlineHandler pleaOnlineHandler;

    @Test
    public void shouldPleaOnline() throws EventStreamException {
        final UUID defendantId = UUID.randomUUID();
        final PleadOnline pleadOnline = new PleadOnline(
                defendantId.toString(), new ArrayList<>(), "unavailability",
                "French", "witnessDetails", "witnessDispute",
                new FinancialMeans(
                    defendantId, new Income(IncomeFrequency.WEEKLY, BigDecimal.valueOf(2000.22)), new Benefits(), "employmentStatus"
                ),
                new Employer(
                        defendantId,  "employer", "employeeReference", "phone", new Address("address")
                ),
                new ArrayList<>()
        );

        when(converter.convert(jsonObject, PleadOnline.class)).thenReturn(pleadOnline);
        when(caseAggregate.pleaOnline(any(UUID.class), eq(pleadOnline))).thenReturn(events);

        pleaOnlineHandler.pleaOnline(jsonEnvelope);

        verify(converter).convert(jsonObject, PleadOnline.class);
        verify(caseAggregate).pleaOnline(any(UUID.class), eq(pleadOnline));
    }
}