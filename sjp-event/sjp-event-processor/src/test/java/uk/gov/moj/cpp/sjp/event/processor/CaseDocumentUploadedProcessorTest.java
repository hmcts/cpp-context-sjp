package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.nameUUIDFromBytes;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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
    private static final String DOCUMENT_URI = "https://sadevfilestore.blob.core.windows.net/stack-stagingdvla/generated/doc.pdf";
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

    @Test
    public void shouldForwardTheUriToMaterialWhenDocumentIsBlobAddressed() {
        final String documentUri = "https://sadevfilestore.blob.core.windows.net/stack-stagingdvla/generated/doc.pdf";
        final JsonObject payload = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("documentReferenceUri", documentUri)
                .add("documentType", DOCUMENT_TYPE).build();

        caseDocumentProcessor.handleCaseDocumentUploaded(createEnvelope("sjp.events.case-document-uploaded", payload));

        verify(sender, times(2)).send(envelopeCaptor.capture());
        final List<JsonEnvelope> sent = envelopeCaptor.getAllValues();

        // public event carries the uri variant, not documentId
        assertThat(sent.get(0).payloadAsJsonObject().toString(), isJson(allOf(
                withJsonPath("$.caseId", equalTo(caseId.toString())),
                withJsonPath("$.documentUri", equalTo(documentUri)))));
        assertThat(sent.get(0).payloadAsJsonObject().containsKey("documentId"), is(false));

        // Material gets fileUri and, crucially, NOT fileServiceId - it rejects a command
        // carrying more than one file reference.
        assertThat(sent.get(1).metadata().name(), is("material.command.upload-file"));
        assertThat(sent.get(1).payloadAsJsonObject().toString(), isJson(allOf(
                withJsonPath("$.materialId", notNullValue()),
                withJsonPath("$.fileUri", equalTo(documentUri)))));
        assertThat(sent.get(1).payloadAsJsonObject().containsKey("fileServiceId"), is(false));
    }

    @Test
    public void shouldHandleABlobAddressedDocumentWithoutError() {
        final JsonObject payload = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("documentReferenceUri", DOCUMENT_URI)
                .add("documentType", DOCUMENT_TYPE).build();

        caseDocumentProcessor.handleCaseDocumentUploaded(createEnvelope("sjp.events.case-document-uploaded", payload));

        verify(sender, times(2)).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getAllValues().get(0).metadata().name(), is("public.sjp.case-document-uploaded"));
        assertThat(envelopeCaptor.getAllValues().get(1).metadata().name(), is("material.command.upload-file"));
    }

    @Test
    public void shouldCarryBothReferencesOnwardWhenTheCallerSuppliesBoth() {
        // The shape staging-dvla now sends: it derives the uuid from the uri itself and sends the
        // pair, so a uuid being present must NOT be read as "file service addressed".
        final JsonObject payload = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("documentReference", documentReference.toString())
                .add("documentReferenceUri", DOCUMENT_URI)
                .add("documentType", DOCUMENT_TYPE).build();

        caseDocumentProcessor.handleCaseDocumentUploaded(createEnvelope("sjp.events.case-document-uploaded", payload));

        verify(sender, times(2)).send(envelopeCaptor.capture());
        final List<JsonEnvelope> sent = envelopeCaptor.getAllValues();

        // the public event keeps everything we were given
        assertThat(sent.get(0).payloadAsJsonObject().toString(), isJson(allOf(
                withJsonPath("$.documentId", equalTo(documentReference.toString())),
                withJsonPath("$.documentUri", equalTo(DOCUMENT_URI)))));

        // material takes exactly one, and it has to be the uri - its handler throws on more than
        // one reference, and a derived uuid means nothing to the file service.
        assertThat(sent.get(1).metadata().name(), is("material.command.upload-file"));
        assertThat(sent.get(1).payloadAsJsonObject().toString(), isJson(
                withJsonPath("$.fileUri", equalTo(DOCUMENT_URI))));
        assertThat(sent.get(1).payloadAsJsonObject().containsKey("fileServiceId"), is(false));

        // and the sjp metadata carries both, so handleMaterialAdded uses the supplied uuid
        assertThat(sent.get(1).metadata().asJsonObject().toString(), isJson(allOf(
                withJsonPath("$.sjpMetadata.documentId", equalTo(documentReference.toString())),
                withJsonPath("$.sjpMetadata.documentUri", equalTo(DOCUMENT_URI)))));
    }

    @Test
    public void shouldUseTheSuppliedIdRatherThanDerivingOneWhenBothAreKnown() {
        // The whole point of the change: a derived uuid is still a valid uuid, so a regression here
        // would be invisible without pinning the supplied value explicitly.
        final Metadata enriched = metadataFrom(
                JsonObjects.createObjectBuilder(materialAddedMetadata.asJsonObject())
                        .add("sjpMetadata", createObjectBuilder()
                                .add("caseId", caseId.toString())
                                .add("documentId", documentReference.toString())
                                .add("documentUri", DOCUMENT_URI)
                                .add("documentType", DOCUMENT_TYPE)
                                .build()).build())
                .build();

        caseDocumentProcessor.handleMaterialAdded(envelopeFrom(enriched, materialAddedPayload));

        verify(sender).send(envelopeCaptor.capture());

        assertThat(envelopeCaptor.getValue().payloadAsJsonObject().toString(), isJson(allOf(
                withJsonPath("$.id", equalTo(documentReference.toString())),
                withJsonPath("$.documentUri", equalTo(DOCUMENT_URI)))));

        // specifically NOT the value the fallback would have produced
        assertThat(envelopeCaptor.getValue().payloadAsJsonObject().getString("id"),
                is(not(nameUUIDFromBytes(DOCUMENT_URI.getBytes(UTF_8)).toString())));
    }

    @Test
    public void shouldAddCaseDocumentForABlobAddressedDocumentWithAnIdDerivedFromTheUri() {
        // A blob-addressed document has no file service id, and case_document.id is a uuid primary
        // key, so the id is derived from the uri. The uri rides along so the case document records
        // where it came from and the calling context can correlate the filing.
        caseDocumentProcessor.handleMaterialAdded(blobAddressedMaterialAdded());

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope command = envelopeCaptor.getValue();
        assertThat(command.metadata().name(), is("sjp.command.add-case-document"));
        assertThat(command.payloadAsJsonObject().toString(), isJson(allOf(
                withJsonPath("$.id", equalTo(nameUUIDFromBytes(DOCUMENT_URI.getBytes(UTF_8)).toString())),
                withJsonPath("$.caseId", equalTo(caseId.toString())),
                withJsonPath("$.materialId", equalTo(materialId.toString())),
                withJsonPath("$.documentUri", equalTo(DOCUMENT_URI)))));
    }

    @Test
    public void shouldDeriveTheSameIdEveryTimeSoARedeliveryIsCaughtAsADuplicate() {
        caseDocumentProcessor.handleMaterialAdded(blobAddressedMaterialAdded());
        caseDocumentProcessor.handleMaterialAdded(blobAddressedMaterialAdded());

        verify(sender, times(2)).send(envelopeCaptor.capture());
        final List<JsonEnvelope> sent = envelopeCaptor.getAllValues();

        // Same uri, same id - so the aggregate's containsKey check rejects the second filing
        // exactly as it does on the file-service path.
        assertThat(sent.get(1).payloadAsJsonObject().getString("id"),
                is(sent.get(0).payloadAsJsonObject().getString("id")));
    }

    @Test
    public void shouldNotCarryDocumentUriForAFileServiceAddressedDocument() {
        final Metadata enriched = metadataFrom(
                JsonObjects.createObjectBuilder(materialAddedMetadata.asJsonObject())
                        .add("sjpMetadata", createObjectBuilder()
                                .add("caseId", caseId.toString())
                                .add("documentId", documentReference.toString())
                                .add("documentType", DOCUMENT_TYPE)
                                .build()).build())
                .build();

        caseDocumentProcessor.handleMaterialAdded(envelopeFrom(enriched, materialAddedPayload));

        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().payloadAsJsonObject().containsKey("documentUri"), is(false));
    }

    private JsonEnvelope blobAddressedMaterialAdded() {
        final Metadata enriched = metadataFrom(
                JsonObjects.createObjectBuilder(materialAddedMetadata.asJsonObject())
                        .add("sjpMetadata", createObjectBuilder()
                                .add("caseId", caseId.toString())
                                .add("documentUri", DOCUMENT_URI)
                                .add("documentType", DOCUMENT_TYPE)
                                .build()).build())
                .build();

        return envelopeFrom(enriched, materialAddedPayload);
    }

    @Test
    public void shouldPromoteCaseDocumentUploadRejectedToAPublicEvent() {
        final String description = "Case Document Upload rejected as case is referred to court for hearing";
        final JsonEnvelope privateEvent = createEnvelope("sjp.events.case-document-upload-rejected",
                createObjectBuilder()
                        .add("documentId", documentReference.toString())
                        .add("description", description)
                        .build());

        caseDocumentProcessor.handleCaseDocumentUploadRejected(privateEvent);

        verify(sender).send(envelopeCaptor.capture());
        final JsonEnvelope publicEvent = envelopeCaptor.getValue();

        assertThat(publicEvent.metadata().name(), is("public.sjp.events.case-document-upload-rejected"));

        // Republished verbatim: the payload passes through untouched.
        assertThat(publicEvent.payloadAsJsonObject().toString(), isJson(allOf(
                withJsonPath("$.documentId", equalTo(documentReference.toString())),
                withJsonPath("$.description", equalTo(description)))));

        // Characterising current behaviour, which differs from the sibling promotion in
        // CaseDocumentUpdatedProcessor: this handler copies metadata with
        // JsonEnvelope.metadataFrom rather than Enveloper.withMetadataFrom, so the public event
        // reuses the private event's message id and carries no causation chain. If that is ever
        // brought into line with the other processor, these two assertions are the ones to update.
        assertThat(publicEvent.metadata().id(), is(privateEvent.metadata().id()));
        assertThat(publicEvent.metadata().causation().isEmpty(), is(true));
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
