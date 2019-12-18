package uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.sjp.domain.transformation.connection.ConnectionProvider;
import uk.gov.moj.cpp.sjp.domain.transformation.service.IdMappingCache;
import uk.gov.moj.cpp.sjp.domain.transformation.service.SjpViewStoreService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SjpViewStoreServiceTest {

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private ConnectionProvider connectionProvider;

    @Mock
    private PreparedStatement offenceIdAndDefendantIdPreparedStatement;

    @Mock
    private ResultSet offenceIdAndDefendantIdResultSet;

    private static final String OFFENCE_AND_DEFENDANT_ID = "select CAST(cd.id as text) as caseId, CAST(o.id as text) as offenceId, CAST(d.id as text) as defendantId from case_details cd, defendant d, offence o where cd.id = d.case_id and d.id = o.defendant_id";

    static final String CASE_ID = UUID.randomUUID().toString();
    static final UUID OFFENCE_ID = UUID.randomUUID();

    private static final String DEFENDANT_ID = "e4ec3a35-76bf-4649-b43a-761e71c3765e";

    private SjpViewStoreService sjpViewStoreService;

    @Before
    public void setUp() throws SQLException {
        sjpViewStoreService = new SjpViewStoreService(connectionProvider);

        when(connectionProvider.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("select o.id as offenceId from case_details cd, defendant d, offence o where cd.id = d.case_id and d.id = o.defendant_id and cd.id = ? limit 1")).thenReturn(preparedStatement);
        when(connection.prepareStatement("SELECT COUNT(*) FROM ready_cases rc INNER JOIN case_details cd ON rc.case_id = cd.id WHERE NOT cd.completed AND rc.assignee_id IS NULL AND cd.id = CAST(? as uuid)")).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        when(connection.prepareStatement(OFFENCE_AND_DEFENDANT_ID)).thenReturn(offenceIdAndDefendantIdPreparedStatement);
        when(offenceIdAndDefendantIdPreparedStatement.executeQuery()).thenReturn(offenceIdAndDefendantIdResultSet);
        when(offenceIdAndDefendantIdResultSet.next()).thenReturn(true, false);
        when(offenceIdAndDefendantIdResultSet.getString("offenceId")).thenReturn(OFFENCE_ID.toString());
        when(offenceIdAndDefendantIdResultSet.getString("defendantId")).thenReturn(DEFENDANT_ID);
        when(offenceIdAndDefendantIdResultSet.getString("caseId")).thenReturn(CASE_ID);

    }

    @Test
    public void shouldGetOffenceIdWhenProperDataIsPresent() throws SQLException {
        assertThat(sjpViewStoreService.getOffenceId(CASE_ID).get(), is(OFFENCE_ID.toString()));
    }


    @Test
    public void getWhetherCaseIsCandidateForMigrationShouldReturnTrue() throws SQLException {
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(1);

        Assert.assertThat(sjpViewStoreService.getWhetherCaseIsCandidateForMigration(randomUUID().toString()), Matchers.equalTo(true));
    }

    @Test
    public void getWhetherCaseIsCandidateForMigrationShouldReturnFalse() throws SQLException {
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(0);

        Assert.assertThat(sjpViewStoreService.getWhetherCaseIsCandidateForMigration(randomUUID().toString()), Matchers.equalTo(false));
    }
}