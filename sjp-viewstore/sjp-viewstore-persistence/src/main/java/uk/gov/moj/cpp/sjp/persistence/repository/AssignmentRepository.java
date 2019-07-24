package uk.gov.moj.cpp.sjp.persistence.repository;


import static com.google.common.collect.Sets.newHashSet;
import static java.util.stream.Collectors.toSet;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PIA;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.WITHDRAWAL_REQUESTED;

import uk.gov.moj.cpp.sjp.domain.AssignmentCandidate;
import uk.gov.moj.cpp.sjp.domain.AssignmentRuleType;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.EntityManager;

public class AssignmentRepository {

    @Inject
    private EntityManager em;

    private static final String ASSIGNMENTS_CANDIDATES_QUERY =
            "SELECT " +
                    "   rc.case_id," +
                    "   s.position AS case_stream_version" +
                    " FROM ready_cases rc" +
                    "   JOIN case_details c ON rc.case_id = c.id AND c.completed = FALSE" +
                    "   JOIN stream_status s ON s.stream_id = rc.case_id AND s.source = 'sjp' AND s.component = 'EVENT_LISTENER'" +
                    " WHERE (rc.assignee_id IS NULL OR rc.assignee_id = :assigneeId)" +
                    "   AND rc.reason IN(:reasons)" +
                    "   AND" +
                    "     CASE " +
                    "       WHEN :allowPersecutors = '" + AssignmentRuleType.ALLOW + "' THEN c.prosecuting_authority IN :prosecutingAuthorities" +
                    "       WHEN :allowPersecutors = '" + AssignmentRuleType.DISALLOW + "' THEN c.prosecuting_authority NOT IN :prosecutingAuthorities" +
                    "     END" +
                    " ORDER BY" +
                    "   rc.assignee_id NULLS LAST," +
                    "   CASE" +
                    "     WHEN rc.reason = '" + WITHDRAWAL_REQUESTED + "' THEN 1" +
                    //    when any PLEADED -> sort them between WITHDRAWN and PIA
                    "     WHEN rc.reason = '" + PIA + "' THEN 9" +
                    "     ELSE 2" +
                    "   END," +
                    "   c.posting_date ASC" +
                    " LIMIT :limit";

    public List<AssignmentCandidate> getAssignmentCandidatesForMagistrateSession(final UUID assigneeId, final Set<String> prosecutingAuthorities, final AssignmentRuleType assignmentRule, int limit) {
        return getAssignmentCandidates(
                newHashSet(PLEADED_GUILTY, PIA),
                assignmentRule, assigneeId, prosecutingAuthorities, limit);
    }

    public List<AssignmentCandidate> getAssignmentCandidatesForDelegatedPowersSession(final UUID assigneeId, final Set<String> prosecutingAuthorities, final AssignmentRuleType assignmentRule, int limit) {
        return getAssignmentCandidates(
                newHashSet(PLEADED_GUILTY_REQUEST_HEARING, PLEADED_NOT_GUILTY, WITHDRAWAL_REQUESTED),
                assignmentRule, assigneeId, prosecutingAuthorities, limit);
    }

    @SuppressWarnings("unchecked")
    private List<AssignmentCandidate> getAssignmentCandidates(final Set<CaseReadinessReason> reasons, final AssignmentRuleType assignmentRule, final UUID assigneeId, final Set<String> prosecutingAuthorities, int limit) {
        return em.createNativeQuery(ASSIGNMENTS_CANDIDATES_QUERY, CaseDetail.RESULT_SET_MAPPING_ASSIGNMENT_CANDIDATES)
                .setParameter("allowPersecutors", assignmentRule.name())
                .setParameter("assigneeId", assigneeId)
                .setParameter("prosecutingAuthorities", prosecutingAuthorities)
                .setParameter("reasons", reasons.stream().map(Enum::name).collect(toSet()))
                .setParameter("limit", limit)
                .getResultList();
    }

}
