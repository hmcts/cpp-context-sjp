package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseAssignmentRestriction;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@ApplicationScoped
public class CaseAssignmentRestrictionRepository {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    private EntityManager entityManager;

    private static final String INSERT_STATEMENT =
            "INSERT INTO case_assignment_restriction(prosecuting_authority, include_only, exclude, date_time_created, valid_from, valid_to) " +
                    "VALUES (:prosecuting_authority, cast(:include_only as jsonb), cast(:exclude as jsonb), :date_time_created, :valid_from, :valid_to ) " +
                    "ON CONFLICT (prosecuting_authority) " +
                    "DO UPDATE " +
                    "SET include_only = cast(:include_only as jsonb), exclude = cast(:exclude as jsonb), date_time_created = :date_time_created,  valid_from = :valid_from, valid_to = :valid_to ";

    private static final String FIND_BY_PROSECUTING_AUTHORITY = "SELECT * " +
            " FROM case_assignment_restriction " +
            " WHERE prosecuting_authority =:prosecuting_authority  " +
            " AND (valid_from IS NULL or valid_from <= :now) and (valid_to IS NULL OR valid_to >= :now)";

    private static final String FIND_PROSECUTING_AUTHORITIES_BY_LJA = "SELECT prosecuting_authority " +
            " FROM case_assignment_restriction " +
            " WHERE (include_only = '[]' OR jsonb_exists(include_only, :lja)) " +
            " AND NOT jsonb_exists(exclude, :lja)" +
            " AND (valid_from IS NULL or valid_from <= :now) and (valid_to IS NULL OR valid_to >= :now)";

    public List<CaseAssignmentRestriction> findByProsecutingAuthority(final String prosecutingAuthority, final LocalDate now) {
        return entityManager.createNativeQuery(FIND_BY_PROSECUTING_AUTHORITY, CaseAssignmentRestriction.class)
                .setParameter("prosecuting_authority", prosecutingAuthority)
                .setParameter("now", now)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<String> findProsecutingAuthoritiesByLja(final String lja, final LocalDate now) {
        return entityManager.createNativeQuery(FIND_PROSECUTING_AUTHORITIES_BY_LJA)
                .setParameter("lja", lja)
                .setParameter("now", now)
                .getResultList();
    }

    public void saveCaseAssignmentRestriction(final String prosecutingAuthority, final String includeOnly, final String exclude,
                                              final ZonedDateTime dateTimeCreated, final LocalDate validFrom, final LocalDate validTo) {
        final Query insertStatement = entityManager.createNativeQuery(INSERT_STATEMENT);
        insertStatement.setParameter("prosecuting_authority", prosecutingAuthority);
        insertStatement.setParameter("include_only", includeOnly);
        insertStatement.setParameter("exclude", exclude);
        insertStatement.setParameter("date_time_created", dateTimeCreated);

        if (validFrom != null) {
            insertStatement.setParameter("valid_from", validFrom);
        } else {
            insertStatement.setParameter("valid_from", LocalDate.of(1900, 1, 1));
        }

        if (validTo != null) {
            insertStatement.setParameter("valid_to", validTo);
        } else {
            insertStatement.setParameter("valid_to", LocalDate.of(9999, 1, 1));
        }
        insertStatement.executeUpdate();
    }

    public CaseAssignmentRestriction findBy(final String id) {
        return entityManager.find(CaseAssignmentRestriction.class, id);
    }

    public CaseAssignmentRestriction save(final CaseAssignmentRestriction entity) {
        return entityManager.merge(entity);
    }

    public void remove(final CaseAssignmentRestriction entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public Long count() {
        return entityManager.createQuery("SELECT COUNT(e) FROM CaseAssignmentRestriction e", Long.class).getSingleResult();
    }

    public List<CaseAssignmentRestriction> findAll() {
        return entityManager.createQuery("SELECT e FROM CaseAssignmentRestriction e", CaseAssignmentRestriction.class).getResultList();
    }
}
