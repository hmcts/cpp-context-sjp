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
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubAllReferenceData;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubResultIds;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.MarkedAsLegalSocChecked;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.processor.CaseLegalSocCheckedProcessor;
import uk.gov.moj.sjp.it.helper.DecisionHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;
import uk.gov.moj.sjp.it.util.builders.DismissBuilder;

import java.time.LocalDate;
import java.util.UUID;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MarkCaseAsLeagalSocCheckedIT extends BaseIntegrationTest {

    private final User user = new User("John", "Smith", USER_ID);
    private UUID sessionId = randomUUID();
    private UUID caseId = randomUUID();
    private UUID offenceId = randomUUID();
    private LocalDate postingDate = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);
    private final EventListener eventListener = new EventListener();

    @BeforeEach
    public void setUp() throws Exception {

        new SjpDatabaseCleaner().cleanViewStore();

        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubForUserDetails(user, "ALL");
        stubAllReferenceData();
        CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));

        stubResultIds();
        createCase(caseId, asList(offenceId), postingDate);
        dismissCase();
    }

    @Test
    public void shouldMarkAsLegalSocChecked() {
        eventListener
                .subscribe(MarkedAsLegalSocChecked.EVENT_NAME)
                .subscribe(CaseLegalSocCheckedProcessor.CASE_LEGAL_SOC_CHECKED_PUBLIC_EVENT_NAME)
                .run(() -> markSocCheck());

        final MarkedAsLegalSocChecked markedAsLegalSocChecked = eventListener.popEventPayload(MarkedAsLegalSocChecked.class);

        assertThat(markedAsLegalSocChecked, is(notNullValue()));
    }

    public void markSocCheck() {

        final String path = format("/cases/%s", caseId);
        final String mediaType = "application/vnd.sjp.mark-as-legal-soc-checked+json";

        makePostCall(USER_ID, path, mediaType, null, ACCEPTED);
    }

    private static void startSessionAndRequestAssignment(final UUID sessionId, final SessionType sessionType) {
        startSession(sessionId, USER_ID, DEFAULT_LONDON_COURT_HOUSE_OU_CODE, sessionType).get();
        requestCaseAssignment(sessionId, USER_ID);

    }

    private void dismissCase() {
        startSessionAndRequestAssignment(sessionId, MAGISTRATE);

        final OffenceDecision offenceDecision = DismissBuilder.withDefaults(offenceId).build();
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, asList(offenceDecision), null);

        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CaseCompleted.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decision));

        final DecisionSaved decisionSaved = eventListener.popEventPayload(DecisionSaved.class);
        assertThat(decisionSaved, is(notNullValue()));
    }
}
