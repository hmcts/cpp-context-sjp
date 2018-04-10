package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.sjp.it.EventSelector.EVENT_SELECTOR_PLEA_CANCELLED;
import static uk.gov.moj.sjp.it.EventSelector.PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED;
import static uk.gov.moj.sjp.it.pollingquery.PendingDatesToAvoidPoller.pollUntilPendingDatesToAvoidIsOk;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetEmptyAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.ResultingStub.stubGetCaseDecisionsWithNoDecision;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubGroupForUser;

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

    static final String PLEA_NOT_GUILTY = "NOT_GUILTY";
    private static final String LONDON_COURT_HOUSE_OU_CODE = "B01GU";
    private static final String LONDON_COURT_HOUSE_LJA_NATIONAL_COURT_CODE = "2577";

    private UUID tflUserId;
    private UUID tvlUserId;
    private CreateCase.CreateCasePayloadBuilder tflCaseBuilder;
    private CreateCase.CreateCasePayloadBuilder tvlCaseBuilder;
    private int tflInitialPendingDatesToAvoidCount = 0;
    private int tvlInitialPendingDatesToAvoidCount = 0;

    @Before
    public void setUp() {
        tflUserId = UUID.randomUUID();
        this.tflCaseBuilder = createCase(ProsecutingAuthority.TFL, 1);
        stubGroupForUser(tflUserId.toString(), UsersGroupsStub.SJP_PROSECUTORS);
        stubForUserDetails(tflUserId.toString(), ProsecutingAuthority.TFL.toString());

        tvlUserId = UUID.randomUUID();
        this.tvlCaseBuilder = createCase(ProsecutingAuthority.TVL, 2);
        stubGroupForUser(tvlUserId.toString(), UsersGroupsStub.SJP_PROSECUTORS);
        stubForUserDetails(tvlUserId.toString(), ProsecutingAuthority.TVL.toString());

        tflInitialPendingDatesToAvoidCount = pollForPendingDatesToAvoidCount(tflUserId);
        tvlInitialPendingDatesToAvoidCount = pollForPendingDatesToAvoidCount(tvlUserId);

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
        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper(tflCaseBuilder.getId(), tflCaseBuilder.getOffenceId());
             final CancelPleaHelper cancelPleaHelper = new CancelPleaHelper(tflCaseBuilder.getId(), tflCaseBuilder.getOffenceId(),
                     EVENT_SELECTOR_PLEA_CANCELLED, PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED);
        ) {
            //checks that dates-to-avoid is pending submission when NOT_GUILTY plea submitted
            updatePleaToNotGuilty(updatePleaHelper);
            assertThatDatesToAvoidIsPendingSubmissionForCase(tflUserId, tflCaseBuilder.getId());

            //checks that dates-to-avoid NOT pending submission when plea is cancelled
            cancelPleaHelper.cancelPlea();
            cancelPleaHelper.verifyPleaCancelled();
            assertThatNumberOfCasesPendingDatesToAvoidIsAccurate(tflUserId, tflInitialPendingDatesToAvoidCount);

            //checks that dates-to-avoid is pending submission again when plea updated to NOT_GUILTY
            updatePleaToNotGuilty(updatePleaHelper);
            assertThatDatesToAvoidIsPendingSubmissionForCase(tflUserId, tflCaseBuilder.getId());

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
        try (final UpdatePleaHelper tflUpdatePleaHelper = new UpdatePleaHelper(tflCaseBuilder.getId(), tflCaseBuilder.getOffenceId());
             final UpdatePleaHelper tvlUpdatePleaHelper = new UpdatePleaHelper(tvlCaseBuilder.getId(), tvlCaseBuilder.getOffenceId());
        ) {
            //updates plea to NOT_GUILTY for TFL case (making it pending dates-to-avoid submission)
            updatePleaToNotGuilty(tflUpdatePleaHelper);

            //check tvl count not gone up (as a result of tfl plea update above)
            assertEquals(tvlInitialPendingDatesToAvoidCount, pollForPendingDatesToAvoidCount(tvlUserId));

            //updates plea to NOT_GUILTY for TVL case (making it pending dates-to-avoid submission)
            updatePleaToNotGuilty(tvlUpdatePleaHelper);

            //checks that correct dates-to-avoid record is retrieved (i.e. the one related to the case passed in)
            assertThatDatesToAvoidIsPendingSubmissionForCase(userId, createCasePayloadBuilder.getId());

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

    private void updatePleaToNotGuilty(final UpdatePleaHelper updatePleaHelper) {
        String plea = PLEA_NOT_GUILTY;
        final String pleaMethod = "POSTAL";
        updatePleaHelper.updatePlea(getPleaPayload(plea));
        updatePleaHelper.verifyPleaUpdated(plea, pleaMethod);
    }

    private JsonObject getPleaPayload(final String plea) {
        final JsonObjectBuilder builder = createObjectBuilder().add("plea", plea);
        builder.add("interpreterRequired", false);
        return builder.build();
    }

    private int assertThatDatesToAvoidIsPendingSubmissionForCase(final UUID userId, final UUID caseId) {
        final Matcher pendingDatesToAvoidMatcher = allOf(
                withJsonPath("$.pendingDatesToAvoid[0].caseId")
        );
        final JsonPath path = pollUntilPendingDatesToAvoidIsOk(userId.toString(), pendingDatesToAvoidMatcher);
        final List<Map> pendingDatesToAvoid = path.getList("pendingDatesToAvoid");
        final Integer datesToAvoidCount = path.get("pendingDatesToAvoidCount");
        final Map map = pendingDatesToAvoid.get(pendingDatesToAvoid.size() - 1);

        assertEquals(caseId.toString(), map.get("caseId"));
        assertEquals(new Integer(pendingDatesToAvoid.size()), datesToAvoidCount);

        return pendingDatesToAvoid.size();
    }

    private int pollForPendingDatesToAvoidCount(final UUID userId) {
        final Matcher pendingDatesToAvoidMatcher = allOf(
                withJsonPath("$.pendingDatesToAvoidCount")
        );
        final JsonPath path = pollUntilPendingDatesToAvoidIsOk(userId.toString(), pendingDatesToAvoidMatcher);
        return path.get("pendingDatesToAvoidCount");
    }

    private void assertThatNumberOfCasesPendingDatesToAvoidIsAccurate(final UUID userId, final int pendingDatesToAvoidCount) {
        final Matcher pendingDatesToAvoidMatcher = allOf(
                withJsonPath("$.pendingDatesToAvoidCount", equalTo(pendingDatesToAvoidCount))
        );
        pollUntilPendingDatesToAvoidIsOk(userId.toString(), pendingDatesToAvoidMatcher);
    }
}
