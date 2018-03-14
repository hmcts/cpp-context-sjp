package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.allOf;
import static org.junit.Assert.assertEquals;
import static uk.gov.moj.sjp.it.EventSelector.EVENT_SELECTOR_PLEA_CANCELLED;
import static uk.gov.moj.sjp.it.EventSelector.PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED;
import static uk.gov.moj.sjp.it.pollingquery.PendingDatesToAvoidPoller.*;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetEmptyAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.ResultingStub.stubGetCaseDecisionsWithNoDecision;

import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CancelPleaHelper;
import uk.gov.moj.sjp.it.helper.CompleteCaseHelper;
import uk.gov.moj.sjp.it.helper.SessionHelper;
import uk.gov.moj.sjp.it.helper.UpdatePleaHelper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DatesToAvoidIT extends BaseIntegrationTest {

    static final String PLEA_NOT_GUILTY = "NOT_GUILTY";
    private static final String LONDON_COURT = "2572";

    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;
    private SessionHelper sessionHelper;
    private CompleteCaseHelper completeCaseHelper;

    @Before
    public void setUp() {
        this.createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        //this will make it first on the list for assignment (as earlier than default posting date)
        this.createCasePayloadBuilder.withPostingDate(LocalDate.of(2000, 12, 1));

        CreateCase.createCaseForPayloadBuilder(this.createCasePayloadBuilder);
        stubGetCaseDecisionsWithNoDecision(createCasePayloadBuilder.getId());

        sessionHelper = new SessionHelper();
        completeCaseHelper = new CompleteCaseHelper(createCasePayloadBuilder.getId());
    }

    @After
    public void close() throws Exception {
        sessionHelper.close();
        completeCaseHelper.close();
    }

    @Test
    public void shouldCaseBePendingDatesToAvoidForAllUpdateAndInSessionScenarios() {
        stubGetEmptyAssignmentsByDomainObjectId(createCasePayloadBuilder.getId());
        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId());
             final CancelPleaHelper cancelPleaHelper = new CancelPleaHelper(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId(),
                     EVENT_SELECTOR_PLEA_CANCELLED, PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED);
        ) {
            //checks that dates-to-avoid is pending submission when NOT_GUILTY plea submitted
            updatePleaToNotGuilty(updatePleaHelper);
            int noOfCasesPendingDatesToAvoidSubmission = assertThatDatesToAvoidIsPendingSubmissionForThisCase();

            //checks that dates-to-avoid NOT pending submission when plea is cancelled
            cancelPleaHelper.cancelPlea();
            cancelPleaHelper.verifyPleaCancelled();
            assertThatNumberOfCasesPendingDatesToAvoidIsAccurate(noOfCasesPendingDatesToAvoidSubmission-1);

            //checks that dates-to-avoid is pending submission again when plea updated to NOT_GUILTY
            updatePleaToNotGuilty(updatePleaHelper);
            noOfCasesPendingDatesToAvoidSubmission = assertThatDatesToAvoidIsPendingSubmissionForThisCase();

            //checks that dates-to-avoid NOT pending submission when case in session
            sessionHelper.startSession(randomUUID(), randomUUID(), LONDON_COURT, Optional.empty());
            assertThatNumberOfCasesPendingDatesToAvoidIsAccurate(noOfCasesPendingDatesToAvoidSubmission-1);
        }
    }

    @Test
    public void shouldCaseBePendingDatesToAvoidForResultedCaseScenario() {
        stubGetEmptyAssignmentsByDomainObjectId(createCasePayloadBuilder.getId());
        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId());
             final CancelPleaHelper cancelPleaHelper = new CancelPleaHelper(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId(),
                     EVENT_SELECTOR_PLEA_CANCELLED, PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED);
        ) {
            //checks that dates-to-avoid is pending submission when NOT_GUILTY plea submitted
            updatePleaToNotGuilty(updatePleaHelper);
            int noOfCasesPendingDatesToAvoidSubmission = assertThatDatesToAvoidIsPendingSubmissionForThisCase();

            //checks that dates-to-avoid NOT pending submission when case completed
            completeCaseHelper.completeCase();
            assertThatNumberOfCasesPendingDatesToAvoidIsAccurate(noOfCasesPendingDatesToAvoidSubmission-1);
        }
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

    private int assertThatDatesToAvoidIsPendingSubmissionForThisCase() {
        final Matcher pendingDatesToAvoidMatcher = allOf(
                withJsonPath("$.pendingDatesToAvoid[0].caseId")
        );
        final JsonPath path = pollUntilPendingDatesToAvoidIsOk(pendingDatesToAvoidMatcher);
        final List<Map> pendingDatesToAvoid = path.getList("pendingDatesToAvoid");
        final Integer datesToAvoidCount = path.get("pendingDatesToAvoidCount");
        final Map map = pendingDatesToAvoid.get(pendingDatesToAvoid.size()-1);

        assertEquals(createCasePayloadBuilder.getId().toString(), map.get("caseId"));
        assertEquals(new Integer(pendingDatesToAvoid.size()), datesToAvoidCount);

        return pendingDatesToAvoid.size();
    }

    private void assertThatNumberOfCasesPendingDatesToAvoidIsAccurate(int noOfCasesPendingDatesToAvoid) {
        final Matcher pendingDatesToAvoidMatcher = allOf(
                withJsonPath("$.pendingDatesToAvoidCount", equalTo(noOfCasesPendingDatesToAvoid))
        );
        pollUntilPendingDatesToAvoidIsOk(pendingDatesToAvoidMatcher);
    }
}
