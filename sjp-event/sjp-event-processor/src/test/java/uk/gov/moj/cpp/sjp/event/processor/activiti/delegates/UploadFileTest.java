package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.UuidStringMatcher.isAUuid;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.event.processor.utils.MetadataHelper;

import java.util.Optional;
import java.util.UUID;

import org.activiti.engine.delegate.DelegateExecution;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UploadFileTest {

    @InjectMocks
    private UploadFile uploadFileTask;

    @Spy
    private MetadataHelper metadataHelper = new MetadataHelper();

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void sendsMaterialAndRaisesPublicEvent() {
        final Metadata originalMetadata = metadataWithRandomUUIDAndName().build();
        final UUID documentReference = UUID.randomUUID();
        final String originalMetadataString = metadataHelper.metadataToString(originalMetadata);
        final String executionId = "executionId";

        when(delegateExecution.getVariable("metadata", String.class)).thenReturn(originalMetadataString);
        when(delegateExecution.getVariable("documentReference", String.class)).thenReturn(documentReference.toString());
        when(delegateExecution.getId()).thenReturn(executionId);

        uploadFileTask.execute(delegateExecution);

        verify(sender, times(2)).send(envelopeCaptor.capture());

        final JsonEnvelope capturedAddMaterialCommand = envelopeCaptor.getAllValues().get(0);
        assertThat(capturedAddMaterialCommand, jsonEnvelope(
                metadata()
                        .withName("material.command.upload-file"),
                payloadIsJson(allOf(
                        withJsonPath("$.fileServiceId", is(documentReference.toString())),
                        withJsonPath("$.materialId", isAUuid())
                ))
        ));

        final Optional<String> sjpProcessId = metadataHelper.getSjpProcessId(capturedAddMaterialCommand);
        assertTrue(sjpProcessId.isPresent());
        assertThat(sjpProcessId.get(), is(executionId));

        final JsonEnvelope capturedPublicEventCommand = envelopeCaptor.getAllValues().get(1);
        assertThat(capturedPublicEventCommand, jsonEnvelope(
                metadata()
                        .withName("public.sjp.case-document-uploaded"),
                payloadIsJson(allOf(
                        withJsonPath("$.documentId", is(documentReference.toString()))
                ))
        ));

    }

}