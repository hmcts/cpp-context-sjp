package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.Constants.EVENT_OFFENCES_WITHDRAWAL_STATUS_SET;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_OFFENCES_WITHDRAWAL_STATUS_SET;
import static uk.gov.moj.sjp.it.test.BaseIntegrationTest.USER_ID;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.getReadUrl;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaults;
import static uk.gov.moj.sjp.it.util.TopicUtil.retrieveMessageAsJsonObject;

import uk.gov.justice.json.schemas.fragments.sjp.WithdrawalRequestsStatus;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.messaging.DefaultJsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.util.TopicUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.hamcrest.Matcher;

public class OffencesWithdrawalRequestHelper implements AutoCloseable {
    private static final String WRITE_MEDIA_TYPE = "application/vnd.sjp.set-offences-withdrawal-requests-status+json";
    private static final String GET_CASE_MEDIA_TYPE = "application/vnd.sjp.query.case+json";
    private final MessageConsumer privateMessageConsumer;
    private final MessageConsumer publicMessageConsumer;

    private UUID userId;

    public OffencesWithdrawalRequestHelper(final UUID userId) {
        this(userId, EVENT_OFFENCES_WITHDRAWAL_STATUS_SET);
    }

    public OffencesWithdrawalRequestHelper(final UUID userId, final String... eventSelectors) {
        this.userId = userId;
        privateMessageConsumer = TopicUtil.privateEvents.createConsumerForMultipleSelectors(eventSelectors);
        publicMessageConsumer = TopicUtil.publicEvents.createConsumerForMultipleSelectors(PUBLIC_EVENT_OFFENCES_WITHDRAWAL_STATUS_SET);
    }

    public List<WithdrawalRequestsStatus> preparePayloadWithDefaultsForCase(final CreateCase.CreateCasePayloadBuilder aCase) {
        return aCase.getOffenceIds()
                .stream()
                .map(id -> new WithdrawalRequestsStatus(id, randomUUID()))
                .collect(toList());
    }

    public void requestWithdrawalOfOffences(final UUID caseId, final List<WithdrawalRequestsStatus> withdrawalRequestsStatuses) {
        requestWithdrawalOfOffences(caseId, userId, withdrawalRequestsStatuses);
    }

    public static void requestWithdrawalOfOffences(final UUID caseId, final UUID userId, final List<WithdrawalRequestsStatus> withdrawalRequestsStatuses) {
        final String writeUrl = String.format("/cases/%s/offences-withdrawal-requests-status", caseId);
        final JsonArrayBuilder jsonArrayBuilder = createArrayBuilder();
        withdrawalRequestsStatuses.stream().forEach(withdrawalRequestsStatus -> {
            jsonArrayBuilder.add(createObjectBuilder()
                    .add("offenceId", withdrawalRequestsStatus.getOffenceId().toString())
                    .add("withdrawalRequestReasonId", withdrawalRequestsStatus.getWithdrawalRequestReasonId().toString()));
        });
        final JsonObject payload = createObjectBuilder().add("withdrawalRequestsStatus", jsonArrayBuilder).build();
        makePostCall(userId, writeUrl, WRITE_MEDIA_TYPE, payload.toString(), Response.Status.ACCEPTED);
    }

    public JsonEnvelope getEventFromTopic() {
        return getEventFromTopic(privateMessageConsumer);
    }

    public JsonEnvelope getEventFromPublicTopic() {
        return getEventFromTopic(publicMessageConsumer);
    }

    private JsonEnvelope getEventFromTopic(final MessageConsumer messageConsumer) {
        return retrieveMessageAsJsonObject(messageConsumer)
                .map(event -> new DefaultJsonObjectEnvelopeConverter().asEnvelope(event)).orElse(null);
    }

    public static void assertCaseQueryReturnsWithdrawalReasons(final UUID caseId, final List<WithdrawalRequestsStatus> requestPayload) {
        requestPayload.forEach(e -> assertCaseQueryWithdrawalReasons(caseId, e.getOffenceId(), withJsonPath("withdrawalRequestReasonId", equalTo(e.getWithdrawalRequestReasonId().toString()))));
    }

    public static void assertCaseQueryReturnsWithdrawalReasons(final UUID caseId, final List<WithdrawalRequestsStatus> requestPayload, final Map<UUID, String> withdrawalReasons) {
        requestPayload.forEach(withdrawalRequest -> assertCaseQueryWithdrawalReasons(caseId, withdrawalRequest.getOffenceId(), allOf(
                withJsonPath("withdrawalRequestReasonId", equalTo(withdrawalRequest.getWithdrawalRequestReasonId().toString())),
                withJsonPath("withdrawalRequestReason", equalTo(withdrawalReasons.get(withdrawalRequest.getWithdrawalRequestReasonId())))
        )));
    }

    public static void assertCaseQueryDoesNotReturnWithdrawalReasons(final UUID caseId, final UUID offenceId) {
        assertCaseQueryWithdrawalReasons(caseId, offenceId, withoutJsonPath("withdrawalRequestReasonId"));
    }

    private static void assertCaseQueryWithdrawalReasons(final UUID caseId, final UUID offenceId, final Matcher reasonMatcher) {
        final String queryUrl = String.format("/cases/%s", caseId);
        pollWithDefaults(getCaseDetails(queryUrl))
                .until(status().is(OK), payload().isJson(allOf(
                        withJsonPath("$.id", equalTo(caseId.toString())),
                        withJsonPath("$.defendant.offences[*]", hasItem(isJson(
                                allOf(withJsonPath("id", is(offenceId.toString())), reasonMatcher)
                        )))
                )));
    }

    private static RequestParamsBuilder getCaseDetails(final String queryUrl) {
        return requestParams(getReadUrl(queryUrl), GET_CASE_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
    }

    @Override
    public void close() throws JMSException {
        privateMessageConsumer.close();
        publicMessageConsumer.close();
    }
}
