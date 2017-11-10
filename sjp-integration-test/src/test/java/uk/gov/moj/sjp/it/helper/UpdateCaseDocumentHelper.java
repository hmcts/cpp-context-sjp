package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonassert.JsonAssert.with;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.EventSelector.EVENT_SELECTOR_CASE_DOCUMENT_NAME_UPDATED;
import static uk.gov.moj.sjp.it.EventSelector.PUBLIC_SELECTOR_STRUCTURE_CASE_DOCUMENT_NAME_UPDATED;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseDocumentsByCaseId;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;
import static uk.gov.moj.sjp.it.util.SchemaValidatorUtil.validateAgainstSchema;

import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.core.Response;

import com.jayway.restassured.path.json.JsonPath;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateCaseDocumentHelper extends AbstractTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateCaseDocumentHelper.class);

    private static final String WRITE_MEDIA_TYPE = "application/vnd.sjp.update-case-document-name+json";
    private static final String READ_MEDIA_TYPE_BY_ID = "application/vnd.sjp.query.case-documents+json";
    private static final String TEMPLATE_ADD_CASE_DOCUMENT_PAYLOAD = "raml/json/sjp.update-case-document-name.json";

    private static final String CASE_ID_PROPERTY = "caseId";
    private static final String DOCUMENT_ID_PROPERTY = "documentId";

    private UUID caseId;
    private UUID documentId;
    private String documentName;
    private String request;
    private MessageConsumerClient caseDocumentNameUpdatedMC = new MessageConsumerClient();

    public UpdateCaseDocumentHelper(UUID caseId, UUID documentId) {
        this.caseId = caseId;
        this.documentId = documentId;
        caseDocumentNameUpdatedMC.startConsumer(EVENT_SELECTOR_CASE_DOCUMENT_NAME_UPDATED, STRUCTURE_EVENT_TOPIC);
        publicConsumer.startConsumer(PUBLIC_SELECTOR_STRUCTURE_CASE_DOCUMENT_NAME_UPDATED, PUBLIC_ACTIVE_MQ_TOPIC);
    }

    public void updateCaseDocumentName(String documentName) {
        this.documentName = documentName;

        String writeUrl = String.format("/cases/%s/case-document/%s/", caseId, documentId);
        String payload = getPayload(TEMPLATE_ADD_CASE_DOCUMENT_PAYLOAD);
        JSONObject jsonObject = new JSONObject(payload);
        jsonObject.put("documentName", documentName);

        request = jsonObject.toString();
        makePostCall(getWriteUrl(writeUrl), WRITE_MEDIA_TYPE, request);
    }

    public void verifyInActiveMQ() {
        Optional<String> messagePayload = caseDocumentNameUpdatedMC.retrieveMessage();
        JsonPath jsonResponse = new JsonPath(messagePayload.get());
        LOGGER.info("Response: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get(CASE_ID_PROPERTY), is(caseId.toString()));
        assertThat(jsonResponse.get(DOCUMENT_ID_PROPERTY), is(documentId.toString()));
        assertThat(jsonResponse.get("documentName"), is(documentName));
        assertThat(jsonResponse.get("policeMaterialId"), is(notNullValue()));
    }

    public void assertDocumentNameUpdated() {
        JsonPath jsonResponse = getCaseDocumentResponseUsingId(documentName);
        verifyCaseDocumentCreated(jsonResponse);
    }

    private JsonPath getCaseDocumentResponseUsingId(String searchStr) {
        final ResponseData responseData = poll(getCaseDocumentsByCaseId(caseId.toString()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.caseDocuments[0].documentName", is(searchStr)))
                        )
                );

        return new JsonPath(responseData.getPayload());
    }

    public void verifyNotInActiveMQ() {
        Optional<String> messagePayload = caseDocumentNameUpdatedMC.retrieveMessage();
        assertThat(messagePayload.isPresent(), is(false));
    }

    private void verifyCaseDocumentCreated(JsonPath jsonResponse) {
        validateAgainstSchema("sjp.query.case-documents.json", JSONObject.class, jsonResponse);
        List<Map<String, Object>> caseDocuments = jsonResponse.getList("caseDocuments");
        Optional<Map<String, Object>> result = caseDocuments.stream()
                .filter(caseDocument -> caseDocument.get("id").equals(documentId.toString())
                        && caseDocument.get("documentName").equals(documentName))
                .findFirst();
        assertTrue(result.isPresent());
    }

    @Override
    public void assertQueryCallResponseStatusIs(Response.Status status) {
        poll(getCaseDocumentsByCaseId(caseId.toString()))
                .until(
                        status().is(status)
                );
    }

    public void verifyInPublicActiveMQ() {
        final String caseDocumentNameUpdatedEvent = publicConsumer.retrieveMessage().orElse(null);

        assertThat(caseDocumentNameUpdatedEvent, notNullValue());

        with(caseDocumentNameUpdatedEvent)
            .assertThat("$.documentName", is(documentName))
            .assertThat("$.policeMaterialId", is(notNullValue()));
    }

    @Override
    public void close() {
        super.close();
        caseDocumentNameUpdatedMC.close();
    }
}
