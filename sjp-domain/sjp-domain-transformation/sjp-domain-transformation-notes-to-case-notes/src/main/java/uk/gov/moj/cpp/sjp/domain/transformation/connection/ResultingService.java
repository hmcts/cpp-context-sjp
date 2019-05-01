package uk.gov.moj.cpp.sjp.domain.transformation.connection;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import uk.gov.moj.cpp.sjp.domain.transformation.notes.CaseDecisionDetails;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@SuppressWarnings({"squid:S2221", "squid:S00112"})
public class ResultingService {

    private static final String RESULTING_DS_EVENTSTORE = "java:/app/resulting/DS.eventstore";

    private static final String GET_CASE_NOTES_FOR_CASE = "select CAST(el.payload::json ->> 'id' as text) as decision_id, " +
            "terminalEntries ->> 'value' as note, " +
            "CAST(el.metadata::json -> 'context' ->> 'user' as text) as user_id, \n" +
            "el.metadata::json ->> 'createdAt' as created_at, " +
            "'LISTING' as note_type\n" +
            "from event_log el, json_array_elements(el.payload::json -> 'offences') as offences,\n" +
            "json_array_elements(offences -> 'results') as results,\n" +
            "json_array_elements(results -> 'terminalEntries') as terminalEntries\n" +
            "where el.name = 'resulting.events.referenced-decisions-saved' and el.stream_id = CAST(? as uuid)\n" +
            "and results ->> 'code' = 'SUMRCC'\n" +
            "and terminalEntries ->> 'index' = '10'\n" +
            "union\n" +
            "select CAST(el.payload::json ->> 'id' as text) as decision_id, " +
            "CAST(el.payload::json ->> 'note' as text) as note, " +
            "CAST(el.metadata::json -> 'context' ->> 'user' as text) as user_id, " +
            "el.metadata::json ->> 'createdAt' as created_at, " +
            "'DECISION' as note_type\n" +
            "from event_log el\n" +
            "where el.name = 'resulting.events.referenced-decisions-saved' and el.payload::json ->> 'note' is not null and el.stream_id = CAST(? as uuid);";

    private ConnectionProvider connectionProvider = new ConnectionProvider(RESULTING_DS_EVENTSTORE);

    public synchronized Optional<CaseDecisionDetails> getCaseDecisionFor(String caseId) {
        try (final Connection connection = connectionProvider.getConnection();
             final PreparedStatement statement = connection.prepareStatement(GET_CASE_NOTES_FOR_CASE)) {
            statement.setString(1, caseId);
            statement.setString(2, caseId);
            return buildDecision(statement);
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving data ", e);
        }
    }

    private Optional<CaseDecisionDetails> buildDecision(final PreparedStatement statement) {
        try (final ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                final String note = rs.getString("note");
                if (isNotEmpty(note)) {
                    return of(new CaseDecisionDetails(fromString(rs.getString("decision_id")),
                            note,
                            rs.getString("user_id"),
                            rs.getString("created_at"),
                            rs.getString("note_type")));
                }
                return empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving data from result set", e);
        }
        return empty();
    }
}
