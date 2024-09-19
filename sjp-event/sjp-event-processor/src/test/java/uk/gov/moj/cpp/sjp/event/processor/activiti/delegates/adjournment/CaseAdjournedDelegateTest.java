package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates.adjournment;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.CASE_ADJOURNED_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.METADATA_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.utils.MetadataHelper.metadataToString;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import uk.gov.moj.cpp.sjp.event.processor.activiti.delegates.AbstractCaseDelegateTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseAdjournedDelegateTest extends AbstractCaseDelegateTest {

    @InjectMocks
    private CaseAdjournedDelegate caseAdjournedDelegate;

    @BeforeEach
    public void init() {
        caseId = UUID.randomUUID();
        metadata = metadataWithRandomUUIDAndName().build();

        when(delegateExecution.getProcessBusinessKey()).thenReturn(caseId.toString());
        when(delegateExecution.getVariable(METADATA_VARIABLE, String.class))
                .thenReturn(metadataToString(metadata));
    }

    @Test
    public void shouldSetCaseAdjournmentVariable() {
        caseAdjournedDelegate.execute(delegateExecution);

        verify(delegateExecution).setVariable(CASE_ADJOURNED_VARIABLE, true);
    }

}
