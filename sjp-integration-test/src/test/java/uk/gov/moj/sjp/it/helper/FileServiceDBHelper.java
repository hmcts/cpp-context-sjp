package uk.gov.moj.sjp.it.helper;

import static java.util.Optional.ofNullable;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.io.FileUtils.readFileToByteArray;
import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;

import uk.gov.justice.services.jdbc.persistence.DataAccessException;
import uk.gov.moj.sjp.it.util.JsonHelper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

public class FileServiceDBHelper {

    private static final String DOCUMENT_EXISTS_SQL = "SELECT count(*) from content WHERE file_id = ?";
    private static final String GET_METADATA_BY_FILE_ID_SQL = "SELECT metadata FROM metadata WHERE file_id = ?";

    private static final String INSERT_CONTENT_SQL = "INSERT INTO content (file_id, deleted, content) VALUES(?, ?, ?)";

    private static final String INSERT_METADATA_SQL = "INSERT INTO metadata (file_id, metadata) VALUES(?, to_json(?::json))";

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

    public static Optional<JsonObject> findMetadataByFileId(final UUID fileId) {
        try (final Connection connection = getFileServiceConnection("fileservice");
             final PreparedStatement preparedStatement = connection.prepareStatement(GET_METADATA_BY_FILE_ID_SQL)) {

            preparedStatement.setObject(1, fileId);
            final ResultSet resultSet = preparedStatement.executeQuery();
            Optional<JsonObject> medatada = Optional.empty();

            if (resultSet.next()) {
                medatada = ofNullable(resultSet.getString(1)).map(JsonHelper::getJsonObject);
            }

            return medatada;

        } catch (final SQLException e) {
            throw new RuntimeException("Failed while fetching the data from file store", e);
        }
    }

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
            return null;
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

    public static UUID createStubFile(final String fileName,
                                      final ZonedDateTime createdAt) {
        final String path = "src/test/resources/documents/";
        try {
            byte[] fileBytes = readFileToByteArray(new File(path.concat("scrooge-full.pdf")));
            final JsonObject metadata = createObjectBuilder()
                    .add("fileName", fileName)
                    .add("createdAt", createdAt.format(DateTimeFormatter.ISO_INSTANT))
                    .add("mediaType", "application/pdf")
                    .build();
            return storeFile(metadata, new ByteArrayInputStream(fileBytes));

        } catch (IOException e) {
            return null;
        }
    }
}
