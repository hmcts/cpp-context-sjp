package uk.gov.moj.cpp.sjp.persistence.repository;


import uk.gov.moj.cpp.sjp.domain.AssignmentCandidate;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.EntityManager;

public class AssignmentRepository {

    @Inject
    private EntityManager em;

    private static final String MAGISTRATE_SESSION_QUERY =
            "SELECT c.id as case_id, s.version as case_stream_version FROM case_details c" +
                    " JOIN defendant d ON c.id = d.case_id" +
                    " JOIN offence o ON d.id = o.defendant_id" +
                    " JOIN stream_status s ON s.stream_id = c.id" +
                    " WHERE c.completed is false" +
                    "  AND (c.assignee_id IS NULL OR c.assignee_id = :assigneeId)" +
                    "  AND c.prosecuting_authority NOT IN :excludedProsecutingAuthorities" +
                    "  AND o.pending_withdrawal is not true" +
                    "  AND (o.plea = 'GUILTY' OR (o.plea IS NULL AND c.posting_date <= current_date - 28))" +
                    " ORDER BY c.assignee_id NULLS LAST, o.plea NULLS LAST, c.posting_date ASC" +
                    " LIMIT :limit";

    private static final String DELEGATED_POWERS_SESSION_QUERY =
            "SELECT c.id as case_id, s.version as case_stream_version FROM case_details c" +
                    " JOIN defendant d ON c.id = d.case_id" +
                    " JOIN offence o ON d.id = o.defendant_id" +
                    " JOIN stream_status s ON s.stream_id = c.id" +
                    " LEFT OUTER JOIN pending_dates_to_avoid pda ON pda.case_id = c.id" +
                    " WHERE c.completed IS false" +
                    "  AND (c.assignee_id IS NULL OR c.assignee_id = :assigneeId)" +
                    "  AND c.prosecuting_authority NOT IN :excludedProsecutingAuthorities" +
                    "  AND (" +
                    "    o.pending_withdrawal IS true " +
                    "    OR o.plea = 'GUILTY_REQUEST_HEARING' " +
                    "    OR (o.plea = 'NOT_GUILTY' AND (pda.plea_date is NULL OR pda.plea_date <= current_date - 10))" +
                    "  ) " +
                    " ORDER BY c.assignee_id NULLS LAST, o.pending_withdrawal DESC NULLS LAST, c.posting_date ASC" +
                    " LIMIT :limit";

    public List<AssignmentCandidate> getAssignmentCandidatesForMagistrateSession(final UUID assigneeId, final Set<String> excludedProsecutingAuthorities, int limit) {
        return em.createNativeQuery(MAGISTRATE_SESSION_QUERY, "assignmentCandidates")
                .setParameter("assigneeId", assigneeId)
                .setParameter("excludedProsecutingAuthorities", excludedProsecutingAuthorities)
                .setParameter("limit", limit)
                .getResultList();
    }

    public List<AssignmentCandidate> getAssignmentCandidatesForDelegatedPowersSession(final UUID assigneeId, final Set<String> excludedProsecutingAuthorities, int limit) {
        return em.createNativeQuery(DELEGATED_POWERS_SESSION_QUERY, "assignmentCandidates")
                .setParameter("assigneeId", assigneeId)
                .setParameter("excludedProsecutingAuthorities", excludedProsecutingAuthorities)
                .setParameter("limit", limit)
                .getResultList();
    }

}
