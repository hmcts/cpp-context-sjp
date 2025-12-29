package uk.gov.moj.cpp.sjp.query.api.decorator;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.query.api.matcher.Matchers.materialMetadataRequest;

import org.junit.jupiter.api.BeforeEach;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.query.api.service.DocumentMetadataService;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DocumentMetadataDecoratorTest {

    private static final String FILE_NAME = "abc.txt";
    private static final String MIME_TYPE = "application/pdf";
    private static final String ADDED_AT = "2018-03-20T16:14:29.000Z";
    @Mock
    private Requester requester;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private DocumentMetadataService documentMetadataService;//= new DocumentMetadataService();

    @InjectMocks
    private DocumentMetadataDecorator documentMetadataDecorator;

    @BeforeEach
    public void setup(){
    }

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

        final JsonObject caseDocumentMetadataJson = createObjectBuilder()
                .add("fileName", FILE_NAME)
                .add("mimeType", MIME_TYPE)
                .add("addedAt", ADDED_AT)
                .build();

        final JsonEnvelope originalEnvelope = createEnvelope("dummy", createObjectBuilder().build());

        final JsonObject metadataJson = createObjectBuilder()
                .add("fileName", FILE_NAME)
                .add("mimeType", MIME_TYPE)
                .add("addedAt", ADDED_AT)
                .add("caseDocumentMetadata",caseDocumentMetadataJson)
                .build();

        when(documentMetadataService.getMaterialMetadata(any(),any())).thenReturn(Optional.of(metadataJson));

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
        final JsonObject caseDocumentMetadataJson = createObjectBuilder()
                .add("fileName", FILE_NAME)
                .add("mimeType", MIME_TYPE)
                .add("addedAt", ADDED_AT)
                .build();

        final JsonObject caseJson = createObjectBuilder()
                .add("id", caseId.toString())
                .add("caseDocuments",
                        createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("id", caseId.toString())
                                        .add("materialId", materialId1.toString())
                                        .add("caseDocumentMetadata",caseDocumentMetadataJson)
                                )
                                .add(createObjectBuilder()
                                        .add("id", caseId.toString())
                                        .add("materialId", materialId2.toString())
                                        .add("caseDocumentMetadata",caseDocumentMetadataJson)
                                )
                )
                .build();

        final JsonEnvelope originalEnvelope = createEnvelope("dummy", createObjectBuilder().build());

        final JsonObject decoratedCaseJson = documentMetadataDecorator.decorateDocumentsForACase(caseJson, originalEnvelope);

        final JsonObject expectedMetadata1Json = createObjectBuilder()
                .add("fileName",  FILE_NAME)
                .add("mimeType", MIME_TYPE)
                .add("addedAt", ADDED_AT)
                .build();
        final JsonObject expectedMetadata2Json = createObjectBuilder()
                .add("fileName",  FILE_NAME)
                .add("mimeType", MIME_TYPE)
                .add("addedAt", ADDED_AT)
                .build();

        final JsonObject expectedJsonResonse = createObjectBuilder()
                .add("id", caseId.toString())
                .add("caseDocuments", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("id", caseId.toString())
                                .add("materialId", materialId1.toString())
                                .add("caseDocumentMetadata", expectedMetadata1Json)
                        )
                        .add(createObjectBuilder()
                                .add("id", caseId.toString())
                                .add("materialId", materialId2.toString())
                                .add("caseDocumentMetadata", expectedMetadata2Json)
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
