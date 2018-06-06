package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.MATERIAL_ID;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.event.processor.activiti.SjpProcessManagerService;
import uk.gov.moj.cpp.sjp.event.processor.utils.MetadataHelper;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseDocumentUploadedProcessorTest {

    private final String DOCUMENT_TYPE = "PLEA";
    private final String SJP_ID_METADATA_FIELD = "sjpId";

    @InjectMocks
    private CaseDocumentProcessor processor = new CaseDocumentProcessor();

    @Mock
    private SjpProcessManagerService sjpProcessManagerService;

    @Spy
    private MetadataHelper metadataHelper = new MetadataHelper();

    @Test
    public void publishes() {
        //given
        String caseId = UUID.randomUUID().toString();
        UUID documentReference = UUID.randomUUID();

        //when
        final JsonEnvelope envelope = prepareCaseDocumentUploadedEnvelope(caseId, documentReference, DOCUMENT_TYPE);

        processor.handleCaseDocumentUploaded(envelope);

        //then
        ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);

        verify(sjpProcessManagerService).startUploadFileProcess(envelopeArgumentCaptor.capture(),
                argThat(is(UUID.fromString(caseId))),
                argThat(is(documentReference)),
                argThat(is(DOCUMENT_TYPE))
        );

        final JsonEnvelope capturedEnvelope = envelopeArgumentCaptor.getValue();

        assertThat(capturedEnvelope,
                jsonEnvelope(
                        metadata().of(envelope.metadata()),
                        payload().isJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId)),
                                withJsonPath("$.documentReference", equalTo(documentReference.toString())),
                                withJsonPath("$.documentType", equalTo(DOCUMENT_TYPE))
                                )
                        )
                ));
    }

    @Test
    public void handlesMaterialAddedForDocumentsUploadedThroughSjpContext() {

        final String sjpIdValue = "test";
        final Metadata metadata = metadataFrom(createObjectBuilder()
                .add(SJP_ID_METADATA_FIELD, sjpIdValue)
                .add("id", UUID.randomUUID().toString())
                .add("name", "material.material-added")
                .build());

        final UUID materialId = UUID.randomUUID();

        final JsonObject payload = createObjectBuilder()
                .add(MATERIAL_ID, materialId.toString())
                .build();

        final JsonEnvelope materialAddedEnvelopedEvent = envelopeFrom(metadata, payload);
        processor.handleMaterialAdded(materialAddedEnvelopedEvent);

        verify(sjpProcessManagerService).signalUploadFileProcess(materialAddedEnvelopedEvent, sjpIdValue, materialId);
    }

    @Test
    public void ignoresMaterialAddedForDocumentsUploadedRaisedFromDifferentOrigin() {
        final Metadata metadata = metadataFrom(createObjectBuilder()
                .add("id", UUID.randomUUID().toString())
                .add("name", "material.material-added")
                .build());

        final UUID materialId = UUID.randomUUID();

        final JsonObject payload = createObjectBuilder()
                .add(MATERIAL_ID, materialId.toString())
                .build();

        final JsonEnvelope materialAddedEnvelopedEvent = envelopeFrom(metadata, payload);
        processor.handleMaterialAdded(materialAddedEnvelopedEvent);

        verify(sjpProcessManagerService, never()).signalUploadFileProcess(any(), any(), any());
    }



    private JsonEnvelope prepareCaseDocumentUploadedEnvelope(String caseId, UUID documentReference, String documentType) {
        JsonObject payload = createObjectBuilder()
                .add("caseId", caseId)
                .add("documentReference", documentReference.toString())
                .add("documentType", documentType).build();

        return createEnvelope("sjp.events.case-document-uploaded",
                payload);
    }


}
