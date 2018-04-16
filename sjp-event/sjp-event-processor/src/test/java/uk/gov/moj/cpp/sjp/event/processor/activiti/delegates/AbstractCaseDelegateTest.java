package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;

import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.METADATA_VARIABLE;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.event.processor.utils.MetadataHelper;

import java.util.UUID;

import org.activiti.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Spy;

public abstract class AbstractCaseDelegateTest {

    protected UUID caseId;

    protected Metadata metadata;

    @Mock
    protected Sender sender;

    @Mock
    protected DelegateExecution delegateExecution;

    @Spy
    private MetadataHelper metadataHelper = new MetadataHelper();

    @Before
    public void init() {
        caseId = UUID.randomUUID();
        metadata = metadataWithRandomUUIDAndName().build();

        final String metadataAsString = metadataHelper.metadataToString(metadata);

        when(delegateExecution.getProcessBusinessKey()).thenReturn(caseId.toString());
        when(delegateExecution.getVariable(METADATA_VARIABLE, String.class)).thenReturn(metadataAsString);
    }

}
