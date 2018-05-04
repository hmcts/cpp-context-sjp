package uk.gov.moj.cpp.sjp.command.handler;

import static java.time.ZoneOffset.UTC;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.command.CancelPlea;
import uk.gov.moj.cpp.sjp.domain.command.UpdatePlea;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpdatePleaHandlerTest extends CaseCommandHandlerTest {

    @InjectMocks
    private UpdatePleaHandler updatePleaHandler;

    @Spy
    private Clock clock = new StoppedClock(ZonedDateTime.now(UTC));

    @Test
    public void shouldUpdatePlea() throws EventStreamException {
        final UpdatePlea updatePlea = new UpdatePlea(UUID.randomUUID(), UUID.randomUUID(), GUILTY);

        when(converter.convert(jsonObject, UpdatePlea.class)).thenReturn(updatePlea);
        when(caseAggregate.updatePlea(updatePlea, clock.now())).thenReturn(events);

        updatePleaHandler.updatePlea(jsonEnvelope);

        verify(converter).convert(jsonObject, UpdatePlea.class);
        verify(caseAggregate).updatePlea(updatePlea, clock.now());
    }

    @Test
    public void shouldCancelPlea() throws EventStreamException {
        final CancelPlea cancelPlea = new CancelPlea(UUID.randomUUID(), UUID.randomUUID());

        when(converter.convert(jsonObject, CancelPlea.class)).thenReturn(cancelPlea);
        when(caseAggregate.cancelPlea(cancelPlea, clock.now())).thenReturn(events);

        updatePleaHandler.cancelPlea(jsonEnvelope);

        verify(converter).convert(jsonObject, CancelPlea.class);
        verify(caseAggregate).cancelPlea(cancelPlea, clock.now());
    }
}