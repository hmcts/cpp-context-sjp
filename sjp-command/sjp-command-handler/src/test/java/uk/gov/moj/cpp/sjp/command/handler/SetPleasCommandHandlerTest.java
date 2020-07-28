package uk.gov.moj.cpp.sjp.command.handler;

import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds.NO_DISABILITY_NEEDS;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtInterpreter;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.domain.plea.SetPleas;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SetPleasCommandHandlerTest extends CaseCommandHandlerTest {

    @InjectMocks
    private SetPleasCommandHandler setPleasCommandHandler;

    @Spy
    private Clock clock = new StoppedClock(ZonedDateTime.now(UTC));

    @Test
    public void shouldSetPleas() throws EventStreamException {
        final UUID defendantId = UUID.randomUUID();
        final UUID offence1Id = UUID.randomUUID();
        final UUID offence2Id = UUID.randomUUID();

        final SetPleas setPleas = new SetPleas(
                new DefendantCourtOptions(
                        new DefendantCourtInterpreter("ES", true),
                        false,
                        NO_DISABILITY_NEEDS),
                asList(
                        new Plea(defendantId, offence1Id, NOT_GUILTY),
                        new Plea(defendantId, offence2Id, NOT_GUILTY)
                ));

        when(converter.convert(jsonObject, SetPleas.class)).thenReturn(setPleas);
        when(caseAggregate.setPleas(CASE_ID, setPleas, userId, clock.now())).thenReturn(events);

        setPleasCommandHandler.setPleas(jsonEnvelope);

        verify(converter).convert(jsonObject, SetPleas.class);
        verify(caseAggregate).setPleas(CASE_ID, setPleas, userId, clock.now());
    }

}
