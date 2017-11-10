package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.EventSelector.EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA;
import static uk.gov.moj.sjp.it.EventSelector.EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UNDONE;
import static uk.gov.moj.sjp.it.EventSelector.EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UPDATED;
import static uk.gov.moj.sjp.it.EventSelector.PUBLIC_EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA;
import static uk.gov.moj.sjp.it.EventSelector.PUBLIC_EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UNDONE;
import static uk.gov.moj.sjp.it.EventSelector.PUBLIC_EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UPDATED;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseById;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;
import static uk.gov.moj.sjp.it.util.QueueUtil.retrieveMessage;

import uk.gov.moj.cpp.sjp.domain.CaseReopenDetails;
import uk.gov.moj.sjp.it.util.QueueUtil;

import java.time.LocalDate;

import javax.jms.MessageConsumer;

import com.jayway.restassured.path.json.JsonPath;

/**
 * Helper class for Case reopened in libra IT.
 */
public abstract class CaseReopenedInLibraHelper extends AbstractTestHelper {

    public static class MarkCaseReopenedInLibraHelper extends CaseReopenedInLibraHelper {
        public MarkCaseReopenedInLibraHelper(final CaseSjpHelper caseSjpHelper) {
            super(caseSjpHelper, EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA, PUBLIC_EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA);
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
        public UpdateCaseReopenedInLibraHelper(final CaseSjpHelper caseSjpHelper) {
            super(caseSjpHelper, EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UPDATED, PUBLIC_EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UPDATED);
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
        public UndoCaseReopenedInLibraHelper(final CaseSjpHelper caseSjpHelper) {
            super(caseSjpHelper, EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UNDONE, PUBLIC_EVENT_SELECTOR_CASE_REOPENED_IN_LIBRA_UNDONE);
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

    protected final CaseSjpHelper caseSjpHelper;

    protected final CaseReopenDetails markCaseReopenDetails;

    protected final CaseReopenDetails updateCaseReopenDetails;

    protected CaseReopenedInLibraHelper(final CaseSjpHelper caseSjpHelper, final String privateSelector, final String publicSelector) {
        this.caseSjpHelper = caseSjpHelper;

        this.markCaseReopenDetails = new CaseReopenDetails(
                caseSjpHelper.getCaseId(),
                LocalDate.parse("2017-01-01"),
                "LIBRA12345",
                "Mandatory reason"
        );

        this.updateCaseReopenDetails = new CaseReopenDetails(
                caseSjpHelper.getCaseId(),
                LocalDate.parse("2010-01-01"),
                "LIBRA98765",
                "Optional reason"
        );

        privateEventsConsumer = QueueUtil.privateEvents.createConsumer(privateSelector);
        publicEventsConsumer = QueueUtil.publicEvents.createConsumer(publicSelector);
    }

    protected void verifyCaseReopenUndoneInLibraInActiveMQ(final MessageConsumer messageConsumer) {
        final JsonPath jsonResponse = retrieveMessage(messageConsumer);
        assertThat(jsonResponse, notNullValue());

        assertThat(jsonResponse.get("caseId"), equalTo(caseSjpHelper.getCaseId()));
    }

    protected void verifyCaseReopenedInLibraInActiveMQ(final MessageConsumer messageConsumer, final CaseReopenDetails caseReopenDetails) {
        final JsonPath jsonResponse = retrieveMessage(messageConsumer);
        assertThat(jsonResponse, notNullValue());

        assertThat(jsonResponse.get("caseId"), equalTo(caseReopenDetails.getCaseId()));
        assertThat(jsonResponse.get("reopenedDate"), equalTo(caseReopenDetails.getReopenedDate().toString()));
        assertThat(jsonResponse.get("libraCaseNumber"), equalTo(caseReopenDetails.getLibraCaseNumber()));
        assertThat(jsonResponse.get("reason"), equalTo(caseReopenDetails.getReason()));
    }

    protected void assertCaseNotReopenedInLibra() {
        poll(getCaseById(caseSjpHelper.getCaseId()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.id", is(caseSjpHelper.getCaseId())),
                                withoutJsonPath("$.reopenedDate"),
                                withoutJsonPath("$.libraCaseNumber"),
                                withoutJsonPath("$.reopenedInLibraReason")))
                );
    }

    protected void assertCaseReopenedInLibra(final CaseReopenDetails caseReopenDetails) {
        poll(getCaseById(caseSjpHelper.getCaseId()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.id", is(caseReopenDetails.getCaseId())),
                                withJsonPath("$.reopenedDate", is(caseReopenDetails.getReopenedDate().toString())),
                                withJsonPath("$.libraCaseNumber", is(caseReopenDetails.getLibraCaseNumber())),
                                withJsonPath("$.reopenedInLibraReason", is(caseReopenDetails.getReason()))))
                );
    }

    protected void markCaseReopenedInLibra() {
        final String writeUrl = "/cases/CASEID/mark-reopened-in-libra".replace("CASEID", caseSjpHelper.getCaseId());
        final String request = getPayload(TEMPLATE_CASE_REOPENED_IN_LIBRA_PAYLOAD);
        makePostCall(getWriteUrl(writeUrl), MARK_WRITE_MEDIA_TYPE, request);
    }

    protected void updateCaseReopenedInLibra() {
        final String writeUrl = "/cases/CASEID/update-reopened-in-libra".replace("CASEID", caseSjpHelper.getCaseId());
        String request = getPayload(TEMPLATE_CASE_REOPENED_IN_LIBRA_PAYLOAD)
                .replace(markCaseReopenDetails.getLibraCaseNumber(), updateCaseReopenDetails.getLibraCaseNumber())
                .replace(markCaseReopenDetails.getReopenedDate().toString(), updateCaseReopenDetails.getReopenedDate().toString())
                .replace(markCaseReopenDetails.getReason(), updateCaseReopenDetails.getReason());

        makePostCall(getWriteUrl(writeUrl), UPDATE_WRITE_MEDIA_TYPE, request);
    }

    protected void undoCaseReopenedInLibra() {
        final String writeUrl = "/cases/CASEID/undo-reopened-in-libra".replace("CASEID", caseSjpHelper.getCaseId());

        makePostCall(getWriteUrl(writeUrl), UNDO_WRITE_MEDIA_TYPE, "{}");
    }

    public abstract void verifyEventInActiveMQ();

    public abstract void verifyEventInPublicTopic();

    public abstract void assertCaseReopenedDetailsSet();

    public abstract void call();
}
