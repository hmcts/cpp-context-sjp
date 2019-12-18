package uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.casereferredforcourthearing.service;

import com.google.common.annotations.VisibleForTesting;
import uk.gov.moj.cpp.sjp.domain.transformation.exception.TransformationException;
import uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.connection.ConnectionProvider;

import javax.json.Json;
import javax.json.JsonReader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ResultingEventStoreRepository {

    private static ResultingEventStoreRepository instance;
    private ConnectionProvider connectionProvider;
    private static final String RESULTING_EVENT_STORE_DS_NAME = "java:/app/resulting/DS.eventstore";

    private static final String QUERY_STATEMENT =
            "SELECT payload " +
                    "FROM event_log el " +
                    "WHERE el.name = 'resulting.events.referenced-decisions-saved' " +
                    "AND el.stream_id = CAST(? as uuid)";

    @VisibleForTesting
    public ResultingEventStoreRepository(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    private ResultingEventStoreRepository() {
        this(new ConnectionProvider(RESULTING_EVENT_STORE_DS_NAME));
    }

    public static final synchronized ResultingEventStoreRepository getInstance() {
        if (instance == null) {
            instance = new ResultingEventStoreRepository();
        }
        return instance;
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
}
