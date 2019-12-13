package uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.withdrawoffence;


import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;
import static uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.withdrawoffence.WithdrawOffenceDecisionTransformer.ALL_OFFENCES_WITHDRAWAL_REQUESTED;
import static uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.withdrawoffence.WithdrawOffenceDecisionTransformer.ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.moj.cpp.sjp.domain.transformation.connection.ConnectionProvider;
import uk.gov.moj.cpp.sjp.domain.transformation.service.SjpEventStoreService;
import uk.gov.moj.cpp.sjp.domain.transformation.service.SjpViewStoreService;
import uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.plea.PleaUpdatedTransformer;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import liquibase.sql.Sql;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WithdrawOffenceDecisionTransformerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();

    @Mock
    private ConnectionProvider connectionProvider;

    @Mock
    private PreparedStatement offenceIdAndDefendantIdPreparedStatement;

    @Mock
    private Connection connection;

    @Mock
    private ResultSet offenceIdAndDefendantIdResultSet;

    private WithdrawOffenceDecisionTransformer sut;

    private static final String OFFENCE_AND_DEFENDANT_ID = "select CAST(cd.id as text) as caseId, CAST(o.id as text) as offenceId, CAST(d.id as text) as defendantId from case_details cd, defendant d, offence o where cd.id = d.case_id and d.id = o.defendant_id";

    private static final String OFFENCE_ID = "k4ec3a35-76bf-4649-b43a-761e71c3765k";
    private static final String DEFENDANT_ID = "e4ec3a35-76bf-4649-b43a-761e71c3765e";

    @Before
    public void setUp() throws SQLException {
        final SjpViewStoreService sjpViewStoreService = new SjpViewStoreService(connectionProvider);
        sut = new WithdrawOffenceDecisionTransformer(sjpViewStoreService);

        when(connectionProvider.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(OFFENCE_AND_DEFENDANT_ID)).thenReturn(offenceIdAndDefendantIdPreparedStatement);
        when(offenceIdAndDefendantIdPreparedStatement.executeQuery()).thenReturn(offenceIdAndDefendantIdResultSet);
        when(offenceIdAndDefendantIdResultSet.next()).thenReturn(true, false);
        when(offenceIdAndDefendantIdResultSet.getString("defendantId")).thenReturn(DEFENDANT_ID);

    }


    @Test
    public void shouldActionTransformForTheEventAllOffencesWithdrawalRequested() {
        // given
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.all-offences-withdrawal-requested"),
                JsonValue.NULL);

        // when
        final Action action = sut.actionFor(envelope);

        // then
        assertThat(action, is(TRANSFORM));
    }


    @Test
    public void shouldActionTransformForTheEventAllOffencesWithdrawRequestCancelled() {
        // given
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.all-offences-withdrawal-request-cancelled"),
                JsonValue.NULL);

        // when
        final Action action = sut.actionFor(envelope);

        // then
        assertThat(action, is(TRANSFORM));
    }


    @Test
    public void shouldReturnEmptyStreamWhenTheTransformationIsAppliedOnNonWithdrawalEvent() {
        // given
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.dummy"),
                readJson("withdrawal-requested/all-offences-withdrawal-requested.json", JsonValue.class));

        // when
        final Stream transformedEventStream = sut.apply(envelope);

        // then
        assertThat(transformedEventStream.count(), equalTo(0L));
    }

    @Test
    public void shouldTransformTheEventAllOffencesWithdrawalRequested() throws SQLException {
        // given
        when(offenceIdAndDefendantIdResultSet.getString("caseId")).thenReturn("c4ec3a35-76bf-4649-b43a-761e71c3765e");
        when(offenceIdAndDefendantIdResultSet.getString("offenceId")).thenReturn(OFFENCE_ID);


        final String createdAt = "2019-08-05T12:13:43.362Z";
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID(ALL_OFFENCES_WITHDRAWAL_REQUESTED)
                        .withUserId("4c8770db-02e6-4f4b-829e-2af9ea97d12f")
                        .withId(UUID.fromString("3c8770db-02e6-4f4b-829e-2af9ea97d12f"))
                        .createdAt(ZonedDateTimes.fromString(createdAt))
                ,
                readJson("withdrawal-requested/all-offences-withdrawal-requested.json", JsonValue.class));

        // when
        final Stream<JsonEnvelope> transformedEventStream = sut.apply(envelope);

        // then
        final List<JsonEnvelope> actualTransformedEvents = transformedEventStream.collect(toList());
        assertThat(actualTransformedEvents, hasSize(2));

        assertThat(actualTransformedEvents.get(0).metadata().asJsonObject(),
                is(readJson("withdrawal-requested/offences-withdrawal-status-set-metadata.json",
                        JsonValue.class)));
        assertThat(actualTransformedEvents.get(0).payloadAsJsonObject(),
                is(readJson("withdrawal-requested/offences-withdrawal-status-set-payload.json",
                        JsonValue.class)));

        assertThat(actualTransformedEvents.get(1).metadata().asJsonObject(),
                is(equalTo(readJson("withdrawal-requested/offence-withdrawal-requested-metadata.json",
                        JsonValue.class))));
        assertThat(actualTransformedEvents.get(1).payloadAsJsonObject(),
                is(equalTo(readJson("withdrawal-requested/offence-withdrawal-requested-payload.json",
                        JsonValue.class))));

    }

    @Test
    public void shouldTransformTheEventAllOffencesWithdrawalRequestCancelled() throws SQLException{
        // given
        when(offenceIdAndDefendantIdResultSet.getString("caseId")).thenReturn("d4ec3a35-76bf-4649-b43a-761e71c3765e");
        when(offenceIdAndDefendantIdResultSet.getString("offenceId")).thenReturn("k4ec3a35-76bf-4649-b43a-761e71c3765e");


        final String createdAt = "2019-08-05T12:13:44.362Z";
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID(ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED)
                        .withUserId("4c8770db-02e6-4f4b-829e-2af9ea97d12f")
                        .withId(UUID.fromString("3c8770db-02e6-4f4b-829e-2af9ea97d12f"))
                        .createdAt(ZonedDateTimes.fromString(createdAt))
                ,
                readJson("withdrawal-request-cancelled/all-offences-withdrawal-request-cancelled.json", JsonValue.class));

        // when
        final Stream<JsonEnvelope> transformedEventStream = sut.apply(envelope);

        // then
        final List<JsonEnvelope> actualTransformedEvents = transformedEventStream.collect(toList());
        assertThat(actualTransformedEvents, hasSize(2));

        assertThat(actualTransformedEvents.get(0).metadata().asJsonObject(),
                is(readJson("withdrawal-request-cancelled/offences-withdrawal-status-set-metadata.json",
                        JsonValue.class)));
        assertThat(actualTransformedEvents.get(0).payloadAsJsonObject(),
                is(readJson("withdrawal-request-cancelled/offences-withdrawal-status-set-payload.json",
                        JsonValue.class)));

        assertThat(actualTransformedEvents.get(1).metadata().asJsonObject(),
                is(equalTo(readJson("withdrawal-request-cancelled/offence-withdrawal-request-cancelled-metadata.json",
                        JsonValue.class))));
        assertThat(actualTransformedEvents.get(1).payloadAsJsonObject(),
                is(equalTo(readJson("withdrawal-request-cancelled/offence-withdrawal-request-cancelled-payload.json",
                        JsonValue.class))));
    }

    private static <T> T readJson(final String jsonFilePath, final Class<T> clazz, final Object... placeholders) {
        try (final InputStream systemResourceAsStream = getSystemResourceAsStream(jsonFilePath)) {
            return OBJECT_MAPPER.readValue(format(IOUtils.toString(systemResourceAsStream), placeholders), clazz);
        } catch (IOException e) {
            throw new IllegalStateException("Resource " + jsonFilePath + " inaccessible ", e);
        }
    }


}