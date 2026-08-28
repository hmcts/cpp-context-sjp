package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates.timers;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.event.processor.activiti.ActivitiJUnit5Extension;
import uk.gov.moj.cpp.sjp.event.processor.activiti.TimerExpirationProcess;

import java.time.LocalDate;
import java.util.UUID;

import org.activiti.engine.runtime.Job;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.test.Deployment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TimerExpirationProcessTest {

    private static final String TIMEOUT_PROCESS_PATH = "processes/timerTimeout.bpmn20.xml";

    @RegisterExtension
    ActivitiJUnit5Extension rule = new ActivitiJUnit5Extension();

    @Mock
    private Sender sender;

    private TimerExpirationProcess process;

    private UUID caseId;

    @Captor
    private ArgumentCaptor<JsonEnvelope> argumentCaptor;

    private Metadata metadata;

    @BeforeEach
    public void init() {
        Mockito.reset(MockSender.sender);
        caseId = randomUUID();

        this.metadata = metadataWithRandomUUIDAndName().build();
        process = new TimerExpirationProcess(rule.getRuntimeService());
    }

    @Test
    @Deployment(resources = TIMEOUT_PROCESS_PATH)
    public void shouldTriggerTimeoutTaskAfterTimeout() {
        final LocalDate postingDate = LocalDate.now();
        final LocalDate defendantResponseExpiryDate = postingDate.plusDays(28);

        process.startTimerForDelayAndCommand(caseId, defendantResponseExpiryDate, "dummy.command", metadata);

        final ProcessInstance processInstance = rule.getRuntimeService().createProcessInstanceQuery()
                .processInstanceBusinessKey(caseId.toString())
                .singleResult();

        final Job job = rule.getProcessEngine().getManagementService()
                .createJobQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .singleResult();

        assertThat(job.getDuedate().toInstant(), equalTo(defendantResponseExpiryDate.atStartOfDay(UTC).toInstant()));

        rule.getProcessEngine().getManagementService().executeJob(job.getId());

        verify(MockSender.sender).sendAsAdmin(argumentCaptor.capture());

        final JsonEnvelope sentEnvelope = argumentCaptor.getValue();

        assertThat(sentEnvelope.metadata().name(), equalTo("dummy.command"));
        assertThat(sentEnvelope.payloadAsJsonObject().getString(CASE_ID), equalTo(caseId.toString()));
    }
}
