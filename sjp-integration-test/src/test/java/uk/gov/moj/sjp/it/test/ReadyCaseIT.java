package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.unmodifiableMap;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.UpdatePleaHelper.getPleaPayload;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetEmptyAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.ResultingStub.stubGetCaseDecisionsWithNoDecision;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.getReadUrl;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.PleaType;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CancelPleaHelper;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestCancelHelper;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestHelper;
import uk.gov.moj.sjp.it.helper.UpdatePleaHelper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.jayway.jsonpath.ReadContext;
import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

public class ReadyCaseIT extends BaseIntegrationTest {

    private static final int NOTICE_PERIOD_IN_DAYS = 28;
    private ReadyCasesPoller readyCasesPoller;
    private UUID caseId, offenceId;
    private LocalDate postingDate;

    @Before
    public void init() {
        readyCasesPoller = ReadyCasesPoller.init();

        caseId = randomUUID();
        offenceId = randomUUID();
        postingDate = LocalDate.now().minusDays(NOTICE_PERIOD_IN_DAYS).minusDays(1);

        stubGetCaseDecisionsWithNoDecision(caseId);
        stubGetEmptyAssignmentsByDomainObjectId(caseId);
    }

    @Test
    public void shouldChangeCaseReadinessWhenCaseAfterNoticeEndDate() {

        postingDate = LocalDate.now().minusDays(NOTICE_PERIOD_IN_DAYS).minusDays(1);

        createCaseForPayloadBuilder(CreateCase.CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withOffenceId(offenceId)
                .withPostingDate(postingDate));

        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper();
             final CancelPleaHelper cancelPleaHelper = new CancelPleaHelper(caseId, offenceId);
             final OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(caseId);
             final OffencesWithdrawalRequestCancelHelper offencesWithdrawalRequestCancelHelper = new OffencesWithdrawalRequestCancelHelper(caseId)) {

            readyCasesPoller.pollUntilReadyWithReason(CaseReadinessReason.PIA);

            updatePleaHelper.updatePlea(caseId, offenceId, getPleaPayload(PleaType.GUILTY));

            readyCasesPoller.pollUntilReadyWithReason(CaseReadinessReason.PLEADED_GUILTY);

            offencesWithdrawalRequestHelper.requestWithdrawalForAllOffences(UUID.fromString(USER_ID));

            readyCasesPoller.pollUntilReadyWithReason(CaseReadinessReason.WITHDRAWAL_REQUESTED);

            offencesWithdrawalRequestCancelHelper.cancelRequestWithdrawalForAllOffences(UUID.fromString(USER_ID));

            readyCasesPoller.pollUntilReadyWithReason(CaseReadinessReason.PLEADED_GUILTY);

            cancelPleaHelper.cancelPlea();

            readyCasesPoller.pollUntilReadyWithReason(CaseReadinessReason.PIA);
        }

    }

    @Test
    public void shouldChangeCaseReadinessWhenCaseBeforeNoticeEndDate() {

        postingDate = LocalDate.now().minusDays(NOTICE_PERIOD_IN_DAYS).plusDays(1);

        createCaseForPayloadBuilder(CreateCase.CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withOffenceId(offenceId)
                .withPostingDate(postingDate));

        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper();
             final CancelPleaHelper cancelPleaHelper = new CancelPleaHelper(caseId, offenceId);
             final OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(caseId);
             final OffencesWithdrawalRequestCancelHelper offencesWithdrawalRequestCancelHelper = new OffencesWithdrawalRequestCancelHelper(caseId)) {

            updatePleaHelper.updatePlea(caseId, offenceId, getPleaPayload(PleaType.GUILTY));

            readyCasesPoller.pollUntilReadyWithReason(CaseReadinessReason.PLEADED_GUILTY);

            updatePleaHelper.updatePlea(caseId, offenceId, getPleaPayload(PleaType.NOT_GUILTY));

            readyCasesPoller.pollUntilReadyWithReason(CaseReadinessReason.PLEADED_NOT_GUILTY);

            cancelPleaHelper.cancelPlea();

            readyCasesPoller.pollUntilNotReady();

            offencesWithdrawalRequestHelper.requestWithdrawalForAllOffences(UUID.fromString(USER_ID));

            readyCasesPoller.pollUntilReadyWithReason(CaseReadinessReason.WITHDRAWAL_REQUESTED);

            offencesWithdrawalRequestCancelHelper.cancelRequestWithdrawalForAllOffences(UUID.fromString(USER_ID));

            readyCasesPoller.pollUntilNotReady();
        }

    }

    private static class ReadyCasesPoller {

        private final Map<CaseReadinessReason, Integer> initialReadyCaseCountByReadinessReason;

        public static ReadyCasesPoller init() {
            return new ReadyCasesPoller();
        }

        private ReadyCasesPoller() {
            this.initialReadyCaseCountByReadinessReason = unmodifiableMap(getNumberOfReadyCases());
        }

        private void pollUntilReadyWithReason(final CaseReadinessReason readinessReason) {
            final Map<CaseReadinessReason, Integer> expectedReadyCaseCountByReadinessReason = initialReadyCaseCountByReadinessReason
                    .entrySet()
                    .stream()
                    .filter(es -> !es.getValue().equals(0))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            expectedReadyCaseCountByReadinessReason.compute(readinessReason, (reason, count) -> (count == null) ? 1 : count + 1);

            pollUntilInState(expectedReadyCaseCountByReadinessReason);
        }

        private void pollUntilNotReady() {
            pollUntilInState(initialReadyCaseCountByReadinessReason);
        }

        private JsonPath pollUntilInState(final Map<CaseReadinessReason, Integer> original) {
            final List<Matcher> reasonsMatchers = original.entrySet()
                    .stream()
                    .map(e -> hasItem(isJson(allOf(withJsonPath("reason", equalTo(e.getKey().name())), withJsonPath("count", equalTo(e.getValue()))))))
                    .collect(toList());

            final Matcher jsonPayloadMatcher = withJsonPath("reasons.*", allOf(reasonsMatchers.toArray(new Matcher[original.size()])));

            return pollReadyCasesReasonsCounts(jsonPayloadMatcher);
        }

        private static JsonPath pollReadyCasesReasonsCounts(final Matcher<? super ReadContext> jsonPayloadMatcher) {
            final RequestParamsBuilder requestParams = requestParams(getReadUrl("/cases/ready-cases-reasons-counts"), "application/vnd.sjp.query.ready-cases-reasons-counts+json")
                    .withHeader(HeaderConstants.USER_ID, USER_ID);

            final ResponseData responseData = poll(requestParams)
                    .until(anyOf(
                            allOf(status().is(OK), payload().isJson(jsonPayloadMatcher)),
                            status().is(INTERNAL_SERVER_ERROR),
                            status().is(FORBIDDEN)
                    ));

            if (responseData.getStatus() != OK) {
                fail("Polling interrupted, please fix the error before continue. Status code: " + responseData.getStatus());
            }

            return new JsonPath(responseData.getPayload());
        }

        private Map<CaseReadinessReason, Integer> getNumberOfReadyCases() {
            return pollReadyCasesReasonsCounts(any(ReadContext.class))
                    .getList("reasons")
                    .stream()
                    .map(Map.class::cast)
                    .collect(toMap(
                            e -> CaseReadinessReason.valueOf(e.get("reason").toString()),
                            e -> Integer.valueOf(e.get("count").toString()))
                    );
        }

    }

}
