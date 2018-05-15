package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import com.jayway.restassured.path.json.JsonPath;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import javax.json.JsonObject;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import org.hamcrest.Matcher;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;

import org.hamcrest.MatcherAssert;
import org.junit.After;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;

import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TFL;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TVL;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import static uk.gov.moj.sjp.it.Constants.EVENT_SELECTOR_PLEA_CANCELLED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED;
import static uk.gov.moj.sjp.it.command.AddDatesToAvoid.addDatesToAvoid;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.AssignmentHelper;
import uk.gov.moj.sjp.it.helper.CancelPleaHelper;
import uk.gov.moj.sjp.it.helper.EventedListener;
import uk.gov.moj.sjp.it.helper.SessionHelper;
import uk.gov.moj.sjp.it.helper.UpdatePleaHelper;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;

import static uk.gov.moj.sjp.it.pollingquery.PendingDatesToAvoidPoller.pollUntilPendingDatesToAvoidIsOk;
import uk.gov.moj.sjp.it.producer.CompleteCaseProducer;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetEmptyAssignmentsByDomainObjectId;
import uk.gov.moj.sjp.it.stub.ReferenceDataStub;
import static uk.gov.moj.sjp.it.stub.ResultingStub.stubGetCaseDecisionsWithNoDecision;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.*;

public class DatesToAvoidIT extends BaseIntegrationTest {

    private static final String LONDON_COURT_HOUSE_OU_CODE = "B01GU";
    private static final String LONDON_COURT_HOUSE_LJA_NATIONAL_COURT_CODE = "2577";
    private static final Clock CLOCK = new UtcClock();
    private static final String DATE_TO_AVOID = "a-date-to-avoid";
    private static final String DATE_TO_AVOID_UPDATE = "I cannot come on Thursday";

    private UUID tflUserId;
    private UUID tvlUserId;
    private CreateCase.CreateCasePayloadBuilder tflCaseBuilder;
    private CreateCase.CreateCasePayloadBuilder tvlCaseBuilder;
    private int tflInitialPendingDatesToAvoidCount;
    private int tvlInitialPendingDatesToAvoidCount;
    private UpdatePleaHelper updatePleaHelper;

    @Before
    public void setUp() {
        tflUserId = randomUUID();
        stubGroupForUser(tflUserId, SJP_PROSECUTORS_GROUP);
        stubForUserDetails(tflUserId, TFL);
        this.tflCaseBuilder = createCase(TFL, 1);
        this.tflInitialPendingDatesToAvoidCount = pollForPendingDatesToAvoidCount(tflUserId);

        tvlUserId = randomUUID();
        this.tvlCaseBuilder = createCase(TVL, 2);
        stubGroupForUser(tvlUserId, SJP_PROSECUTORS_GROUP);
        stubForUserDetails(tvlUserId, TVL);
        this.tvlInitialPendingDatesToAvoidCount = pollForPendingDatesToAvoidCount(tvlUserId);

        ReferenceDataStub.stubCourtByCourtHouseOUCodeQuery(LONDON_COURT_HOUSE_OU_CODE, LONDON_COURT_HOUSE_LJA_NATIONAL_COURT_CODE);
        updatePleaHelper = new UpdatePleaHelper();
    }

    @After
    public void after() {
        updatePleaHelper.close();
    }

    private static CreateCase.CreateCasePayloadBuilder createCase(final ProsecutingAuthority prosecutingAuthority, final int dayOfMonth) {
        final CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        createCasePayloadBuilder.withProsecutingAuthority(prosecutingAuthority);
        //will make it first on the list for assignment (as earlier than default posting date)
        createCasePayloadBuilder.withPostingDate(LocalDate.of(2000, 12, dayOfMonth));

        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);
        stubGetCaseDecisionsWithNoDecision(createCasePayloadBuilder.getId());

        return createCasePayloadBuilder;
    }

    @Test
    public void shouldCaseBePendingDatesToAvoidForPleaChangeScenarios() {
        stubGetEmptyAssignmentsByDomainObjectId(tflCaseBuilder.getId());
        try (final CancelPleaHelper cancelPleaHelper = new CancelPleaHelper(tflCaseBuilder.getId(), tflCaseBuilder.getOffenceId(),
                     EVENT_SELECTOR_PLEA_CANCELLED, PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED);
        ) {
            //checks that dates-to-avoid is pending submission when NOT_GUILTY plea submitted
            updatePleaToNotGuilty(tflCaseBuilder.getId(), tflCaseBuilder.getOffenceId(), updatePleaHelper);
            assertThatDatesToAvoidIsPendingSubmissionForCase(tflUserId, tflCaseBuilder);

            //checks that dates-to-avoid NOT pending submission when plea is cancelled
            cancelPleaHelper.cancelPlea();
            cancelPleaHelper.verifyPleaCancelled();
            assertThatNumberOfCasesPendingDatesToAvoidIsAccurate(tflUserId, tflInitialPendingDatesToAvoidCount);

            //checks that dates-to-avoid is pending submission again when plea updated to NOT_GUILTY
            updatePleaToNotGuilty(tflCaseBuilder.getId(), tflCaseBuilder.getOffenceId(), updatePleaHelper);
            assertThatDatesToAvoidIsPendingSubmissionForCase(tflUserId, tflCaseBuilder);

            UUID sessionId = randomUUID();
            UUID userId = randomUUID();

            //checks that dates-to-avoid NOT pending submission when case in session
            addDatesToAvoid(tflCaseBuilder.getId(), DATE_TO_AVOID);
            assertThatNumberOfCasesPendingDatesToAvoidIsAccurate(tflUserId, tflInitialPendingDatesToAvoidCount);

            SessionHelper.startSession(sessionId, userId, LONDON_COURT_HOUSE_OU_CODE, DELEGATED_POWERS);
            AssignmentHelper.requestCaseAssignment(sessionId, userId);
            assertThatNumberOfCasesPendingDatesToAvoidIsAccurate(tflUserId, tflInitialPendingDatesToAvoidCount);
        }
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
        stubGetEmptyAssignmentsByDomainObjectId(tflCaseBuilder.getId());

        //when
        addDatesToAvoid(caseId, DATE_TO_AVOID);

        //then
        EventedListener datesToAvoidAddedListener = new EventedListener()
                .subscribe(DATES_TO_AVOID_ADDED_PUBLIC_EVENT_NAME)
                .run(() -> assertThatCaseHasDatesToAvoid(DATE_TO_AVOID));

        assertThatDatesToAvoidPublicEventWasRaised(datesToAvoidAddedListener,
                DATES_TO_AVOID_ADDED_PUBLIC_EVENT_NAME, caseId, DATE_TO_AVOID);
    }

    @Test
    public void shouldUpdateDatesToAvoidOnCase() {
        //given
        final String DATES_TO_AVOID_UPDATED_PUBLIC_EVENT_NAME = "public.sjp.dates-to-avoid-updated";

        final UUID caseId = tflCaseBuilder.getId();

        stubGetEmptyAssignmentsByDomainObjectId(caseId);
        addDatesToAvoid(caseId, DATE_TO_AVOID);

        //when
        addDatesToAvoid(caseId, DATE_TO_AVOID_UPDATE);

        //then
        EventedListener datesToAvoidListener = new EventedListener()
                .subscribe(DATES_TO_AVOID_UPDATED_PUBLIC_EVENT_NAME)
                .run(() -> assertThatCaseHasDatesToAvoidUpdated(DATE_TO_AVOID_UPDATE));

        assertThatDatesToAvoidPublicEventWasRaised(datesToAvoidListener,
                DATES_TO_AVOID_UPDATED_PUBLIC_EVENT_NAME, caseId, DATE_TO_AVOID_UPDATE);
    }

    private void shouldCaseBePendingDatesToAvoidForResultedCaseScenario(UUID userId, CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder, int expectedPendingDatesToAvoidCount) {
        stubGetEmptyAssignmentsByDomainObjectId(tflCaseBuilder.getId());
        stubGetEmptyAssignmentsByDomainObjectId(tvlCaseBuilder.getId());

        //updates plea to NOT_GUILTY for TFL case (making it pending dates-to-avoid submission)
        updatePleaToNotGuilty(tflCaseBuilder.getId(), tflCaseBuilder.getOffenceId(), updatePleaHelper);

        //check tvl count not gone up (as a result of tfl plea update above)
        assertEquals("Ensure that access control is ON", tvlInitialPendingDatesToAvoidCount, pollForPendingDatesToAvoidCount(tvlUserId));

        //updates plea to NOT_GUILTY for TVL case (making it pending dates-to-avoid submission)
        updatePleaToNotGuilty(tvlCaseBuilder.getId(), tvlCaseBuilder.getOffenceId(), updatePleaHelper);

        //checks that correct dates-to-avoid record is retrieved (i.e. the one related to the case passed in)
        assertThatDatesToAvoidIsPendingSubmissionForCase(userId, createCasePayloadBuilder);

        //checks that dates-to-avoid NOT pending submission when case completed
        completeCase(createCasePayloadBuilder);
        assertThatNumberOfCasesPendingDatesToAvoidIsAccurate(userId, expectedPendingDatesToAvoidCount);
    }

    private void completeCase(CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder) {
        final CompleteCaseProducer completeCaseProducer = new CompleteCaseProducer(createCasePayloadBuilder.getId());
        completeCaseProducer.completeCase();
    }

    private static void updatePleaToNotGuilty(final UUID caseId, final UUID offenceId, final UpdatePleaHelper updatePleaHelper) {
        final PleaType pleaType = PleaType.NOT_GUILTY;
        final PleaMethod pleaMethod = PleaMethod.POSTAL;
        updatePleaHelper.updatePlea(caseId, offenceId, getPleaPayload(pleaType));
        updatePleaHelper.verifyPleaUpdated(caseId, pleaType, pleaMethod);
    }

    private static JsonObject getPleaPayload(final PleaType pleaType) {
        return createObjectBuilder()
                .add("plea", pleaType.name())
                .add("interpreterRequired", false)
                .build();
    }

    private void assertThatDatesToAvoidIsPendingSubmissionForCase(final UUID userId, final CreateCase.CreateCasePayloadBuilder aCase) {
        final Matcher pendingDatesToAvoidMatcher = allOf(
                withJsonPath("$.cases[0].caseId"),
                withJsonPath("$.cases[0].pleaEntry"),
                withJsonPath("$.cases[0].firstName"),
                withJsonPath("$.cases[0].lastName"),
                withJsonPath("$.cases[0].address.address1"),
                withJsonPath("$.cases[0].address.address2"),
                withJsonPath("$.cases[0].address.address3"),
                withJsonPath("$.cases[0].address.address4"),
                withJsonPath("$.cases[0].address.postcode"),
                withJsonPath("$.cases[0].referenceNumber"),
                withJsonPath("$.cases[0].dateOfBirth")
        );
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
        assertEquals(aCase.getDefendantBuilder().getAddressBuilder().getPostcode(), ((Map) map.get("address")).get("postcode"));
        assertEquals(aCase.getUrn(), map.get("referenceNumber"));
        assertEquals(aCase.getDefendantBuilder().getDateOfBirth().toString(), map.get("dateOfBirth"));
        assertThat(pendingDatesToAvoid, hasSize(datesToAvoidCount));
    }

    private int pollForPendingDatesToAvoidCount(final UUID userId) {
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

    private void assertThatDatesToAvoidPublicEventWasRaised(final EventedListener listener,
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
