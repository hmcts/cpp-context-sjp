package uk.gov.moj.sjp.it.test;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_NOT_GUILTY;
import static uk.gov.moj.sjp.it.Constants.NOTICE_PERIOD_IN_DAYS;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.pollCaseUnassigned;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignmentAndConfirm;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseNotReady;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.createCaseWithoutDefendantPostcode;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.saveDecision;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSessionAndConfirm;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.SjpDatabaseCleaner.cleanViewStore;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.decision.Dismiss;
import uk.gov.moj.sjp.it.model.DecisionCommand;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DecisionOnCasesWithoutDefendantPostcodeIT extends BaseIntegrationTest {

    private final User user = new User("John", "Smith", USER_ID);
    private UUID sessionId = randomUUID();
    private UUID caseId = randomUUID();
    private UUID offence1Id = randomUUID();
    private UUID offence2Id = randomUUID();
    private UUID offence3Id = randomUUID();
    private LocalDate postingDate = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);

    @BeforeEach
    public void setUp() throws Exception {

        cleanViewStore();

        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubForUserDetails(user, "ALL");
        provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));

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

        saveDecision(decision);
        pollCaseUnassigned(caseId);
        pollUntilCaseNotReady(caseId);
    }

    private void startSessionAndRequestAssignment(final UUID sessionId, final SessionType sessionType) {
        startSessionAndConfirm(sessionId, USER_ID, DEFAULT_LONDON_COURT_HOUSE_OU_CODE, sessionType);
        requestCaseAssignmentAndConfirm(sessionId, USER_ID, caseId);
    }

}
