package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.justice.json.schemas.fragments.sjp.WithdrawalRequestsStatus.withdrawalRequestsStatus;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.PLEA_RECEIVED_NOT_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.sjp.it.command.AddDatesToAvoid.addDatesToAvoid;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.DatesToAvoidHelper.makeDatesToAvoidExpired;
import static uk.gov.moj.sjp.it.helper.DatesToAvoidHelper.verifyDatesToAvoidExpiredEventEmitted;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.saveDefaultDecision;
import static uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestHelper.requestWithdrawalOfOffences;
import static uk.gov.moj.sjp.it.helper.SetPleasHelper.requestSetPleasAndConfirm;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollForCase;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseHasStatus;
import static uk.gov.moj.sjp.it.pollingquery.PendingDatesToAvoidPoller.pollUntilPendingDatesToAvoidIsOk;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.SJP_PROSECUTORS_GROUP;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubGroupForUser;
import static uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions;
import static uk.gov.moj.sjp.it.util.SjpDatabaseCleaner.cleanViewStore;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.google.common.collect.Sets;
import com.jayway.jsonpath.ReadContext;
import io.restassured.path.json.JsonPath;
import org.apache.commons.lang3.tuple.Triple;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatesToAvoidIT extends BaseIntegrationTest {

    private static final Clock CLOCK = new UtcClock();
    private static final String DATE_TO_AVOID = "a-date-to-avoid";
    private static final String DATE_TO_AVOID_UPDATE = "I cannot come on Thursday";

    private UUID tflUserId;
    private UUID tvlUserId;
    private CreateCase.CreateCasePayloadBuilder tflCaseBuilder;
    private CreateCase.CreateCasePayloadBuilder tvlCaseBuilder;
    private int tflInitialPendingDatesToAvoidCount;
    private int tvlInitialPendingDatesToAvoidCount;
    private EventListener eventListener;

    private static final String DEFENDANT_REGION = "croydon";
    private static final String NATIONAL_COURT_CODE = "1080";

    @BeforeEach
    public void setUp() throws Exception {
        cleanViewStore();

        tflUserId = randomUUID();
        tvlUserId = randomUUID();

        stubGroupForUser(tflUserId, SJP_PROSECUTORS_GROUP);
        stubForUserDetails(tflUserId, TFL);

        stubDefaultCourtByCourtHouseOUCodeQuery();
        provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));

        this.tflCaseBuilder = createCaseBuilder(TFL, 1);

        stubEnforcementAreaByPostcode(tflCaseBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, DEFENDANT_REGION);

        new EventListener()
                .subscribe(CaseMarkedReadyForDecision.EVENT_NAME)
                .run(() -> CreateCase.createCaseForPayloadBuilder(tflCaseBuilder))
                .popEvent(CaseMarkedReadyForDecision.EVENT_NAME);

        this.tflInitialPendingDatesToAvoidCount = pollForCountOfCasesPendingDatesToAvoid(tflUserId);

        stubProsecutorQuery(TFL.name(), TFL.getFullName(), randomUUID());
        stubProsecutorQuery(TVL.name(), TVL.getFullName(), randomUUID());

        this.tvlCaseBuilder = createCaseBuilder(TVL, 2);
        createCaseForPayloadBuilder(this.tvlCaseBuilder);
        stubGroupForUser(tvlUserId, SJP_PROSECUTORS_GROUP);
        stubForUserDetails(tvlUserId, TVL);
        this.tvlInitialPendingDatesToAvoidCount = pollForCountOfCasesPendingDatesToAvoid(tvlUserId);

        eventListener = new EventListener();
    }

    @Test
    public void shouldCaseBePendingDatesToAvoidForPleaChangeScenariosAndBeResultedAfterDatesToAvoid() {
        setupIdMapperStub();
        shouldCaseBePendingDatesToAvoidForPleaChangeScenarios();

        addDatesToAvoid(tflCaseBuilder.getId(), DATE_TO_AVOID);
        assertThatNumberOfCasesPendingDatesToAvoidIsAccurate(tflUserId, tflInitialPendingDatesToAvoidCount);

        completeCase(tflCaseBuilder);

        assertThatNumberOfCasesPendingDatesToAvoidIsAccurate(tflUserId, tflInitialPendingDatesToAvoidCount);
    }

    @Test
    public void shouldCaseBePendingDatesToAvoidForPleaChangeScenariosAndBeReadyAfterDatesToAvoidExpiry() {
        shouldCaseBePendingDatesToAvoidForPleaChangeScenarios();
        pollUntilCaseHasStatus(tflCaseBuilder.getId(), PLEA_RECEIVED_NOT_READY_FOR_DECISION);
        makeDatesToAvoidExpired(eventListener, tflCaseBuilder.getId());
        verifyDatesToAvoidExpiredEventEmitted(eventListener, tflCaseBuilder.getId());
        pollUntilCaseHasStatus(tflCaseBuilder.getId(), PLEA_RECEIVED_READY_FOR_DECISION);
    }

    @Test
    void shouldCaseBePendingDatesToAvoidForResultedCaseScenarioForTflUser() {
        setupIdMapperStub();
        ReferenceDataServiceStub.stubWithdrawalReasons();
        shouldCaseBePendingDatesToAvoidForResultedCaseScenario(tflUserId, tflCaseBuilder, tflInitialPendingDatesToAvoidCount);
    }

    @Test
    void shouldCaseBePendingDatesToAvoidForResultedCaseScenarioForTvlUser() {
        setupIdMapperStub();
        ReferenceDataServiceStub.stubWithdrawalReasons();
        shouldCaseBePendingDatesToAvoidForResultedCaseScenario(tvlUserId, tvlCaseBuilder, tvlInitialPendingDatesToAvoidCount);
    }

    @Test
    public void shouldAddAndUpdateDatesToAvoidToCase() {
        //given
        final String DATES_TO_AVOID_ADDED_PUBLIC_EVENT_NAME = "public.sjp.dates-to-avoid-added";
        final String DATES_TO_AVOID_UPDATED_PUBLIC_EVENT_NAME = "public.sjp.dates-to-avoid-updated";

        final UUID caseId = tflCaseBuilder.getId();

        updatePleaToNotGuiltyAndConfirm(caseId, tflCaseBuilder.getOffenceId(), tflCaseBuilder.getDefendantBuilder().getId());

        final EventListener addEventListener = new EventListener()
                .subscribe(DATES_TO_AVOID_ADDED_PUBLIC_EVENT_NAME)
                //when
                .run(() -> addDatesToAvoid(caseId, DATE_TO_AVOID))
                //then
                .run(() -> pollForACaseWith(DATE_TO_AVOID));

        assertThatDatesToAvoidPublicEventWasRaised(addEventListener,
                DATES_TO_AVOID_ADDED_PUBLIC_EVENT_NAME, caseId, DATE_TO_AVOID);

        final EventListener updateEventListener = new EventListener()
                .subscribe(DATES_TO_AVOID_UPDATED_PUBLIC_EVENT_NAME)
                //when
                .run(() -> addDatesToAvoid(caseId, DATE_TO_AVOID_UPDATE))
                //then
                .run(() -> pollForACaseWith(DATE_TO_AVOID_UPDATE));

        assertThatDatesToAvoidPublicEventWasRaised(updateEventListener,
                DATES_TO_AVOID_UPDATED_PUBLIC_EVENT_NAME, caseId, DATE_TO_AVOID_UPDATE);
    }

    private void shouldCaseBePendingDatesToAvoidForResultedCaseScenario(UUID userId, CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder, int expectedPendingDatesToAvoidCount) {

        //updates plea to NOT_GUILTY for TFL case (making it pending dates-to-avoid submission)
        updatePleaToNotGuiltyAndConfirm(tflCaseBuilder.getId(), tflCaseBuilder.getOffenceId(), tflCaseBuilder.getDefendantBuilder().getId());

        //check tvl count not gone up (as a result of tfl plea update above)
        assertEquals(tvlInitialPendingDatesToAvoidCount, pollForCountOfCasesPendingDatesToAvoid(tvlUserId), "Ensure that access control is ON");

        //updates plea to NOT_GUILTY for TVL case (making it pending dates-to-avoid submission)
        updatePleaToNotGuiltyAndConfirm(tvlCaseBuilder.getId(), tvlCaseBuilder.getOffenceId(), tvlCaseBuilder.getDefendantBuilder().getId());

        //checks that correct dates-to-avoid record is retrieved (i.e. the one related to the case passed in)
        assertThatDatesToAvoidIsPendingSubmissionForCase(userId, createCasePayloadBuilder);

        requestWithdrawalOfOffences(createCasePayloadBuilder.getId(), userId, asList(withdrawalRequestsStatus()
                .withOffenceId(createCasePayloadBuilder.getOffenceId())
                .withWithdrawalRequestReasonId(randomUUID())
                .build()));

        //checks that dates-to-avoid NOT pending submission when case completed
        completeCase(createCasePayloadBuilder);
        assertThatNumberOfCasesPendingDatesToAvoidIsAccurate(userId, expectedPendingDatesToAvoidCount);
    }

    private void shouldCaseBePendingDatesToAvoidForPleaChangeScenarios() {
        //checks that dates-to-avoid is pending submission when NOT_GUILTY plea submitted
        updatePleaToNotGuiltyAndConfirm(tflCaseBuilder.getId(), tflCaseBuilder.getOffenceId(), tflCaseBuilder.getDefendantBuilder().getId());
//        verifyDatesToAvoidRequiredEvent(tflCaseBuilder.getId());
        assertThatDatesToAvoidIsPendingSubmissionForCase(tflUserId, tflCaseBuilder);

        //checks that dates-to-avoid NOT pending submission when plea is cancelled
        cancelPlea(tflCaseBuilder.getId(), tflCaseBuilder.getOffenceId(), tflCaseBuilder.getDefendantBuilder().getId());
        //verifyPleaCancelled(tflCaseBuilder.getId(), tflCaseBuilder.getOffenceId(), tflCaseBuilder.getDefendantBuilder().getId());
        assertThatNumberOfCasesPendingDatesToAvoidIsAccurate(tflUserId, tflInitialPendingDatesToAvoidCount);

        //checks that dates-to-avoid is pending submission again when plea updated to NOT_GUILTY
        updatePleaToNotGuiltyAndConfirm(tflCaseBuilder.getId(), tflCaseBuilder.getOffenceId(), tflCaseBuilder.getDefendantBuilder().getId());
        assertThatDatesToAvoidIsPendingSubmissionForCase(tflUserId, tflCaseBuilder);
    }

    private void completeCase(CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder) {
        saveDefaultDecision(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceIds());
    }

    private void updatePleaToNotGuiltyAndConfirm(final UUID caseId, final UUID offenceId, final UUID defendantId) {
        requestSetPleasAndConfirm(caseId, true, false,
                true, null,
                false, null, singletonList(Triple.of(offenceId, defendantId, NOT_GUILTY)));
    }

    private void cancelPlea(final UUID caseId, final UUID offenceId, final UUID defendantId) {
        requestSetPleasAndConfirm(caseId, false, false,
                false, null,
                false, null, singletonList(Triple.of(offenceId, defendantId, null)));
    }

    private void assertThatDatesToAvoidIsPendingSubmissionForCase(final UUID userId, final CreateCase.CreateCasePayloadBuilder aCase) {
        final Matcher<? super ReadContext> pendingDatesToAvoidMatcher =
                withJsonPath("$.cases[-1].caseId", equalTo(aCase.getId().toString()));

        final JsonPath path = pollUntilPendingDatesToAvoidIsOk(userId.toString(), pendingDatesToAvoidMatcher);
        final List<Map> pendingDatesToAvoid = path.getList("cases");
        final Integer datesToAvoidCount = path.get("count");
        final Map map = pendingDatesToAvoid.get(pendingDatesToAvoid.size() - 1);

        assertEquals(aCase.getId().toString(), map.get("caseId"));
        assertWithinSeconds(ZonedDateTimes.fromString(map.get("pleaEntry").toString()).toEpochSecond(), 150);
        assertEquals(aCase.getDefendantBuilder().getFirstName(), map.get("firstName"));
        assertEquals(aCase.getDefendantBuilder().getLastName(), map.get("lastName"));
        assertEquals(aCase.getDefendantBuilder().getAddressBuilder().getAddress1(), ((Map) map.get("address")).get("address1"));
        assertEquals(aCase.getDefendantBuilder().getAddressBuilder().getAddress2(), ((Map) map.get("address")).get("address2"));
        assertEquals(aCase.getDefendantBuilder().getAddressBuilder().getAddress3(), ((Map) map.get("address")).get("address3"));
        assertEquals(aCase.getDefendantBuilder().getAddressBuilder().getAddress4(), ((Map) map.get("address")).get("address4"));
        assertEquals(aCase.getDefendantBuilder().getAddressBuilder().getAddress5(), ((Map) map.get("address")).get("address5"));
        assertEquals(aCase.getDefendantBuilder().getAddressBuilder().getPostcode(), ((Map) map.get("address")).get("postcode"));
        assertEquals(aCase.getUrn(), map.get("referenceNumber"));
        assertEquals(aCase.getDefendantBuilder().getDateOfBirth().toString(), map.get("dateOfBirth"));
        assertThat(pendingDatesToAvoid, hasSize(datesToAvoidCount));
    }

    private int pollForCountOfCasesPendingDatesToAvoid(final UUID userId) {
        return pollUntilPendingDatesToAvoidIsOk(userId.toString(), withJsonPath("$.count"))
                .get("count");
    }

    private void assertThatNumberOfCasesPendingDatesToAvoidIsAccurate(final UUID userId, final int pendingDatesToAvoidCount) {
        pollUntilPendingDatesToAvoidIsOk(userId.toString(), withJsonPath("$.count", equalTo(pendingDatesToAvoidCount)));
    }

    private void assertWithinSeconds(final long value, final long tolerance) {
        assertThat(Math.abs(CLOCK.now().toEpochSecond() - value), lessThan(tolerance));
    }

    private void pollForACaseWith(final String datesToAvoid) {
        pollForCase(tflCaseBuilder.getId(),
                new Matcher[]{withJsonPath("$.datesToAvoid", equalTo(datesToAvoid))});
    }

    private void assertThatDatesToAvoidPublicEventWasRaised(final EventListener listener,
                                                            final String eventName,
                                                            final UUID caseId,
                                                            final String datesToAvoid) {
        final Optional<JsonEnvelope> datesToAvoidPublicEvent = listener.popEvent(eventName);

        assertThat(datesToAvoidPublicEvent.isPresent(), is(true));

        assertThat(datesToAvoidPublicEvent.get(),
                jsonEnvelope(
                        metadata().withName(eventName),
                        payload().isJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.datesToAvoid", equalTo(datesToAvoid))
                        ))));
    }

    private CreateCase.CreateCasePayloadBuilder createCaseBuilder(final ProsecutingAuthority prosecutingAuthority, final int dayOfMonth) {
        final CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        createCasePayloadBuilder.withProsecutingAuthority(prosecutingAuthority);
        //will make it first on the list for assignment (as earlier than default posting date)
        createCasePayloadBuilder.withPostingDate(LocalDate.of(2000, 12, dayOfMonth));

        return createCasePayloadBuilder;
    }
}
