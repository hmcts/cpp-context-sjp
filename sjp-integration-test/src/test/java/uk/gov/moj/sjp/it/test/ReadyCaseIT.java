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
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryOffenceById;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.getReadUrl;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaultsUntilResponseIsJson;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CancelPleaHelper;
import uk.gov.moj.sjp.it.helper.CaseSearchResultHelper;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestCancelHelper;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestHelper;
import uk.gov.moj.sjp.it.helper.ReadyCaseHelper;
import uk.gov.moj.sjp.it.helper.UpdatePleaHelper;
import uk.gov.moj.sjp.it.producer.CompleteCaseProducer;

import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonObject;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

public class ReadyCaseIT extends BaseIntegrationTest {

    private static final String QUERY_READY_CASES_RESOURCE = "/cases/ready-cases";
    private static final String QUERY_READY_CASES = "application/vnd.sjp.query.ready-cases+json";
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;
    private final UUID caseId = randomUUID();
    private final UUID offenceId = randomUUID();

    @Before
    public void setUp() throws Exception {

        stubQueryOffenceById(offenceId);
    }

    @Test
    public void shouldChangeCaseReadinessWhenCaseAfterNoticeEndDate() throws Exception {
        final LocalDate postingDate = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);

        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper();
             final CancelPleaHelper cancelPleaHelper = new CancelPleaHelper(caseId, offenceId);
             final OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(caseId);
             final OffencesWithdrawalRequestCancelHelper offencesWithdrawalRequestCancelHelper = new OffencesWithdrawalRequestCancelHelper(caseId);
             final ReadyCaseHelper readyCaseHelper = new ReadyCaseHelper()
        ) {
            createCase(postingDate);

            final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(caseId,
                    createCasePayloadBuilder.getUrn(),
                    createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                    createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());

            readyCaseHelper.verifyCaseMarkedReadyForDecisionEventEmitted(caseId, PIA);
            verifyCaseReadyInViewStore(caseId, PIA);

            updatePleaHelper.updatePlea(caseId, offenceId, getPleaPayload(PleaType.GUILTY));

            caseSearchResultHelper.verifyCaseStatus(CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION);

            readyCaseHelper.verifyCaseMarkedReadyForDecisionEventEmitted(caseId, PLEADED_GUILTY);
            verifyCaseReadyInViewStore(caseId, PLEADED_GUILTY);

            offencesWithdrawalRequestHelper.requestWithdrawalForAllOffences(USER_ID);

            caseSearchResultHelper.verifyCaseStatus(CaseStatus.WITHDRAWAL_REQUEST_READY_FOR_DECISION);

            readyCaseHelper.verifyCaseMarkedReadyForDecisionEventEmitted(caseId, WITHDRAWAL_REQUESTED);
            verifyCaseReadyInViewStore(caseId, WITHDRAWAL_REQUESTED);

            offencesWithdrawalRequestCancelHelper.cancelRequestWithdrawalForAllOffences(USER_ID);

            readyCaseHelper.verifyCaseMarkedReadyForDecisionEventEmitted(caseId, PLEADED_GUILTY);
            verifyCaseReadyInViewStore(caseId, PLEADED_GUILTY);

            cancelPleaHelper.cancelPlea();

            caseSearchResultHelper.verifyCaseStatus(CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION);

            readyCaseHelper.verifyCaseMarkedReadyForDecisionEventEmitted(caseId, PIA);
            verifyCaseReadyInViewStore(caseId, PIA);
        }
    }

    @Test
    public void shouldChangeCaseReadinessWhenCaseBeforeNoticeEndDate() throws Exception {
        final LocalDate postingDate = now().minusDays(NOTICE_PERIOD_IN_DAYS - 1);

        createCase(postingDate);

        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper();
             final CancelPleaHelper cancelPleaHelper = new CancelPleaHelper(caseId, offenceId);
             final OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(caseId);
             final OffencesWithdrawalRequestCancelHelper offencesWithdrawalRequestCancelHelper = new OffencesWithdrawalRequestCancelHelper(caseId);
             final ReadyCaseHelper readyCaseHelper = new ReadyCaseHelper()
        ) {

            final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(caseId,
                    createCasePayloadBuilder.getUrn(),
                    createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                    createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());

            caseSearchResultHelper.verifyCaseStatus(CaseStatus.NO_PLEA_RECEIVED);

            updatePleaHelper.updatePlea(caseId, offenceId, getPleaPayload(PleaType.GUILTY));

            caseSearchResultHelper.verifyCaseStatus(CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION);

            readyCaseHelper.verifyCaseMarkedReadyForDecisionEventEmitted(caseId, PLEADED_GUILTY);
            verifyCaseReadyInViewStore(caseId, PLEADED_GUILTY);

            updatePleaHelper.updatePlea(caseId, offenceId, getPleaPayload(PleaType.NOT_GUILTY));

            readyCaseHelper.verifyCaseUnmarkedReadyForDecisionEventEmitted(caseId, now().plusDays(10));
            verifyCaseNotReadyInViewStore(caseId);

            addDatesToAvoid(caseId, "my-dates-to-avoid");

            readyCaseHelper.verifyCaseMarkedReadyForDecisionEventEmitted(caseId, CaseReadinessReason.PLEADED_NOT_GUILTY);
            verifyCaseReadyInViewStore(caseId, CaseReadinessReason.PLEADED_NOT_GUILTY);

            caseSearchResultHelper.verifyCaseStatus(CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION);

            cancelPleaHelper.cancelPlea();

            readyCaseHelper.verifyCaseUnmarkedReadyForDecisionEventEmitted(caseId, postingDate.plusDays(28));
            verifyCaseNotReadyInViewStore(caseId);

            caseSearchResultHelper.verifyCaseStatus(CaseStatus.NO_PLEA_RECEIVED);

            verifyCaseNotReadyInViewStore(caseId);

            offencesWithdrawalRequestHelper.requestWithdrawalForAllOffences(USER_ID);

            caseSearchResultHelper.verifyCaseStatus(CaseStatus.WITHDRAWAL_REQUEST_READY_FOR_DECISION);

            readyCaseHelper.verifyCaseMarkedReadyForDecisionEventEmitted(caseId, WITHDRAWAL_REQUESTED);
            verifyCaseReadyInViewStore(caseId, WITHDRAWAL_REQUESTED);

            offencesWithdrawalRequestCancelHelper.cancelRequestWithdrawalForAllOffences(USER_ID);

            caseSearchResultHelper.verifyCaseStatus(CaseStatus.NO_PLEA_RECEIVED);

            verifyCaseNotReadyInViewStore(caseId);
        }
    }

    @Test
    public void shouldUnmarkCaseReadyWhenCaseCompleted() {
        final LocalDate postingDate = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);

        CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withPostingDate(postingDate);
        createCaseForPayloadBuilder(createCasePayloadBuilder);

        CompleteCaseProducer completeCaseProducer = new CompleteCaseProducer(caseId, createCasePayloadBuilder.getDefendantBuilder().getId(), createCasePayloadBuilder.getOffenceBuilder().getId());
        verifyCaseReadyInViewStore(caseId, PIA);

        completeCaseProducer.completeCase();

        verifyCaseNotReadyInViewStore(caseId);
    }

    private void createCase(final LocalDate postingDate) {

        createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withOffenceId(offenceId)
                .withPostingDate(postingDate);
        createCaseForPayloadBuilder(createCasePayloadBuilder);
    }

    private static void verifyCaseReadyInViewStore(final UUID caseId, final CaseReadinessReason readinessReason) {
        pollReadyCasesUntilResponseIsJson(withJsonPath("readyCases.*", hasItem(
                isJson(allOf(
                        withJsonPath("caseId", equalTo(caseId.toString())),
                        withJsonPath("reason", equalTo(readinessReason.name())))
                ))));
    }

    private static void verifyCaseNotReadyInViewStore(final UUID caseId) {
        pollReadyCasesUntilResponseIsJson(withJsonPath("readyCases.*", not(hasItem(
                isJson(withJsonPath("caseId", equalTo(caseId.toString())))
        ))));
    }

    private static JsonObject pollReadyCasesUntilResponseIsJson(final Matcher<? super ReadContext> matcher) {
        final RequestParamsBuilder requestParams = requestParams(getReadUrl(QUERY_READY_CASES_RESOURCE), QUERY_READY_CASES)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
        return pollWithDefaultsUntilResponseIsJson(requestParams.build(), matcher);
    }
}
