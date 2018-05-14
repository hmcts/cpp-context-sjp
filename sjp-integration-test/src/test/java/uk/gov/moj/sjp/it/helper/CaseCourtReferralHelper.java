package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCasesReferredToCourt;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;

import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.sjp.it.Constants;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonObject;

public class CaseCourtReferralHelper implements AutoCloseable {

    private static final String COMMAND_URL = "/cases/%s/court-referral";
    private static final String ACTION_COURT_REFERRAL_MEDIA_TYPE = "application/vnd.sjp.action-court-referral+json";
    public static final String CASES_REFERRED_TO_COURT_MEDIA_TYPE = "application/vnd.sjp.query.cases-referred-to-court+json";

    private final LocalDate hearingDate;
    private MessageConsumerClient publicConsumer = new MessageConsumerClient();

    public CaseCourtReferralHelper() {
        publicConsumer.startConsumer("public.sjp.court-referral-actioned", Constants.PUBLIC_ACTIVE_MQ_TOPIC);
        this.hearingDate = LocalDate.now().plusWeeks(1);
    }

    public void createCourtReferral(final String caseId) {
        final JsonObject payload = Json.createObjectBuilder()
                .add("caseId", caseId)
                .add("hearingDate", hearingDate.toString())
                .build();

        try (final MessageProducerClient producerClient = new MessageProducerClient()) {
            producerClient.startProducer("public.event");
            producerClient.sendMessage("public.resulting.case-referred-to-court", payload);
        }
    }

    public void actionCourtReferral(final String caseId) {

        makePostCall(String.format(COMMAND_URL, caseId),
                ACTION_COURT_REFERRAL_MEDIA_TYPE, "{}");
    }

    public void verifyCaseCourtReferral(final String caseId,
                                        final String caseUrn,
                                        final String firstName,
                                        final String lastName,
                                        final String interpreterLanguage) {
        poll(getCasesReferredToCourt())
                .timeout(20, TimeUnit.SECONDS)
                .until(status().is(OK), payload().isJson(withJsonPath("$.cases",
                        hasItem(isJson(allOf(
                                withJsonPath("caseId", is(caseId)),
                                withJsonPath("urn", is(caseUrn)),
                                withJsonPath("firstName", is(firstName)),
                                withJsonPath("lastName", is(lastName)),
                                withJsonPath("interpreterLanguage", is(interpreterLanguage)),
                                withJsonPath("hearingDate", is(hearingDate.toString()))
                        ))))));
    }

    public void verifyCaseCourtReferralActioned(final String caseId) {
        poll(getCasesReferredToCourt())
                .until(status().is(OK), payload().isJson(withJsonPath("$.cases",
                        not(hasItem(isJson(withJsonPath("caseId", is(caseId))))))));
    }

    public String verifyPublicTopicEvent(final String caseId) {
        final String event = publicConsumer.retrieveMessage().get();
        assertThat(event, isJson(withJsonPath("$.caseId", is(caseId))));
        return event;
    }

    @Override
    public void close() {
        publicConsumer.close();
    }
}
