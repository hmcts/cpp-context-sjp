package uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.casereferredforcourthearing.service;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.sjp.domain.transformation.connection.ConnectionProvider;
import uk.gov.moj.cpp.sjp.domain.transformation.service.ResultingService;
import uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.casereferredforcourthearing.TestUtils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.json.JsonObject;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResultingEventStoreRepositoryTest {

    private ResultingService resultingService;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private ConnectionProvider connectionProvider;

    @Before
    public void setUp() throws SQLException {
        resultingService = new ResultingService(connectionProvider);

        when(connectionProvider.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("SELECT payload FROM event_log el WHERE el.name = 'resulting.events.referenced-decisions-saved' AND el.stream_id = CAST(? as uuid)")).thenReturn(preparedStatement);
    }

    @Test
    public void getReferencedDecisionSavedEventByCaseIdShouldReturnSuccessfulResult() throws SQLException, IOException {

        String decisionId = randomUUID().toString();
        final JsonObject payload = TestUtils.readJson("referenced-decision-saved/resulting.events.referenced-decisions-saved.json", JsonObject.class);

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString(1)).thenReturn(payload.toString());

        ReferenceDecisionSavedResult result = resultingService.getReferencedDecisionSavedEventByCaseId(decisionId);
        assertThat(result, Matchers.instanceOf(ReferenceDecisionSavedResult.class));
    }
}
