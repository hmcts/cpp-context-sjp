package uk.gov.moj.cpp.sjp.domain.transformation.service;

import com.google.common.annotations.VisibleForTesting;

import uk.gov.moj.cpp.sjp.domain.transformation.connection.ConnectionProvider;
import uk.gov.moj.cpp.sjp.domain.transformation.exception.TransformationException;
import uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.casereferredforcourthearing.service.ReferenceDecisionSavedResult;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class ResultingService {

    private static final String RESULTING_DS_EVENTSTORE = "java:/app/resulting/DS.eventstore";

    private static final String GET_DECISION_SAVED_EVENT_FOR_STREAM_ID = "select payload " +
            "from event_log where stream_id = CAST(? as uuid) and name = 'resulting.events.referenced-decisions-saved'";

    private static final String GET_ADJOURN_EVENT = "select payload " +
            "from event_log el " +
            "where stream_id = CAST(? as uuid) " +
            "and el.payload::json ->> 'sjpSessionId' = ? " +
            "and el.payload::json ->> 'adjournedTo' = ?";

    private static final String QUERY_STATEMENT =
            "SELECT payload " +
                    "FROM event_log el " +
                    "WHERE el.name = 'resulting.events.referenced-decisions-saved' " +
                    "AND el.stream_id = CAST(? as uuid)";

    private ConnectionProvider connectionProvider;

    private static ResultingService instance;

    private ResultingService() {
        this(new ConnectionProvider(RESULTING_DS_EVENTSTORE));
    }

    public synchronized static final ResultingService getInstance() {
        if (instance == null) {
            instance = new ResultingService();
        }
        return instance;
    }

    @VisibleForTesting
    public ResultingService(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public JsonObject getDecisionForACase(final UUID caseId) {
        try (final Connection connection = connectionProvider.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(GET_DECISION_SAVED_EVENT_FOR_STREAM_ID);) {
            preparedStatement.setString(1, caseId.toString());

            return getPayload(caseId, preparedStatement);

        } catch (SQLException e) {
            throw new TransformationException("Cannot open connection to the DB", e);
        }
    }

    public JsonObject getAdjournPayloadForACase(final UUID caseId, final String sessionId, final String adjournedTo) {
        try (final Connection connection = connectionProvider.getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(GET_ADJOURN_EVENT);) {
            preparedStatement.setString(1, caseId.toString());
            preparedStatement.setString(2, sessionId);
            preparedStatement.setString(3, adjournedTo);

            return getPayload(caseId, preparedStatement);

        } catch (SQLException e) {
            throw new TransformationException("Cannot open connection to the DB", e);
        }
    }

    @SuppressWarnings("squid:S134")
    public synchronized ReferenceDecisionSavedResult getReferencedDecisionSavedEventByCaseId(final String caseId) {

        try (final Connection connection = connectionProvider.getConnection();
             final PreparedStatement statement = connection
                     .prepareStatement(QUERY_STATEMENT)) {
            statement.setString(1, caseId);

            try (final ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String payload = resultSet.getString(1);

                    try (final JsonReader reader = Json.createReader(new StringReader(payload))) {
                        return new ReferenceDecisionSavedResult(reader.readObject());
                    }
                }
            }
        } catch (SQLException sqlException) {
            throw new TransformationException(String.format("Error getting resulting.events.referenced-decisions-saved events from resulting event store for case with id %s", caseId), sqlException);
        }

        return null;
    }

    private JsonObject getPayload(UUID caseId, PreparedStatement preparedStatement) throws SQLException {
        try (final ResultSet resultSet = preparedStatement.executeQuery()) {
            if (!resultSet.next()) {
                throw new TransformationException("No decision saved event in the DB for case: " + caseId);
            }
            final String payload = resultSet.getString("payload");

            try (final JsonReader reader = Json.createReader(new StringReader(payload))) {
                return reader.readObject();
            }
        }
    }

}
