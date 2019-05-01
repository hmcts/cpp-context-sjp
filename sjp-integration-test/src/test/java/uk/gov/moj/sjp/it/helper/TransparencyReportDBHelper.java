package uk.gov.moj.sjp.it.helper;

import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;

import uk.gov.justice.services.jdbc.persistence.DataAccessException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class TransparencyReportDBHelper {

    private static final String DOCUMENT_EXISTS_SQL = "SELECT count(*) from content WHERE file_id = ?";

    public static boolean checkIfFileExists(final UUID fileId) {

        try (final Connection connection = getFileServiceConnection("fileservice");
             final PreparedStatement preparedStatement = connection.prepareStatement(DOCUMENT_EXISTS_SQL)) {
            preparedStatement.setObject(1, fileId);

            int numberOfFiles = 0;

            final ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                numberOfFiles = resultSet.getInt(1);
            }

            return numberOfFiles > 0;

        } catch (final SQLException e) {
            throw new RuntimeException("Failed while fetching the data from file store", e);
        }
    }

    private static Connection getFileServiceConnection(final String contextName) {
        final String host = getHost();
        final String url = "jdbc:postgresql://" + host + "/fileservice";
        final String username = contextName;
        final String password = contextName;

        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException sqlException) {
            final String message = String.format("Failed to get JDBC connection to fileservice context url: '%s', username '%s', password '%s'", contextName, url, contextName, contextName);
            throw new DataAccessException(message, sqlException);
        }
    }

}
