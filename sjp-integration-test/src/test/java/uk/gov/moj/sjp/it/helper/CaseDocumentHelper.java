package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonassert.JsonAssert.with;
import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.JsonPath.compile;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.UUID.fromString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.nameUUIDFromBytes;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.matchers.UuidStringMatcher.isAUuid;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_ACTIVE_MQ_TOPIC;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_ADDED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_ALREADY_EXISTS;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_UPLOADED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_UPLOAD_REJECTED;
import static uk.gov.moj.sjp.it.stub.MaterialStub.stubMaterialMetadata;
import static uk.gov.moj.sjp.it.test.BaseIntegrationTest.USER_ID;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseById;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseByIdWithDocumentMetadata;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseDocumentsByCaseId;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makeMultipartFormPostCall;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaults;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.moj.sjp.it.util.HttpClientUtil;
import uk.gov.moj.sjp.it.util.JsonHelper;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.jayway.jsonpath.Filter;
import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper for CaseDocument.
 */
public class CaseDocumentHelper implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseDocumentHelper.class);

    private static final String WRITE_MEDIA_TYPE = "application/vnd.sjp.add-case-document+json";
    private static final String UPLOAD_CASE_DOCUMENT_JSON_MEDIA_TYPE = "application/vnd.sjp.upload-case-document+json";
    public static final String GET_CASE_DOCUMENTS_MEDIA_TYPE = "application/vnd.sjp.query.case-documents+json";

    private static final String TEMPLATE_ADD_CASE_DOCUMENT_PAYLOAD = "payload/sjp.command.add-case-document.json";

    private static final String MATERIAL_ID_PROPERTY = "materialId";
    private static final String DOCUMENT_TYPE_PROPERTY = "documentType";
    private static final String DOCUMENT_TYPE_PLEA = "PLEA";
    private static final String FILE_NAME_PLEA = "SMITH_Fred_TFL2041315_PLEA.pdf";
    private static final String FILE_PATH_PLEA = "src/test/resources/plea";

    private UUID caseId;
    private String request;
    private String id;
    private String materialId;

    private MessageConsumerClient publicCaseDocumentAlreadyExistsConsumer = new MessageConsumerClient();
    private MessageConsumerClient publicCaseDocumentUploaded = new MessageConsumerClient();

    private MessageConsumerClient publicConsumer = new MessageConsumerClient();
    private MessageConsumerClient publicConsumerForRejected = new MessageConsumerClient();

    private ZonedDateTime uploadTime;

    public CaseDocumentHelper(UUID caseId) {
        this.id = randomUUID().toString();
        this.materialId = randomUUID().toString();
        this.caseId = caseId;

        publicConsumer.startConsumer(PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_ADDED, PUBLIC_ACTIVE_MQ_TOPIC);
        publicConsumerForRejected.startConsumer(PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_UPLOAD_REJECTED, PUBLIC_ACTIVE_MQ_TOPIC);

        publicCaseDocumentAlreadyExistsConsumer.startConsumer(PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_ALREADY_EXISTS, PUBLIC_ACTIVE_MQ_TOPIC);
        publicCaseDocumentUploaded.startConsumer(PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_UPLOADED, PUBLIC_ACTIVE_MQ_TOPIC);
    }

    private void addCaseDocument(UUID userId, String payload, String documentType) {
        String writeUrl = format("/cases/%s/case-documents", caseId);
        final String payloadWithReplacedDocumentType;
        if (documentType != null) {
            payloadWithReplacedDocumentType = format(payload, id, documentType);
        } else {
            payloadWithReplacedDocumentType = format(payload, id, "SJPN");
        }
        JSONObject jsonObject = new JSONObject(payloadWithReplacedDocumentType);
        jsonObject.put(MATERIAL_ID_PROPERTY, materialId);

        request = jsonObject.toString();
        makePostCall(userId, writeUrl, WRITE_MEDIA_TYPE, request, Response.Status.ACCEPTED);
    }

    public void addCaseDocument(String payload) {
        addCaseDocument(USER_ID, payload, null);
    }

    public void addCaseDocument() {
        addCaseDocument(getPayload(TEMPLATE_ADD_CASE_DOCUMENT_PAYLOAD));
    }

    public void addCaseDocument(final UUID userId, final UUID documentId, final UUID materialId, final String documentType) {
        this.id = documentId.toString();
        this.materialId = materialId.toString();
        addCaseDocument(userId, getPayload(TEMPLATE_ADD_CASE_DOCUMENT_PAYLOAD), documentType);
    }

    public void addCaseDocumentWithDocumentType(final UUID userId, final String documentType) {
        id = randomUUID().toString();
        addCaseDocument(userId, getPayload(TEMPLATE_ADD_CASE_DOCUMENT_PAYLOAD), documentType);
    }

    public void uploadPleaCaseDocument() {
        uploadDocument(DOCUMENT_TYPE_PLEA);
    }

    public void uploadDocument(String documentType) {
        //It doesn't matter the files are plea, jut to make it simpler
        uploadCaseDocument(USER_ID, documentType, FILE_PATH_PLEA + '/' + FILE_NAME_PLEA);
    }

    public void uploadCaseDocument(UUID userId, String documentType, String fileName) {
        String writeUrl = format("/cases/%s/upload-case-document/%s", caseId, documentType);
        request = fileName;
        makeMultipartFormPostCall(userId, writeUrl, "caseDocument", request);
    }

    /**
     * Uploads via the JSON branch of {@code sjp.upload-case-document}, supplying an existing
     * document reference rather than a binary part.
     *
     * <p>This is the branch other contexts call - staging-dvla among them - and it behaves
     * materially differently from the multipart branch: {@code SjpServiceFileInterceptor} is a
     * no-op here, so the reference the caller supplies is the one that reaches the handler and is
     * forwarded on to Material. The multipart branch stores the binary itself and substitutes its
     * own reference, so it never exercises a caller-supplied one.
     *
     * @param documentReference the reference the caller is handing to SJP
     * @return the same reference, for chaining into the Material stub
     */
    public UUID uploadCaseDocumentByReference(final UUID userId, final String documentType, final UUID documentReference) {
        final String writeUrl = format("/cases/%s/upload-case-document/%s", caseId, documentType);
        final String payload = createObjectBuilder()
                .add("caseDocument", documentReference.toString())
                .build()
                .toString();

        makePostCall(userId, writeUrl, UPLOAD_CASE_DOCUMENT_JSON_MEDIA_TYPE, payload, Response.Status.ACCEPTED);

        return documentReference;
    }

    /**
     * The blob-addressed sibling of {@link #uploadCaseDocumentByReference}: the caller supplies a
     * container uri rather than a file service id. SJP never reads the blob - it forwards the uri
     * to Material, which performs the only read.
     *
     * @param documentUri the blob uri the caller is handing to SJP
     * @return the same uri, for chaining into the Material stub
     */
    public String uploadCaseDocumentByUri(final UUID userId, final String documentType, final String documentUri) {
        final String writeUrl = format("/cases/%s/upload-case-document/%s", caseId, documentType);
        final String payload = createObjectBuilder()
                .add("caseDocumentUri", documentUri)
                .build()
                .toString();

        makePostCall(userId, writeUrl, UPLOAD_CASE_DOCUMENT_JSON_MEDIA_TYPE, payload, Response.Status.ACCEPTED);

        return documentUri;
    }

    public void verifyInPublicTopic() {
        final String caseDocumentAddedEvent = publicConsumer.retrieveMessage().orElse(null);

        assertThat(caseDocumentAddedEvent, notNullValue());

        with(caseDocumentAddedEvent)
                .assertThat("$.caseId", is(caseId.toString()))
                .assertThat("$.id", notNullValue())
                .assertThat("$.materialId", is(materialId));
    }

    /**
     * Asserts the public completion event for an upload-driven document, where the material id is
     * minted by Material rather than seeded by this helper.
     *
     * <p>The no-arg {@link #verifyInPublicTopic()} asserts against the helper's own pre-seeded
     * {@code materialId}, which only holds for the add-case-document path.
     *
     * <p>This is the event a calling context correlates on: {@code id} is the reference it
     * supplied and {@code caseId} tells it which case confirmed.
     */
    public void verifyInPublicTopic(final UUID expectedDocumentId, final UUID expectedMaterialId) {
        final String caseDocumentAddedEvent = publicConsumer.retrieveMessage().orElse(null);

        assertThat(caseDocumentAddedEvent, notNullValue());

        with(caseDocumentAddedEvent)
                .assertThat("$.caseId", is(caseId.toString()))
                .assertThat("$.id", is(expectedDocumentId.toString()))
                .assertThat("$.materialId", is(expectedMaterialId.toString()));
    }

    /**
     * The blob-addressed sibling of {@link #verifyInPublicTopic(UUID, UUID)}. The join key here is
     * {@code documentUri} - the uri the calling context supplied - because there is no file service
     * id to correlate on. {@code id} is derived from that uri, so it is stable but not something
     * the caller knew in advance.
     */
    /**
     * The shape staging-dvla sends now: it derives the case document's uuid from the uri itself and
     * supplies both, so SJP files the document under the caller's id rather than one of its own.
     *
     * @return the uri, for chaining into the Material stub - material is still sent only the uri
     */
    public String uploadCaseDocumentByReferenceAndUri(final UUID userId, final String documentType,
                                                      final UUID documentReference, final String documentUri) {
        final String writeUrl = format("/cases/%s/upload-case-document/%s", caseId, documentType);
        final String payload = createObjectBuilder()
                .add("caseDocument", documentReference.toString())
                .add("caseDocumentUri", documentUri)
                .build()
                .toString();

        makePostCall(userId, writeUrl, UPLOAD_CASE_DOCUMENT_JSON_MEDIA_TYPE, payload, Response.Status.ACCEPTED);

        return documentUri;
    }

    /**
     * Asserts the public completion event for a document whose id was supplied by the caller rather
     * than derived by SJP - both references ride the event.
     */
    public void verifyInPublicTopicWithSuppliedId(final UUID expectedDocumentId, final String expectedDocumentUri,
                                                  final UUID expectedMaterialId) {
        final String caseDocumentAddedEvent = publicConsumer.retrieveMessage().orElse(null);

        assertThat(caseDocumentAddedEvent, notNullValue());

        with(caseDocumentAddedEvent)
                .assertThat("$.caseId", is(caseId.toString()))
                .assertThat("$.documentUri", is(expectedDocumentUri))
                .assertThat("$.id", is(expectedDocumentId.toString()))
                .assertThat("$.materialId", is(expectedMaterialId.toString()));
    }

    public void verifyInPublicTopicForBlobAddressedDocument(final String expectedDocumentUri, final UUID expectedMaterialId) {
        final String caseDocumentAddedEvent = publicConsumer.retrieveMessage().orElse(null);

        assertThat(caseDocumentAddedEvent, notNullValue());

        with(caseDocumentAddedEvent)
                .assertThat("$.caseId", is(caseId.toString()))
                .assertThat("$.documentUri", is(expectedDocumentUri))
                .assertThat("$.id", is(nameUUIDFromBytes(expectedDocumentUri.getBytes(UTF_8)).toString()))
                .assertThat("$.materialId", is(expectedMaterialId.toString()));
    }

    /**
     * The blob-addressed sibling of {@link #verifyCaseDocumentUploadedEventRaised()}: the public
     * upload event carries {@code documentUri} instead of {@code documentId}.
     */
    public String verifyCaseDocumentUploadedEventRaisedForUri() {
        final String caseDocumentUploadedEvent = publicCaseDocumentUploaded.retrieveMessage().orElse(null);

        assertThat(caseDocumentUploadedEvent, notNullValue());

        with(caseDocumentUploadedEvent)
                .assertThat("$.caseId", isAUuid())
                .assertThat("$.documentUri", notNullValue());

        return new JsonPath(caseDocumentUploadedEvent).getString("documentUri");
    }

    public void verifyUploadRejectedInPublicTopic() {
        final String caseDocumentUploadRejected = publicConsumerForRejected.retrieveMessage().orElse(null);

        assertThat(caseDocumentUploadRejected, notNullValue());

        with(caseDocumentUploadRejected)
                .assertThat("$.documentId", isAUuid())
                .assertThat("$.description", notNullValue());
    }

    public void stubGetMetadata() {
        if (uploadTime == null) {
            uploadTime = new UtcClock().now().truncatedTo(ChronoUnit.MILLIS);
        }

        stubMaterialMetadata(fromString(materialId), FILE_NAME_PLEA, "application/pdf", uploadTime);
    }

    public UUID verifyCaseDocumentUploadedEventRaised() {
        final String caseDocumentUploadedEvent = publicCaseDocumentUploaded.retrieveMessage().orElse(null);

        assertThat(caseDocumentUploadedEvent, notNullValue());

        with(caseDocumentUploadedEvent)
                .assertThat("$.documentId", isAUuid());
        with(caseDocumentUploadedEvent)
                .assertThat("$.caseId", isAUuid());

        return UUID.fromString(new JsonPath(caseDocumentUploadedEvent).getString("documentId"));
    }

    public void assertDocumentAdded() {
        assertDocumentAdded(USER_ID);
    }

    public void assertDocumentAdded(final UUID userId) {
        final JsonPath jsonRequest = new JsonPath(request);
        assertDocumentAdded(userId, caseId, UUID.fromString(jsonRequest.getString(MATERIAL_ID_PROPERTY)), UUID.fromString(id), jsonRequest.getString(DOCUMENT_TYPE_PROPERTY));
    }

    public static void assertDocumentAdded(final UUID userId, final UUID caseId, final UUID materialId, final UUID documentId, final String documentType) {
        pollWithDefaults(getCaseDocumentsByCaseId(caseId, userId))
                .until(payload().isJson(
                        withJsonPath("$.caseDocuments[*]", hasItem(isJson(
                                allOf(
                                        withJsonPath("id", equalTo(documentId.toString())),
                                        withJsonPath("materialId", equalTo(materialId.toString())),
                                        withJsonPath("documentType", equalTo(documentType))
                                ))))));
    }

    public void assertDocumentMetadataAvailable() {
        final JsonPath jsonRequest = new JsonPath(request);

        pollWithDefaults(getCaseByIdWithDocumentMetadata(caseId, USER_ID))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.caseDocuments[0].materialId", Matchers.equalTo(jsonRequest.getString(MATERIAL_ID_PROPERTY))),
                                withJsonPath("$.caseDocuments[0].documentType", Matchers.equalTo(jsonRequest.getString(DOCUMENT_TYPE_PROPERTY))),
                                withJsonPath("$.caseDocuments[0].metadata.fileName", Matchers.equalTo(FILE_NAME_PLEA)),
                                withJsonPath("$.caseDocuments[0].metadata.mimeType", Matchers.equalTo("application/pdf")),
                                withJsonPath("$.caseDocuments[0].metadata.addedAt", Matchers.equalTo(ZonedDateTimes.toString(uploadTime)))
                        ))
                );

    }


    public static void pollForCaseDocument(final UUID caseId, final UUID userId, final Matcher[] matchers) {
        pollWithDefaults(getCaseDocumentsByCaseId(caseId, userId))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(matchers))
                );
    }

    public JsonObject findAllDocumentsForTheUser(final UUID userId) {
        final ResponseData documents = pollWithDefaults(getCaseDocumentsByCaseId(caseId, userId))
                .until(
                        status().is(OK),
                        payload().isJson(withJsonPath("$.caseDocuments[" + 0 + "].documentNumber", Matchers.notNullValue())
                        ));
        return JsonHelper.getJsonObject(documents.getPayload()).getJsonArray("caseDocuments").getJsonObject(0);
    }

    public void assertDocumentNotExist(final UUID userId, final UUID caseId) {
        pollWithDefaults(getCaseDocumentsByCaseId(caseId, userId))
                .until(
                        status().is(Response.Status.OK),
                        payload().isJson(allOf(
                                withJsonPath("$.caseDocuments", emptyIterable())
                        ))
                );
    }

    public void verifyDocumentNotVisibleForProsecutorWhenQueryingForCaseDocuments(final UUID tflUserId) {
        final Filter caseDocumentFilter = Filter.filter(where("id").is(id));
        pollWithDefaults(getCaseDocumentsByCaseId(caseId, tflUserId))
                .until(payload()
                        .isJson(
                                withJsonPath(compile("$.caseDocuments[?]", caseDocumentFilter), hasSize(0))
                        ));
    }

    public void verifyDocumentNotVisibleForProsecutorWhenQueryingForACase(final UUID tflUserId) {
        final Filter caseDocumentFilter = Filter.filter(where("id").is(id));
        pollWithDefaults(getCaseById(caseId, tflUserId))
                .until(payload()
                        .isJson(
                                withJsonPath(compile("$.caseDocuments[?]", caseDocumentFilter), hasSize(0))
                        ));
    }

    public void addDocumentAndVerifyAdded() {
        addCaseDocument();
        assertDocumentAdded();
    }

    public String getDocumentId() {
        return id;
    }

    @Override
    public void close() {
        publicConsumer.close();
        publicConsumerForRejected.close();
        publicCaseDocumentAlreadyExistsConsumer.close();
        publicCaseDocumentUploaded.close();
    }

    public static Response getCaseDocumentMetadata(final UUID caseId, final UUID documentId, final UUID userId) {
        final String contentType = "application/vnd.sjp.query.case-document-metadata+json";
        final String url = String.format("/cases/%s/documents/%s/metadata", caseId, documentId);
        return HttpClientUtil.makeGetCall(url, contentType, userId);
    }

    public static Response getCaseDocumentContent(final UUID caseId, final UUID documentId, final UUID userId) {
        final String contentType = "application/vnd.sjp.query.case-document-content+json";
        final String url = String.format("/cases/%s/documents/%s/content", caseId, documentId);
        return HttpClientUtil.makeGetCall(url, contentType, userId);
    }

}
