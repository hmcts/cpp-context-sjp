package uk.gov.moj.sjp.it.producer;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.moj.sjp.it.helper.SessionHelper.endSession;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startMagistrateSessionAndWaitForEvent;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;
import static uk.gov.moj.sjp.it.stub.ProsecutionCaseFileServiceStub.stubCaseDetails;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubCourtByCourtHouseOUCodeQuery;

import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.event.processor.SessionProcessor;
import uk.gov.moj.sjp.it.test.BaseIntegrationTest;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

public class CompleteCaseProducer extends BaseIntegrationTest {

    private final UUID caseId;
    private static final String LONDON_LJA_NATIONAL_COURT_CODE = "2572";
    private static final String LONDON_COURT_HOUSE_OU_CODE = "B01OK";

    public CompleteCaseProducer(final UUID caseId, final UUID defendantId, final UUID offenceId) {
        stubCaseDetails(caseId, defendantId, offenceId, "stub-data/prosecutioncasefile.query.case-details.json");
        this.caseId = caseId;
    }

    public void completeCase() {

        final JsonObject payload = getPayload(startAndEndSession());

        try (final MessageProducerClient producerClient = new MessageProducerClient()) {
            producerClient.startProducer("public.event");
            producerClient.sendMessage("public.resulting.referenced-decisions-saved", payload);
        }
    }

    private UUID startAndEndSession() {
        final UUID sessionId = randomUUID();
        stubCourtByCourtHouseOUCodeQuery(LONDON_COURT_HOUSE_OU_CODE, LONDON_LJA_NATIONAL_COURT_CODE);
        startMagistrateSessionAndWaitForEvent(sessionId, USER_ID, LONDON_COURT_HOUSE_OU_CODE, "Alan Smith", SessionProcessor.PUBLIC_SJP_SESSION_STARTED);
        endSession(sessionId, USER_ID);
        return sessionId;
    }

    public void assertCaseCompleted() {
        pollUntilCaseByIdIsOk(caseId, allOf(
                withJsonPath("$.completed", is(true)),
                withJsonPath("$.status", is(CaseStatus.COMPLETED.name())
                )));
    }

    public void completeCaseResults() {

        final JsonObject payload = getPayload(startAndEndSession());

        try (final MessageProducerClient producerClient = new MessageProducerClient()) {
            producerClient.startProducer("public.event");
            producerClient.sendMessage("public.resulting.referenced-decisions-saved", payload);
        }
    }

    private JsonObject getPayload(final UUID sessionId) {
        return Json.createObjectBuilder()
                    .add("caseId", caseId.toString())
                    .add("resultedOn", now().toString())
                    .add("sjpSessionId", sessionId.toString())
                    .build();
    }

    public void assertCaseResults() {
        pollUntilCaseByIdIsOk(caseId, allOf(
                withJsonPath("$.completed", is(true)),
                withJsonPath("$.status", is(CaseStatus.COMPLETED.name())
                )));
    }


}
