package uk.gov.moj.cpp.sjp.domain.testutils;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.sjp.domain.CaseAssignmentType.MAGISTRATE_DECISION;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.domain.aggregate.Session;
import uk.gov.moj.cpp.sjp.domain.decision.Decision;
import uk.gov.moj.cpp.sjp.domain.decision.Dismiss;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;

import java.util.List;
import java.util.stream.Collectors;

public class AggregateHelper {

    public static void saveDecision(final CaseAggregate caseAggregate, final Case aCase, final Session session, final VerdictType verdictType) {
        final User user = new User("John", "Smith", randomUUID());
        caseAggregate.assignCase(user.getUserId(), now(), MAGISTRATE_DECISION);

        final List<OffenceDecision> offenceDecisions = aCase.getDefendant().getOffences().stream()
                .map(offence -> createOffenceDecisionInformation(offence.getId(), verdictType))
                .map(offenceDecisionInformation -> new Dismiss(randomUUID(), offenceDecisionInformation, null))
                .collect(Collectors.toList());

        caseAggregate.saveDecision(new Decision(randomUUID(), randomUUID(), aCase.getId(), "", now(), user, offenceDecisions, null, null), session);
    }
}
