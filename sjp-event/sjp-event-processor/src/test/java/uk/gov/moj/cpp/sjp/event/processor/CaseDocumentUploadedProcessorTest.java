package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.event.processor.activiti.SjpProcessManagerService;
import uk.gov.moj.cpp.sjp.event.processor.utils.MetadataHelper;

import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseDocumentUploadedProcessorTest {

    private static final String DOCUMENT_TYPE = "PLEA";
    private final UUID caseId = randomUUID();
    private final UUID documentReference = randomUUID();
    private final UUID materialId = randomUUID();
    private Metadata materialAddedMetadata;
    private JsonObject materialAddedPayload;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Mock
    private Sender sender;

    @Mock
    private SjpProcessManagerService sjpProcessManagerService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Spy
    private MetadataHelper metadataHelper = new MetadataHelper();

    @InjectMocks
    private CaseDocumentProcessor caseDocumentProcessor = new CaseDocumentProcessor();

    @BeforeEach
    public void setup() {
        materialAddedMetadata = metadataWithRandomUUID("material.material-added").build();
        materialAddedPayload = createObjectBuilder().add("materialId", materialId.toString()).build();
    }

    @Test
    public void shouldHandleCaseDocumentUploadedEvent() {
        final JsonEnvelope envelope = prepareCaseDocumentUploadedEnvelope(caseId, documentReference, DOCUMENT_TYPE);

        caseDocumentProcessor.handleCaseDocumentUploaded(envelope);

        verify(sender, times(2)).send(envelopeCaptor.capture());

        final List<JsonEnvelope> envelopesSent = envelopeCaptor.getAllValues();

        final JsonEnvelope firstEnvelope = envelopesSent.get(0);
        final JsonEnvelope secondEnvelope = envelopesSent.get(1);

        assertThat(firstEnvelope, jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("public.sjp.case-document-uploaded"),
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                        withJsonPath("$.documentId", equalTo(documentReference.toString())))
                )));

        assertThat(secondEnvelope.metadata(), metadata().of(envelope.metadata()).withName("material.command.upload-file")
                .isJson(withJsonPath("sjpMetadata", isJson(allOf(
                        withJsonPath("caseId", equalTo(caseId.toString())),
                        withJsonPath("documentId", equalTo(documentReference.toString())),
                        withJsonPath("documentType", equalTo(DOCUMENT_TYPE)))
                ))));

        assertThat(secondEnvelope.payloadAsJsonObject().toString(), isJson(allOf(
                withJsonPath("materialId", notNullValue()),
                withJsonPath("fileServiceId", is(documentReference.toString())))
        ));
    }

    @Test
    public void shouldHandleMaterialAddedEventWithSjpMetadata() {
        final Metadata enrichedMaterialAddedMetadata = metadataFrom(
                JsonObjects.createObjectBuilder(materialAddedMetadata.asJsonObject())
                        .add("sjpMetadata", createObjectBuilder()
                                .add("caseId", caseId.toString())
                                .add("documentId", documentReference.toString())
                                .add("documentType", DOCUMENT_TYPE)
                                .build()).build())
                .build();

        final JsonEnvelope materialAddedEnvelopedEvent = envelopeFrom(enrichedMaterialAddedMetadata, materialAddedPayload);

        caseDocumentProcessor.handleMaterialAdded(materialAddedEnvelopedEvent);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(materialAddedEnvelopedEvent).withName("sjp.command.add-case-document"),
                payloadIsJson(
                        allOf(
                                withJsonPath("$.id", equalTo(documentReference.toString())),
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.materialId", equalTo(materialId.toString())),
                                withJsonPath("$.documentType", equalTo(DOCUMENT_TYPE))
                        )
                ))));
    }

    @Test
    public void shouldHandleMaterialAddedEventWithSjpProcessId() {
        final String sjpProcessId = randomAlphanumeric(10);

        final Metadata enrichedMaterialAddedMetadata = metadataFrom(
                JsonObjects.createObjectBuilder(materialAddedMetadata.asJsonObject())
                        .add("sjpId", sjpProcessId).build())
                .build();

        final JsonEnvelope materialAddedEnvelopedEvent = envelopeFrom(enrichedMaterialAddedMetadata, materialAddedPayload);

        caseDocumentProcessor.handleMaterialAdded(materialAddedEnvelopedEvent);

        verify(sender, never()).send(any());
        verify(sjpProcessManagerService).signalUploadFileProcess(materialAddedEnvelopedEvent, sjpProcessId, materialId);
    }

    @Test
    public void shouldIgnoreMaterialAddedEventNotInitiatedBySjp() {
        final JsonEnvelope materialAddedEnvelopedEvent = envelopeFrom(materialAddedMetadata, materialAddedPayload);

        caseDocumentProcessor.handleMaterialAdded(materialAddedEnvelopedEvent);

        verify(sender, never()).send(any());
        verify(sjpProcessManagerService, never()).signalUploadFileProcess(any(), any(), any());
    }

    private JsonEnvelope prepareCaseDocumentUploadedEnvelope(final UUID caseId, final UUID documentReference, final String documentType) {
        JsonObject payload = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("documentReference", documentReference.toString())
                .add("documentType", documentType).build();

        return createEnvelope("sjp.events.case-document-uploaded",
                payload);
    }
}
