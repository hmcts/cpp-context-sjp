package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.time.ZonedDateTime.now;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.json.schemas.domains.sjp.NoteType.DECISION;
import static uk.gov.justice.json.schemas.domains.sjp.NoteType.LISTING;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.ADJOURN;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.REFER_FOR_COURT_HEARING;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;

import uk.gov.justice.json.schemas.domains.sjp.Note;
import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.json.schemas.domains.sjp.events.CaseNoteAdded;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtInterpreter;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.aggregate.Session;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.decision.Decision;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.decision.ReferForCourtHearing;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class CaseDecisionHandlerValidVerdictPleaCasesTest {

    private final UUID referralReasonId = randomUUID();
    private final UUID decisionId = randomUUID();
    private final UUID sessionId = randomUUID();
    private final UUID caseId = randomUUID();
    private final UUID legalAdviserId = randomUUID();
    private final UUID defendantId = randomUUID();
    private static final UUID offenceId1 = randomUUID();
    private final ZonedDateTime savedAt = now();
    private final String note = "wrongly convicted";
    private final User legalAdviser = new User("John", "Smith", legalAdviserId);
    private DefendantCourtOptions courtOptions;
    private CaseAggregateState caseAggregateState;
    private Session session;
    private final String courtHouseCode = "1008";
    private final String courtHouseName = "Test court";
    private final String localJusticeAreaNationalCode = "1009";


    @Parameterized.Parameter(0)
    public SessionType sessionType;

    @Parameterized.Parameter(1)
    public PleaType pleaType;

    @Parameterized.Parameter(2)
    public DecisionType decisionType;

    @Parameterized.Parameter(3)
    public VerdictType verdictType;

    @Parameterized.Parameters(name = "Validation rules test Session type {0} decisions-pleas {1} verdicts {2}")
    public static Collection<Object[]> testData() {
        return asList(new Object[][]{
                { DELEGATED_POWERS, null, REFER_FOR_COURT_HEARING, NO_VERDICT},
                { DELEGATED_POWERS, PleaType.GUILTY, REFER_FOR_COURT_HEARING, NO_VERDICT},
                { DELEGATED_POWERS, PleaType.GUILTY_REQUEST_HEARING, REFER_FOR_COURT_HEARING, NO_VERDICT},
                { DELEGATED_POWERS, PleaType.NOT_GUILTY, REFER_FOR_COURT_HEARING, NO_VERDICT},
                { MAGISTRATE, null, REFER_FOR_COURT_HEARING, NO_VERDICT},
                { MAGISTRATE, null, REFER_FOR_COURT_HEARING, PROVED_SJP},
                { MAGISTRATE, PleaType.GUILTY, REFER_FOR_COURT_HEARING, NO_VERDICT},
                { MAGISTRATE, PleaType.GUILTY, REFER_FOR_COURT_HEARING, FOUND_GUILTY},
                { DELEGATED_POWERS, null, ADJOURN, NO_VERDICT},
                { DELEGATED_POWERS, PleaType.GUILTY, ADJOURN, NO_VERDICT},
                { DELEGATED_POWERS, PleaType.GUILTY_REQUEST_HEARING, ADJOURN, NO_VERDICT},
                { DELEGATED_POWERS, PleaType.NOT_GUILTY, ADJOURN, NO_VERDICT},
                { MAGISTRATE, null, ADJOURN, NO_VERDICT},
                { MAGISTRATE, null, ADJOURN, PROVED_SJP},
                { MAGISTRATE, PleaType.GUILTY, ADJOURN, NO_VERDICT},
                { MAGISTRATE, PleaType.GUILTY, ADJOURN, FOUND_GUILTY}
        });
    }

    @Before
    public void init() {
        caseAggregateState = new CaseAggregateState();
        session = new Session();
        switch (sessionType){
            case MAGISTRATE:
                session.startMagistrateSession(sessionId, legalAdviserId, courtHouseCode, courtHouseName,
                        localJusticeAreaNationalCode, now(), "magistrate");
                break;
            case DELEGATED_POWERS:
                session.startDelegatedPowersSession(sessionId, legalAdviserId, courtHouseCode, courtHouseName,
                        localJusticeAreaNationalCode, now());
                break;
        }
        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1), legalAdviserId);
        courtOptions =
                new DefendantCourtOptions(
                        new DefendantCourtInterpreter("EN", true),
                        false);
    }

    @Test
    public void test() {

        final List<OffenceDecision> offenceDecisions = newArrayList(new ReferForCourtHearing(randomUUID(), newArrayList(OffenceDecisionInformation.createOffenceDecisionInformation(offenceId1, verdictType)), referralReasonId, "note", 0, courtOptions));

        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, offenceDecisions, null);

        caseAggregateState.setPleas(asList(new Plea(null, offenceId1, pleaType)));

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);
        final List<Object> eventList = eventStream.collect(toList());
        thenTheDecisionIsAcceptedAlongWithCaseReferForCourtHearingRecordedEvent(decision, eventList);
    }

    private void givenCaseExistsWithMultipleOffences(final HashSet<UUID> uuids, final UUID savedByUser) {
        caseAggregateState.addOffenceIdsForDefendant(defendantId, uuids);
        caseAggregateState.setDefendantId(defendantId);
        caseAggregateState.setAssigneeId(savedByUser);
    }

    private void thenTheDecisionIsAcceptedAlongWithCaseReferForCourtHearingRecordedEvent(final Decision decision, final List<Object> eventList) {
        Optional<Object> ds = eventList.stream().filter(obj -> obj instanceof DecisionSaved).findAny();
        assertTrue("DecisionSaved is not raised", ds.isPresent());
        DecisionSaved actualDecisionSaved = ds.map(o -> (DecisionSaved)o).get();
        assertThat(String.format("DecisionSaved has no matching CaseId. Expected : %s Actual : %s", caseId, actualDecisionSaved.getCaseId()),  actualDecisionSaved.getCaseId(), equalTo(caseId));
        assertThat(String.format("DecisionSaved has no matching decisionId. Expected : %s Actual : %s", decisionId, actualDecisionSaved.getDecisionId()), actualDecisionSaved.getDecisionId(), equalTo(decisionId));
        assertThat(String.format("DecisionSaved has no matching sessionId. Expected : %s Actual : %s", sessionId, actualDecisionSaved.getSessionId()), actualDecisionSaved.getSessionId(), equalTo(sessionId));
        assertThat(String.format("DecisionSaved has no matching savedAt. Expected : %s Actual : %s", savedAt, actualDecisionSaved.getSavedAt()), actualDecisionSaved.getSavedAt(), equalTo(savedAt));

        OffenceDecision actualOffenceDecision = actualDecisionSaved.getOffenceDecisions().get(0);
        OffenceDecision expectedOffenceDecision = decision.getOffenceDecisions().get(0);

        List<UUID> actualOffenceIds = actualOffenceDecision.offenceDecisionInformationAsList().stream().map(OffenceDecisionInformation::getOffenceId).collect(Collectors.toList());
        List<UUID> expectedOffenceIds = expectedOffenceDecision.offenceDecisionInformationAsList().stream().map(OffenceDecisionInformation::getOffenceId).collect(Collectors.toList());

        assertThat(String.format("DecisionSaved has no matching offenceIds. Expected : %s Actual : %s", expectedOffenceIds, actualOffenceIds), actualOffenceIds, equalTo(expectedOffenceIds));
        assertThat(String.format("DecisionSaved has no matching decisionType. Expected : %s Actual : %s", expectedOffenceDecision.getType(), actualOffenceDecision.getType()), actualOffenceDecision.getType(), equalTo(expectedOffenceDecision.getType()));

        List<CaseNoteAdded> caseNoteAddedList = eventList.stream().filter(obj -> obj instanceof CaseNoteAdded).map(o -> (CaseNoteAdded)o).collect(toList());

        assertTrue("CaseNoteAdded is not raised", !caseNoteAddedList.isEmpty());

        List<CaseNoteAdded> actualListeningCaseNoteAddedList = caseNoteAddedList.stream().filter(cn -> cn.getNote().getType().equals(LISTING)).collect(toList());
        assertTrue("CaseNoteAdded for Listening is not raised", !actualListeningCaseNoteAddedList.isEmpty());
        assertCaseAddedNote(actualListeningCaseNoteAddedList.get(0));



        List<CaseNoteAdded> actualDecisionCaseNoteAddedList = caseNoteAddedList.stream().filter(cn -> cn.getNote().getType().equals(DECISION)).collect(toList());
        assertTrue("CaseNoteAdded for Decision is not raised", !actualDecisionCaseNoteAddedList.isEmpty());
        assertCaseAddedNote(actualDecisionCaseNoteAddedList.get(0));

        assertThat("CaseUnassigned event is not raised", eventList, hasItem(new CaseUnassigned(caseId)));
        assertThat(String.format("Expected number of events raised to be %d Actual : %d", 7, eventList.size()), eventList.size(), is(7));
    }

    private void assertCaseAddedNote(final CaseNoteAdded actualCaseNoteAdded) {

        assertThat(String.format("CaseNoteAdded has no matching CaseId. Expected : %s Actual : %s", caseId,actualCaseNoteAdded.getCaseId()), actualCaseNoteAdded.getCaseId(), is(caseId));
        assertThat(String.format("CaseNoteAdded has no matching decisionId. Expected : %s Actual : %s", decisionId, actualCaseNoteAdded.getDecisionId()), actualCaseNoteAdded.getDecisionId(), is(decisionId));
        User actualAuthor = actualCaseNoteAdded.getAuthor();
        assertThat(String.format("CaseNoteAdded has no matching Author FirstName. Expected : %s Actual : %s", "John", actualAuthor.getFirstName()), actualAuthor.getFirstName(), is("John"));
        assertThat(String.format("CaseNoteAdded has no matching Author LastName. Expected : %s Actual : %s", "Smith", actualAuthor.getLastName()), actualAuthor.getLastName(), is("Smith"));
        assertThat(String.format("CaseNoteAdded has no matching Author userId. Expected : %s Actual : %s", legalAdviserId, actualAuthor.getUserId()), actualAuthor.getUserId(), is(legalAdviserId));

        Note actualNote = actualCaseNoteAdded.getNote();
        assertThat(String.format("CaseNoteAdded has no matching Note savedAt. Expected : %s Actual : %s", savedAt, actualNote.getAddedAt()), actualNote.getAddedAt(), is(savedAt));
        assertThat(String.format("CaseNoteAdded has no matching Note Id. Expected : %s Actual : %s", savedAt, actualNote.getAddedAt()), actualNote.getId(), is(any(UUID.class)));

        if(actualNote.getType().equals(LISTING)){
            assertThat(String.format("CaseNoteAdded has no matching Note type. Expected : %s Actual : %s", this.note, actualNote.getType()), actualNote.getType(), is(LISTING));
            assertThat(String.format("CaseNoteAdded has no matching Note text. Expected : %s Actual : %s", this.note, actualNote.getText()), actualNote.getText(), is("note"));
        } else if(actualNote.getType().equals(DECISION)){
            assertThat(String.format("CaseNoteAdded has no matching Note type. Expected : %s Actual : %s", this.note, actualNote.getType()), actualNote.getType(), is(DECISION));
            assertThat(String.format("CaseNoteAdded has no matching Note text. Expected : %s Actual : %s", this.note, actualNote.getText()), actualNote.getText(), is(this.note));
        }
    }


}
