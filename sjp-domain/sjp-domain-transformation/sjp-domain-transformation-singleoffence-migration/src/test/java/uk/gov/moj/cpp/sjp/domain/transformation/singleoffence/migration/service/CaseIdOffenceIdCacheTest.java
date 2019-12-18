package uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.service;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.connection.ConnectionProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CaseIdOffenceIdCacheTest {

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private ConnectionProvider connectionProvider;

    @InjectMocks
    private IdMappingCache caseIdOffenceIdCache;

    static final String caseId = UUID.randomUUID().toString();
    static final String offenceId = UUID.randomUUID().toString();
    static final String defendantId = UUID.randomUUID().toString();

    private static final String QUERY = "select CAST(cd.id as text) as caseId, " +
            "CAST(o.id as text) as offenceId, " +
            "CAST(d.id as text) as defendantId " +
            "from case_details cd, defendant d, offence o " +
            "where cd.id = d.case_id " +
            "and d.id = o.defendant_id";

    @Before
    public void setUp() throws SQLException {
        caseIdOffenceIdCache = new IdMappingCache(connectionProvider);

        when(connectionProvider.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(QUERY)).thenReturn(preparedStatement);

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true,false);
    }

    @Test
    public void shouldGetOffenceIdWhenProperDataIsPresent() throws SQLException {
        when(resultSet.getString("offenceId")).thenReturn(offenceId);
        when(resultSet.getString("defendantId")).thenReturn(defendantId);
        when(resultSet.getString("caseId")).thenReturn(caseId);

        assertThat(caseIdOffenceIdCache.getId(caseId, "offenceId"), Matchers.is(offenceId));

        verify(resultSet,times(2)).next();
        verify(connection,times(1)).prepareStatement(QUERY);
        verify(preparedStatement,times(1)).executeQuery();

        reset(resultSet, connection, preparedStatement);

        assertThat(caseIdOffenceIdCache.getId(caseId.toString(), "offenceId"), Matchers.is(offenceId));

        verifyNoMoreInteractions(resultSet,connection,preparedStatement);
    }

}
