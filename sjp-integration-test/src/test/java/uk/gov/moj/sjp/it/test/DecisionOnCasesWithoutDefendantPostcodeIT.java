package uk.gov.moj.sjp.it.test;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_NOT_GUILTY;
import static uk.gov.moj.sjp.it.Constants.NOTICE_PERIOD_IN_DAYS;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.createCaseWithoutDefendantPostcode;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyCaseNotReadyInViewStore;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyCaseUnassigned;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyDecisionSaved;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubAssignmentReplicationCommands;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubEndSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubStartSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.decision.Dismiss;
import uk.gov.moj.cpp.sjp.event.CaseUnmarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;
import uk.gov.moj.sjp.it.helper.DecisionHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

public class DecisionOnCasesWithoutDefendantPostcodeIT extends BaseIntegrationTest {

    private final User user = new User("John", "Smith", USER_ID);
    private UUID sessionId = randomUUID();
    private UUID caseId = randomUUID();
    private UUID offence1Id = randomUUID();
    private UUID offence2Id = randomUUID();
    private UUID offence3Id = randomUUID();
    private LocalDate postingDate = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);
    private final EventListener eventListener = new EventListener();

    @Before
    public void setUp() throws Exception {

        new SjpDatabaseCleaner().cleanViewStore();

        stubStartSjpSessionCommand();
        stubEndSjpSessionCommand();
        stubAssignmentReplicationCommands();
        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubForUserDetails(user, "ALL");
        CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));

        createCaseWithoutDefendantPostcode(caseId, offence1Id, offence2Id, offence3Id, postingDate);
    }

    @Test
    public void shouldSaveDecisionForDefendantWithoutPostcode() {

        startSessionAndRequestAssignment(sessionId, MAGISTRATE);

        final Dismiss dismiss1 = new Dismiss(null, createOffenceDecisionInformation(offence1Id, FOUND_NOT_GUILTY));
        final Dismiss dismiss2 = new Dismiss(null, createOffenceDecisionInformation(offence2Id, FOUND_NOT_GUILTY));
        final Dismiss dismiss3 = new Dismiss(null, createOffenceDecisionInformation(offence3Id, FOUND_NOT_GUILTY));

        final List<Dismiss> offencesDecisions = asList(dismiss1, dismiss2, dismiss3);

        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, offencesDecisions, null);

        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CaseUnassigned.EVENT_NAME)
                .subscribe(CaseUnmarkedReadyForDecision.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decision));

        final DecisionSaved decisionSaved = eventListener.popEventPayload(DecisionSaved.class);
        final CaseUnassigned caseUnassigned = eventListener.popEventPayload(CaseUnassigned.class);

        decision.getOffenceDecisions().stream()
                .flatMap(offenceDecision -> offenceDecision.offenceDecisionInformationAsList().stream())
                .forEach(offDcnInfo -> offDcnInfo.setPressRestrictable(false));
        verifyDecisionSaved(decision, decisionSaved);
        verifyCaseUnassigned(caseId, caseUnassigned);

        verifyCaseNotReadyInViewStore(caseId, USER_ID);
    }

    private static void startSessionAndRequestAssignment(final UUID sessionId, final SessionType sessionType) {
        startSession(sessionId, USER_ID, DEFAULT_LONDON_COURT_HOUSE_OU_CODE, sessionType).get();
        requestCaseAssignment(sessionId, USER_ID);
    }

}
