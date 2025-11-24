package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.DATES_TO_AVOID;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.DATES_TO_AVOID_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.METADATA_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_READY_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PLEA_TYPE_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PROCESS_MIGRATION_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.utils.MetadataHelper.metadataToString;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DatesToAvoidProcessedDelegateTest extends AbstractCaseDelegateTest {

    @Captor
    private ArgumentCaptor<JsonEnvelope> argumentCaptor;

    @InjectMocks
    private DatesToAvoidProcessedDelegate datesToAvoidAddedDelegate;

    @BeforeEach
    public void init() {
        caseId = UUID.randomUUID();
        metadata = metadataWithRandomUUIDAndName().build();

        when(delegateExecution.getProcessBusinessKey()).thenReturn(caseId.toString());
        when(delegateExecution.getVariable(METADATA_VARIABLE, String.class))
                .thenReturn(metadataToString(metadata));
    }

    @Test
    public void shouldSetProcessVariables() {
        // GIVEN
        final String datesToAvoid = "dates-to-avoid";

        when(delegateExecution.getVariable(METADATA_VARIABLE, String.class)).thenReturn(metadataToString(metadata));
        when(delegateExecution.hasVariable(DATES_TO_AVOID_VARIABLE)).thenReturn(true);
        when(delegateExecution.hasVariable(PROCESS_MIGRATION_VARIABLE)).thenReturn(false);
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
        when(delegateExecution.getVariable(METADATA_VARIABLE, String.class)).thenReturn(metadataToString(metadata));
        when(delegateExecution.hasVariable(DATES_TO_AVOID_VARIABLE)).thenReturn(false);
        when(delegateExecution.hasVariable(PROCESS_MIGRATION_VARIABLE)).thenReturn(false);
        when(delegateExecution.getVariable(PLEA_TYPE_VARIABLE, String.class)).thenReturn(PleaType.NOT_GUILTY.name());

        // WHEN
        datesToAvoidAddedDelegate.execute(delegateExecution);

        // THEN
        verify(delegateExecution).setVariable(PLEA_READY_VARIABLE, true);
        verify(delegateExecution).setVariable(DATES_TO_AVOID_VARIABLE, "DATES-TO-AVOID not submitted after 10 days.");
        verify(sender, never()).send(any(JsonEnvelope.class));
    }

}