package uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.plea;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.connection.ConnectionProvider;
import uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.service.IdMappingCache;
import uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.service.SjpEventStoreService;
import uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.service.SjpViewStoreService;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;

import javax.json.JsonValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PleaUpdatedTransformerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();

    private static final String GET_INTERPRETER =
            "with tmp as " +
                    "(select position_in_stream from event_log  " +
                    "where name = 'sjp.events.plea-updated'  " +
                    "and  stream_id = CAST(? as uuid) and  metadata::json ->> 'id' =  ?) " +
                    "select payload::json -> 'interpreter' as interpreter  " +
                    "from event_log " +
                    "where position_in_stream > " +
                    "(select position_in_stream from tmp) " +
                    "and position_in_stream <= ( " +
                    "SELECT position_in_stream from event_log  " +
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

    private PleaUpdatedTransformer pleaUpdatedTransformer;

    @Mock
    private ConnectionProvider connectionProvider;

    @Mock
    private IdMappingCache caseIdOffenceIdCache;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement interpreterPreparedStatement;

    @Mock
    private ResultSet interpreterResultSet;

    @Mock
    private PreparedStatement welshSpeakPreparedStatement;

    @Mock
    private ResultSet welshSpeakResultSet;

    private static final String OFFENCE_ID = "d4ec3a35-76bf-4649-b43a-761e71c3765e";
    private static final String METADATA_ID = "b881819c-d8e7-43ce-8ab2-40b0a70978ed";
    private static final String SJP_EVENTS_PLEA_UPDATED = "sjp.events.plea-updated";
    private static final String INTERPRETER_STRING = "{\"language\": \"French\", \"needed\": true }";

    @Before
    public void setUp() throws SQLException {
        SjpEventStoreService sjpEventStoreService = new SjpEventStoreService(connectionProvider);
        SjpViewStoreService sjpViewStoreService = new SjpViewStoreService(connectionProvider, caseIdOffenceIdCache);
        pleaUpdatedTransformer = new PleaUpdatedTransformer(sjpEventStoreService, sjpViewStoreService);

        when(connectionProvider.getConnection()).thenReturn(connection);

        when(connection.prepareStatement(GET_INTERPRETER)).thenReturn(interpreterPreparedStatement);
        when(interpreterPreparedStatement.executeQuery()).thenReturn(interpreterResultSet);
        when(interpreterResultSet.next()).thenReturn(true, false);

        when(connection.prepareStatement(GET_WELSH_SPEAK)).thenReturn(welshSpeakPreparedStatement);
        when(welshSpeakPreparedStatement.executeQuery()).thenReturn(welshSpeakResultSet);
    }

    @Test
    public void shouldTransformPleaUpdatedWhenThePleaTypeIsGuilty() throws SQLException {
        when(interpreterResultSet.getString(1)).thenReturn(INTERPRETER_STRING);

        when(welshSpeakResultSet.next()).thenReturn(false);
        when(welshSpeakResultSet.getBoolean(1)).thenReturn(false);

        when(caseIdOffenceIdCache.getId(anyString(), anyString())).thenReturn(OFFENCE_ID);


        final JsonValue payload = readJson("plea/guilty/plea-updated-guilty.json", JsonValue.class);
        final JsonEnvelope envelope = envelopeFrom(
                metadataOf(METADATA_ID, SJP_EVENTS_PLEA_UPDATED), payload);

        // when
        final Stream<JsonEnvelope> transformedEventStream = pleaUpdatedTransformer.apply(envelope);

        // then
        final List<JsonEnvelope> actualTransformedEvents = transformedEventStream.collect(toList());
        assertThat(actualTransformedEvents, hasSize(2));

        assertThat(actualTransformedEvents.get(0).metadata().asJsonObject(),
                is(readJson("plea/guilty/plea-set-guilty-metadata.json",
                        JsonValue.class)));
        assertThat(actualTransformedEvents.get(0).payloadAsJsonObject(),
                is(readJson("plea/guilty/plea-set-guilty-payload.json",
                        JsonValue.class)));

        assertThat(actualTransformedEvents.get(1).metadata().asJsonObject(),
                is(equalTo(readJson("plea/guilty/plea-type-guilty-metadata.json",
                        JsonValue.class))));
        assertThat(actualTransformedEvents.get(1).payloadAsJsonObject(),
                is(equalTo(readJson("plea/guilty/plea-type-guilty-payload.json",
                        JsonValue.class))));
    }

    @Test
    public void shouldTransformPleaUpdatedWhenThePleaTypeIsNotGuilty() throws SQLException {
        when(interpreterResultSet.getString(1)).thenReturn(INTERPRETER_STRING);

        when(welshSpeakResultSet.next()).thenReturn(true, false);
        when(welshSpeakResultSet.getBoolean(1)).thenReturn(false);

        when(caseIdOffenceIdCache.getId(anyString(), anyString())).thenReturn(OFFENCE_ID);


        final JsonValue payload = readJson("plea/notguilty/plea-updated-not-guilty.json", JsonValue.class);
        final JsonEnvelope envelope = envelopeFrom(
                metadataOf(METADATA_ID, SJP_EVENTS_PLEA_UPDATED), payload);

        // when
        final Stream<JsonEnvelope> transformedEventStream = pleaUpdatedTransformer.apply(envelope);

        // then
        final List<JsonEnvelope> actualTransformedEvents = transformedEventStream.collect(toList());
        assertThat(actualTransformedEvents, hasSize(2));

        assertThat(actualTransformedEvents.get(0).metadata().asJsonObject(),
                is(readJson("plea/notguilty/plea-set-not-guilty-metadata.json",
                        JsonValue.class)));
        assertThat(actualTransformedEvents.get(0).payloadAsJsonObject(),
                is(readJson("plea/notguilty/plea-set-not-guilty-payload.json",
                        JsonValue.class)));

        assertThat(actualTransformedEvents.get(1).metadata().asJsonObject(),
                is(equalTo(readJson("plea/notguilty/plea-type-not-guilty-metadata.json",
                        JsonValue.class))));
        assertThat(actualTransformedEvents.get(1).payloadAsJsonObject(),
                is(equalTo(readJson("plea/notguilty/plea-type-not-guilty-payload.json",
                        JsonValue.class))));
    }

    @Test
    public void shouldTransformPleaUpdatedWhenThePleaTypeIsGuiltyHearingRequested() throws SQLException {
        when(interpreterResultSet.getString(1)).thenReturn(INTERPRETER_STRING);

        when(welshSpeakResultSet.next()).thenReturn(true, false);
        when(welshSpeakResultSet.getBoolean(1)).thenReturn(true);

        when(caseIdOffenceIdCache.getId(anyString(), anyString())).thenReturn(OFFENCE_ID);


        final JsonValue payload = readJson("plea/guiltyhearing/plea-updated-guiltyhearing.json", JsonValue.class);
        final JsonEnvelope envelope = envelopeFrom(
                metadataOf(METADATA_ID, SJP_EVENTS_PLEA_UPDATED), payload);

        // when
        final Stream<JsonEnvelope> transformedEventStream = pleaUpdatedTransformer.apply(envelope);

        // then
        final List<JsonEnvelope> actualTransformedEvents = transformedEventStream.collect(toList());
        assertThat(actualTransformedEvents, hasSize(2));

        assertThat(actualTransformedEvents.get(0).metadata().asJsonObject(),
                is(readJson("plea/guiltyhearing/plea-set-guiltyhearing-metadata.json",
                        JsonValue.class)));
        assertThat(actualTransformedEvents.get(0).payloadAsJsonObject(),
                is(readJson("plea/guiltyhearing/plea-set-guiltyhearing-payload.json",
                        JsonValue.class)));

        assertThat(actualTransformedEvents.get(1).metadata().asJsonObject(),
                is(equalTo(readJson("plea/guiltyhearing/plea-type-guiltyhearing-metadata.json",
                        JsonValue.class))));
        assertThat(actualTransformedEvents.get(1).payloadAsJsonObject(),
                is(equalTo(readJson("plea/guiltyhearing/plea-type-guiltyhearing-payload.json",
                        JsonValue.class))));
    }

    private static <T> T readJson(final String jsonFilePath, final Class<T> clazz, final Object... placeholders) {
        try (final InputStream systemResourceAsStream = getSystemResourceAsStream(jsonFilePath)) {
            assert systemResourceAsStream != null;
            return OBJECT_MAPPER.readValue(format(IOUtils.toString(systemResourceAsStream), placeholders), clazz);
        } catch (IOException e) {
            throw new IllegalStateException("Resource " + jsonFilePath + " inaccessible ", e);
        }
    }

}