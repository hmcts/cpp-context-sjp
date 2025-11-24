package uk.gov.moj.sjp.it.test;

import static java.lang.String.format;
import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.sjp.it.Constants.NOTICE_PERIOD_IN_DAYS;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.createCase;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.saveDecision;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSessionAndConfirm;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseStatusCompleted;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;
import static uk.gov.moj.sjp.it.util.SjpDatabaseCleaner.cleanViewStore;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.event.processor.CaseLegalSocCheckedProcessor;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.util.builders.DismissBuilder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MarkCaseAsLegalSocCheckedIT extends BaseIntegrationTest {

    private final User user = new User("John", "Smith", USER_ID);
    private UUID sessionId = randomUUID();
    private UUID caseId = randomUUID();
    private UUID offenceId = randomUUID();
    private LocalDate postingDate = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);
    private final EventListener eventListener = new EventListener();

    @BeforeEach
    public void setUp() throws Exception {

        cleanViewStore();

        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubForUserDetails(user, "ALL");
        provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));

        createCase(caseId, asList(offenceId), postingDate);
        dismissCase();
    }

    @Test
    void shouldMarkAsLegalSocChecked() {
        eventListener
                .subscribe(CaseLegalSocCheckedProcessor.CASE_LEGAL_SOC_CHECKED_PUBLIC_EVENT_NAME)
                .run(() -> markSocCheck());

        final Optional<JsonEnvelope> publicEvent = eventListener.popEvent("public.sjp.case-legal-soc-checked");

        assertThat(publicEvent.isPresent(), is(true));
        assertThat(publicEvent.get().payloadAsJsonObject().getString("checkedBy"), is(notNullValue()));
        assertThat(publicEvent.get().payloadAsJsonObject().getString("caseId"), is(caseId.toString()));
        assertThat(publicEvent.get().payloadAsJsonObject().getString("checkedAt"), is(notNullValue()));
    }

    public void markSocCheck() {

        final String path = format("/cases/%s", caseId);
        final String mediaType = "application/vnd.sjp.mark-as-legal-soc-checked+json";

        makePostCall(USER_ID, path, mediaType, null, ACCEPTED);
    }

    private static void startSessionAndRequestAssignment(final UUID sessionId, final SessionType sessionType) {
        startSessionAndConfirm(sessionId, USER_ID, DEFAULT_LONDON_COURT_HOUSE_OU_CODE, sessionType);
        requestCaseAssignment(sessionId, USER_ID);
    }

    private void dismissCase() {
        startSessionAndRequestAssignment(sessionId, MAGISTRATE);

        final OffenceDecision offenceDecision = DismissBuilder.withDefaults(offenceId).build();
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, asList(offenceDecision), null);

        saveDecision(decision);

        pollUntilCaseStatusCompleted(caseId);
    }
}
