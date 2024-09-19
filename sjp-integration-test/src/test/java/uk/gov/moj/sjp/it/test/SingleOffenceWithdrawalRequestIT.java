package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonValueIsJsonMatcher.isJson;
import static uk.gov.moj.sjp.it.Constants.EVENT_OFFENCES_WITHDRAWAL_STATUS_SET;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_OFFENCES_WITHDRAWAL_STATUS_SET;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.defaultCaseBuilder;
import static uk.gov.moj.sjp.it.command.CreateCase.OffenceBuilder.defaultOffenceBuilder;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestHelper.assertCaseQueryDoesNotReturnWithdrawalReasons;
import static uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestHelper.assertCaseQueryReturnsWithdrawalReasons;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.util.EventUtil.eventsByName;

import uk.gov.justice.json.schemas.fragments.sjp.WithdrawalRequestsStatus;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.OffenceWithdrawalRequestCancelled;
import uk.gov.moj.cpp.sjp.event.OffenceWithdrawalRequestReasonChanged;
import uk.gov.moj.cpp.sjp.event.OffenceWithdrawalRequested;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestHelper;
import uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Enable this when merging to master")
public class SingleOffenceWithdrawalRequestIT extends BaseIntegrationTest {

    private final UUID withdrawalRequestReasonId1 = randomUUID();
    private final UUID withdrawalRequestReasonId2 = randomUUID();
    private final Map<UUID, String> withdrawalReasons = ImmutableMap.of(withdrawalRequestReasonId1, "Insufficient Evidence", withdrawalRequestReasonId2, "Not in public interest to proceed");
    private final UUID userId = randomUUID();
    private final UUID caseId = randomUUID();
    private final UUID offenceId = randomUUID();
    private static final String NATIONAL_COURT_CODE = "1080";

    @BeforeEach
    public void setUp() {
        final CreateCase.CreateCasePayloadBuilder casePayloadBuilder = defaultCaseBuilder().withId(caseId).withOffenceBuilder(defaultOffenceBuilder().withId(offenceId));
        final ProsecutingAuthority prosecutingAuthority = casePayloadBuilder.getProsecutingAuthority();

        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
        stubEnforcementAreaByPostcode(casePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, "TestRegion");

        new EventListener()
                .subscribe(CaseReceived.EVENT_NAME)
                .run(() -> {
                    createCaseForPayloadBuilder(casePayloadBuilder);
                })
                .popEvent(CaseReceived.EVENT_NAME);
        ReferenceDataServiceStub.stubWithdrawalReasonsQuery(withdrawalReasons);
    }

    @Test
    public void offenceWithdrawalRequested() throws Exception {
        try (final OffencesWithdrawalRequestHelper withdrawalRequestHelper = new OffencesWithdrawalRequestHelper(userId, EVENT_OFFENCES_WITHDRAWAL_STATUS_SET, OffenceWithdrawalRequested.EVENT_NAME)) {
            withdrawalRequestHelper.requestWithdrawalOfOffences(caseId, requestPayload());

            final Map<String, List<JsonEnvelope>> privateEventsByName = eventsByName(
                    withdrawalRequestHelper.getEventFromTopic(),
                    withdrawalRequestHelper.getEventFromTopic());

            final JsonEnvelope offencesWithdrawalStatusSetPublicEvent = withdrawalRequestHelper.getEventFromPublicTopic();
            final JsonEnvelope offencesWithdrawalStatusSetPrivateEvent = privateEventsByName.get(EVENT_OFFENCES_WITHDRAWAL_STATUS_SET).get(0);
            final JsonEnvelope offencesWithdrawalRequestedPrivateEvent = privateEventsByName.get(OffenceWithdrawalRequested.EVENT_NAME).get(0);

            final Matcher offencesWithdrawalStatusSetPayloadMatcher = allOf(
                    withJsonPath("$.caseId", equalTo(caseId.toString())),
                    withJsonPath("$.setAt", notNullValue()),
                    withJsonPath("$.setBy", equalTo(userId.toString())),
                    withJsonPath("$.withdrawalRequestsStatus[0].offenceId", equalTo(offenceId.toString())),
                    withJsonPath("$.withdrawalRequestsStatus[0].withdrawalRequestReasonId", equalTo(withdrawalRequestReasonId1.toString())),
                    withJsonPath("$.withdrawalRequestsStatus.length()", equalTo(1)));

            assertThat(offencesWithdrawalStatusSetPublicEvent, jsonEnvelope(
                    metadata().withName(PUBLIC_EVENT_OFFENCES_WITHDRAWAL_STATUS_SET),
                    payload(isJson(allOf(offencesWithdrawalStatusSetPayloadMatcher)))));

            assertThat(offencesWithdrawalStatusSetPrivateEvent, jsonEnvelope(
                    metadata().withName(EVENT_OFFENCES_WITHDRAWAL_STATUS_SET),
                    payload(isJson(allOf(offencesWithdrawalStatusSetPayloadMatcher)))));

            assertThat(offencesWithdrawalRequestedPrivateEvent, jsonEnvelope(
                    metadata().withName(OffenceWithdrawalRequested.EVENT_NAME),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", equalTo(caseId.toString())),
                            withJsonPath("$.offenceId", equalTo(offenceId.toString())),
                            withJsonPath("$.withdrawalRequestReasonId", equalTo(withdrawalRequestReasonId1.toString())),
                            withJsonPath("$.requestedBy", equalTo(userId.toString())),
                            withJsonPath("$.requestedAt", notNullValue())
                    )))));

            assertCaseQueryReturnsWithdrawalReasons(caseId, requestPayload(), withdrawalReasons);
        }
    }


    @Test
    public void offenceWithdrawalRequestCancelled() throws Exception {
        try (final OffencesWithdrawalRequestHelper withdrawalHelper = new OffencesWithdrawalRequestHelper(userId, EVENT_OFFENCES_WITHDRAWAL_STATUS_SET)) {
            withdrawalHelper.requestWithdrawalOfOffences(caseId, requestPayload());
            assertThat(withdrawalHelper.getEventFromPublicTopic(), notNullValue());
            assertThat(withdrawalHelper.getEventFromTopic(), notNullValue());
        }

        try (final OffencesWithdrawalRequestHelper withdrawalHelper = new OffencesWithdrawalRequestHelper(userId, EVENT_OFFENCES_WITHDRAWAL_STATUS_SET, OffenceWithdrawalRequestCancelled.EVENT_NAME)) {

            withdrawalHelper.requestWithdrawalOfOffences(caseId, new ArrayList<>());

            final Map<String, List<JsonEnvelope>> privateEventsByName = eventsByName(
                    withdrawalHelper.getEventFromTopic(),
                    withdrawalHelper.getEventFromTopic());

            final JsonEnvelope offencesWithdrawalStatusSetPublicEvent = withdrawalHelper.getEventFromPublicTopic();
            final JsonEnvelope offencesWithdrawalStatusSetPrivateEvent = privateEventsByName.get(EVENT_OFFENCES_WITHDRAWAL_STATUS_SET).get(0);
            final JsonEnvelope offencesWithdrawalRequestCancelledPrivateEvent = privateEventsByName.get(OffenceWithdrawalRequestCancelled.EVENT_NAME).get(0);

            final Matcher offencesWithdrawalStatusSetPayloadMatcher = allOf(
                    withJsonPath("$.caseId", equalTo(caseId.toString())),
                    withJsonPath("$.setAt", notNullValue()),
                    withJsonPath("$.setBy", equalTo(userId.toString())),
                    withJsonPath("$.withdrawalRequestsStatus.length()", equalTo(0)));

            assertThat(offencesWithdrawalStatusSetPublicEvent, jsonEnvelope(
                    metadata().withName(PUBLIC_EVENT_OFFENCES_WITHDRAWAL_STATUS_SET),
                    payload(isJson(offencesWithdrawalStatusSetPayloadMatcher))));

            assertThat(offencesWithdrawalStatusSetPrivateEvent, jsonEnvelope(
                    metadata().withName(EVENT_OFFENCES_WITHDRAWAL_STATUS_SET),
                    payload(isJson(offencesWithdrawalStatusSetPayloadMatcher))));

            assertThat(offencesWithdrawalRequestCancelledPrivateEvent, jsonEnvelope(
                    metadata().withName(OffenceWithdrawalRequestCancelled.EVENT_NAME),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", equalTo(caseId.toString())),
                            withJsonPath("$.offenceId", equalTo(offenceId.toString())),
                            withJsonPath("$.cancelledBy", equalTo(userId.toString())),
                            withJsonPath("$.cancelledAt", notNullValue())
                    )))));

            assertCaseQueryDoesNotReturnWithdrawalReasons(caseId, offenceId);
        }
    }

    @Test
    public void offenceWithdrawalRequestReasonForSingleOffence() throws Exception {
        try (final OffencesWithdrawalRequestHelper withdrawalHelper = new OffencesWithdrawalRequestHelper(userId, EVENT_OFFENCES_WITHDRAWAL_STATUS_SET)) {
            withdrawalHelper.requestWithdrawalOfOffences(caseId, requestPayload());
            assertThat(withdrawalHelper.getEventFromPublicTopic(), notNullValue());
            assertThat(withdrawalHelper.getEventFromTopic(), notNullValue());
        }

        try (final OffencesWithdrawalRequestHelper withdrawalHelper = new OffencesWithdrawalRequestHelper(userId, EVENT_OFFENCES_WITHDRAWAL_STATUS_SET, OffenceWithdrawalRequestReasonChanged.EVENT_NAME)) {

            withdrawalHelper.requestWithdrawalOfOffences(caseId, requestForReasonChangePayload());

            final Map<String, List<JsonEnvelope>> privateEventsByName = eventsByName(
                    withdrawalHelper.getEventFromTopic(),
                    withdrawalHelper.getEventFromTopic());

            final JsonEnvelope offencesWithdrawalStatusSetPublicEvent = withdrawalHelper.getEventFromPublicTopic();
            final JsonEnvelope offencesWithdrawalStatusSetPrivateEvent = privateEventsByName.get(EVENT_OFFENCES_WITHDRAWAL_STATUS_SET).get(0);
            final JsonEnvelope offencesWithdrawalRequestReasonChangedPrivateEvent = privateEventsByName.get(OffenceWithdrawalRequestReasonChanged.EVENT_NAME).get(0);

            final Matcher offencesWithdrawalStatusSetPayloadMatcher = allOf(
                    withJsonPath("$.caseId", equalTo(caseId.toString())),
                    withJsonPath("$.setAt", notNullValue()),
                    withJsonPath("$.setBy", equalTo(userId.toString())),
                    withJsonPath("$.withdrawalRequestsStatus[0].offenceId", equalTo(offenceId.toString())),
                    withJsonPath("$.withdrawalRequestsStatus[0].withdrawalRequestReasonId", equalTo(withdrawalRequestReasonId2.toString())),
                    withJsonPath("$.withdrawalRequestsStatus.length()", equalTo(1)));

            assertThat(offencesWithdrawalStatusSetPublicEvent, jsonEnvelope(
                    metadata().withName(PUBLIC_EVENT_OFFENCES_WITHDRAWAL_STATUS_SET),
                    payload(isJson(offencesWithdrawalStatusSetPayloadMatcher))));

            assertThat(offencesWithdrawalStatusSetPrivateEvent, jsonEnvelope(
                    metadata().withName(EVENT_OFFENCES_WITHDRAWAL_STATUS_SET),
                    payload(isJson(offencesWithdrawalStatusSetPayloadMatcher))));

            assertThat(offencesWithdrawalRequestReasonChangedPrivateEvent, jsonEnvelope(
                    metadata().withName(OffenceWithdrawalRequestReasonChanged.EVENT_NAME),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", equalTo(caseId.toString())),
                            withJsonPath("$.offenceId", equalTo(offenceId.toString())),
                            withJsonPath("$.changedBy", equalTo(userId.toString())),
                            withJsonPath("$.changedAt", notNullValue()),
                            withJsonPath("$.newWithdrawalRequestReasonId", equalTo(withdrawalRequestReasonId2.toString())),
                            withJsonPath("$.oldWithdrawalRequestReasonId", equalTo(withdrawalRequestReasonId1.toString()))
                    )))));

            assertCaseQueryReturnsWithdrawalReasons(caseId, requestForReasonChangePayload());
        }
    }

    private List<WithdrawalRequestsStatus> requestPayload() {
        final List<WithdrawalRequestsStatus> withdrawalRequestsStatuses = new ArrayList<>();
        withdrawalRequestsStatuses.add(new WithdrawalRequestsStatus(offenceId, withdrawalRequestReasonId1));
        return withdrawalRequestsStatuses;
    }

    private List<WithdrawalRequestsStatus> requestForReasonChangePayload() {
        final List<WithdrawalRequestsStatus> withdrawalRequestsStatuses = new ArrayList<>();
        withdrawalRequestsStatuses.add(new WithdrawalRequestsStatus(offenceId, withdrawalRequestReasonId2));
        return withdrawalRequestsStatuses;
    }

}
