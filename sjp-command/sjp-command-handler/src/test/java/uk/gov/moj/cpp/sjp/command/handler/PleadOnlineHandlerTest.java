package uk.gov.moj.cpp.sjp.command.handler;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static java.util.UUID.randomUUID;

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
import uk.gov.moj.cpp.sjp.domain.onlineplea.PleadOnlinePcqVisited;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
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
        final UUID defendantId = randomUUID();
        final Address address = new Address("l1", "l2", "l3", "l4", "l5", "postcode");
        final Boolean outstandingFines = false;

        final PleadOnline pleadOnline = new PleadOnline(
                defendantId, emptyList(), "unavailability", "French", TRUE, "witnessDetails", "witnessDispute", outstandingFines,
                new PersonalDetails("firstName", "lastName", address,
                        new ContactDetails("homeTelephone", "mobile", "business", "email1@aaa.bbb", "email2@aaa.bbb"),
                        null, "nationalInsuranceNumber", "region","TESTY708166G99KZ", null,null),
                new FinancialMeans(
                        defendantId,
                        new Income(IncomeFrequency.WEEKLY, BigDecimal.valueOf(2000.22)),
                        new Benefits(),
                        "employmentStatus",
                        null, null, null, null),
                new Employer(defendantId, "employer", "employeeReference", "phone", address),
                emptyList(),
                FALSE,
                null,
                null,
                null
        );

        when(converter.convert(jsonObject, PleadOnline.class)).thenReturn(pleadOnline);
        when(caseAggregate.pleadOnline(CASE_ID, pleadOnline, clock.now(), userId)).thenReturn(events);

        pleadOnlineHandler.pleadOnline(jsonEnvelope);

        verify(converter).convert(jsonObject, PleadOnline.class);
        verify(caseAggregate).pleadOnline(CASE_ID, pleadOnline, clock.now(), userId);
    }

    @Test
    public void shouldPleadOnlinePcqVisited() throws EventStreamException {
        final UUID defendantId = randomUUID();

        final PleadOnlinePcqVisited pleadOnlinePcqVisited = new PleadOnlinePcqVisited(
                defendantId,CASE_ID , "case_run" ,"type", randomUUID());

        when(converter.convert(jsonObject, PleadOnlinePcqVisited.class)).thenReturn(pleadOnlinePcqVisited);
        when(caseAggregate.pleadOnlinePcqVisited(CASE_ID, pleadOnlinePcqVisited, clock.now())).thenReturn(events);

        pleadOnlineHandler.pleadOnlinePcqVisited(jsonEnvelope);

        verify(converter).convert(jsonObject, PleadOnlinePcqVisited.class);
        verify(caseAggregate).pleadOnlinePcqVisited(CASE_ID, pleadOnlinePcqVisited, clock.now());
    }

}
