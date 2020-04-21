package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseAssignmentRestriction;

import java.time.ZonedDateTime;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public abstract class CaseAssignmentRestrictionRepository implements EntityRepository<CaseAssignmentRestriction, String> {

    @Inject
    private EntityManager entityManager;

    private static final String INSERT_STATEMENT =
            "INSERT INTO case_assignment_restriction(prosecuting_authority, include_only, exclude, date_time_created) " +
                    "VALUES (:prosecuting_authority, cast(:include_only as jsonb), cast(:exclude as jsonb), :date_time_created) " +
                    "ON CONFLICT (prosecuting_authority) " +
                    "DO UPDATE " +
                    "SET include_only = cast(:include_only as jsonb), exclude = cast(:exclude as jsonb), date_time_created = :date_time_created ";

    private static final String SELECT_CANDIDATE_PROSECUTING_AUTHORITIES_QUERY = "SELECT prosecuting_authority " +
            " FROM case_assignment_restriction " +
            " WHERE (include_only = '[]' OR jsonb_exists(include_only, :lja)) " +
            " AND NOT jsonb_exists(exclude, :lja)";

    public void saveCaseAssignmentRestriction(final String prosecutingAuthority, final String includeOnly, final String exclude, final ZonedDateTime dateTimeCreated) {
        final Query insertStatement = entityManager.createNativeQuery(INSERT_STATEMENT);
        insertStatement.setParameter("prosecuting_authority", prosecutingAuthority);
        insertStatement.setParameter("include_only", includeOnly);
        insertStatement.setParameter("exclude", exclude);
        insertStatement.setParameter("date_time_created", dateTimeCreated);
        insertStatement.executeUpdate();
    }

    public List<String> findProsecutingAuthoritiesByLja(final String lja) {
        return entityManager.createNativeQuery(SELECT_CANDIDATE_PROSECUTING_AUTHORITIES_QUERY)
                .setParameter("lja", lja)
                .getResultList();
    }
}
