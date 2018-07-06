package uk.gov.moj.cpp.sjp.query.api;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonValueNullMatcher.isJsonValueNull;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SjpDocumentApiTest {

    @Mock
    private Requester requester;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @InjectMocks
    private SjpDocumentApi sjpDocumentApi;

    final UUID caseId = randomUUID();
    final UUID documentId = randomUUID();
    final UUID materialId = randomUUID();

    @Test
    public void shouldGetDocumentMetadataFromMaterialContext() {
        final String fileName = "testFile";
        final String mimeType = "application";
        final ZonedDateTime addedAt = ZonedDateTime.now();

        final JsonEnvelope documentMetadataQuery = documentMetadataQuery(caseId, documentId);
        final JsonEnvelope documentDetails = documentDetails(documentId, materialId);
        final JsonEnvelope materialMetadata = materialMetadata(materialId, fileName, mimeType, addedAt);

        when(requester.request(argThat(documentRequest(documentMetadataQuery, caseId, documentId)))).thenReturn(documentDetails);
        when(requester.requestAsAdmin(argThat(materialMetadataRequest(documentDetails, materialId)))).thenReturn(materialMetadata);

        final JsonEnvelope documentMetadata = sjpDocumentApi.getCaseDocumentMetadata(documentMetadataQuery);

        assertThat(documentMetadata, jsonEnvelope(
                withMetadataEnvelopedFrom(documentDetails).withName("sjp.query.case-document-metadata"),
                payloadIsJson(allOf(
                        withJsonPath("$.caseDocumentMetadata.fileName", equalTo(fileName)),
                        withJsonPath("$.caseDocumentMetadata.mimeType", equalTo(mimeType)),
                        withJsonPath("$.caseDocumentMetadata.addedAt", equalTo(addedAt.toString())),
                        withoutJsonPath("$.caseDocumentMetadata.materialId"),
                        withoutJsonPath("$.caseDocumentMetadata.alfrescoAssetId"),
                        withoutJsonPath("$.caseDocumentMetadata.externalLink")
                ))));

        verifyMaterialCall(documentDetails, materialId);
    }

    @Test
    public void shouldReturnNullJsonWhenDocumentNotFound() {
        final JsonEnvelope documentMetadataQuery = documentMetadataQuery(caseId, documentId);

        final JsonEnvelope documentDetails = missingDocumentDetails();

        when(requester.request(argThat(documentRequest(documentMetadataQuery, caseId, documentId)))).thenReturn(documentDetails);

        final JsonEnvelope documentMetadata = sjpDocumentApi.getCaseDocumentMetadata(documentMetadataQuery);

        assertThat(documentMetadata, jsonEnvelope(
                withMetadataEnvelopedFrom(documentDetails).withName("sjp.query.case-document-metadata"),
                payload().isJsonValue(isJsonValueNull())));

        verify(requester, never()).requestAsAdmin(any());
    }

    @Test
    public void shouldReturnNullJsonWhenMaterialNotFound() {
        final JsonEnvelope documentMetadataQuery = documentMetadataQuery(caseId, documentId);

        final JsonEnvelope documentDetails = documentDetails(documentId, materialId);
        final JsonEnvelope materialMetadata = missingMaterialMetadata();

        when(requester.request(argThat(documentRequest(documentMetadataQuery, caseId, documentId)))).thenReturn(documentDetails);
        when(requester.requestAsAdmin(argThat(materialMetadataRequest(documentDetails, materialId)))).thenReturn(materialMetadata);

        final JsonEnvelope documentMetadata = sjpDocumentApi.getCaseDocumentMetadata(documentMetadataQuery);

        assertThat(documentMetadata, jsonEnvelope(
                withMetadataEnvelopedFrom(documentDetails).withName("sjp.query.case-document-metadata"),
                payload().isJsonValue(isJsonValueNull())));

        verifyMaterialCall(documentDetails, materialId);
    }

    @Test
    public void shouldHandlesDocumentQueries() {
        assertThat(SjpDocumentApi.class, isHandlerClass(Component.QUERY_API)
                .with(allOf(
                        method("getCaseDocuments").thatHandles("sjp.query.case-documents").withRequesterPassThrough(),
                        method("getCaseDocument").thatHandles("sjp.query.case-document").withRequesterPassThrough(),
                        method("getCaseDocumentMetadata").thatHandles("sjp.query.case-document-metadata"),
                        method("getCaseDocumentDetails").thatHandles("sjp.query.case-document-content")
                )));
    }

    private JsonEnvelope documentMetadataQuery(final UUID caseId, final UUID documentId) {
        return envelope().with(metadataWithRandomUUID("sjp.query.case-document-metadata"))
                .withPayloadOf(caseId.toString(), "caseId")
                .withPayloadOf(documentId.toString(), "documentId")
                .build();
    }

    private static JsonEnvelope documentDetails(final UUID documentId, final UUID materialId) {
        final JsonObject caseDocument = Json.createObjectBuilder()
                .add("id", documentId.toString())
                .add("materialId", materialId.toString())
                .add("documentType", "OTHER-Travelcard")
                .add("documentNumber", 1)
                .build();

        return envelope().with(metadataWithRandomUUID("sjp.query.case-document"))
                .withPayloadOf(caseDocument, "caseDocument")
                .build();
    }

    private static JsonEnvelope missingDocumentDetails() {
        return envelope().with(metadataWithRandomUUID("sjp.query.case-document")).withNullPayload().build();
    }

    private static JsonEnvelope materialMetadata(final UUID materialId, final String fileName, final String mimeType, final ZonedDateTime addedAt) {
        return envelope().with(metadataWithRandomUUID("material.query.material-metadata"))
                .withPayloadOf(materialId.toString(), "materialId")
                .withPayloadOf(fileName, "fileName")
                .withPayloadOf(mimeType, "mimeType")
                .withPayloadOf(addedAt.toString(), "materialAddedDate")
                .withPayloadOf(random(10), "alfrescoAssetId")
                .withPayloadOf(random(10), "externalLink")
                .build();
    }

    private static JsonEnvelope missingMaterialMetadata() {
        return envelope().with(metadataWithRandomUUID("material.query.material-metadata")).withNullPayload().build();
    }

    private static JsonEnvelopeMatcher documentRequest(final JsonEnvelope sourceEnvelope, final UUID caseId, final UUID documentId) {
        return jsonEnvelope(withMetadataEnvelopedFrom(sourceEnvelope).withName("sjp.query.case-document"),
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                        withJsonPath("$.documentId", equalTo(documentId.toString()))
                )));
    }

    private static JsonEnvelopeMatcher materialMetadataRequest(final JsonEnvelope sourceEnvelope, final UUID materialId) {
        return jsonEnvelope(withMetadataEnvelopedFrom(sourceEnvelope).withName("material.query.material-metadata"),
                payloadIsJson(withJsonPath("$.materialId", equalTo(materialId.toString()))));
    }

    private void verifyMaterialCall(final JsonEnvelope sourceEnvelope, final UUID materialId) {
        verify(requester).requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(sourceEnvelope).withName("material.query.material-metadata"),
                payloadIsJson(withJsonPath("$.materialId", equalTo(materialId.toString()))))));
    }
}
