package uk.gov.moj.cpp.sjp.event.processor.activiti;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.event.processor.utils.MetadataHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ExecutionQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SjpProcessManagerServiceTest {

    @InjectMocks
    private SjpProcessManagerService service;

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private MetadataHelper metadataHelper;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private Metadata metadata;

    @Mock
    private ProcessInstance processInstance;

    @Mock
    private ExecutionQuery executionQuery;

    @Mock
    private Execution execution;

    @Test
    public void startsUploadFileProcess() {
        when(envelope.metadata()).thenReturn(metadata);
        when(metadataHelper.metadataToString(envelope.metadata())).thenReturn("metadataAsString");

        final UUID caseId = UUID.randomUUID();
        final UUID documentReference = UUID.randomUUID();
        final String documentType = "PLEA";

        final Map<String, Object> expectedProcessVariables = new HashMap<>();
        expectedProcessVariables.put("metadata", "metadataAsString");
        expectedProcessVariables.put("caseId", caseId.toString());
        expectedProcessVariables.put("documentReference", documentReference.toString());
        expectedProcessVariables.put("documentType", documentType);

        when(runtimeService.startProcessInstanceByKey(anyString(), any(Map.class))).thenReturn(processInstance);

        service.startUploadFileProcess(envelope, caseId, documentReference, documentType);

        verify(runtimeService).startProcessInstanceByKey("upload-file", expectedProcessVariables);
    }

    @Test
    public void signalsUploadFileProcess() {
        final String processId = "processId";
        when(runtimeService.createExecutionQuery()).thenReturn(executionQuery);
        when(executionQuery.processInstanceId(processId)).thenReturn(executionQuery);
        when(executionQuery.activityId("wait-for-material-added")).thenReturn(executionQuery);
        when(executionQuery.singleResult()).thenReturn(execution);

        when(envelope.metadata()).thenReturn(metadata);
        when(metadataHelper.metadataToString(envelope.metadata())).thenReturn("metadataAsString");

        final UUID materialId = UUID.randomUUID();

        final Map<String, Object> expectedProcessVariables = new HashMap<>();
        expectedProcessVariables.put("metadata", "metadataAsString");
        expectedProcessVariables.put("materialId", materialId.toString());

        final String executionId = "executionId";
        when(execution.getId()).thenReturn(executionId);
        service.signalUploadFileProcess(envelope, processId, materialId);

        verify(runtimeService).signal(executionId, expectedProcessVariables);
    }
}