package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.sjp.it.EventSelector.EVENT_SELECTOR_PLEA_CANCELLED;
import static uk.gov.moj.sjp.it.EventSelector.PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED;
import static uk.gov.moj.sjp.it.pollingquery.PendingDatesToAvoidPoller.pollUntilPendingDatesToAvoidIsOk;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetEmptyAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.ResultingStub.stubGetCaseDecisionsWithNoDecision;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubGroupForUser;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.AssignmentHelper;
import uk.gov.moj.sjp.it.helper.CancelPleaHelper;
import uk.gov.moj.sjp.it.helper.CompleteCaseHelper;
import uk.gov.moj.sjp.it.helper.SessionHelper;
import uk.gov.moj.sjp.it.helper.UpdatePleaHelper;
import uk.gov.moj.sjp.it.stub.ReferenceDataStub;
import uk.gov.moj.sjp.it.stub.UsersGroupsStub;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

public class DatesToAvoidIT extends BaseIntegrationTest {

    private static final String PLEA_NOT_GUILTY = "NOT_GUILTY";
    private static final String LONDON_COURT_HOUSE_OU_CODE = "B01GU";
    private static final String LONDON_COURT_HOUSE_LJA_NATIONAL_COURT_CODE = "2577";
    private static final Clock clock = new UtcClock();

    private UUID tflUserId;
    private UUID tvlUserId;
    private CreateCase.CreateCasePayloadBuilder tflCaseBuilder;
    private CreateCase.CreateCasePayloadBuilder tvlCaseBuilder;
    private int tflInitialPendingDatesToAvoidCount = 0;
    private int tvlInitialPendingDatesToAvoidCount = 0;

    @Before
    public void setUp() {
        tflUserId = UUID.randomUUID();
        stubGroupForUser(tflUserId.toString(), UsersGroupsStub.SJP_PROSECUTORS_GROUP);
        stubForUserDetails(tflUserId.toString(), ProsecutingAuthority.TFL.toString());
        this.tflCaseBuilder = createCase(ProsecutingAuthority.TFL, 1);
        this.tflInitialPendingDatesToAvoidCount = pollForPendingDatesToAvoidCount(tflUserId);

        tvlUserId = UUID.randomUUID();
        this.tvlCaseBuilder = createCase(ProsecutingAuthority.TVL, 2);
        stubGroupForUser(tvlUserId.toString(), UsersGroupsStub.SJP_PROSECUTORS_GROUP);
        stubForUserDetails(tvlUserId.toString(), ProsecutingAuthority.TVL.toString());
        this.tvlInitialPendingDatesToAvoidCount = pollForPendingDatesToAvoidCount(tvlUserId);

        ReferenceDataStub.stubCourtByCourtHouseOUCodeQuery(LONDON_COURT_HOUSE_OU_CODE, LONDON_COURT_HOUSE_LJA_NATIONAL_COURT_CODE);
    }

    private CreateCase.CreateCasePayloadBuilder createCase(ProsecutingAuthority prosecutingAuthority, int dayOfMonth) {
        CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        createCasePayloadBuilder.withProsecutingAuthority(prosecutingAuthority);
        //will make it first on the list for assignment (as earlier than default posting date)
        createCasePayloadBuilder.withPostingDate(LocalDate.of(2000, 12, dayOfMonth));

        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);
        stubGetCaseDecisionsWithNoDecision(createCasePayloadBuilder.getId());

        return createCasePayloadBuilder;
    }

    @Test
    public void shouldCaseBePendingDatesToAvoidForAllUpdateAndInSessionScenarios() {
        stubGetEmptyAssignmentsByDomainObjectId(tflCaseBuilder.getId());
        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper();
             final CancelPleaHelper cancelPleaHelper = new CancelPleaHelper(tflCaseBuilder.getId(), tflCaseBuilder.getOffenceId(),
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
            SessionHelper.startSession(sessionId, userId, LONDON_COURT_HOUSE_OU_CODE, DELEGATED_POWERS);
            AssignmentHelper.requestCaseAssignment(sessionId, userId, LONDON_COURT_HOUSE_LJA_NATIONAL_COURT_CODE, DELEGATED_POWERS);
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

    private void shouldCaseBePendingDatesToAvoidForResultedCaseScenario(UUID userId, CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder, int expectedPendingDatesToAvoidCount) {
        stubGetEmptyAssignmentsByDomainObjectId(tflCaseBuilder.getId());
        stubGetEmptyAssignmentsByDomainObjectId(tvlCaseBuilder.getId());
        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper();
        ) {
            //
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
    }

    private void completeCase(CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder) {
        CompleteCaseHelper completeCaseHelper = new CompleteCaseHelper(createCasePayloadBuilder.getId());
        completeCaseHelper.completeCase();
        completeCaseHelper.close();
    }

    private void updatePleaToNotGuilty(final UUID caseId, final UUID offenceId, final UpdatePleaHelper updatePleaHelper) {
        String plea = PLEA_NOT_GUILTY;
        final String pleaMethod = "POSTAL";
        updatePleaHelper.updatePlea(caseId, offenceId, getPleaPayload(plea));
        updatePleaHelper.verifyPleaUpdated(caseId, plea, pleaMethod);
    }

    private JsonObject getPleaPayload(final String plea) {
        final JsonObjectBuilder builder = createObjectBuilder().add("plea", plea);
        builder.add("interpreterRequired", false);
        return builder.build();
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
        assertWithinSeconds(ZonedDateTimes.fromString(map.get("pleaEntry").toString()).toEpochSecond(), 30);
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
        final Matcher pendingDatesToAvoidMatcher = allOf(
                withJsonPath("$.count")
        );
        final JsonPath path = pollUntilPendingDatesToAvoidIsOk(userId.toString(), pendingDatesToAvoidMatcher);
        return path.get("count");
    }

    private void assertThatNumberOfCasesPendingDatesToAvoidIsAccurate(final UUID userId, final int pendingDatesToAvoidCount) {
        final Matcher pendingDatesToAvoidMatcher = allOf(
                withJsonPath("$.count", equalTo(pendingDatesToAvoidCount))
        );
        pollUntilPendingDatesToAvoidIsOk(userId.toString(), pendingDatesToAvoidMatcher);
    }

    private void assertWithinSeconds(final long value, final long tolerance) {
        assertEquals(clock.now().toEpochSecond(), value, tolerance);
    }
}
