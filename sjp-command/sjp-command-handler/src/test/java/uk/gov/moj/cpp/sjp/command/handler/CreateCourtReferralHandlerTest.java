package uk.gov.moj.cpp.sjp.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

import java.time.LocalDate;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CreateCourtReferralHandlerTest extends CaseCommandHandlerTest {

    @InjectMocks
    private CreateCourtReferralHandler createCourtReferralHandler;

    @Test
    public void shouldCreateCourtReferral() throws EventStreamException {
        final LocalDate hearingDate = LocalDate.now().plusWeeks(1);
        when(jsonObject.getString("hearingDate")).thenReturn(hearingDate.toString());

        when(caseAggregate.createCourtReferral(CASE_ID.toString(), hearingDate)).thenReturn(events);

        createCourtReferralHandler.createCourtReferral(jsonEnvelope);

        verify(caseAggregate).createCourtReferral(CASE_ID.toString(), hearingDate);
        verify(jsonObject).getString("hearingDate");
    }
}