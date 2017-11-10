package uk.gov.moj.cpp.sjp.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.domain.plea.Plea.Type.GUILTY;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.moj.cpp.sjp.domain.command.CancelPlea;
import uk.gov.moj.cpp.sjp.domain.command.UpdatePlea;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpdatePleaHandlerTest extends CaseCommandHandlerTest {

    @InjectMocks
    private UpdatePleaHandler updatePleaHandler;

    @Test
    public void shouldUpdatePlea() throws EventStreamException {
        final UpdatePlea updatePlea = new UpdatePlea(UUID.randomUUID(), UUID.randomUUID(), GUILTY.name());

        when(converter.convert(jsonObject, UpdatePlea.class)).thenReturn(updatePlea);
        when(caseAggregate.updatePlea(updatePlea)).thenReturn(events);

        updatePleaHandler.updatePlea(jsonEnvelope);

        verify(converter).convert(jsonObject, UpdatePlea.class);
        verify(caseAggregate).updatePlea(updatePlea);
    }

    @Test
    public void shouldCancelPlea() throws EventStreamException {
        final CancelPlea cancelPlea = new CancelPlea(UUID.randomUUID(), UUID.randomUUID());

        when(converter.convert(jsonObject, CancelPlea.class)).thenReturn(cancelPlea);
        when(caseAggregate.cancelPlea(cancelPlea)).thenReturn(events);

        updatePleaHandler.cancelPlea(jsonEnvelope);

        verify(converter).convert(jsonObject, CancelPlea.class);
        verify(caseAggregate).cancelPlea(cancelPlea);
    }
}