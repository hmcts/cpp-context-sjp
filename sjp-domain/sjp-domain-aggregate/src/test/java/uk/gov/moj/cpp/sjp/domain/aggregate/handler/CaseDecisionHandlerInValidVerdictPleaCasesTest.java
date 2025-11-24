package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.time.ZonedDateTime.now;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.ADJOURN;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.REFER_FOR_COURT_HEARING;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds.disabilityNeedsOf;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.justice.core.courts.DelegatedPowers;
import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtInterpreter;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.aggregate.Session;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.decision.Adjourn;
import uk.gov.moj.cpp.sjp.domain.decision.Decision;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.ReferForCourtHearing;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.decision.DecisionRejected;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runners.Parameterized;

public class CaseDecisionHandlerInValidVerdictPleaCasesTest {

    private static final UUID offenceId1 = randomUUID();
    private final UUID referralReasonId = randomUUID();
    private final UUID decisionId = randomUUID();
    private final UUID sessionId = randomUUID();
    private final UUID caseId = randomUUID();
    private final UUID legalAdviserId = randomUUID();
    private final UUID defendantId = randomUUID();
    private final ZonedDateTime savedAt = now();
    private final String note = "wrongly convicted";
    private final User legalAdviser = new User("John", "Smith", legalAdviserId);
    private final String courtHouseCode = "1008";
    private final String courtHouseName = "Test court";
    private final String localJusticeAreaNationalCode = "1009";
    private final Optional<DelegatedPowers> legalAdviserMagistrate = Optional.of(DelegatedPowers.delegatedPowers().withFirstName("Erica").withLastName("Wilson").withUserId(randomUUID()).build());

    static Stream<Arguments> testData() {
        return Stream.of(
                Arguments.of(SessionType.DELEGATED_POWERS, null, REFER_FOR_COURT_HEARING, PROVED_SJP,
                        newArrayList("For Delegated Power session only NO_VERDICT is allowed, REFER_FOR_COURT_HEARING decision can not be saved for offence " + offenceId1)),

                Arguments.of(SessionType.DELEGATED_POWERS, PleaType.NOT_GUILTY, REFER_FOR_COURT_HEARING, PROVED_SJP,
                        newArrayList("For Delegated Power session only NO_VERDICT is allowed, REFER_FOR_COURT_HEARING decision can not be saved for offence " + offenceId1,
                                "Offence with Plea can not have verdict as PROVED_SJP, REFER_FOR_COURT_HEARING decision can not be saved for offence " + offenceId1,
                                "Not Guilty plea should have no verdict, REFER_FOR_COURT_HEARING decision can not be saved for offence " + offenceId1)),

                Arguments.of(SessionType.DELEGATED_POWERS, PleaType.NOT_GUILTY, REFER_FOR_COURT_HEARING, FOUND_GUILTY,
                        newArrayList("For Delegated Power session only NO_VERDICT is allowed, REFER_FOR_COURT_HEARING decision can not be saved for offence " + offenceId1,
                                "Not Guilty plea should have no verdict, REFER_FOR_COURT_HEARING decision can not be saved for offence " + offenceId1)),

                Arguments.of(SessionType.DELEGATED_POWERS, PleaType.NOT_GUILTY, REFER_FOR_COURT_HEARING, FOUND_NOT_GUILTY,
                        newArrayList("For Delegated Power session only NO_VERDICT is allowed, REFER_FOR_COURT_HEARING decision can not be saved for offence " + offenceId1,
                                "Not Guilty plea should have no verdict, REFER_FOR_COURT_HEARING decision can not be saved for offence " + offenceId1)),

                Arguments.of(SessionType.DELEGATED_POWERS, PleaType.GUILTY, REFER_FOR_COURT_HEARING, PROVED_SJP,
                        newArrayList("For Delegated Power session only NO_VERDICT is allowed, REFER_FOR_COURT_HEARING decision can not be saved for offence " + offenceId1,
                                "Offence with Plea can not have verdict as PROVED_SJP, REFER_FOR_COURT_HEARING decision can not be saved for offence " + offenceId1,
                                "Guilty plea should have Found Guilty Verdict, REFER_FOR_COURT_HEARING decision can not be saved for offence " + offenceId1)),

                Arguments.of(SessionType.DELEGATED_POWERS, PleaType.GUILTY, REFER_FOR_COURT_HEARING, FOUND_GUILTY,
                        newArrayList("For Delegated Power session only NO_VERDICT is allowed, REFER_FOR_COURT_HEARING decision can not be saved for offence " + offenceId1)),

                Arguments.of(SessionType.DELEGATED_POWERS, PleaType.GUILTY, REFER_FOR_COURT_HEARING, FOUND_NOT_GUILTY,
                        newArrayList("For Delegated Power session only NO_VERDICT is allowed, REFER_FOR_COURT_HEARING decision can not be saved for offence " + offenceId1,
                                "Guilty plea should have Found Guilty Verdict, REFER_FOR_COURT_HEARING decision can not be saved for offence " + offenceId1)),

                Arguments.of(SessionType.DELEGATED_POWERS, PleaType.GUILTY_REQUEST_HEARING, REFER_FOR_COURT_HEARING, PROVED_SJP,
                        newArrayList("For Delegated Power session only NO_VERDICT is allowed, REFER_FOR_COURT_HEARING decision can not be saved for offence " + offenceId1,
                                "Offence with Plea can not have verdict as PROVED_SJP, REFER_FOR_COURT_HEARING decision can not be saved for offence " + offenceId1)),

                Arguments.of(SessionType.DELEGATED_POWERS, PleaType.GUILTY_REQUEST_HEARING, REFER_FOR_COURT_HEARING, FOUND_GUILTY,
                        newArrayList("For Delegated Power session only NO_VERDICT is allowed, REFER_FOR_COURT_HEARING decision can not be saved for offence " + offenceId1)),

                Arguments.of(SessionType.DELEGATED_POWERS, PleaType.GUILTY_REQUEST_HEARING, REFER_FOR_COURT_HEARING, FOUND_NOT_GUILTY,
                        newArrayList("For Delegated Power session only NO_VERDICT is allowed, REFER_FOR_COURT_HEARING decision can not be saved for offence " + offenceId1)),

                Arguments.of(SessionType.MAGISTRATE, PleaType.NOT_GUILTY, REFER_FOR_COURT_HEARING, PROVED_SJP,
                        newArrayList("For Magistrate Session, NOT GUILTY cannot be with refer to court hearing post conviction, REFER_FOR_COURT_HEARING decision can not be saved for offence " + offenceId1,
                                "Offence with Plea can not have verdict as PROVED_SJP, REFER_FOR_COURT_HEARING decision can not be saved for offence " + offenceId1,
                                "Not Guilty plea should have no verdict, REFER_FOR_COURT_HEARING decision can not be saved for offence " + offenceId1)),

                Arguments.of(SessionType.MAGISTRATE, PleaType.GUILTY, REFER_FOR_COURT_HEARING, FOUND_NOT_GUILTY,
                        newArrayList("Guilty plea should have Found Guilty Verdict, REFER_FOR_COURT_HEARING decision can not be saved for offence " + offenceId1)),

                Arguments.of(SessionType.MAGISTRATE, PleaType.GUILTY, REFER_FOR_COURT_HEARING, PROVED_SJP,
                        newArrayList("Offence with Plea can not have verdict as PROVED_SJP, REFER_FOR_COURT_HEARING decision can not be saved for offence " + offenceId1,
                                "Guilty plea should have Found Guilty Verdict, REFER_FOR_COURT_HEARING decision can not be saved for offence " + offenceId1)),

                Arguments.of(SessionType.MAGISTRATE, null, REFER_FOR_COURT_HEARING, FOUND_GUILTY,
                        newArrayList("Offence with No Plea should have verdict as either NO_VERDICT or PROVED_SJP, REFER_FOR_COURT_HEARING decision can not be saved for offence " + offenceId1)),

                Arguments.of(SessionType.MAGISTRATE, null, REFER_FOR_COURT_HEARING, FOUND_NOT_GUILTY,
                        newArrayList("Offence with No Plea should have verdict as either NO_VERDICT or PROVED_SJP, REFER_FOR_COURT_HEARING decision can not be saved for offence " + offenceId1)),

                Arguments.of(SessionType.DELEGATED_POWERS, PleaType.GUILTY, ADJOURN, FOUND_GUILTY,
                        newArrayList("For Delegated Power session only NO_VERDICT is allowed, ADJOURN decision can not be saved for offence " + offenceId1))
        );
    }

    @ParameterizedTest
    @MethodSource("testData")
    public void test(SessionType sessionType, PleaType pleaType, DecisionType decisionType, VerdictType verdictType, List<String> rejectionReason) {
        var caseAggregateState = new CaseAggregateState();
        var session = new Session();
        switch (sessionType) {
            case MAGISTRATE:
                session.startMagistrateSession(sessionId, legalAdviserId, courtHouseCode, courtHouseName,
                        localJusticeAreaNationalCode, now(), "magistrate", legalAdviserMagistrate, Arrays.asList("TFL", "DVL"));
                break;
            case DELEGATED_POWERS:
                session.startDelegatedPowersSession(sessionId, legalAdviserId, courtHouseCode, courtHouseName,
                        localJusticeAreaNationalCode, now(), Arrays.asList("TFL", "DVL"));
                break;
        }
        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1), legalAdviserId, caseAggregateState);
        var courtOptions =
                new DefendantCourtOptions(
                        new DefendantCourtInterpreter("EN", true),
                        false,
                        disabilityNeedsOf("disability needs"));

        final List<OffenceDecision> offenceDecisions = createOffenceDecisions(decisionType, verdictType, courtOptions);
        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, offenceDecisions, null, null);
        caseAggregateState.setPleas(Arrays.asList(new Plea(null, offenceId1, pleaType)));

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);

        thenTheDecisionIsRejected(decision, rejectionReason, eventStream);
    }

    private ArrayList<OffenceDecision> createOffenceDecisions(DecisionType decisionType, VerdictType verdictType, DefendantCourtOptions courtOptions) {
        switch (decisionType) {
            case ADJOURN:
                final LocalDate adjournedTo = LocalDate.now().plusDays(10);
                final String adjournmentReason = "Not enough documents for decision";
                return newArrayList(new Adjourn(randomUUID(), asList(
                        createOffenceDecisionInformation(offenceId1, verdictType)),
                        adjournmentReason, adjournedTo));
            case REFER_FOR_COURT_HEARING:
                return newArrayList(
                        new ReferForCourtHearing(UUID.randomUUID(),
                                newArrayList(createOffenceDecisionInformation(offenceId1, verdictType)),
                                referralReasonId, "note", 0, courtOptions, null));
            default:
                throw new NotImplementedException();
        }
    }

    private void givenCaseExistsWithMultipleOffences(final HashSet<UUID> uuids, final UUID savedByUser, CaseAggregateState caseAggregateState) {
        caseAggregateState.addOffenceIdsForDefendant(defendantId, uuids);
        caseAggregateState.setDefendantId(defendantId);
        caseAggregateState.setAssigneeId(savedByUser);
    }

    private void thenTheDecisionIsRejected(final Decision decision, final List<String> expectedRejectionReasons, final Stream<Object> eventStream) {
        final List<Object> eventList = eventStream.collect(toList());
        assertThat(eventList.size(), is(1));
        assertThat(eventList, hasItem(new DecisionRejected(decision, expectedRejectionReasons)));
    }
}
