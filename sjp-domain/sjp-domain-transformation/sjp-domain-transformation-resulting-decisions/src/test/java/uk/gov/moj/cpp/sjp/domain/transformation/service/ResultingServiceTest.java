package uk.gov.moj.cpp.sjp.domain.transformation.service;

import static java.util.UUID.randomUUID;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.moj.cpp.sjp.domain.transformation.connection.ConnectionProvider;
import uk.gov.moj.cpp.sjp.domain.transformation.service.ResultingService;

@RunWith(MockitoJUnitRunner.class)
public class ResultingServiceTest {

    @Mock
    private ConnectionProvider connectionProvider;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private ResultingService service;

    @Before
    public void setup() {
        service = new ResultingService(connectionProvider);
    }

    @Test
    public void shouldReturnReferencedDecisionSaved() throws SQLException, IOException {
        when(connectionProvider.getConnection()).thenReturn(connection);

        when(
                connection.prepareStatement("select payload from event_log where stream_id = CAST(? as uuid) and name = 'resulting.events.referenced-decisions-saved'")
        ).thenReturn(preparedStatement);

        final UUID caseId = randomUUID();

        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true);

        final String decisionSavedFileContent = readFileToString(new File("resulting.events.referenced-decisions-saved.json"));
        when(resultSet.getString("payload"))
                .thenReturn(decisionSavedFileContent);

        try (final JsonReader reader = Json.createReader(new StringReader(decisionSavedFileContent))) {
            final JsonObject expectedDecisionPayload = reader.readObject();
            final JsonObject decisionForACase = service.getDecisionForACase(caseId);

            assertThat(decisionForACase, equalTo(expectedDecisionPayload));
        }

        verify(preparedStatement).setString(1, caseId.toString());
        verify(connection).close();
    }


}