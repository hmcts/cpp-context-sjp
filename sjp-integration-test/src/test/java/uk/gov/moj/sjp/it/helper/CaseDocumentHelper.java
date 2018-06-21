package uk.gov.moj.sjp.it.helper;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.TEN_SECONDS;
import static com.jayway.jsonassert.JsonAssert.with;
import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.JsonPath.compile;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.matchers.UuidStringMatcher.isAUuid;
import static uk.gov.moj.sjp.it.Constants.EVENT_SELECTOR_CASE_DOCUMENT_ADDED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_ADDED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_ALREADY_EXISTS;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_UPLOADED;
import static uk.gov.moj.sjp.it.test.BaseIntegrationTest.USER_ID;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseById;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseDocumentsByCaseId;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makeMultipartFormPostCall;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;
import static uk.gov.moj.sjp.it.util.QueueUtil.retrieveMessage;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaults;

import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.moj.sjp.it.Constants;
import uk.gov.moj.sjp.it.stub.MaterialStub;
import uk.gov.moj.sjp.it.util.QueueUtil;

import java.util.Map;
import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.ws.rs.core.Response;

import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.client.UrlMatchingStrategy;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.jayway.jsonpath.Filter;
import com.jayway.restassured.path.json.JsonPath;
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

    private static final String ID_PROPERTY = "id";
    private static final String CASE_ID_PROPERTY = "caseId";
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
    private MessageConsumer privateEventsConsumer;

    public CaseDocumentHelper(UUID caseId) {
        this.id = UUID.randomUUID().toString();
        this.materialId = UUID.randomUUID().toString();
        this.caseId = caseId;
        privateEventsConsumer = QueueUtil.privateEvents.createConsumer(EVENT_SELECTOR_CASE_DOCUMENT_ADDED);
        publicConsumer.startConsumer(PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_ADDED, Constants.PUBLIC_ACTIVE_MQ_TOPIC);

        publicCaseDocumentAlreadyExistsConsumer.startConsumer(PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_ALREADY_EXISTS, Constants.PUBLIC_ACTIVE_MQ_TOPIC);
        publicCaseDocumentUploaded.startConsumer(PUBLIC_EVENT_SELECTOR_CASE_DOCUMENT_UPLOADED, Constants.PUBLIC_ACTIVE_MQ_TOPIC);
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
        LOGGER.info("Adding case document with payload: {}", request);
        makePostCall(userId, writeUrl, WRITE_MEDIA_TYPE, request, Response.Status.ACCEPTED);
    }

    public void addCaseDocument(String payload) {
        addCaseDocument(USER_ID, payload, null);
    }

    public void addCaseDocument() {
        addCaseDocument(getPayload(TEMPLATE_ADD_CASE_DOCUMENT_PAYLOAD));
    }

    public void addCaseDocumentWithDocumentType(UUID userId, String documentType) {
        id = UUID.randomUUID().toString();
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
        LOGGER.info("Uploading case document with payload from file: {}", request);
        makeMultipartFormPostCall(userId, writeUrl, "caseDocument", request);
    }

    public void verifyInActiveMQ() {
        JsonPath jsonResponse = retrieveMessage(privateEventsConsumer);

        LOGGER.info("Response: {}", jsonResponse.prettify());
        JsonPath jsonRequest = new JsonPath(request);

        Map caseDocument = jsonResponse.getJsonObject("caseDocument");
        assertThat(caseDocument.get(ID_PROPERTY), is(id));
        assertThat(jsonResponse.get(CASE_ID_PROPERTY), is(caseId.toString()));
        assertJsonPayload(jsonRequest, caseDocument);
    }

    public void verifyInPublicTopic() {
        final String caseDocumentAddedEvent = publicConsumer.retrieveMessage().orElse(null);

        assertThat(caseDocumentAddedEvent, notNullValue());

        with(caseDocumentAddedEvent)
                .assertThat("$.caseId", is(caseId.toString()))
                .assertThat("$.id", notNullValue())
                .assertThat("$.materialId", is(materialId));
    }


    public void assertCaseMaterialAdded(final String documentReference) {
        UrlMatchingStrategy url = new UrlMatchingStrategy();
        url.setUrlPath(MaterialStub.QUERY_URL);

        System.out.println("documentReference: " + documentReference);
        await().atMost(TEN_SECONDS).until(() -> WireMock.findAll(new RequestPatternBuilder(RequestMethod.POST, url)
                        .withHeader("Content-Type", equalTo(MaterialStub.QUERY_MEDIA_TYPE))
                        .withRequestBody(containing("\"fileServiceId\":\"" + documentReference + "\""))
                ).size() > 0
        );
    }

    public String verifyCaseDocumentUploadedEventRaised() {
        final String caseDocumentUploadedEvent = publicCaseDocumentUploaded.retrieveMessage().orElse(null);

        assertThat(caseDocumentUploadedEvent, notNullValue());

        with(caseDocumentUploadedEvent)
                .assertThat("$.documentId", isAUuid());

        return new JsonPath(caseDocumentUploadedEvent).getString("documentId");
    }

    public void assertDocumentAdded() {
        assertDocumentAdded(USER_ID);
    }

    public void assertDocumentAdded(final UUID userId) {
        final JsonPath jsonRequest = new JsonPath(request);

        final Filter caseDocumentFilter = Filter.filter(where("id").is(id));
        pollWithDefaults(getCaseDocumentsByCaseId(caseId, userId))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(compile("$.caseDocuments[?]", caseDocumentFilter), hasSize(1)),
                                withJsonPath(compile("$.caseDocuments[?].materialId", caseDocumentFilter), hasItem(jsonRequest.getString(MATERIAL_ID_PROPERTY))),
                                withJsonPath(compile("$.caseDocuments[?].documentType", caseDocumentFilter), hasItem(jsonRequest.getString(DOCUMENT_TYPE_PROPERTY)))
                        ))
                );
    }

    public void assertDocumentNumber(final UUID userId, final int index, final String documentType, final int documentNumber) {
        pollWithDefaults(getCaseDocumentsByCaseId(caseId, userId))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.caseDocuments[" + index + "].documentNumber", Matchers.is(documentNumber)),
                                withJsonPath("$.caseDocuments[" + index + "].documentType", Matchers.is(documentType)),
                                withJsonPath("$.caseDocuments[" + index + "].materialId", Matchers.notNullValue())
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

    private void assertJsonPayload(JsonPath jsonRequest, Map caseDocument) {
        assertThat(caseDocument.get(MATERIAL_ID_PROPERTY), is(jsonRequest.getString(MATERIAL_ID_PROPERTY)));
        assertThat(caseDocument.get(DOCUMENT_TYPE_PROPERTY), is(jsonRequest.getString(DOCUMENT_TYPE_PROPERTY)));
    }

    public String getMaterialId() {
        return materialId;
    }

    @Override
    public void close() {
        publicConsumer.close();
        publicCaseDocumentAlreadyExistsConsumer.close();
        publicCaseDocumentUploaded.close();
    }

}
