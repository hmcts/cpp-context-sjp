package uk.gov.moj.cpp.sjp.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ActionCourtReferralHandlerTest extends CaseCommandHandlerTest {

    @InjectMocks
    private ActionCourtReferralHandler actionCourtReferralHandler;

    @Test
    public void shouldActionCourtReferral() throws EventStreamException {

        when(caseAggregate.actionCourtReferral(CASE_ID)).thenReturn(events);

        actionCourtReferralHandler.actionCourtReferral(jsonEnvelope);

        verify(caseAggregate).actionCourtReferral(CASE_ID);
    }

}