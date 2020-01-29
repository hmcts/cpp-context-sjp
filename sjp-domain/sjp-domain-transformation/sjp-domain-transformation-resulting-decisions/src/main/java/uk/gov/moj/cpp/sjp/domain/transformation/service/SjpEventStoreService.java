package uk.gov.moj.cpp.sjp.domain.transformation.service;

import static org.slf4j.LoggerFactory.getLogger;

import uk.gov.moj.cpp.sjp.domain.transformation.connection.ConnectionProvider;
import uk.gov.moj.cpp.sjp.domain.transformation.exception.TransformationException;
import uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.casemarkedready.CaseMarkedReadyReadyMetaData;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;

public class SjpEventStoreService {

    private static final String SJP_EVENT_STORE_DS_NAME = "java:/app/event-tool/DS.eventstore";
    private static final Logger LOGGER = getLogger(SjpEventStoreService.class);
    private static final Map<String, Set<String>> cache = new HashMap<>();
    private static final Map<String, List<CaseMarkedReadyReadyMetaData>> caseMarkedForDecisionCache = new HashMap<>();

    private static final String DECISION_SAVED_COUNT =
            "select stream_id, cast(payload::json ->> 'decisionId'  as text) as decision_id " +
                    "from event_log " +
                    "where name = 'sjp.events.decision-saved'";

    private static final String GET_INTERPRETER =
            "with tmp as " +
                    "(select position_in_stream " +
                    "from event_log  " +
                    "where name = 'sjp.events.plea-updated'  " +
                    "and  stream_id = CAST(? as uuid) " +
                    "and  metadata::json ->> 'id' =  ?) " +
                    "select payload::json -> 'interpreter' as interpreter  " +
                    "from event_log " +
                    "where position_in_stream > " +
                    "(select position_in_stream from tmp) " +
                    "and position_in_stream <= ( " +
                    "SELECT position_in_stream " +
                    "from event_log  " +
                    "where ( name = 'sjp.events.plea-updated' or name = 'sjp.events.interpreter-for-defendant-updated' ) " +
                    "and position_in_stream > (select position_in_stream from tmp) " +
                    "and stream_id = CAST(? as uuid) " +
                    "order by position_in_stream " +
                    "limit 1)" +
                    "and name = 'sjp.events.interpreter-for-defendant-updated' " +
                    "and stream_id = CAST(? as uuid) " +
                    "limit 1";

    private static final String GET_WELSH_SPEAK =
            "with tmp as " +
                    "(select position_in_stream " +
                    "from event_log  " +
                    "where name = 'sjp.events.plea-updated'  " +
                    "and  stream_id = CAST(? as uuid) " +
                    "and  metadata::json ->> 'id' =  ?) " +
                    "select payload::json -> 'speakWelsh'  " +
                    "from event_log " +
                    "where position_in_stream > " +
                    "(select position_in_stream from tmp) " +
                    "and position_in_stream <= ( " +
                    "SELECT position_in_stream " +
                    "from event_log  " +
                    "where (name = 'sjp.events.plea-updated' or name = 'sjp.events.hearing-language-preference-for-defendant-updated') " +
                    "and position_in_stream > (select position_in_stream from tmp) " +
                    "and stream_id = CAST(? as uuid) " +
                    "order by position_in_stream  " +
                    "limit 1)" +
                    "and name = 'sjp.events.hearing-language-preference-for-defendant-updated' " +
                    "and stream_id = CAST(? as uuid) " +
                    "limit 1";

    private final String GET_MARKED_READY_FOR_DECISION_METADATA =
            "select stream_id, metadata::json ->> 'id' as metadata_id, " +
                    "payload::json ->> 'markedAt' as marked_at " +
                    "from event_log  " +
                    "where name = 'sjp.events.case-marked-ready-for-decision' " +
                    "order by stream_id, position_in_stream";


    private final ConnectionProvider connectionProvider;

    private static SjpEventStoreService instance;

    private SjpEventStoreService() {
        this(new ConnectionProvider(SJP_EVENT_STORE_DS_NAME));
        initMap();
        loadMarkedDecisionData();
    }

    @VisibleForTesting
    public SjpEventStoreService(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public static synchronized  SjpEventStoreService getInstance() {
        if (instance == null) {
            instance = new SjpEventStoreService();
        }
        return instance;
    }

    public boolean decisionTransformed(final String caseId, final String decisionId) {
        return cache.get(caseId) != null &&  cache.get(caseId).contains(decisionId);
    }

    @SuppressWarnings({"squid:S00112", "squid:S1141"})
    private void initMap() {
        LOGGER.debug("Obtaining caseId-s and decision events count  from view store");
        try (
                final Connection connection = connectionProvider.getConnection();
                final PreparedStatement preparedStatement = connection.prepareStatement(DECISION_SAVED_COUNT);
                final ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                final String streamId = resultSet.getObject("stream_id", UUID.class).toString();
                final String decisionId = resultSet.getString("decision_id");
                if (!cache.containsKey(streamId)) {
                    cache.put(streamId, new HashSet<>());
                }
                cache.get(streamId).add(decisionId);
            }
        } catch (final SQLException e) {
            throw new RuntimeException("Error retrieving data using prepared statement", e);
        }
        LOGGER.info("Total Number of cases {}", cache.entrySet().size());
    }


    @SuppressWarnings("squid:S2139")
    public JsonObject getInterpreter(final String caseId, final String metaDataId) {
        try (
                final Connection connection = connectionProvider.getConnection();
                final PreparedStatement preparedStatement =
                        connection.prepareStatement(GET_INTERPRETER)) {
            preparedStatement.setString(1, caseId);
            preparedStatement.setString(2, metaDataId);
            preparedStatement.setString(3, caseId);
            preparedStatement.setString(4, caseId);
            try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return readJson(resultSet.getString(1));
                }
            }
        } catch (final SQLException e) {
            LOGGER.error("Exception while retrieving interpreter", e);
            throw new TransformationException("Error retrieving data using prepared statement", e);
        }
        return null;
    }

    @SuppressWarnings({"squid:S2139","squid:S2447"})
    public Boolean getWelshHearing(final String caseId, final String metaDataId) {
        try (final Connection connection = connectionProvider.getConnection();
             final PreparedStatement preparedStatement =
                     connection.prepareStatement(GET_WELSH_SPEAK)) {
            preparedStatement.setString(1, caseId);
            preparedStatement.setString(2, metaDataId);
            preparedStatement.setString(3, caseId);
            preparedStatement.setString(4, caseId);
            try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBoolean(1);
                }
            }
        } catch (final SQLException e) {
            LOGGER.error("Exception while retrieving welsh hearing ", e);
            throw new TransformationException("Error retrieving data using prepared statement", e);
        }
        return null;
    }

    @SuppressWarnings({"squid:S2139", "squid:S2447"})
    public List<CaseMarkedReadyReadyMetaData> getMarkedReadyForDecisionMetadata(final String caseId) {
        return caseMarkedForDecisionCache.get(caseId);
    }

    @SuppressWarnings({"squid:S134", "squid:S2139"})
    private void loadMarkedDecisionData() {
        try (final Connection connection = connectionProvider.getConnection()) {
            try (final PreparedStatement preparedStatement = connection.prepareStatement(GET_MARKED_READY_FOR_DECISION_METADATA)) {
                try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        final String streamId = resultSet.getObject("stream_id", UUID.class).toString();
                        final String metadataId = resultSet.getString("metadata_id");
                        final String markedAt = resultSet.getString("marked_at");

                        if (!caseMarkedForDecisionCache.containsKey(streamId)) {
                            caseMarkedForDecisionCache.put(streamId, new ArrayList<>());
                        }
                        caseMarkedForDecisionCache.get(streamId)
                                .add(new CaseMarkedReadyReadyMetaData(metadataId, ZonedDateTime.parse(markedAt).toLocalDate()));
                    }
                }
            }
        }
        catch (final SQLException e) {
            LOGGER.error("Exception while retrieving marked ready for decision metadata", e);
            throw new TransformationException("Exception while retrieving marked ready for decision metadata", e);
        }
        LOGGER.info("Total Number of unique cases marked for decision {}", caseMarkedForDecisionCache.entrySet().size());
    }

    private static JsonObject readJson(final String json) {
        try (final JsonReader jsonReader = Json.createReader(new StringReader(json))) {
            return jsonReader.readObject();
        }
    }

}
