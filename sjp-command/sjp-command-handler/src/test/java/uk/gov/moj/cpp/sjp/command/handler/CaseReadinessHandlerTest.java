package uk.gov.moj.cpp.sjp.command.handler;

import static java.time.ZonedDateTime.now;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PIA;

import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;

@ExtendWith(MockitoExtension.class)
public class CaseReadinessHandlerTest extends CaseCommandHandlerTest {

    @InjectMocks
    private CaseReadinessHandler caseReadinessHandler;

    @BeforeEach
    void setUp() {
        super.setupMocks();
    }

    @Test
    public void shouldHandleMarkCaseReadyForDecisionCommand() throws EventStreamException {
        final CaseReadinessReason readinessReason = PIA;
        final ZonedDateTime markedAt = now();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonObject.getString(CaseCommandHandler.STREAM_ID)).thenReturn(CASE_ID.toString());
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(enveloper.withMetadataFrom(jsonEnvelope)).thenReturn(function);
        when(events.map(function)).thenReturn(jsonEvents);
        when(jsonObject.getString("reason")).thenReturn(readinessReason.name());
        when(jsonObject.getString("markedAt")).thenReturn(markedAt.toString());
        when(caseAggregate.markCaseReadyForDecision(readinessReason, markedAt)).thenReturn(events);

        caseReadinessHandler.markCaseReadyForDecision(jsonEnvelope);

        verify(jsonObject).getString("reason");
        verify(jsonObject).getString("markedAt");
        verify(caseAggregate).markCaseReadyForDecision(readinessReason, markedAt);
    }

    @Test
    public void shouldHandleUnmarkCaseReadyForDecisionCommand() throws EventStreamException {
        final LocalDate expectedDateReady = LocalDate.now();
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonObject.getString(CaseCommandHandler.STREAM_ID)).thenReturn(CASE_ID.toString());
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(enveloper.withMetadataFrom(jsonEnvelope)).thenReturn(function);
        when(events.map(function)).thenReturn(jsonEvents);
        when(jsonObject.getString("expectedDateReady")).thenReturn(expectedDateReady.toString());
        when(caseAggregate.unmarkCaseReadyForDecision(expectedDateReady)).thenReturn(events);

        caseReadinessHandler.unmarkCaseReadyForDecision(jsonEnvelope);

        verify(jsonObject).getString("expectedDateReady");
        verify(caseAggregate).unmarkCaseReadyForDecision(expectedDateReady);
    }

    @AfterEach
    @SuppressWarnings("unchecked")
    public void verifyMocks() throws EventStreamException {
        verify(jsonEnvelope, atLeast(1)).payloadAsJsonObject();
        verify(jsonObject, atLeast(1)).getString(CaseCommandHandler.STREAM_ID);
        verify(eventSource).getStreamById(CASE_ID);
        verify(aggregateService).get(eventStream, CaseAggregate.class);

        verify(enveloper).withMetadataFrom(jsonEnvelope);
        verify(eventStream).append(jsonEvents);
        verify(events).map(function);

        verify(jsonEnvelope, atLeast(0)).metadata();
        verify(metadata, atLeast(0)).userId();

        verifyNoMoreInteractions(eventSource);
        verifyNoMoreInteractions(enveloper);
        verifyNoMoreInteractions(aggregateService);
        verifyNoMoreInteractions(converter);
        verifyNoMoreInteractions(jsonEnvelope);
        verifyNoMoreInteractions(jsonObject);
        verifyNoMoreInteractions(eventStream);
        verifyNoMoreInteractions(caseAggregate);
        verifyNoMoreInteractions(function);
        verifyNoMoreInteractions(events);
        verifyNoMoreInteractions(jsonEvents);
    }

}
