package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.DATES_TO_AVOID;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.DATES_TO_AVOID_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_READY_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_TYPE_VARIABLE;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DatesToAvoidProcessedDelegateTest extends AbstractCaseDelegateTest {

    @Captor
    private ArgumentCaptor<JsonEnvelope> argumentCaptor;

    @InjectMocks
    private DatesToAvoidProcessedDelegate datesToAvoidAddedDelegate;

    @Test
    public void shouldSetProcessVariables() {
        // GIVEN
        final String datesToAvoid = "dates-to-avoid";

        when(delegateExecution.hasVariable(DATES_TO_AVOID_VARIABLE)).thenReturn(true);
        when(delegateExecution.getVariable(PLEA_TYPE_VARIABLE, String.class)).thenReturn(PleaType.NOT_GUILTY.name());
        when(delegateExecution.getVariable(DATES_TO_AVOID_VARIABLE, String.class)).thenReturn(datesToAvoid);

        // WHEN
        datesToAvoidAddedDelegate.execute(delegateExecution);

        // THEN
        verify(delegateExecution).setVariable(PLEA_READY_VARIABLE, true);

        verify(sender).send(argumentCaptor.capture());

        final JsonEnvelope sentEnvelope = argumentCaptor.getValue();
        assertThat(sentEnvelope.metadata().name(), equalTo("public.sjp.dates-to-avoid-added"));
        assertThat(sentEnvelope.payloadAsJsonObject().getString(CASE_ID), equalTo(caseId.toString()));
        assertThat(sentEnvelope.payloadAsJsonObject().getString(DATES_TO_AVOID), equalTo(datesToAvoid));
    }

    @Test
    public void shouldSetDatesToAvoidMessageWhenNeverSubmitted() {
        // GIVEN
        when(delegateExecution.hasVariable(DATES_TO_AVOID_VARIABLE)).thenReturn(false);
        when(delegateExecution.getVariable(PLEA_TYPE_VARIABLE, String.class)).thenReturn(PleaType.NOT_GUILTY.name());

        // WHEN
        datesToAvoidAddedDelegate.execute(delegateExecution);

        // THEN
        verify(delegateExecution).setVariable(PLEA_READY_VARIABLE, true);
        verify(delegateExecution).setVariable(DATES_TO_AVOID_VARIABLE, "DATES-TO-AVOID not submitted after 10 days.");
        verify(sender, never()).send(any(JsonEnvelope.class));
    }

}