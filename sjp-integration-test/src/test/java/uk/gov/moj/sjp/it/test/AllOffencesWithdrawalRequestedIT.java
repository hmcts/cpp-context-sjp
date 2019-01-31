package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonValueIsJsonMatcher.isJson;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED;
import static uk.gov.moj.sjp.it.Constants.SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED;
import static uk.gov.moj.sjp.it.Constants.SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseSearchResultHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestCancelHelper;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestHelper;

import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class AllOffencesWithdrawalRequestedIT extends BaseIntegrationTest {

    private UUID userId, caseId;
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    @Before
    public void setUp() {
        userId = randomUUID();
        caseId = randomUUID();
        createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults().withId(caseId);
        new EventListener()
                .subscribe(CaseReceived.EVENT_NAME)
                .run(() -> createCaseForPayloadBuilder(createCasePayloadBuilder));
    }

    @Test
    public void shouldWithdrawThenCancelWithdrawAllOffencesWhenCaseNotAssigned() {
        try (
                final OffencesWithdrawalRequestCancelHelper offencesWithdrawalRequestCancelHelper = new OffencesWithdrawalRequestCancelHelper(caseId,
                        SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED, PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED);
                final OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(caseId,
                        SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED, PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED)) {
            final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(caseId,
                    createCasePayloadBuilder.getUrn(),
                    createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                    createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());
            caseSearchResultHelper.verifyPersonInfoByUrn();

            final EventListener eventListener = new EventListener();

            //check successful standard withdrawal request
            eventListener
                    .subscribe(SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED)
                    .subscribe(PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED)
                    .run(() -> offencesWithdrawalRequestHelper.requestWithdrawalForAllOffences(userId));

            final Optional<JsonEnvelope> withdrawalRequestedPrivateEvent = eventListener.popEvent(SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED);
            final Optional<JsonEnvelope> withdrawalRequestedPublicEvent = eventListener.popEvent(PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED);

            assertThat(withdrawalRequestedPrivateEvent.isPresent(), is(true));
            assertThat(withdrawalRequestedPrivateEvent.get(), jsonEnvelope(
                    metadata().withName(SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED),
                    payload(isJson(withJsonPath("caseId", equalTo(caseId.toString()))))
            ));

            assertThat(withdrawalRequestedPublicEvent.isPresent(), is(true));
            assertThat(withdrawalRequestedPublicEvent.get(), jsonEnvelope(
                    metadata().withName(PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED),
                    payload(isJson(withJsonPath("caseId", equalTo(caseId.toString()))))
            ));

            caseSearchResultHelper.verifyWithdrawalRequestedDateAndCaseStatus();

            //check successful cancel withdrawal request
            eventListener
                    .reset()
                    .subscribe(SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED)
                    .subscribe(PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED)
                    .run(() -> offencesWithdrawalRequestCancelHelper.cancelRequestWithdrawalForAllOffences(userId));

            final Optional<JsonEnvelope> withdrawalCancelledPrivateEvent = eventListener.popEvent(SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED);
            final Optional<JsonEnvelope> withdrawalCancelledPublicEvent = eventListener.popEvent(PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED);

            assertThat(withdrawalCancelledPrivateEvent.isPresent(), is(true));
            assertThat(withdrawalCancelledPrivateEvent.get(), jsonEnvelope(
                    metadata().withName(SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED),
                    payload(isJson(withJsonPath("caseId", equalTo(caseId.toString()))))
            ));

            assertThat(withdrawalRequestedPrivateEvent.isPresent(), is(true));
            assertThat(withdrawalCancelledPublicEvent.get(), jsonEnvelope(
                    metadata().withName(PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED),
                    payload(isJson(withJsonPath("caseId", equalTo(caseId.toString()))))
            ));

            caseSearchResultHelper.verifyNoWithdrawalRequestedDateAndCaseStatus();
        }
    }

    @Test
    public void shouldWithdrawThenCancelWithdrawAllOffencesWhenCaseAssignedToCaller() {
        try (
                final OffencesWithdrawalRequestCancelHelper offencesWithdrawalRequestCancelHelper = new OffencesWithdrawalRequestCancelHelper(caseId,
                        SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED, PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED);
                final OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(caseId,
                        SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED, PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED)
        ) {
            final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(caseId,
                    createCasePayloadBuilder.getUrn(),
                    createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                    createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());
            caseSearchResultHelper.verifyPersonInfoByUrn();

            final EventListener eventListener = new EventListener();

            //check successful standard withdrawal request
            eventListener
                    .subscribe(SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED)
                    .subscribe(PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED)
                    .run(() -> offencesWithdrawalRequestHelper.requestWithdrawalForAllOffences(userId));

            final Optional<JsonEnvelope> withdrawalRequestedPrivateEvent = eventListener.popEvent(SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED);
            final Optional<JsonEnvelope> withdrawalRequestedPublicEvent = eventListener.popEvent(PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED);

            assertThat(withdrawalRequestedPrivateEvent.isPresent(), is(true));
            assertThat(withdrawalRequestedPrivateEvent.get(), jsonEnvelope(
                    metadata().withName(SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED),
                    payload(isJson(withJsonPath("caseId", equalTo(caseId.toString()))))
            ));

            assertThat(withdrawalRequestedPublicEvent.isPresent(), is(true));
            assertThat(withdrawalRequestedPublicEvent.get(), jsonEnvelope(
                    metadata().withName(PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED),
                    payload(isJson(withJsonPath("caseId", equalTo(caseId.toString()))))
            ));

            caseSearchResultHelper.verifyWithdrawalRequestedDateAndCaseStatus();

            //check successful cancel withdrawal request
            eventListener
                    .reset()
                    .subscribe(SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED)
                    .subscribe(PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED)
                    .run(() -> offencesWithdrawalRequestCancelHelper.cancelRequestWithdrawalForAllOffences(userId));

            final Optional<JsonEnvelope> withdrawalCancelledPrivateEvent = eventListener.popEvent(SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED);
            final Optional<JsonEnvelope> withdrawalCancelledPublicEvent = eventListener.popEvent(PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED);

            assertThat(withdrawalCancelledPrivateEvent.isPresent(), is(true));
            assertThat(withdrawalCancelledPrivateEvent.get(), jsonEnvelope(
                    metadata().withName(SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED),
                    payload(isJson(withJsonPath("caseId", equalTo(caseId.toString()))))
            ));

            assertThat(withdrawalCancelledPublicEvent.isPresent(), is(true));
            assertThat(withdrawalCancelledPublicEvent.get(), jsonEnvelope(
                    metadata().withName(PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED),
                    payload(isJson(withJsonPath("caseId", equalTo(caseId.toString()))))
            ));

            caseSearchResultHelper.verifyNoWithdrawalRequestedDateAndCaseStatus();
        }
    }

}
