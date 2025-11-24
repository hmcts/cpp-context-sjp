package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonassert.JsonAssert.with;
import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.JsonPath.compile;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
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

    public void verifyInPublicTopic() {
        final String caseDocumentAddedEvent = publicConsumer.retrieveMessage().orElse(null);

        assertThat(caseDocumentAddedEvent, notNullValue());

        with(caseDocumentAddedEvent)
                .assertThat("$.caseId", is(caseId.toString()))
                .assertThat("$.id", notNullValue())
                .assertThat("$.materialId", is(materialId));
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
