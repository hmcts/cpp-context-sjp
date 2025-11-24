package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;

import uk.gov.justice.json.schemas.domains.sjp.NoteType;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonValue;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseNoteAddedProcessorTest {

    @InjectMocks
    private CaseNoteAddedProcessor caseNoteAddedProcessor;

    @Mock
    protected Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<JsonValue>> envelopeCaptor;


    @Test
    public void shouldHandleCaseNoteAddedEvent() {
        assertThat(CaseNoteAddedProcessor.class, isHandlerClass(EVENT_PROCESSOR).with(
                method("handleCaseNoteAdded").thatHandles("sjp.events.case-note-added")));
    }

    @Test
    public void shouldRaisePublicEvent() {
        final UUID caseId = randomUUID();
        final UUID noteId = randomUUID();
        final String addedAt = ZonedDateTimes.toString(ZonedDateTime.now());
        final JsonEnvelope privateEvent = EnvelopeFactory.createEnvelope("sjp.events.case-note-added",
                createObjectBuilder()
                        .add(CASE_ID, caseId.toString())
                        .add("author", createObjectBuilder()
                                .add("userId", "userId")
                                .add("firstName", "test")
                                .add("lastName", "user")
                        )
                        .add("note", createObjectBuilder()
                                .add("id", noteId.toString())
                                .add("text", "A sample note")
                                .add("type", NoteType.CASE_MANAGEMENT.toString())
                                .add("addedAt", addedAt)
                        )
                        .build()
        );
        caseNoteAddedProcessor.handleCaseNoteAdded(privateEvent);
        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonValue> sentEnvelope = envelopeCaptor.getValue();

        assertThat(sentEnvelope.metadata().name(), equalTo(CaseNoteAddedProcessor.CASE_NOTE_ADDED_PUBLIC_EVENT_NAME));
        assertThat(sentEnvelope.payload(),
                payloadIsJson(allOf(
                        withJsonPath("$.id", CoreMatchers.equalTo(caseId.toString())),
                        withJsonPath("$.authorId", CoreMatchers.equalTo("userId")),
                        withJsonPath("$.noteId", CoreMatchers.equalTo(noteId.toString())),
                        withJsonPath("$.noteType", CoreMatchers.equalTo("CASE_MANAGEMENT")),
                        withJsonPath("$.addedAt", CoreMatchers.equalTo(addedAt))

                        )
                )
        );
    }

}
