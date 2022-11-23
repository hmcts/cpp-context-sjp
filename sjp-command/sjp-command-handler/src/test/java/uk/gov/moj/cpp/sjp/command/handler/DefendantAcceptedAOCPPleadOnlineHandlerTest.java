package uk.gov.moj.cpp.sjp.command.handler;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.ContactDetails;
import uk.gov.moj.cpp.sjp.domain.onlineplea.Offence;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PersonalDetails;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PleadAocpOnline;

import java.time.ZonedDateTime;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefendantAcceptedAOCPPleadOnlineHandlerTest extends CaseCommandHandlerTest {

    @InjectMocks
    private DefendantAcceptedAOCPPleadOnlineHandler defendantAcceptedAOCPPleadOnlineHandler;

    @Spy
    private Clock clock = new StoppedClock(ZonedDateTime.now(UTC));

    @Test
    public void shouldPleadAocpAcceptedOnline() throws EventStreamException {

        final Address address = new Address("l1", "l2", "l3", "l4", "l5", "postcode");
        final PersonalDetails personalDetails = new PersonalDetails("firstName", "lastName", address,
                new ContactDetails("homeTelephone", "mobile", "business", "email1@aaa.bbb", "email2@aaa.bbb"),
                null, "nationalInsuranceNumber", "region", "TESTY708166G99KZ", null, null);


        final Offence offence = new Offence(randomUUID(), NOT_GUILTY, null, null);

        final PleadAocpOnline pleadAocpOnline = new PleadAocpOnline(randomUUID(), randomUUID(),
                Arrays.asList(offence), true, personalDetails);

        when(converter.convert(jsonObject, PleadAocpOnline.class)).thenReturn(pleadAocpOnline);
        when(caseAggregate.pleadAocpAcceptedOnline(pleadAocpOnline, clock.now())).thenReturn(events);

        defendantAcceptedAOCPPleadOnlineHandler.pleadAocpAcceptedOnline(jsonEnvelope);

        verify(converter).convert(jsonObject, PleadAocpOnline.class);
        verify(caseAggregate).pleadAocpAcceptedOnline(pleadAocpOnline, clock.now());
    }
}
