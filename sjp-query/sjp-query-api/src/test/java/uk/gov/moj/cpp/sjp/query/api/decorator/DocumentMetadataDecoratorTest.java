package uk.gov.moj.cpp.sjp.query.api.decorator;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.query.api.matcher.Matchers.materialMetadataRequest;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.query.api.service.DocumentMetadataService;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DocumentMetadataDecoratorTest {

    private static final String FILE_NAME = "abc.txt";
    private static final String MIME_TYPE = "application/pdf";
    private static final String ADDED_AT = "2018-03-20T16:14:29.000Z";
    @Mock
    private Requester requester;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @InjectMocks
    @Spy
    private DocumentMetadataService documentMetadataService = new DocumentMetadataService();

    @InjectMocks
    private DocumentMetadataDecorator documentMetadataDecorator;

    private static JsonEnvelope materialMetadata(final String filename) {
        return envelope().with(metadataWithRandomUUID("material.query.material-metadata"))
                .withPayloadOf(filename, "fileName")
                .withPayloadOf(MIME_TYPE, "mimeType")
                .withPayloadOf(ADDED_AT, "materialAddedDate")
                .build();
    }

    @Test
    public void shouldDecorateDocumentWithMetadataInformation() {
        final UUID documentId = randomUUID();
        final UUID materialId = randomUUID();

        final JsonObject documentJson = createObjectBuilder()
                .add("id", documentId.toString())
                .add("materialId", materialId.toString())
                .build();

        final JsonEnvelope originalEnvelope = createEnvelope("dummy", createObjectBuilder().build());

        when(requester.requestAsAdmin(argThat(materialMetadataRequest(originalEnvelope, materialId)))).thenReturn(materialMetadata(FILE_NAME));

        final JsonObject decoratedDocumentJson = documentMetadataDecorator.decorateDocumentPayload(documentJson, originalEnvelope);

        final JsonObject expectedMetadataJson = createObjectBuilder()
                .add("fileName", FILE_NAME)
                .add("mimeType", MIME_TYPE)
                .add("addedAt", ADDED_AT)
                .build();

        final JsonObject expectedJsonResonse = createObjectBuilder()
                .add("id", documentId.toString())
                .add("materialId", materialId.toString())
                .add("metadata", expectedMetadataJson)
                .build();

        assertThat(decoratedDocumentJson, equalTo(expectedJsonResonse));
    }

    @Test
    public void shouldReturnOriginalObjectWhenMetadataServiceReturnsNullPayload() {
        final UUID documentId = randomUUID();
        final UUID materialId = randomUUID();

        final JsonObject documentJson = createObjectBuilder()
                .add("id", documentId.toString())
                .add("materialId", materialId.toString())
                .build();

        final JsonEnvelope originalEnvelope = createEnvelope("dummy", createObjectBuilder().build());

        when(requester.requestAsAdmin(argThat(materialMetadataRequest(originalEnvelope, materialId)))).thenReturn(materialMetadataWithNullPayload());

        final JsonObject decoratedDocumentJson = documentMetadataDecorator.decorateDocumentPayload(documentJson, originalEnvelope);

        assertSame(decoratedDocumentJson, documentJson);
    }

    @Test
    public void shouldReturnOriginalObjectWhenNoMaterialIdIsPresent() {
        final UUID documentId = randomUUID();

        final JsonObject documentJson = createObjectBuilder()
                .add("id", documentId.toString())
                .build();

        final JsonEnvelope originalEnvelope = createEnvelope("dummy", createObjectBuilder().build());

        final JsonObject decoratedDocumentJson = documentMetadataDecorator.decorateDocumentPayload(documentJson, originalEnvelope);

        assertSame(decoratedDocumentJson, documentJson);
    }

    @Test
    public void shouldDecorateCasePayloadWithDocumentMetadata() {
        final UUID caseId = randomUUID();
        final UUID materialId1 = randomUUID();
        final UUID materialId2 = randomUUID();

        final JsonObject caseJson = createObjectBuilder()
                .add("id", caseId.toString())
                .add("caseDocuments",
                        createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("materialId", materialId1.toString())
                                )
                                .add(createObjectBuilder()
                                        .add("materialId", materialId2.toString())
                                )
                )
                .build();

        final JsonEnvelope originalEnvelope = createEnvelope("dummy", createObjectBuilder().build());

        when(requester.requestAsAdmin(argThat(materialMetadataRequest(originalEnvelope, materialId1)))).thenReturn(materialMetadata("1" + FILE_NAME));
        when(requester.requestAsAdmin(argThat(materialMetadataRequest(originalEnvelope, materialId2)))).thenReturn(materialMetadata("2" + FILE_NAME));

        final JsonObject decoratedCaseJson = documentMetadataDecorator.decorateDocumentsForACase(caseJson, originalEnvelope);

        final JsonObject expectedMetadata1Json = createObjectBuilder()
                .add("fileName", "1" + FILE_NAME)
                .add("mimeType", MIME_TYPE)
                .add("addedAt", ADDED_AT)
                .build();
        final JsonObject expectedMetadata2Json = createObjectBuilder()
                .add("fileName", "2" + FILE_NAME)
                .add("mimeType", MIME_TYPE)
                .add("addedAt", ADDED_AT)
                .build();

        final JsonObject expectedJsonResonse = createObjectBuilder()
                .add("id", caseId.toString())
                .add("caseDocuments", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("materialId", materialId1.toString())
                                .add("metadata", expectedMetadata1Json)
                        )
                        .add(createObjectBuilder()
                                .add("materialId", materialId2.toString())
                                .add("metadata", expectedMetadata2Json)
                        )
                )
                .build();

        assertThat(decoratedCaseJson, equalTo(expectedJsonResonse));

    }

    private JsonEnvelope materialMetadataWithNullPayload() {
        return envelope().with(metadataWithRandomUUID("material.query.material-metadata"))
                .withNullPayload()
                .build();

    }

}
