package uk.gov.moj.cpp.sjp.domain.transformation.connection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SjpServiceTest {

    final String caseId = UUID.randomUUID().toString();

    @Mock
    private ConnectionProvider connectionProvider;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private SjpService sjpService;

    @Before
    public void init() throws Exception {
        sjpService = new SjpService(connectionProvider);

        when(connectionProvider.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("select count(*) as listing_notes_count from case_note where case_id = CAST(? as uuid) and note_type='LISTING'")).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
    }

    @Test
    public void shouldReturnTrueIfSingleNoteExists() throws Exception {
        when(resultSet.getInt("listing_notes_count")).thenReturn(1);
        assertThat(sjpService.hasListingNote(caseId), is(true));
    }

    @Test
    public void shouldReturnTrueIfMultipleNotesExist() throws Exception {
        when(resultSet.getInt("listing_notes_count")).thenReturn(2);
        assertThat(sjpService.hasListingNote(caseId), is(true));
    }

    @Test
    public void shouldReturnFalseIfNotesDoNotExist() throws Exception {
        when(resultSet.getInt("listing_notes_count")).thenReturn(0);
        assertThat(sjpService.hasListingNote(caseId), is(false));
    }
}
