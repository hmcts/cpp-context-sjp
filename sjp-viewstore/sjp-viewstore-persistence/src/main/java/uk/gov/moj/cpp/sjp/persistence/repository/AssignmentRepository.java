package uk.gov.moj.cpp.sjp.persistence.repository;


import com.google.common.collect.Lists;
import uk.gov.moj.cpp.sjp.domain.AssignmentCandidate;
import uk.gov.moj.cpp.sjp.domain.SessionType;
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
                    "   JOIN stream_status s ON s.stream_id = rc.case_id AND s.source = 'sjp' AND s.component = 'EVENT_LISTENER'" +
                    " WHERE (rc.assignee_id IS NULL OR rc.assignee_id = :assigneeId)" +
                    "   AND rc.session_type = :sessionType" +
                    "   AND" +
                    "     rc.prosecuting_authority IN :prosecutingAuthorities" +
                    " ORDER BY" +
                    "   rc.assignee_id NULLS LAST," +
                    "   rc.priority ASC, " +
                    "   rc.posting_date ASC" +
                    " LIMIT :limit";


    public List<AssignmentCandidate> getAssignmentCandidatesForMagistrateSession(final UUID assigneeId, final Set<String> prosecutingAuthorities, int limit) {
        return getAssignmentCandidates(
                SessionType.MAGISTRATE, // Pleaded guilty or PIA
                assigneeId, prosecutingAuthorities, limit);
    }

    public List<AssignmentCandidate> getAssignmentCandidatesForDelegatedPowersSession(final UUID assigneeId, final Set<String> prosecutingAuthorities, int limit) {
        return getAssignmentCandidates(
                SessionType.DELEGATED_POWERS, assigneeId, prosecutingAuthorities, limit);
    }

    @SuppressWarnings("unchecked")
    private List<AssignmentCandidate> getAssignmentCandidates(final SessionType sessionType, final UUID assigneeId, final Set<String> prosecutingAuthorities, int limit) {
        if (prosecutingAuthorities.isEmpty()) {
            return Lists.newArrayList();
        }

        return em.createNativeQuery(ASSIGNMENTS_CANDIDATES_QUERY, CaseDetail.RESULT_SET_MAPPING_ASSIGNMENT_CANDIDATES)
                .setParameter("assigneeId", assigneeId)
                .setParameter("prosecutingAuthorities", prosecutingAuthorities)
                .setParameter("sessionType", sessionType.name())
                .setParameter("limit", limit)
                .getResultList();
    }

}
