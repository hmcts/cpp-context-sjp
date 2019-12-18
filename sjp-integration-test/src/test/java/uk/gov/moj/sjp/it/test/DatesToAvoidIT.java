package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.json.schemas.fragments.sjp.WithdrawalRequestsStatus.withdrawalRequestsStatus;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TFL;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TVL;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.PLEA_RECEIVED_NOT_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.sjp.it.Constants.EVENT_SELECTOR_DATES_TO_AVOID_ADDED;
import static uk.gov.moj.sjp.it.command.AddDatesToAvoid.addDatesToAvoid;
import static uk.gov.moj.sjp.it.helper.DatesToAvoidHelper.makeDatesToAvoidExpired;
import static uk.gov.moj.sjp.it.helper.DatesToAvoidHelper.verifyDatesToAvoidExpiredEventEmitted;
import static uk.gov.moj.sjp.it.helper.DatesToAvoidHelper.verifyDatesToAvoidRequiredEventEmitted;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.saveDefaultDecision;
import static uk.gov.moj.sjp.it.helper.SetPleasHelper.requestSetPleas;
import static uk.gov.moj.sjp.it.helper.SetPleasHelper.verifyCaseStatus;
import static uk.gov.moj.sjp.it.helper.SetPleasHelper.verifyPleaCancelledEventEmitted;
import static uk.gov.moj.sjp.it.helper.SetPleasHelper.verifyPleadedNotGuiltyEventEmitted;
import static uk.gov.moj.sjp.it.helper.SetPleasHelper.verifySetPleasEventEmitted;
import static uk.gov.moj.sjp.it.pollingquery.PendingDatesToAvoidPoller.pollUntilPendingDatesToAvoidIsOk;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubAssignmentReplicationCommands;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.SJP_PROSECUTORS_GROUP;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubGroupForUser;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidRequired;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.PleadedNotGuilty;
import uk.gov.moj.cpp.sjp.event.PleasSet;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestHelper;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;
import uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.ReadContext;
import com.jayway.restassured.path.json.JsonPath;
import org.apache.commons.lang3.tuple.Triple;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;

public class DatesToAvoidIT extends BaseIntegrationTest {

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

    private final SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();

    @Before
    public void setUp() throws Exception {
        databaseCleaner.cleanAll();
        tflUserId = randomUUID();
        tvlUserId = randomUUID();

        stubAssignmentReplicationCommands();
        stubGroupForUser(tflUserId, SJP_PROSECUTORS_GROUP);
        stubForUserDetails(tflUserId, TFL);
        this.tflCaseBuilder = createCase(TFL, 1);
        this.tflInitialPendingDatesToAvoidCount = pollForCountOfCasesPendingDatesToAvoid(tflUserId);

        this.tvlCaseBuilder = createCase(TVL, 2);
        stubGroupForUser(tvlUserId, SJP_PROSECUTORS_GROUP);
        stubForUserDetails(tvlUserId, TVL);
        this.tvlInitialPendingDatesToAvoidCount = pollForCountOfCasesPendingDatesToAvoid(tvlUserId);

        ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery();
        eventListener = new EventListener();
    }

    private static CreateCase.CreateCasePayloadBuilder createCase(final ProsecutingAuthority prosecutingAuthority, final int dayOfMonth) {
        final CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        createCasePayloadBuilder.withProsecutingAuthority(prosecutingAuthority);
        //will make it first on the list for assignment (as earlier than default posting date)
        createCasePayloadBuilder.withPostingDate(LocalDate.of(2000, 12, dayOfMonth));

        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);

        return createCasePayloadBuilder;
    }

    @Test
    public void shouldCaseBePendingDatesToAvoidForPleaChangeScenariosAndBeResultedAfterDatesToAvoid() {
        shouldCaseBePendingDatesToAvoidForPleaChangeScenarios();

        addDatesToAvoid(tflCaseBuilder.getId(), DATE_TO_AVOID);
        assertThatNumberOfCasesPendingDatesToAvoidIsAccurate(tflUserId, tflInitialPendingDatesToAvoidCount);

        completeCase(tflCaseBuilder);

        assertThatNumberOfCasesPendingDatesToAvoidIsAccurate(tflUserId, tflInitialPendingDatesToAvoidCount);
    }

    @Test
    public void shouldCaseBePendingDatesToAvoidForPleaChangeScenariosAndBeReadyAfterDatesToAvoidExpiry() {
        shouldCaseBePendingDatesToAvoidForPleaChangeScenarios();
        verifyCaseStatus(tflCaseBuilder.getId(), PLEA_RECEIVED_NOT_READY_FOR_DECISION);
        makeDatesToAvoidExpired(eventListener, tflCaseBuilder.getId());
        verifyDatesToAvoidExpiredEventEmitted(eventListener, tflCaseBuilder.getId());
        verifyCaseStatus(tflCaseBuilder.getId(), PLEA_RECEIVED_READY_FOR_DECISION);
    }

    @Test
    public void shouldCaseBePendingDatesToAvoidForResultedCaseScenarioForTflUser() {
        shouldCaseBePendingDatesToAvoidForResultedCaseScenario(tflUserId, tflCaseBuilder, tflInitialPendingDatesToAvoidCount);
    }

    @Test
    public void shouldCaseBePendingDatesToAvoidForResultedCaseScenarioForTvlUser() {
        shouldCaseBePendingDatesToAvoidForResultedCaseScenario(tvlUserId, tvlCaseBuilder, tvlInitialPendingDatesToAvoidCount);
    }

    @Test
    public void shouldAddDatesToAvoidToCase() {
        //given
        final String DATES_TO_AVOID_ADDED_PUBLIC_EVENT_NAME = "public.sjp.dates-to-avoid-added";

        final UUID caseId = tflCaseBuilder.getId();

        updatePleaToNotGuiltyAndConfirmPleasSet(caseId, tflCaseBuilder.getOffenceId(), tflCaseBuilder.getDefendantBuilder().getId());
        verifyPleadedNotGuiltyEvent(tflCaseBuilder.getId(), tflCaseBuilder.getOffenceId(), tflCaseBuilder.getDefendantBuilder().getId());

        final EventListener datesToAvoidAddedListener = new EventListener()
                .subscribe(DATES_TO_AVOID_ADDED_PUBLIC_EVENT_NAME)
                //when
                .run(() -> addDatesToAvoid(caseId, DATE_TO_AVOID))
                //then
                .run(() -> assertThatCaseHasDatesToAvoid(DATE_TO_AVOID));

        assertThatDatesToAvoidPublicEventWasRaised(datesToAvoidAddedListener,
                DATES_TO_AVOID_ADDED_PUBLIC_EVENT_NAME, caseId, DATE_TO_AVOID);
    }

    @Test
    public void shouldUpdateDatesToAvoidOnCase() {
        //given
        final String DATES_TO_AVOID_UPDATED_PUBLIC_EVENT_NAME = "public.sjp.dates-to-avoid-updated";

        final UUID caseId = tflCaseBuilder.getId();

        new EventListener()
                .subscribe(EVENT_SELECTOR_DATES_TO_AVOID_ADDED)
                .run(() -> addDatesToAvoid(caseId, DATE_TO_AVOID))
                .popEvent(EVENT_SELECTOR_DATES_TO_AVOID_ADDED);

        //then
        EventListener datesToAvoidListener = new EventListener()
                .subscribe(DATES_TO_AVOID_UPDATED_PUBLIC_EVENT_NAME)
                //when
                .run(() -> addDatesToAvoid(caseId, DATE_TO_AVOID_UPDATE))
                //then
                .run(() -> assertThatCaseHasDatesToAvoidUpdated(DATE_TO_AVOID_UPDATE));

        assertThatDatesToAvoidPublicEventWasRaised(datesToAvoidListener,
                DATES_TO_AVOID_UPDATED_PUBLIC_EVENT_NAME, caseId, DATE_TO_AVOID_UPDATE);
    }

    private void shouldCaseBePendingDatesToAvoidForResultedCaseScenario(UUID userId, CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder, int expectedPendingDatesToAvoidCount) {

        //updates plea to NOT_GUILTY for TFL case (making it pending dates-to-avoid submission)
        updatePleaToNotGuiltyAndConfirmPleasSet(tflCaseBuilder.getId(), tflCaseBuilder.getOffenceId(), tflCaseBuilder.getDefendantBuilder().getId());
        verifyPleadedNotGuiltyEvent(tflCaseBuilder.getId(), tflCaseBuilder.getOffenceId(), tflCaseBuilder.getDefendantBuilder().getId());

        //check tvl count not gone up (as a result of tfl plea update above)
        assertEquals("Ensure that access control is ON", tvlInitialPendingDatesToAvoidCount, pollForCountOfCasesPendingDatesToAvoid(tvlUserId));

        //updates plea to NOT_GUILTY for TVL case (making it pending dates-to-avoid submission)
        updatePleaToNotGuiltyAndConfirmPleasSet(tvlCaseBuilder.getId(), tvlCaseBuilder.getOffenceId(), tvlCaseBuilder.getDefendantBuilder().getId());
        verifyPleadedNotGuiltyEvent(tvlCaseBuilder.getId(), tvlCaseBuilder.getOffenceId(), tvlCaseBuilder.getDefendantBuilder().getId());

        //checks that correct dates-to-avoid record is retrieved (i.e. the one related to the case passed in)
        assertThatDatesToAvoidIsPendingSubmissionForCase(userId, createCasePayloadBuilder);

        OffencesWithdrawalRequestHelper.requestWithdrawalOfOffences(createCasePayloadBuilder.getId(), userId, asList(withdrawalRequestsStatus()
                .withOffenceId(createCasePayloadBuilder.getOffenceId())
                .withWithdrawalRequestReasonId(randomUUID())
                .build()));

        //checks that dates-to-avoid NOT pending submission when case completed
        completeCase(createCasePayloadBuilder);
        assertThatNumberOfCasesPendingDatesToAvoidIsAccurate(userId, expectedPendingDatesToAvoidCount);
    }

    private void shouldCaseBePendingDatesToAvoidForPleaChangeScenarios() {
        //checks that dates-to-avoid is pending submission when NOT_GUILTY plea submitted
        updatePleaToNotGuiltyAndConfirmPleasSet(tflCaseBuilder.getId(), tflCaseBuilder.getOffenceId(), tflCaseBuilder.getDefendantBuilder().getId());
        verifyPleadedNotGuiltyEvent(tflCaseBuilder.getId(), tflCaseBuilder.getOffenceId(), tflCaseBuilder.getDefendantBuilder().getId());
        verifyDatesToAvoidRequiredEvent(tflCaseBuilder.getId());
        assertThatDatesToAvoidIsPendingSubmissionForCase(tflUserId, tflCaseBuilder);

        //checks that dates-to-avoid NOT pending submission when plea is cancelled
        cancelPlea(tflCaseBuilder.getId(), tflCaseBuilder.getOffenceId(), tflCaseBuilder.getDefendantBuilder().getId());
        verifyPleaCancelled(tflCaseBuilder.getId(), tflCaseBuilder.getOffenceId(), tflCaseBuilder.getDefendantBuilder().getId());
        assertThatNumberOfCasesPendingDatesToAvoidIsAccurate(tflUserId, tflInitialPendingDatesToAvoidCount);

        //checks that dates-to-avoid is pending submission again when plea updated to NOT_GUILTY
        updatePleaToNotGuiltyAndConfirmPleasSet(tflCaseBuilder.getId(), tflCaseBuilder.getOffenceId(), tflCaseBuilder.getDefendantBuilder().getId());
        assertThatDatesToAvoidIsPendingSubmissionForCase(tflUserId, tflCaseBuilder);
    }

    private void completeCase(CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder) {
        saveDefaultDecision(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceIds());
    }

    private void updatePleaToNotGuiltyAndConfirmPleasSet(final UUID caseId, final UUID offenceId, final UUID defendantId) {
        requestSetPleas(caseId, eventListener, true, false,
                true, null,
                false, singletonList(Triple.of(offenceId, defendantId, NOT_GUILTY)),
                PleasSet.EVENT_NAME, PleadedNotGuilty.EVENT_NAME, DatesToAvoidRequired.EVENT_NAME);

        final Map pleaTypeByOffence = ImmutableMap.of(offenceId, NOT_GUILTY);
        verifySetPleasEventEmitted(eventListener, caseId, defendantId, pleaTypeByOffence);
    }

    private void cancelPlea(final UUID caseId, final UUID offenceId, final UUID defendantId) {
        requestSetPleas(caseId, eventListener, false, false,
                false, null,
                false, singletonList(Triple.of(offenceId, defendantId, null)),
                PleasSet.EVENT_NAME, PleaCancelled.EVENT_NAME);
        final Map<UUID, PleaType> pleaTypeByOffence = new HashMap<>();
        verifySetPleasEventEmitted(eventListener, caseId, defendantId, pleaTypeByOffence);
    }

    private void verifyPleadedNotGuiltyEvent(final UUID caseId, final UUID offenceId, final UUID defendantId) {
        verifyPleadedNotGuiltyEventEmitted(eventListener, caseId, defendantId, offenceId);
    }

    private void verifyDatesToAvoidRequiredEvent(final UUID caseId) {
        verifyDatesToAvoidRequiredEventEmitted(eventListener, caseId);
    }

    private void verifyPleaCancelled(final UUID caseId, final UUID offenceId, final UUID defendantId) {
        verifyPleaCancelledEventEmitted(eventListener, caseId, defendantId, offenceId);
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

    private void assertThatCaseHasDatesToAvoid(final String datesToAvoid) {
        pollForACaseWith(datesToAvoid);
    }

    private void assertThatCaseHasDatesToAvoidUpdated(final String datesToAvoidUpdate) {
        pollForACaseWith(datesToAvoidUpdate);
    }

    private void pollForACaseWith(final String datesToAvoid) {
        CasePoller.pollUntilCaseByIdIsOk(tflCaseBuilder.getId(),
                withJsonPath("$.datesToAvoid", equalTo(datesToAvoid)));
    }

    private void assertThatDatesToAvoidPublicEventWasRaised(final EventListener listener,
                                                            final String eventName,
                                                            final UUID caseId,
                                                            final String datesToAvoid) {
        final Optional<JsonEnvelope> datesToAvoidPublicEvent = listener
                .popEvent(eventName);

        assertThat(datesToAvoidPublicEvent.isPresent(), is(true));

        MatcherAssert.assertThat(datesToAvoidPublicEvent.get(),
                jsonEnvelope(
                        metadata().withName(eventName),
                        payload().isJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.datesToAvoid", equalTo(datesToAvoid))
                        ))));
    }
}
