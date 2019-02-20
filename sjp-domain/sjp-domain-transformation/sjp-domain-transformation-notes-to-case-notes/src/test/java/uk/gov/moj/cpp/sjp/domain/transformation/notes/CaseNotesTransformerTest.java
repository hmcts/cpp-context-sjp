package uk.gov.moj.cpp.sjp.domain.transformation.notes;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;
import static uk.gov.moj.cpp.sjp.domain.transformation.notes.CaseNotesTransformer.CASE_COMPLETED_EVENT;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.domain.transformation.connection.ResultingService;
import uk.gov.moj.cpp.sjp.domain.transformation.connection.UsersAndGroupService;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
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
public class CaseNotesTransformerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();
    private static final String DECISION_NOTES_NEWLY_ADDED = "Decision notes newly added";
    private static final String NOTE_TYPE = "LISTING";
    private static final String CREATED_AT = now().toString();
    private static final UUID DECISION_ID = randomUUID();
    private static final String CASE_NOTE_ADDED_EVENT = "sjp.events.case-note-added";

    @InjectMocks
    private CaseNotesTransformer caseNotesTransformer;

    @Mock
    private ResultingService resultingService;

    @Mock
    private UsersAndGroupService usersAndGroupService;

    private String caseId = randomUUID().toString();

    private UUID userId = randomUUID();

    private JsonEnvelope jsonEnvelope;

    private String expectedFileName = "case-note-added.json";

    @Before
    public void onceBeforeEachTest() {
        jsonEnvelope = envelopeFrom(metadataWithRandomUUID(CASE_COMPLETED_EVENT)
                        .withUserId(randomUUID().toString())
                        .createdAt(ZonedDateTime.now())
                        .withClientCorrelationId(randomUUID().toString())
                        .withCausation(randomUUID(), randomUUID())
                        .build(),
                createObjectBuilder().add("caseId", caseId).build());
    }

    @Test
    public void shouldRaiseTransformActionForOnlyCaseCompletedEvent() {
        assertThat(caseNotesTransformer.actionFor(jsonEnvelope), is(TRANSFORM));
    }

    @Test
    public void shouldNotRaiseTransformActionForAnyOtherEvent() {
        JsonEnvelope caseNoteAddedEventEnvelope = envelopeFrom(metadataWithRandomUUID(CASE_NOTE_ADDED_EVENT).build(),
                createObjectBuilder().add("caseId", caseId).build());
        assertThat(caseNotesTransformer.actionFor(caseNoteAddedEventEnvelope), is(NO_ACTION));
    }

    @Test
    public void shouldRaiseNewCaseNoteCreatedEventWhenThenThereIsANote() {
        when(usersAndGroupService.getUserDetails(userId.toString())).thenReturn(getUserDetails());
        when(resultingService.getCaseDecisionFor(caseId)).thenReturn(getCaseDecisions());
        final List<JsonEnvelope> envelopeList = whenTheTransformationIsApplied();
        thenOutputStreamHasTwoEvents(envelopeList);
        thenCaseNoteAddedEventHasAllTheFieldsSetCorrectly(envelopeList);
        thenTheStreamAlsoHasCaseCompletedEvent(envelopeList);
    }

    @Test
    public void shouldNotRaiseCaseNoteAddedEventWhenThereIsNoNote() {
        when(resultingService.getCaseDecisionFor(caseId)).thenReturn(empty());
        final List<JsonEnvelope> envelopeList = whenTheTransformationIsApplied();
        assertThat(envelopeList, hasSize(1));
        assertThat(envelopeList, contains(jsonEnvelope));
    }


    private void thenOutputStreamHasTwoEvents(final List<JsonEnvelope> envelopeList) {
        assertThat(envelopeList, hasSize(2));
    }

    private void thenTheStreamAlsoHasCaseCompletedEvent(final List<JsonEnvelope> envelopeList) {
        assertThat(envelopeList, hasItem(jsonEnvelope));
    }


    protected void thenCaseNoteAddedEventHasAllTheFieldsSetCorrectly(final List<JsonEnvelope> envelopeList) {
        final JsonEnvelope expectedEnvelope = envelopeFrom(
                metadataFrom(jsonEnvelope.metadata()).withName(CASE_NOTE_ADDED_EVENT),
                readJson(expectedFileName, JsonValue.class, caseId, DECISION_NOTES_NEWLY_ADDED, NOTE_TYPE, CREATED_AT, userId.toString(), DECISION_ID));

        final JsonObject expectedPayload = expectedEnvelope.payloadAsJsonObject();

        final JsonEnvelope actualJsonEnvelope = envelopeList.get(1);

        final JsonObject caseNoteAddedEvent = actualJsonEnvelope.payloadAsJsonObject();
        final Metadata metadata = actualJsonEnvelope.metadata();
        assertThat(metadata.name(), is(expectedEnvelope.metadata().name()));
        assertThat(metadata.userId(), is(expectedEnvelope.metadata().userId()));
        assertThat(metadata.createdAt(), is(expectedEnvelope.metadata().createdAt()));
        assertThat(metadata.causation(), is(expectedEnvelope.metadata().causation()));
        assertThat(metadata.streamId(), is(expectedEnvelope.metadata().streamId()));
        assertThat(metadata.id(), is(not(expectedEnvelope.metadata().id())));

        assertThat(caseNoteAddedEvent.getString("caseId"), is(expectedPayload.getString("caseId")));
        assertThat(caseNoteAddedEvent.getString("decisionId"), is(expectedPayload.getString("decisionId")));
        assertThat(caseNoteAddedEvent.getJsonObject("note").getString("id"), is(notNullValue()));
        assertThat(caseNoteAddedEvent.getJsonObject("note").getString("text"), is(expectedPayload.getJsonObject("note").getString("text")));
        assertThat(caseNoteAddedEvent.getJsonObject("note").getString("type"), is(expectedPayload.getJsonObject("note").getString("type")));
        assertThat(caseNoteAddedEvent.getJsonObject("note").getString("addedAt"), is(expectedPayload.getJsonObject("note").getString("addedAt")));
        assertThat(caseNoteAddedEvent.getJsonObject("author").getString("userId"), is(expectedPayload.getJsonObject("author").getString("userId")));
        assertThat(caseNoteAddedEvent.getJsonObject("author").getString("firstName"), is(expectedPayload.getJsonObject("author").getString("firstName")));
        assertThat(caseNoteAddedEvent.getJsonObject("author").getString("lastName"), is(expectedPayload.getJsonObject("author").getString("lastName")));
    }

    protected List<JsonEnvelope> whenTheTransformationIsApplied() {
        return caseNotesTransformer.apply(jsonEnvelope).collect(toList());
    }

    private Optional<CaseDecisionDetails> getCaseDecisions() {
        CaseDecisionDetails decisionDetails = new CaseDecisionDetails(DECISION_ID,
                DECISION_NOTES_NEWLY_ADDED, userId.toString(), CREATED_AT, NOTE_TYPE);
        return Optional.of(decisionDetails);
    }

    private UserDetails getUserDetails() {
        return new UserDetails("Jonathan", "Smith");
    }


    private static <T> T readJson(final String jsonPath, final Class<T> clazz, final Object... placeholders) {
        try (final InputStream systemResourceAsStream = getSystemResourceAsStream(jsonPath)) {
            return OBJECT_MAPPER.readValue(format(IOUtils.toString(systemResourceAsStream), placeholders), clazz);
        } catch (IOException e) {
            throw new IllegalStateException("Resource " + jsonPath + " inaccessible ", e);
        }
    }
}
