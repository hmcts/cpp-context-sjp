package uk.gov.moj.cpp.sjp.event.processor.activiti.tasks;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectMetadata;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.event.processor.utils.MetadataHelper;

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
public class AddMaterialToCaseTest {

    @InjectMocks
    private AddMaterialToCase addMaterialToCaseTask;

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private Sender sender;

    @Spy
    private MetadataHelper metadataHelper;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void addsDocumentToACaseWhenMaterialAdded() {
        final Metadata originalMetadata = JsonObjectMetadata.metadataWithRandomUUIDAndName().build();
        final String originalMetadataString = metadataHelper.metadataToString(originalMetadata);
        when(delegateExecution.getVariable("metadata", String.class)).thenReturn(originalMetadataString);

        final UUID caseId = randomUUID();
        when(delegateExecution.getVariable("caseId", String.class)).thenReturn(caseId.toString());

        final UUID documentReference = randomUUID();
        when(delegateExecution.getVariable("documentReference", String.class)).thenReturn(documentReference.toString());

        final String documentType = "PLEA";
        when(delegateExecution.getVariable("documentType", String.class)).thenReturn(documentType);

        final UUID materialId = randomUUID();
        when(delegateExecution.getVariable("materialId", String.class)).thenReturn(materialId.toString());

        addMaterialToCaseTask.execute(delegateExecution);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope capturedAddDocumentCommandEnvelope = envelopeCaptor.getValue();
        assertThat(capturedAddDocumentCommandEnvelope, jsonEnvelope(
                metadata()
                        .withName("sjp.command.add-case-document"),
                payloadIsJson(allOf(
                        withJsonPath("$.id", is(documentReference.toString())),
                        withJsonPath("$.caseId", is(caseId.toString())),
                        withJsonPath("$.materialId", is(materialId.toString())),
                        withJsonPath("$.documentType", is(documentType))
                ))
        ));

    }
}