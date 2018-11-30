package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static uk.gov.moj.sjp.it.Constants.EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA;
import static uk.gov.moj.sjp.it.Constants.EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UNDONE;
import static uk.gov.moj.sjp.it.Constants.EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UPDATED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UNDONE;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UPDATED;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;
import static uk.gov.moj.sjp.it.util.QueueUtil.retrieveMessage;

import uk.gov.moj.cpp.sjp.domain.CaseReopenDetails;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;
import uk.gov.moj.sjp.it.util.QueueUtil;

import java.time.LocalDate;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;

import com.jayway.restassured.path.json.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for Case reopened in libra IT.
 */
public abstract class CaseReopenedInLibraHelper implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseReopenedInLibraHelper.class);

    final MessageConsumer privateEventsConsumer;
    final MessageConsumer publicEventsConsumer;

    public static class MarkCaseReopenedInLibraHelper extends CaseReopenedInLibraHelper {
        public MarkCaseReopenedInLibraHelper(final UUID caseId) {
            super(caseId, EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA, PUBLIC_EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA);
        }

        public void verifyEventInActiveMQ() {
            verifyCaseReopenedInLibraInActiveMQ(privateEventsConsumer, markCaseReopenDetails);
        }

        public void verifyEventInPublicTopic() {
            verifyCaseReopenedInLibraInActiveMQ(publicEventsConsumer, markCaseReopenDetails);
        }

        public void assertCaseReopenedDetailsSet() {
            assertCaseReopenedInLibra(markCaseReopenDetails);
        }

        public void call() {
            markCaseReopenedInLibra();
        }
    }

    public static class UpdateCaseReopenedInLibraHelper extends CaseReopenedInLibraHelper {
        public UpdateCaseReopenedInLibraHelper(final UUID caseId) {
            super(caseId, EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UPDATED, PUBLIC_EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UPDATED);
        }

        public void verifyEventInActiveMQ() {
            verifyCaseReopenedInLibraInActiveMQ(privateEventsConsumer, updateCaseReopenDetails);
        }

        public void verifyEventInPublicTopic() {
            verifyCaseReopenedInLibraInActiveMQ(publicEventsConsumer, updateCaseReopenDetails);
        }

        public void assertCaseReopenedDetailsSet() {
            assertCaseReopenedInLibra(updateCaseReopenDetails);
        }

        public void call() {
            updateCaseReopenedInLibra();
        }
    }

    public static class UndoCaseReopenedInLibraHelper extends CaseReopenedInLibraHelper {
        public UndoCaseReopenedInLibraHelper(final UUID caseId) {
            super(caseId, EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UNDONE, PUBLIC_EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UNDONE);
        }

        public void verifyEventInActiveMQ() {
            verifyCaseReopenUndoneInLibraInActiveMQ(privateEventsConsumer);
        }

        public void verifyEventInPublicTopic() {
            verifyCaseReopenUndoneInLibraInActiveMQ(publicEventsConsumer);
        }

        public void assertCaseReopenedDetailsSet() {
            assertCaseNotReopenedInLibra();
        }

        public void call() {
            undoCaseReopenedInLibra();
        }
    }

    private static final String MARK_WRITE_MEDIA_TYPE = "application/vnd.sjp.mark-case-reopened-in-libra+json";

    private static final String UPDATE_WRITE_MEDIA_TYPE = "application/vnd.sjp.update-case-reopened-in-libra+json";

    private static final String UNDO_WRITE_MEDIA_TYPE = "application/vnd.sjp.undo-case-reopened-in-libra+json";

    private static final String TEMPLATE_CASE_REOPENED_IN_LIBRA_PAYLOAD = "raml/json/sjp.case-reopened-in-libra.json";

    protected final UUID caseId;

    final CaseReopenDetails markCaseReopenDetails;

    final CaseReopenDetails updateCaseReopenDetails;

    CaseReopenedInLibraHelper(final UUID caseId, final String privateSelector, final String publicSelector) {
        this.caseId = caseId;

        this.markCaseReopenDetails = new CaseReopenDetails(
                caseId,
                LocalDate.of(2017, 1, 1),
                "LIBRA12345",
                "Mandatory reason"
        );

        this.updateCaseReopenDetails = new CaseReopenDetails(
                caseId,
                LocalDate.of(2010, 1, 1),
                "LIBRA98765",
                "Optional reason"
        );

        privateEventsConsumer = QueueUtil.privateEvents.createConsumer(privateSelector);
        publicEventsConsumer = QueueUtil.publicEvents.createConsumer(publicSelector);
    }

    void verifyCaseReopenUndoneInLibraInActiveMQ(final MessageConsumer messageConsumer) {
        final JsonPath jsonResponse = retrieveMessage(messageConsumer);
        assertThat(jsonResponse, notNullValue());

        assertThat(jsonResponse.get("caseId"), equalTo(caseId.toString()));
    }

    void verifyCaseReopenedInLibraInActiveMQ(final MessageConsumer messageConsumer, final CaseReopenDetails caseReopenDetails) {
        final JsonPath jsonResponse = retrieveMessage(messageConsumer);
        assertThat(jsonResponse, notNullValue());

        assertThat(jsonResponse.get("caseId"), equalTo(caseReopenDetails.getCaseId().toString()));
        assertThat(jsonResponse.get("reopenedDate"), equalTo(caseReopenDetails.getReopenedDate().toString()));
        assertThat(jsonResponse.get("libraCaseNumber"), equalTo(caseReopenDetails.getLibraCaseNumber()));
        assertThat(jsonResponse.get("reason"), equalTo(caseReopenDetails.getReason()));
    }

    void assertCaseNotReopenedInLibra() {
        CasePoller.pollUntilCaseByIdIsOk(caseId,
                allOf(
                        withJsonPath("$.id", is(caseId.toString())),
                        withoutJsonPath("$.reopenedDate"),
                        withoutJsonPath("$.libraCaseNumber"),
                        withoutJsonPath("$.reopenedInLibraReason")
                )
        );
    }

    void assertCaseReopenedInLibra(final CaseReopenDetails caseReopenDetails) {
        CasePoller.pollUntilCaseByIdIsOk(caseId,
                allOf(
                        withJsonPath("$.id", is(caseReopenDetails.getCaseId().toString())),
                        withJsonPath("$.reopenedDate", is(caseReopenDetails.getReopenedDate().toString())),
                        withJsonPath("$.libraCaseNumber", is(caseReopenDetails.getLibraCaseNumber())),
                        withJsonPath("$.reopenedInLibraReason", is(caseReopenDetails.getReason())),
                        withJsonPath("$.status", is(CaseStatus.REOPENED_IN_LIBRA.name()))
                )
        );
    }

    void markCaseReopenedInLibra() {
        final String writeUrl = format("/cases/%s/mark-reopened-in-libra", caseId.toString());
        final String request = getPayload(TEMPLATE_CASE_REOPENED_IN_LIBRA_PAYLOAD);
        makePostCall(writeUrl, MARK_WRITE_MEDIA_TYPE, request);
    }

    void updateCaseReopenedInLibra() {
        final String writeUrl = format("/cases/%s/update-reopened-in-libra", caseId.toString());
        String request = getPayload(TEMPLATE_CASE_REOPENED_IN_LIBRA_PAYLOAD)
                .replace(markCaseReopenDetails.getLibraCaseNumber(), updateCaseReopenDetails.getLibraCaseNumber())
                .replace(markCaseReopenDetails.getReopenedDate().toString(), updateCaseReopenDetails.getReopenedDate().toString())
                .replace(markCaseReopenDetails.getReason(), updateCaseReopenDetails.getReason());

        makePostCall(writeUrl, UPDATE_WRITE_MEDIA_TYPE, request);
    }

    void undoCaseReopenedInLibra() {
        final String writeUrl = format("/cases/%s/undo-reopened-in-libra", caseId.toString());

        makePostCall(writeUrl, UNDO_WRITE_MEDIA_TYPE, "{}");
    }

    public abstract void verifyEventInActiveMQ();

    public abstract void verifyEventInPublicTopic();

    public abstract void assertCaseReopenedDetailsSet();

    public abstract void call();

    @Override
    public void close() {
        try {
            privateEventsConsumer.close();
        } catch (JMSException e) {
            LOGGER.warn("Exception while closing consumer", e);
        }
        try {
            publicEventsConsumer.close();
        } catch (JMSException e) {
            LOGGER.warn("Exception while closing consumer", e);
        }
    }
}
