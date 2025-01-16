package uk.gov.moj.sjp.it.helper;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SELECTOR_DELETE_CASE_DOCUMENT_REQUEST_ACCEPTED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SELECTOR_DELETE_CASE_DOCUMENT_REQUEST_REJECTED;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;
import static uk.gov.moj.sjp.it.util.TopicUtil.retrieveMessage;

import uk.gov.moj.sjp.it.util.TopicUtil;

import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.ws.rs.core.Response;

import io.restassured.path.json.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteCaseDocumentHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteCaseDocumentHelper.class);
    private static final String CASE_ID_PROPERTY = "caseId";
    private static final String DOCUMENT_ID_PROPERTY = "documentId";
    private static final String REASON_PROPERTY = "reason";

    private MessageConsumer publicEventsConsumer;

    public DeleteCaseDocumentHelper() {
        publicEventsConsumer = TopicUtil.publicEvents.createConsumerForMultipleSelectors(PUBLIC_EVENT_SELECTOR_DELETE_CASE_DOCUMENT_REQUEST_ACCEPTED, PUBLIC_EVENT_SELECTOR_DELETE_CASE_DOCUMENT_REQUEST_REJECTED);
    }

    public void deleteCaseDocument(final UUID caseId, final UUID documentId, final UUID userId, final Response.Status expectedStatus) {
        final String url = String.format("/cases/%s/documents/%s/delete", caseId, documentId);
        makePostCall(userId, url, "application/vnd.sjp.delete-case-document+json", createObjectBuilder().build().toString(), expectedStatus);
    }

    public void pollUntilPublicDeleteCaseDocumentRequestAcceptedEvent(final UUID caseId, final UUID documentId) {
        final JsonPath message = retrieveMessage(publicEventsConsumer);

        LOGGER.info("public deleteCaseDocumentRequestAccepted response: {}", message.prettify());
        assertThat(message.get(CASE_ID_PROPERTY), is(caseId.toString()));
        assertThat(message.get(DOCUMENT_ID_PROPERTY), is(documentId.toString()));
    }

    public void pollUntilPublicDeleteCaseDocumentRequestRejectedEvent(final UUID caseId, final UUID documentId) {
        final JsonPath message = retrieveMessage(publicEventsConsumer);

        LOGGER.info("public deleteCaseDocumentRequestRejected response: {}", message.prettify());
        assertThat(message.get(CASE_ID_PROPERTY), is(caseId.toString()));
        assertThat(message.get(DOCUMENT_ID_PROPERTY), is(documentId.toString()));
        assertThat(message.get(REASON_PROPERTY), is("CASE_IN_SESSION"));
    }


}
