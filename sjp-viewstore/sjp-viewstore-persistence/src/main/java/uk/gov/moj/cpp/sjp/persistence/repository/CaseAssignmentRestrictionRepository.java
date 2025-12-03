package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseAssignmentRestriction;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public abstract class CaseAssignmentRestrictionRepository implements EntityRepository<CaseAssignmentRestriction, String> {

    @Inject
    private EntityManager entityManager;

    private static final String INSERT_STATEMENT =
            "INSERT INTO case_assignment_restriction(prosecuting_authority, include_only, exclude, date_time_created, valid_from, valid_to) " +
                    "VALUES (:prosecuting_authority, cast(:include_only as jsonb), cast(:exclude as jsonb), :date_time_created, :valid_from, :valid_to ) " +
                    "ON CONFLICT (prosecuting_authority) " +
                    "DO UPDATE " +
                    "SET include_only = cast(:include_only as jsonb), exclude = cast(:exclude as jsonb), date_time_created = :date_time_created,  valid_from = :valid_from, valid_to = :valid_to ";

    @org.apache.deltaspike.data.api.Query(value = "SELECT * " +
            " FROM case_assignment_restriction " +
            " WHERE prosecuting_authority =:prosecuting_authority  " +
            " AND (valid_from IS NULL or valid_from <= :now) and (valid_to IS NULL OR valid_to >= :now)", isNative = true)
    public abstract List<CaseAssignmentRestriction> findByProsecutingAuthority(@QueryParam("prosecuting_authority") final String prosecutingAuthority, @QueryParam("now") final LocalDate now);

    @org.apache.deltaspike.data.api.Query(value = "SELECT prosecuting_authority " +
            " FROM case_assignment_restriction " +
            " WHERE (include_only = '[]' OR jsonb_exists(include_only, :lja)) " +
            " AND NOT jsonb_exists(exclude, :lja)" +
            " AND (valid_from IS NULL or valid_from <= :now) and (valid_to IS NULL OR valid_to >= :now)", isNative = true)
    public abstract List<String> findProsecutingAuthoritiesByLja(@QueryParam("lja") final String lja, @QueryParam("now") final LocalDate now);

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
            insertStatement.setParameter("valid_from", LocalDate.of(1900,1,1));
        }

        if (validTo != null) {
            insertStatement.setParameter("valid_to", validTo);
        } else {
            insertStatement.setParameter("valid_to", LocalDate.of(9999,1,1));
        }
        insertStatement.executeUpdate();
    }
}
