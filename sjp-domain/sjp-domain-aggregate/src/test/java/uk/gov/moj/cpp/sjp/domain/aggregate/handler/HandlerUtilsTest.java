package uk.gov.moj.cpp.sjp.domain.aggregate.handler;


import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.decision.Adjourn;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.decision.Withdraw;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected.RejectReason;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

public class HandlerUtilsTest {

    private static final UUID USER_ID = randomUUID();
    private static final UUID CASE_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID OFFENCE_ID_1 = randomUUID();
    private static final UUID OFFENCE_ID_2 = randomUUID();
    private static final UUID OFFENCE_ID_3 = randomUUID();
    private final CaseAggregateState caseAggregateState = new CaseAggregateState();

    @Before
    public void onceBeforeEachTest() {
        caseAggregateState.setCaseId(CASE_ID);
        caseAggregateState.addOffenceIdsForDefendant(DEFENDANT_ID, newHashSet(OFFENCE_ID_1, OFFENCE_ID_2, OFFENCE_ID_3));
    }

    @Test
    public void updateShouldBeRejectedWhenPleaIsForPostConvictionAdjournment() {

        final List<OffenceDecision> offenceDecisions = Arrays.asList(
                adjournOffenceDecision(OFFENCE_ID_1, PROVED_SJP),
                withdrawOffenceDecision(OFFENCE_ID_2),
                withdrawOffenceDecision(OFFENCE_ID_3)
        );
        final List<Plea> pleas = singletonList(new Plea(DEFENDANT_ID, OFFENCE_ID_1, PleaType.GUILTY));
        caseAggregateState.setPleas(pleas);
        caseAggregateState.updateOffenceDecisions(offenceDecisions, randomUUID());
        caseAggregateState.updateOffenceConvictionDetails(ZonedDateTime.now().plusDays(7), offenceDecisions, null);

        final List<Object> results = HandlerUtils.createRejectionEvents(USER_ID, caseAggregateState, pleas, "Set pleas").get().collect(toList());

        final List<CaseUpdateRejected> expected = singletonList(new CaseUpdateRejected(CASE_ID, RejectReason.OFFENCE_HAS_CONVICTION));
        assertEquals(expected, results);
    }

    @Test
    public void updateShouldBeAllowedWhenPleaIsForPreConvictionAdjournment() {

        final List<OffenceDecision> offenceDecisions = Arrays.asList(
                adjournOffenceDecision(OFFENCE_ID_1, NO_VERDICT),
                withdrawOffenceDecision(OFFENCE_ID_2),
                withdrawOffenceDecision(OFFENCE_ID_3)
        );
        final List<Plea> pleas = singletonList(new Plea(DEFENDANT_ID, OFFENCE_ID_1, PleaType.GUILTY));
        caseAggregateState.setPleas(pleas);
        caseAggregateState.updateOffenceDecisions(offenceDecisions, randomUUID());
        caseAggregateState.updateOffenceConvictionDetails(ZonedDateTime.now().plusDays(7), offenceDecisions, null);

        final Optional<Stream<Object>> results = HandlerUtils.createRejectionEvents(USER_ID, caseAggregateState, pleas, "Set pleas");

        assertFalse(results.isPresent());
    }

    private Withdraw withdrawOffenceDecision(final UUID offenceId) {
        return new Withdraw(null, new OffenceDecisionInformation(offenceId, NO_VERDICT), randomUUID());
    }

    private Adjourn adjournOffenceDecision(final UUID offenceId, final VerdictType verdict) {
        return new Adjourn(null,
                singletonList(new OffenceDecisionInformation(offenceId, verdict)),
                "A good reason",
                LocalDate.now().plusDays(7)
        );
    }
}
