package uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.service;

import uk.gov.moj.cpp.sjp.domain.transformation.exception.TransformationException;
import uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.connection.ConnectionProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SjpViewStoreService {

    private static final String SJP_VIEW_STORE_DS_NAME = "java:/app/event-tool/DS.viewstore";

    private static SjpViewStoreService instance;

    private static final Logger LOGGER = LoggerFactory.getLogger(SjpViewStoreService.class);

    private static final String GET_OFFENCE_ID_FROM_CASE_ID =
            "select o.id as offenceId " +
                    "from case_details cd, defendant d, offence o " +
                    "where cd.id = d.case_id " +
                    "and d.id = o.defendant_id " +
                    "and cd.id = ? limit 1";

    private static final String GET_COUNT_STATEMENT =
            "SELECT COUNT(*) " +
                    "FROM ready_cases rc " +
                    "INNER JOIN case_details cd ON rc.case_id = cd.id " +
                    "WHERE NOT cd.completed " +
                    "AND rc.assignee_id IS NULL " +
                    "AND cd.id = CAST(? as uuid)";

    private final ConnectionProvider connectionProvider;

    private final IdMappingCache idMappingCache;

    private SjpViewStoreService() {
        this(new ConnectionProvider(SJP_VIEW_STORE_DS_NAME), new IdMappingCache());
    }

    public static final synchronized SjpViewStoreService getInstance() {
        if (instance == null) {
            instance = new SjpViewStoreService();
        }
        return instance;
    }

    @VisibleForTesting
    public SjpViewStoreService(ConnectionProvider connectionProvider,
                               IdMappingCache caseIdOffenceIdCache) {
        this.connectionProvider = connectionProvider;
        this.idMappingCache = caseIdOffenceIdCache;
    }

    public Optional<String> getOffenceId(final String caseId) {
        // first look in the cache

        if (idMappingCache.getId(caseId,"offenceId") != null) {
            return Optional.of(idMappingCache.getId(caseId, "offenceId"));
        }
        // got to database
        return getOffenceIdFromDB(caseId);
    }

    public Optional<String> getDefendantId(final String caseId) {
        // first look in the cache
        if (idMappingCache.getId(caseId,"defendantId") != null) {
            return Optional.of(idMappingCache.getId(caseId, "defendantId"));
        }
        // got to database
        return getOffenceIdFromDB(caseId);
    }

    @SuppressWarnings({"squid:S2139", "squid:S00112"})
    public synchronized Optional<String> getOffenceIdFromDB(final String caseId) {
        try (final Connection connection = connectionProvider.getConnection();
             final PreparedStatement statement = connection
                     .prepareStatement(GET_OFFENCE_ID_FROM_CASE_ID)) {
            statement.setString(1, caseId);
            try (final ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                final String offenceId = resultSet.getObject("offenceId", UUID.class).toString();
                return Optional.ofNullable(offenceId);
            }
        } catch (SQLException sqlException) { // do we need to bailout the whole process ??
            LOGGER.error("Exception while getting offenceId for caseId:{}", caseId, sqlException);
            throw new RuntimeException(sqlException);
        }
    }

    public synchronized boolean getWhetherCaseIsCandidateForMigration(final String caseId) {

        try (final Connection connection = connectionProvider.getConnection();
             final PreparedStatement statement = connection
                     .prepareStatement(GET_COUNT_STATEMENT)) {
            statement.setString(1, caseId);

            try (final ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                int count = resultSet.getInt(1);
                return count == 1;
            }
        } catch (SQLException sqlException) {
            throw new TransformationException(String.format("Error getting count by caseId:%s", caseId), sqlException);
        }
    }

}
