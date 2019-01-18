package uk.gov.moj.cpp.sjp.domain.transformation.datecreated;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MagistrateSessionStartedEventTransformerTest {

    private static final String EVENT_MAGISTRATE_SESSION_STARTED = "sjp.events.magistrate-session-started";
    private static final String SESSION_ID = randomUUID().toString();
    private static final String USER_ID = randomUUID().toString();
    private static final ZonedDateTime STARTED_AT = ZonedDateTime.now(UTC);
    private static final String COURT_HOUSE_NAME = "Willesden Magistrates' Court";
    private static final String COURT_HOUSE_CODE = "B01OK";
    private static final String MAGISTRATE = "test magistrate";
    private static final String LOCAL_JUSTICE_AREA_NATIONAL_COURT_CODE = "testLocalJusticeAreaNationalCourtCode";
    private static final LocalDateTime EVENT_TRANSFORMATION_DATE_TO_BE_FIXED = LocalDateTime.of(2018, 11, 26, 0, 0, 0);
    private static final LocalDateTime EVENT_TRANSFORMATION_DATE_TO_BE_IGNORED = LocalDateTime.of(2018, 12, 26, 0, 0, 0);

    @InjectMocks
    private MagistrateSessionStartedEventTransformer magistrateSessionStartedEventTransformer;

    @Before
    public void setUp() {
        magistrateSessionStartedEventTransformer.setEnveloper(createEnveloper());
    }

    @Test
    public void shouldIgnoreIrrelevantEvents() {
        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataWithRandomUUID("irrelevantEventName")
                        .createdAt(ZonedDateTime.of(EVENT_TRANSFORMATION_DATE_TO_BE_FIXED, UTC)),
                Json.createObjectBuilder().build());

        final Action action = magistrateSessionStartedEventTransformer.actionFor(jsonEnvelope);
        assertThat(action, CoreMatchers.is(NO_ACTION));
    }

    @Test
    public void shouldIgnoreEventsWithOutsideTransformationDate() {
        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataWithRandomUUID(EVENT_MAGISTRATE_SESSION_STARTED)
                        .createdAt(ZonedDateTime.of(EVENT_TRANSFORMATION_DATE_TO_BE_IGNORED, UTC)),
                Json.createObjectBuilder().build());

        final Action action = magistrateSessionStartedEventTransformer.actionFor(jsonEnvelope);
        assertThat(action, CoreMatchers.is(NO_ACTION));
    }

    @Test
    public void shouldTransformMagistrateSessionStartedEvent() {
        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataWithRandomUUID(EVENT_MAGISTRATE_SESSION_STARTED)
                        .createdAt(ZonedDateTime.of(EVENT_TRANSFORMATION_DATE_TO_BE_FIXED, UTC)),
                Json.createObjectBuilder().build());

        final Action action = magistrateSessionStartedEventTransformer.actionFor(jsonEnvelope);
        assertThat(action, CoreMatchers.is(TRANSFORM));
    }

    @Test
    public void shouldUpdateDateCreatedWithEventStartDate() {
        final JsonEnvelope originalEvent = buildEnvelope();
        final Stream<JsonEnvelope> jsonEnvelopeStream
                = magistrateSessionStartedEventTransformer.apply(originalEvent);

        final List<JsonEnvelope> events = jsonEnvelopeStream.collect(toList());

        assertThat(events, hasSize(1));
        assertThat(events.get(0).payloadAsJsonObject(), is(originalEvent.payloadAsJsonObject()));
        assertThat(events.get(0).metadata().asJsonObject(),
                is(JsonObjects.createObjectBuilder(
                        originalEvent.metadata().asJsonObject())
                        .add("createdAt", STARTED_AT.toString()).build()));
    }

    private JsonEnvelope buildEnvelope() {
        final JsonObject payload = createObjectBuilder()
                .add("sessionId", SESSION_ID)
                .add("magistrate", MAGISTRATE)
                .add("userId", USER_ID)
                .add("courtHouseCode", COURT_HOUSE_CODE)
                .add("courtHouseName", COURT_HOUSE_NAME)
                .add("localJusticeAreaNationalCourtCode", LOCAL_JUSTICE_AREA_NATIONAL_COURT_CODE)
                .add("startedAt", STARTED_AT.toString())
                .build();

        return envelopeFrom(metadataWithRandomUUID(EVENT_MAGISTRATE_SESSION_STARTED), payload);
    }
}
