package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;

import static java.time.ZoneOffset.UTC;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.METADATA_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.utils.MetadataHelper.metadataToString;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;

import java.time.ZonedDateTime;
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
    protected Clock clock = new StoppedClock(ZonedDateTime.now(UTC));

    @Before
    public void init() {
        caseId = UUID.randomUUID();
        metadata = metadataWithRandomUUIDAndName().build();

        when(delegateExecution.getProcessBusinessKey()).thenReturn(caseId.toString());
        when(delegateExecution.getVariable(METADATA_VARIABLE, String.class))
                .thenReturn(metadataToString(metadata));
    }

}
