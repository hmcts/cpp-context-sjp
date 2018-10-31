package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PIA;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.WITHDRAWAL_REQUESTED;
import static uk.gov.moj.sjp.it.Constants.NOTICE_PERIOD_IN_DAYS;
import static uk.gov.moj.sjp.it.command.AddDatesToAvoid.addDatesToAvoid;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.UpdatePleaHelper.getPleaPayload;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.getReadUrl;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaultsUntilResponseIsJson;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CancelPleaHelper;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestCancelHelper;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestHelper;
import uk.gov.moj.sjp.it.helper.UpdatePleaHelper;
import uk.gov.moj.sjp.it.producer.CompleteCaseProducer;

import java.time.LocalDate;
import java.util.UUID;

import com.jayway.jsonpath.ReadContext;
import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.junit.Test;

public class ReadyCaseIT extends BaseIntegrationTest {

    private static final String QUERY_READY_CASES_RESOURCE = "/cases/ready-cases";
    private static final String QUERY_READY_CASES = "application/vnd.sjp.query.ready-cases+json";
    private final UUID caseId = randomUUID();
    private final UUID offenceId = randomUUID();

    @Test
    public void shouldChangeCaseReadinessWhenCaseAfterNoticeEndDate() {
        final LocalDate postingDate = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);

        createCaseForPayloadBuilder(CreateCase.CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withOffenceId(offenceId)
                .withPostingDate(postingDate));

        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper();
             final CancelPleaHelper cancelPleaHelper = new CancelPleaHelper(caseId, offenceId);
             final OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(caseId);
             final OffencesWithdrawalRequestCancelHelper offencesWithdrawalRequestCancelHelper = new OffencesWithdrawalRequestCancelHelper(caseId)) {

            pollUntilReadyWithReason(caseId, PIA);

            updatePleaHelper.updatePlea(caseId, offenceId, getPleaPayload(PleaType.GUILTY));

            pollUntilReadyWithReason(caseId, PLEADED_GUILTY);

            offencesWithdrawalRequestHelper.requestWithdrawalForAllOffences(USER_ID);

            pollUntilReadyWithReason(caseId, WITHDRAWAL_REQUESTED);

            offencesWithdrawalRequestCancelHelper.cancelRequestWithdrawalForAllOffences(USER_ID);

            pollUntilReadyWithReason(caseId, PLEADED_GUILTY);

            cancelPleaHelper.cancelPlea();

            pollUntilReadyWithReason(caseId, PIA);
        }
    }

    @Test
    public void shouldChangeCaseReadinessWhenCaseBeforeNoticeEndDate() {
        final LocalDate postingDate = now().minusDays(NOTICE_PERIOD_IN_DAYS - 1);

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

            pollUntilReadyWithReason(caseId, PLEADED_GUILTY);

            updatePleaHelper.updatePlea(caseId, offenceId, getPleaPayload(PleaType.NOT_GUILTY));

            pollUntilNotReady(caseId);

            addDatesToAvoid(caseId, "my-dates-to-avoid");

            pollUntilReadyWithReason(caseId, CaseReadinessReason.PLEADED_NOT_GUILTY);

            cancelPleaHelper.cancelPlea();

            pollUntilNotReady(caseId);

            offencesWithdrawalRequestHelper.requestWithdrawalForAllOffences(USER_ID);

            pollUntilReadyWithReason(caseId, WITHDRAWAL_REQUESTED);

            offencesWithdrawalRequestCancelHelper.cancelRequestWithdrawalForAllOffences(USER_ID);

            pollUntilNotReady(caseId);
        }
    }

    @Test
    public void shouldUnmarkCaseReadyWhenCaseCompleted() {
        final LocalDate postingDate = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);

        createCaseForPayloadBuilder(CreateCase.CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withPostingDate(postingDate));

        CompleteCaseProducer completeCaseProducer = new CompleteCaseProducer(caseId);
        pollUntilReadyWithReason(caseId, PIA);

        completeCaseProducer.completeCase();

        pollUntilNotReady(caseId);
    }

    private static void pollUntilReadyWithReason(final UUID caseId, final CaseReadinessReason readinessReason) {
        pollReadyCasesUntilResponseIsJson(withJsonPath("readyCases.*", hasItem(
                isJson(allOf(
                        withJsonPath("caseId", equalTo(caseId.toString())),
                        withJsonPath("reason", equalTo(readinessReason.name())))
                ))));
    }

    private static void pollUntilNotReady(final UUID caseId) {
        pollReadyCasesUntilResponseIsJson(withJsonPath("readyCases.*", not(hasItem(
                isJson(withJsonPath("caseId", equalTo(caseId.toString())))
        ))));
    }

    private static JsonPath pollReadyCasesUntilResponseIsJson(final Matcher<? super ReadContext> matcher) {
        final RequestParamsBuilder requestParams = requestParams(getReadUrl(QUERY_READY_CASES_RESOURCE), QUERY_READY_CASES)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
        return pollWithDefaultsUntilResponseIsJson(requestParams.build(), matcher);
    }
}
