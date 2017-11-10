package uk.gov.moj.cpp.sjp.command.handler;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class CaseUpdateRejectedHandlerTest extends CaseCommandHandlerTest {

    @InjectMocks
    private CaseUpdateRejectedHandler caseUpdateRejectedHandler;

    @Test
    public void shouldRejectCaseUpdate() throws Exception {

        when(caseAggregate.caseUpdateRejected(CASE_ID.toString(), CaseUpdateRejected.RejectReason.CASE_COMPLETED)).thenReturn(events);
        when(jsonObject.getString(eq("reason"))).thenReturn(CaseUpdateRejected.RejectReason.CASE_COMPLETED.name());

        caseUpdateRejectedHandler.caseUpdateRejected(jsonEnvelope);

        verify(jsonObject).getString(eq("reason"));
        verify(caseAggregate).caseUpdateRejected(CASE_ID.toString(), CaseUpdateRejected.RejectReason.CASE_COMPLETED);
    }
}
