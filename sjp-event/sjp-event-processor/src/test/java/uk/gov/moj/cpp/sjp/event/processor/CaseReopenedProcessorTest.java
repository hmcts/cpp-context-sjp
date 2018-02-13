package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;
import uk.gov.moj.cpp.sjp.event.processor.listener.CaseUpdatedListener;

import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Consumer;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseReopenedProcessorTest {

    static final String EVENT_PUBLIC_CASE_REOPENED_IN_LIBRA = "public.sjp.case-reopened-in-libra";
    static final String EVENT_PUBLIC_CASE_REOPENED_IN_LIBRA_UPDATED = "public.sjp.case-reopened-in-libra-updated";
    static final String EVENT_PUBLIC_CASE_REOPENED_IN_LIBRA_UNDONE = "public.sjp.case-reopened-in-libra-undone";

    private static final String CASE_ID = UUID.randomUUID().toString();

    @InjectMocks
    private CaseUpdatedListener caseUpdatedListener;
    @Mock
    private Sender sender;
    @Captor
    private ArgumentCaptor<JsonEnvelope> captor;
    @Spy
    private Enveloper envelopers = createEnveloper();


    @Test
    public void shouldHandleCaseReopenedInLibraEventMessage() throws Exception {
        verifyCaseReopenedEventMessage(EVENT_PUBLIC_CASE_REOPENED_IN_LIBRA, caseUpdatedListener::handleCaseReopenedInLibra);
    }

    @Test
    public void shouldHandleCaseReopenedInLibraUpdatedEventMessage() throws Exception {
        verifyCaseReopenedEventMessage(EVENT_PUBLIC_CASE_REOPENED_IN_LIBRA_UPDATED, caseUpdatedListener::handleCaseReopenedInLibraUpdated);
    }

    @Test
    public void shouldHandleCaseReopenedInLibraUndoneEventMessage() throws Exception {
        verifyCaseReopenedUndoneEventMessage(EVENT_PUBLIC_CASE_REOPENED_IN_LIBRA_UNDONE,
                caseUpdatedListener::handleCaseReopenedInLibraUndone);
    }

    private void verifyCaseReopenedEventMessage(String eventName, Consumer<JsonEnvelope> consumer) {
        verifyCaseReopenedEventMessage(
                eventName, consumer, LocalDate.now().toString(), "LIBRA12345", "no reason really"
        );
    }

    private void verifyCaseReopenedUndoneEventMessage(final String eventName,
                                                      final Consumer<JsonEnvelope> consumer) {
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder().add("caseId", CASE_ID);

        final JsonEnvelope privateEvent = EnvelopeFactory.createEnvelope(eventName, jsonObjectBuilder.build());

        consumer.accept(privateEvent);

        verify(sender).send(captor.capture());
        final JsonEnvelope publicEvent = captor.getValue();
        assertThat(publicEvent, jsonEnvelope(metadata().withName(eventName),
                payloadIsJson(withJsonPath("$.caseId", equalTo(CASE_ID)))
        ));
    }

    private void verifyCaseReopenedEventMessage(final String eventName,
                                                final Consumer<JsonEnvelope> consumer,
                                                final String reopenedDate,
                                                final String libraCaseNumber,
                                                final String reason) {
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder()
                .add("caseId", CASE_ID)
                .add("reopenedDate", reopenedDate)
                .add("libraCaseNumber", libraCaseNumber)
                .add("reason", reason);

        final JsonEnvelope privateEvent = EnvelopeFactory.createEnvelope(eventName, jsonObjectBuilder.build());

        consumer.accept(privateEvent);

        verify(sender).send(captor.capture());
        final JsonEnvelope publicEvent = captor.getValue();
        assertThat(publicEvent, jsonEnvelope(
                metadata().withName(eventName),
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", equalTo(CASE_ID)),
                        withJsonPath("$.reopenedDate", equalTo(reopenedDate)),
                        withJsonPath("$.libraCaseNumber", equalTo(libraCaseNumber)),
                        withJsonPath("$.reason", equalTo(reason))))
        ));
    }
}