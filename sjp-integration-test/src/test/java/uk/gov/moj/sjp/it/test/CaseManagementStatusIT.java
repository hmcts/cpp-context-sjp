package uk.gov.moj.sjp.it.test;


import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.sjp.domain.common.CaseManagementStatus.DONE;
import static uk.gov.moj.cpp.sjp.domain.common.CaseManagementStatus.IN_PROGRESS;
import static uk.gov.moj.sjp.it.Constants.NOTICE_PERIOD_IN_DAYS;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.createCase;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.casemanagement.CaseManagementStatusChanged;
import uk.gov.moj.cpp.sjp.event.casemanagement.UpdateCasesManagementStatus;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class CaseManagementStatusIT extends BaseIntegrationTest {

    private final UUID case1Id = randomUUID();
    private final UUID case2Id = randomUUID();
    private final UUID offence1Id = randomUUID();
    private final UUID offence2Id = randomUUID();
    private final UUID offence3Id = randomUUID();
    private final UUID offence4Id = randomUUID();
    private final UUID offence5Id = randomUUID();
    private final UUID offence6Id = randomUUID();
    private final LocalDate postingDate = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);
    private final EventListener eventListener = new EventListener();
    private final User user = new User("John", "Smith", USER_ID);
    private final static String PUBLIC_EVENT_CASE_MANAGEMENT_STATUS_UPDATED = "public.sjp.cases-management-status-updated";

    @Before
    public void setUp() throws Exception {
        stubForUserDetails(user, "ALL");
        new SjpDatabaseCleaner().cleanViewStore();
        createCase(case1Id, offence1Id, offence2Id, offence3Id, postingDate);
        createCase(case2Id, offence4Id, offence5Id, offence6Id, postingDate.plusDays(1));
    }

    @Test
    public void shouldUpdateCaseManagementStatus() {
        eventListener
                .subscribe(UpdateCasesManagementStatus.EVENT_NAME)
                .subscribe(CaseManagementStatusChanged.EVENT_NAME)
                .subscribe(PUBLIC_EVENT_CASE_MANAGEMENT_STATUS_UPDATED)
                .run(this::updateCaseManagementStatus);


        final UpdateCasesManagementStatus updateCasesManagementStatus = eventListener.popEventPayload(UpdateCasesManagementStatus.class);
        verifyCaseManagementStatusUpdate(updateCasesManagementStatus);

        final CaseManagementStatusChanged caseManagementStatusChanged = eventListener.popEventPayload(CaseManagementStatusChanged.class);
        verifyCaseManagementStatusChanged(caseManagementStatusChanged);

        final Optional<JsonEnvelope> publicEvent = eventListener.popEvent(PUBLIC_EVENT_CASE_MANAGEMENT_STATUS_UPDATED);
        assertTrue(publicEvent.isPresent());
        assertThat(publicEvent.get().payloadAsJsonObject().getJsonArray("cases").size(), is(2));
    }

    private void verifyCaseManagementStatusChanged(final CaseManagementStatusChanged caseManagementStatusChanged) {
        assertThat(caseManagementStatusChanged.getCaseId(), oneOf(case1Id, case2Id));
        assertThat(caseManagementStatusChanged.getCaseManagementStatus(), oneOf(DONE, IN_PROGRESS));
    }

    private void verifyCaseManagementStatusUpdate(final UpdateCasesManagementStatus updateCasesManagementStatus) {
        assertThat(updateCasesManagementStatus.getCaseByManagementStatusList().get(0).getCaseId(), is(case1Id));
        assertThat(updateCasesManagementStatus.getCaseByManagementStatusList().get(0).getCaseManagementStatus(), is(DONE));
        assertThat(updateCasesManagementStatus.getCaseByManagementStatusList().get(1).getCaseId(), is(case2Id));
        assertThat(updateCasesManagementStatus.getCaseByManagementStatusList().get(1).getCaseManagementStatus(), is(IN_PROGRESS));
    }

    private void updateCaseManagementStatus() {
        final String payload = createObjectBuilder().add("cases", createArrayBuilder()
                .add(createObjectBuilder()
                        .add("caseId", case1Id.toString())
                        .add("caseManagementStatus", DONE.toString()))
                .add(createObjectBuilder()
                        .add("caseId", case2Id.toString())
                        .add("caseManagementStatus", IN_PROGRESS.toString())))
                .build().toString();

        final UUID userId = user.getUserId();
        final String path = "/cases-management-status";
        final String mediaType = "application/vnd.sjp.update-cases-management-status+json";

        makePostCall(userId, path, mediaType, payload, ACCEPTED);
    }
}
