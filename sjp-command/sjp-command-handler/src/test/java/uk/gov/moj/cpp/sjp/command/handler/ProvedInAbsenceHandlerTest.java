package uk.gov.moj.cpp.sjp.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;

@ExtendWith(MockitoExtension.class)
public class ProvedInAbsenceHandlerTest extends CaseCommandHandlerTest {
    @InjectMocks
    private ProvedInAbsenceHandler provedInAbsenceHandler;

    @BeforeEach
    void setUp() {
        super.setupMocks();
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonObject.getString(CaseCommandHandler.STREAM_ID)).thenReturn(CASE_ID.toString());
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(enveloper.withMetadataFrom(jsonEnvelope)).thenReturn(function);
        when(events.map(function)).thenReturn(jsonEvents);
    }

    @Test
    public void testExpireDefendantResponseTimer() throws EventStreamException {
        when(caseAggregate.expireDefendantResponseTimer()).thenReturn(events);
        provedInAbsenceHandler.expireDefendantResponseTimer(jsonEnvelope);

        verify(caseAggregate).expireDefendantResponseTimer();
    }
}