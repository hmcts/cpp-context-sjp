package uk.gov.moj.sjp.it.helper;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayloadAsInputStream;

import uk.gov.justice.services.jdbc.persistence.DataAccessException;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.json.JsonObject;

public class FileServiceDBHelper {

    private static final String INSERT_CONTENT_SQL = "INSERT INTO content (file_id, deleted, content) VALUES(?, ?, ?)";

    private static final String INSERT_METADATA_SQL = "INSERT INTO metadata (file_id, metadata) VALUES(?, to_json(?::json))";

    public static UUID storeFile(final JsonObject metadata, final InputStream fileContentStream) {
        final UUID fileId = UUID.randomUUID();
        try (final Connection connection = getFileServiceConnection("fileservice");
             final PreparedStatement insertContentStatement = connection.prepareStatement(INSERT_CONTENT_SQL);
             final PreparedStatement insertMetadataStatement = connection.prepareStatement(INSERT_METADATA_SQL)) {
            connection.setAutoCommit(false);
            insertContentStatement.setObject(1, fileId);
            insertContentStatement.setBoolean(2, false);
            insertContentStatement.setBinaryStream(3, fileContentStream);

            int rowsInserted = insertContentStatement.executeUpdate();
            if (rowsInserted != 1) {
                throw new RuntimeException("error inserting content for file service");
            }

            insertMetadataStatement.setObject(1, fileId);
            insertMetadataStatement.setString(2, metadata.toString());

            rowsInserted = insertMetadataStatement.executeUpdate();

            if (rowsInserted != 1) {
                throw new RuntimeException("error inserting content for file service");
            }

            connection.commit();
            return fileId;
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static Connection getFileServiceConnection(final String contextName) {
        final String host = getHost();
        final String url = "jdbc:postgresql://" + host + "/fileservice";

        try {
            return DriverManager.getConnection(url, contextName, contextName);
        } catch (SQLException sqlException) {
            final String message = String.format("Failed to get JDBC connection to fileservice context url: '%s', username '%s', password '%s'", contextName, url, contextName, contextName);
            throw new DataAccessException(message, sqlException);
        }
    }

    public static UUID createStubFile(final String fileName,
                                      final ZonedDateTime createdAt) {
        InputStream filePayload = getPayloadAsInputStream("documents/scrooge-full.pdf");
        final JsonObject metadata = createObjectBuilder()
                .add("fileName", fileName)
                .add("createdAt", createdAt.format(DateTimeFormatter.ISO_INSTANT))
                .add("mediaType", "application/pdf")
                .build();
        return storeFile(metadata, filePayload);

    }
}
