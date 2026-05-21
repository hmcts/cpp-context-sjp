package uk.gov.moj.cpp.sjp.query.api.decorator;

import static java.util.UUID.fromString;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.api.service.DocumentMetadataService;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;

public class DocumentMetadataDecorator {

    private static final Logger LOGGER = getLogger(DocumentMetadataDecorator.class);
    private static final String CASE_DOCUMENTS = "caseDocuments";
    @Inject
    private DocumentMetadataService documentMetadataService;

    public JsonObject decorateDocumentPayload(final JsonObject documentJson, final JsonEnvelope originalEnvelope) {
        if (documentJson.containsKey("materialId")) {
            final UUID materialUUID = fromString(documentJson.getString("materialId"));
            final Optional<JsonObject> materialMetadata = documentMetadataService.getMaterialMetadata(materialUUID, originalEnvelope);

            return materialMetadata
                    .map(metadata -> createObjectBuilder(documentJson)
                            .add("metadata", metadata.getJsonObject("caseDocumentMetadata"))
                            .build()
                    )
                    .orElseGet(() -> {
                        LOGGER.warn("Given JsonObject for document ID: {} doesn't have metadata for materialId {}",
                                documentJson.getString("id"), materialUUID);
                        return documentJson;
                    });
        } else {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Given JsonObject for document ID: {} doesn't have materialID in following " +
                                "execution context: {}", documentJson.getString("id"),
                        Arrays.toString(Thread.currentThread().getStackTrace()));
            }
            return documentJson;
        }
    }


    public JsonObject decorateDocumentsForACase(final JsonObject caseJson, final JsonEnvelope requestEnvelope) {
        final JsonObjectBuilder objectBuilder = createObjectBuilderWithFilter(caseJson, key -> !CASE_DOCUMENTS.equals(key));

        final JsonArrayBuilder caseDocumentsDecorated = caseJson.getJsonArray("caseDocuments")
                .getValuesAs(JsonObject.class)
                .stream()
                .map(caseDocument -> this.decorateDocumentPayload(caseDocument, requestEnvelope))
                .reduce(JsonObjects.createArrayBuilder(), JsonArrayBuilder::add, JsonArrayBuilder::add);

        objectBuilder.add("caseDocuments", caseDocumentsDecorated);
        return objectBuilder.build();
    }
}
