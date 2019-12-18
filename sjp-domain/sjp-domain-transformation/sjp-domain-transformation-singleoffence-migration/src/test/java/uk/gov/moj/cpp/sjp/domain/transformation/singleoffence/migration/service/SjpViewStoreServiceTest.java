package uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.connection.ConnectionProvider;

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
    private IdMappingCache caseIdOffenceIdCache;

    static final String CASE_ID = UUID.randomUUID().toString();
    static final UUID OFFENCE_ID = UUID.randomUUID();

    private SjpViewStoreService sjpViewStoreService;

    @Before
    public void setUp() throws SQLException {
        sjpViewStoreService = new SjpViewStoreService(connectionProvider, caseIdOffenceIdCache);

        when(connectionProvider.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("select o.id as offenceId from case_details cd, defendant d, offence o where cd.id = d.case_id and d.id = o.defendant_id and cd.id = ? limit 1")).thenReturn(preparedStatement);
        when(connection.prepareStatement("SELECT COUNT(*) FROM ready_cases rc INNER JOIN case_details cd ON rc.case_id = cd.id WHERE NOT cd.completed AND rc.assignee_id IS NULL AND cd.id = CAST(? as uuid)")).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
    }

    @Test
    public void shouldGetOffenceIdWhenProperDataIsPresent() throws SQLException {
        when(resultSet.getObject("offenceId", UUID.class)).thenReturn(OFFENCE_ID);
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