package uk.gov.moj.cpp.sjp.domain.transformation.service;

import static java.util.Objects.nonNull;
import static org.slf4j.LoggerFactory.getLogger;

import uk.gov.justice.services.common.jpa.converter.LocalDatePersistenceConverter;
import uk.gov.moj.cpp.sjp.domain.transformation.connection.ConnectionProvider;
import uk.gov.moj.cpp.sjp.domain.transformation.exception.TransformationException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

public class SjpViewStoreService {

    private static final String SJP_VIEW_STORE_DS_NAME = "java:/app/event-tool/DS.viewstore";

    private static final String GET_POSTNG_DATE = "select cd.posting_date " +
            "from case_details cd, ready_cases rc " +
            "where cd.id = rc.case_id and rc.case_id = ?";

    private static final String OFFENCE_PLEA =
            "select id as offenceId, plea " +
                    "from offence";

    private static final String GET_OFFENCE_ID_FROM_CASE_ID =
            "select o.id as offenceId " +
                    "from case_details cd, defendant d, offence o " +
                    "where cd.id = d.case_id " +
                    "and d.id = o.defendant_id " +
                    "and cd.id = CAST(? as uuid) limit 1";
    private static final String GET_COUNT_STATEMENT =
            "SELECT COUNT(*) " +
                    "FROM ready_cases rc " +
                    "INNER JOIN case_details cd ON rc.case_id = cd.id " +
                    "WHERE NOT cd.completed " +
                    "AND rc.assignee_id IS NULL " +
                    "AND cd.id = CAST(? as uuid)";

    private static final String SELECT_STATUS_QUERY = "SELECT id, referred_for_court_hearing, reopened_date, completed from case_details where completed = true";

    private static final String OFFENCE_COURT_OPTIONS =
            "select o.id as id, d.speak_welsh as speakWelsh, d.interpreter_language as language from  defendant d, offence o where d.id = o.defendant_id";

    private static final Logger LOGGER = getLogger(SjpViewStoreService.class);

    private static volatile SjpViewStoreService instance;

    private static final Map<String, String> offenceIdPleaCache = new HashMap<>();
    private static final Map<String, String> caseIdStatusCache = new HashMap<>();
    private static final Map<String, Pair<Boolean, String>> offenceIdCourtOptionsCache = new HashMap<>();

    private ConnectionProvider connectionProvider;

    private final IdMappingCache idMappingCache;

    private SjpViewStoreService() {
        this(new ConnectionProvider(SJP_VIEW_STORE_DS_NAME));
        initCaseIdPleatMap();
        initCaseIdCourtOptionsMap();
        initCaseStatusMap();
    }

    @VisibleForTesting
    public SjpViewStoreService(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
        this.idMappingCache = new IdMappingCache(connectionProvider);
    }

    public static synchronized SjpViewStoreService getInstance() {
        if (instance == null) {
            instance = new SjpViewStoreService();
        }
        return instance;
    }

    public String getPlea(final String offenceId) {
        return offenceIdPleaCache.get(offenceId);
    }

    /**
     * @param offenceId offence id
     * @return Pair with speak welsh and interpreter language
     */
    public Pair<Boolean, String> getCourtOptions(final String offenceId) {
        return offenceIdCourtOptionsCache.get(offenceId);
    }

    @SuppressWarnings("squid:S00112")
    private void initCaseIdPleatMap() {
        LOGGER.info("Obtaining case-id/plea  from view store");
        try (
                final Connection connection = connectionProvider.getConnection();
                final PreparedStatement preparedStatement = connection.prepareStatement(OFFENCE_PLEA);
                final ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                offenceIdPleaCache.put(
                        resultSet.getObject("offenceId", UUID.class).toString(),
                        resultSet.getString("plea"));
            }
        } catch (final SQLException e) {
            throw new RuntimeException("Error retrieving data using prepared statement", e);
        }
        LOGGER.info("offenceIdPleaCache size is:{}", offenceIdPleaCache.size());
    }

    private void initCaseIdCourtOptionsMap() {
        LOGGER.debug("Obtaining case-id/court options  from view store");
        try (
            final Connection connection = connectionProvider.getConnection();
            final PreparedStatement preparedStatement = connection.prepareStatement(OFFENCE_COURT_OPTIONS);
            final ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {

                final Boolean speakWelsh = resultSet.getObject("speakWelsh", Boolean.class);

                offenceIdCourtOptionsCache.put(
                        resultSet.getObject("id", UUID.class).toString(),
                        Pair.of(speakWelsh,
                                resultSet.getString("language")));
            }
        } catch (final SQLException e) {
            throw new TransformationException("Error retrieving data using prepared statement", e);
        }
        LOGGER.info("SjpViewStoreService#offenceIdCourtOptionsCache size is:{}", offenceIdCourtOptionsCache.size());
    }

    @SuppressWarnings("squid:S134")
    private void initCaseStatusMap() {

        try (final Connection connection = connectionProvider.getConnection();
             final PreparedStatement statement = connection
                     .prepareStatement(SELECT_STATUS_QUERY)) {

            try (final ResultSet resultSet = statement.executeQuery()) {
                while(resultSet.next()) {
                    final String caseId = resultSet.getString(1);
                    final Boolean referredToCourt = resultSet.getBoolean(2);
                    final Object reopenedDate = resultSet.getObject(3);
                    final Boolean completed = resultSet.getBoolean(4);

                    if (Boolean.TRUE.equals(referredToCourt)) {
                        caseIdStatusCache.put(caseId, "REFERRED_FOR_COURT_HEARING");
                    } else if (nonNull(reopenedDate)) {
                        caseIdStatusCache.put(caseId, "REOPENED_IN_LIBRA");
                    } else if (Boolean.TRUE.equals(completed)) {
                        caseIdStatusCache.put(caseId, "COMPLETED");
                    }
                }
            }
        } catch (SQLException sqlException) {
            throw new TransformationException("Error retrieving data using prepared statement", sqlException);
        }
        LOGGER.info("SjpViewStoreService#caseIdStatusCache size is:{}", caseIdStatusCache.size());
    }

    public Optional<String> getOffenceId(final String caseId) {
        // first look in the cache

        if (idMappingCache.getId(caseId,"offenceId") != null) {
            return Optional.of(idMappingCache.getId(caseId, "offenceId"));
        }
        // got to database
        return Optional.empty();
    }

    public Optional<String> getDefendantId(final String caseId) {
        // first look in the cache
        if (idMappingCache.getId(caseId,"defendantId") != null) {
            return Optional.of(idMappingCache.getId(caseId, "defendantId"));
        }
        // got to database
        return Optional.empty();
    }

    public Optional<String> getStatus(final String caseId) {
        return Optional.ofNullable(caseIdStatusCache.get(caseId));
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


    @SuppressWarnings("squid:S2139")
    public Optional<LocalDate> getPostingDateWhenPresentInReadyCases(final String caseId) {
        final LocalDatePersistenceConverter converter = new LocalDatePersistenceConverter();
        try (final Connection connection = connectionProvider.getConnection();
             final PreparedStatement preparedStatement =
                     connection.prepareStatement(GET_POSTNG_DATE)) {
            preparedStatement.setObject(1, UUID.fromString(caseId));
            try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.ofNullable(converter.convertToEntityAttribute(resultSet.getDate(1)));
                }
            }
        } catch (final SQLException e) {
            LOGGER.error("Exception while retrieving posting date when present in ready cases ", e);
            throw new TransformationException("Error retrieving data using prepared statement", e);
        }
        return Optional.empty();
    }

}
