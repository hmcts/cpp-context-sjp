package uk.gov.moj.cpp.sjp.domain.transformation.service;

import static org.slf4j.LoggerFactory.getLogger;

import uk.gov.moj.cpp.sjp.domain.transformation.connection.ConnectionProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;

public class SjpEventStoreService {

    private static final String SJP_EVENT_STORE_DS_NAME = "java:/app/event-tool/DS.eventstore";

    private static final Logger LOGGER = getLogger(SjpEventStoreService.class);

    private final ConnectionProvider connectionProvider;

    private static final String INITIAL_STREAM_EVENT =
            "select * " +
            "from event_log " +
            "where stream_id = CAST(? as uuid) and position_in_stream = 1";

    private static SjpEventStoreService instance;

    private SjpEventStoreService() {
        this(new ConnectionProvider(SJP_EVENT_STORE_DS_NAME));
    }

    @VisibleForTesting
    public SjpEventStoreService(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public static synchronized  SjpEventStoreService getInstance() {
        if (instance == null) {
            instance = new SjpEventStoreService();
        }
        return instance;
    }

    @SuppressWarnings({"squid:S00112","squid:S1141","squid:S2139"})
    public boolean hasInitialEventInStream(final String streamId) {
        try(
            final Connection connection = connectionProvider.getConnection();
            final PreparedStatement preparedStatement = connection.prepareStatement(INITIAL_STREAM_EVENT)){
            preparedStatement.setString(1, streamId);
            try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            } catch (SQLException e) {
                LOGGER.error("Error checking if a first stream event exists", e);
                throw new RuntimeException("Error checking if a first stream event exists", e);
            }
        } catch (final SQLException e) {
            LOGGER.error("Error checking if a first stream event exists");
            throw new RuntimeException("Error checking if a first stream event exists",e);
        }
    }

}
