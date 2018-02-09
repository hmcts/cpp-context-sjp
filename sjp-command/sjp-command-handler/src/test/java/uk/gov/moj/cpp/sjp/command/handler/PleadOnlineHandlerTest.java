package uk.gov.moj.cpp.sjp.command.handler;

import static java.time.ZoneOffset.UTC;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.ContactDetails;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.domain.IncomeFrequency;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PersonalDetails;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PleadOnline;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PleadOnlineHandlerTest extends CaseCommandHandlerTest {

    @InjectMocks
    private PleadOnlineHandler pleadOnlineHandler;

    @Spy
    private Clock clock = new StoppedClock(ZonedDateTime.now(UTC));

    @Test
    public void shouldPleadOnline() throws EventStreamException {
        final UUID defendantId = UUID.randomUUID();
        final PleadOnline pleadOnline = new PleadOnline(
                defendantId.toString(), new ArrayList<>(), "unavailability",
                "French", "witnessDetails", "witnessDispute",
                new PersonalDetails("firstName", "lastName",
                        new Address("address"),
                        new ContactDetails("email", "homeTelephone", "mobile"),
                        null, "nationalInsuranceNumber"),
                new FinancialMeans(
                    defendantId, new Income(IncomeFrequency.WEEKLY, BigDecimal.valueOf(2000.22)), new Benefits(), "employmentStatus"
                ),
                new Employer(
                        defendantId,  "employer", "employeeReference", "phone", new Address("address")
                ),
                new ArrayList<>()
        );

        when(converter.convert(jsonObject, PleadOnline.class)).thenReturn(pleadOnline);
        when(caseAggregate.pleadOnline(CASE_ID, pleadOnline, clock.now())).thenReturn(events);

        pleadOnlineHandler.pleadOnline(jsonEnvelope);

        verify(converter).convert(jsonObject, PleadOnline.class);
        verify(caseAggregate).pleadOnline(CASE_ID, pleadOnline, clock.now());
    }
}