package uk.gov.moj.cpp.sjp.domain.transformation.defendantdetailsmovedfrompeople;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;
import static uk.gov.moj.cpp.sjp.domain.transformation.defendantdetailsmovedfrompeople.DefendantDetailsMovedFromPeopleTransformer.DEFENDANT_DETAILS_MOVED_FROM_PEOPLE;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefendantDetailsMovedFromPeopleTransformerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();
    private static final String SOME_OTHER_EVENT = "sjp.events.some-other-event";
    private static final String expectedFileName = "sjp.events.defendant-details-moved-from-people.json";
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID PERSON_ID = UUID.randomUUID();
    private static final String DEFENDANT_ID = UUID.randomUUID().toString();

    @InjectMocks
    private DefendantDetailsMovedFromPeopleTransformer defendantDetailsMovedFromPeopleTransformer;

    @Mock
    private CaseIdDefendantIdCache cache;

    private JsonEnvelope jsonEnvelope;

    @Before
    public void onceBeforeEachTest() {
        jsonEnvelope = envelopeFrom(metadataWithRandomUUID(DEFENDANT_DETAILS_MOVED_FROM_PEOPLE)
                        .withUserId(randomUUID().toString())
                        .createdAt(ZonedDateTime.now())
                        .withClientCorrelationId(randomUUID().toString())
                        .withCausation(randomUUID(), randomUUID())
                        .withStreamId(CASE_ID)
                        .build(),
                createObjectBuilder().add("caseId", CASE_ID.toString()).add("personId", PERSON_ID.toString())
                        .build());
    }

    @Test
    public void shouldRaiseTransformActionForOnlyDefendantDetailsMovedFromPeopleEvent() {
        assertThat(defendantDetailsMovedFromPeopleTransformer.actionFor(jsonEnvelope), is(TRANSFORM));
    }

    @Test
    public void shouldNotRaiseTransformActionForAnyOtherEvent() {
        final JsonEnvelope someOtherEventEnvelope = envelopeFrom(metadataWithRandomUUID(SOME_OTHER_EVENT).build(),
                createObjectBuilder().add("title", "title").build());
        assertThat(defendantDetailsMovedFromPeopleTransformer.actionFor(someOtherEventEnvelope), is(NO_ACTION));
    }


    @Test
    public void shouldNotRaiseTransformActionIfDefendantIdExistsAlready() {
        final JsonEnvelope transformedEnvelope = envelopeFrom(metadataWithRandomUUID(DEFENDANT_DETAILS_MOVED_FROM_PEOPLE)
                        .withUserId(randomUUID().toString())
                        .createdAt(ZonedDateTime.now())
                        .withClientCorrelationId(randomUUID().toString())
                        .withCausation(randomUUID(), randomUUID())
                        .build(),
                createObjectBuilder().add("caseId", CASE_ID.toString())
                        .add("personId", PERSON_ID.toString())
                        .add("defendantId", DEFENDANT_ID)
                        .build());
        assertThat(defendantDetailsMovedFromPeopleTransformer.actionFor(transformedEnvelope), is(NO_ACTION));
    }

    @Test
    public void shouldEnrichEventWithDefendantId() {
        final JsonEnvelope sjpCreatedEventEnvelope = envelopeFrom(
                Envelope.metadataFrom(jsonEnvelope.metadata()).withName(DEFENDANT_DETAILS_MOVED_FROM_PEOPLE),
                readJson(expectedFileName, JsonValue.class, CASE_ID, PERSON_ID));
        when(cache.getDefendantId(CASE_ID.toString())).thenReturn(DEFENDANT_ID);
        final List<JsonEnvelope> envelopeList = defendantDetailsMovedFromPeopleTransformer.apply(sjpCreatedEventEnvelope).collect(toList());
        thenOutputStreamHasOneEvent(envelopeList);
        verifySjpCaseCreatedHasAllTheFieldsSetCorrectly(envelopeList);
    }


    private void thenOutputStreamHasOneEvent(final List<JsonEnvelope> envelopeList) {
        assertThat(envelopeList, hasSize(1));
    }

    protected void verifySjpCaseCreatedHasAllTheFieldsSetCorrectly(final List<JsonEnvelope> envelopeList) {
        final JsonEnvelope expectedEnvelope = envelopeFrom(
                Envelope.metadataFrom(jsonEnvelope.metadata()).withName(DEFENDANT_DETAILS_MOVED_FROM_PEOPLE),
                readJson(expectedFileName, JsonValue.class, CASE_ID, PERSON_ID));

        final JsonObject expectedPayload = expectedEnvelope.payloadAsJsonObject();

        final JsonEnvelope actualJsonEnvelope = envelopeList.get(0);
        final JsonObject actualPayload = actualJsonEnvelope.payloadAsJsonObject();

        final Metadata metadata = actualJsonEnvelope.metadata();
        assertThat(metadata.name(), is(expectedEnvelope.metadata().name()));
        assertThat(metadata.userId(), is(expectedEnvelope.metadata().userId()));
        assertThat(metadata.createdAt(), is(expectedEnvelope.metadata().createdAt()));
        assertThat(metadata.causation(), is(expectedEnvelope.metadata().causation()));
        assertThat(metadata.streamId(), is(expectedEnvelope.metadata().streamId()));
        assertThat(metadata.id(), is((expectedEnvelope.metadata().id())));
        assertThat(actualPayload.getString("caseId"), is(CASE_ID.toString()));
        assertThat(actualPayload.getString("personId"), is(PERSON_ID.toString()));

        assertThat(actualPayload.getString("defendantId"), is(DEFENDANT_ID));
        assertThat(actualPayload.getString("title"), is(expectedPayload.getString("title")));
        assertThat(actualPayload.getString("firstName"), is(expectedPayload.getString("firstName")));
        assertThat(actualPayload.getString("lastName"), is(expectedPayload.getString("lastName")));
        assertThat(actualPayload.getString("dateOfBirth"), is(expectedPayload.getString("dateOfBirth")));
        assertThat(actualPayload.getString("gender"), is(expectedPayload.getString("gender")));
        assertThat(actualPayload.getString("email"), is(expectedPayload.getString("email")));
        assertThat(actualPayload.getString("nationalInsuranceNumber"), is(expectedPayload.getString("nationalInsuranceNumber")));

        final JsonObject address = actualPayload.getJsonObject("address");
        final JsonObject expectedAddress = expectedEnvelope.payloadAsJsonObject().getJsonObject("address");

        assertThat(address.getString("address1"), is(expectedAddress.getString("address1")));
        assertThat(address.getString("address2"), is(expectedAddress.getString("address2")));
        assertThat(address.getString("address3"), is(expectedAddress.getString("address3")));
        assertThat(address.getString("address4"), is(expectedAddress.getString("address4")));
        assertThat(address.getString("address5"), is(expectedAddress.getString("address5")));
        assertThat(address.getString("postcode"), is(expectedAddress.getString("postcode")));

        final JsonObject contactNumber = actualPayload.getJsonObject("contactNumber");
        final JsonObject expectedContactNumber = expectedEnvelope.payloadAsJsonObject().getJsonObject("contactNumber");

        assertThat(contactNumber.getString("home"), is(expectedContactNumber.getString("home")));
        assertThat(contactNumber.getString("mobile"), is(expectedContactNumber.getString("mobile")));
    }

    private static <T> T readJson(final String jsonPath, final Class<T> clazz, final Object... placeholders) {
        try (final InputStream systemResourceAsStream = getSystemResourceAsStream(jsonPath)) {
            return OBJECT_MAPPER.readValue(format(IOUtils.toString(systemResourceAsStream), placeholders), clazz);
        } catch (IOException e) {
            throw new IllegalStateException("Resource " + jsonPath + " inaccessible ", e);
        }
    }
}