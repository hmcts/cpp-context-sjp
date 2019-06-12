package uk.gov.moj.cpp.sjp.domain.transformation.connection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.common.annotations.VisibleForTesting;

public class SjpService {

    private static final String SJP_VIEW_STORE_DS_NAME = "java:/app/sjp/DS.viewstore";

    private static final String LISTING_NOTES_COUNT = "select count(*) as listing_notes_count from case_note where case_id = CAST(? as uuid) and note_type='LISTING'";

    private final ConnectionProvider connectionProvider;

    public SjpService() {
        this(new ConnectionProvider(SJP_VIEW_STORE_DS_NAME));
    }

    @VisibleForTesting
    public SjpService(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public synchronized boolean hasListingNote(final String caseId) throws SQLException {
        try (final Connection connection = connectionProvider.getConnection();
             final PreparedStatement statement = connection.prepareStatement(LISTING_NOTES_COUNT)) {
            return hasListingNote(statement, caseId);
        }
    }

    private boolean hasListingNote(final PreparedStatement statement, final String caseId) throws SQLException {
        statement.setString(1, caseId);
        try (final ResultSet rs = statement.executeQuery()) {
            rs.next();
            return rs.getInt("listing_notes_count") > 0;
        }
    }
}
