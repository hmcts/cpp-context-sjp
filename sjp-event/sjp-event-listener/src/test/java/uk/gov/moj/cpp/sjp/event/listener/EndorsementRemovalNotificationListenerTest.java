package uk.gov.moj.cpp.sjp.event.listener;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.UUID.randomUUID;
import static org.exparity.hamcrest.date.ZonedDateTimeMatchers.after;
import static org.exparity.hamcrest.date.ZonedDateTimeMatchers.within;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.NotificationToRemoveEndorsementsFailed;
import uk.gov.moj.cpp.sjp.event.NotificationToRemoveEndorsementsGenerated;
import uk.gov.moj.cpp.sjp.event.NotificationToRemoveEndorsementsGenerationFailed;
import uk.gov.moj.cpp.sjp.event.NotificationToRemoveEndorsementsQueued;
import uk.gov.moj.cpp.sjp.event.NotificationToRemoveEndorsementsSent;
import uk.gov.moj.cpp.sjp.persistence.entity.NotificationOfEndorsementStatus;
import uk.gov.moj.cpp.sjp.persistence.entity.NotificationOfEndorsementStatus.Status;
import uk.gov.moj.cpp.sjp.util.fakes.FakeEndorsementRemovalNotificationRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EndorsementRemovalNotificationListenerTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);
    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);
    @Spy
    private FakeEndorsementRemovalNotificationRepository repository;
    @InjectMocks
    private EndorsementRemovalNotificationListener listener;

    @Test
    public void shouldHandlerNotificationGeneratedEventAndSaveToRepository() {
        final UUID applicationDecisionId = randomUUID();
        final UUID fileId = randomUUID();
        final NotificationToRemoveEndorsementsGenerated payload =
                new NotificationToRemoveEndorsementsGenerated(applicationDecisionId, fileId);

        listener.generated(envelope(payload));

        final NotificationOfEndorsementStatus actual = repository.findBy(applicationDecisionId);
        assertThat(actual.getFileId(), equalTo(fileId));
        assertThat(actual.getStatus(), equalTo(Status.GENERATED));
        assertThat(actual.getUpdated(), within(3, SECONDS, ZonedDateTime.now()));
    }

    @Test
    public void shouldHandlerNotificationGenerationFailedEventSaveToRepository() {
        final UUID applicationDecisionId = randomUUID();
        final NotificationToRemoveEndorsementsGenerationFailed payload =
                new NotificationToRemoveEndorsementsGenerationFailed(applicationDecisionId);

        listener.generationFailed(envelope(payload));

        final NotificationOfEndorsementStatus actual = repository.findBy(applicationDecisionId);
        assertThat(actual.getFileId(), nullValue());
        assertThat(actual.getStatus(), equalTo(Status.GENERATION_FAILED));
        assertThat(actual.getUpdated(), within(2, SECONDS, ZonedDateTime.now()));
    }

    @Test
    public void shouldHandleNotificationGenerationFailedEventAndUpdateRecordIfAlreadyPresent() {
        final NotificationOfEndorsementStatus existingRecord = givenCurrentNotificationStatusIsGenerated();
        final NotificationToRemoveEndorsementsGenerationFailed payload =
                new NotificationToRemoveEndorsementsGenerationFailed(existingRecord.getApplicationDecisionId());

        listener.generationFailed(envelope(payload));

        final NotificationOfEndorsementStatus actual = repository.findBy(existingRecord.getApplicationDecisionId());
        assertThat(actual.getFileId(), nullValue());
        assertThat(actual.getStatus(), equalTo(Status.GENERATION_FAILED));
        assertThat(actual.getUpdated(), after(existingRecord.getUpdated()));
    }

    @Test
    public void shouldHandlerNotificationQueuedEventAndUpdateStatus() {
        final NotificationOfEndorsementStatus existingRecord = givenCurrentNotificationStatusIsGenerated();

        listener.notificationQueued(envelope(new NotificationToRemoveEndorsementsQueued(existingRecord.getApplicationDecisionId())));

        final NotificationOfEndorsementStatus actual = repository.findBy(existingRecord.getApplicationDecisionId());
        assertThat(actual.getStatus(), equalTo(Status.QUEUED));
        assertThat(actual.getUpdated(), after(existingRecord.getUpdated()));
    }

    @Test
    public void shouldHandleNotificationFailedEventAndUpdateStatus() {
        final NotificationOfEndorsementStatus existingRecord = givenCurrentNotificationStatusIsGenerated();

        listener.notificationFailed(envelope(new NotificationToRemoveEndorsementsFailed(
                existingRecord.getApplicationDecisionId(), ZonedDateTime.now())));

        final NotificationOfEndorsementStatus actual = repository.findBy(existingRecord.getApplicationDecisionId());
        assertThat(actual.getStatus(), equalTo(Status.FAILED));
        assertThat(actual.getUpdated(), after(existingRecord.getUpdated()));
    }

    @Test
    public void shouldHandlerNotificationSentEventAndUpdateStatus() {
        final NotificationOfEndorsementStatus existingRecord = givenCurrentNotificationStatusIsGenerated();

        listener.notificationSent(envelope(new NotificationToRemoveEndorsementsSent(existingRecord.getApplicationDecisionId(), ZonedDateTime.now())));

        final NotificationOfEndorsementStatus actual = repository.findBy(existingRecord.getApplicationDecisionId());
        assertThat(actual.getStatus(), equalTo(Status.SENT));
        assertThat(actual.getUpdated(), after(existingRecord.getUpdated()));
    }

    private NotificationOfEndorsementStatus givenCurrentNotificationStatusIsGenerated() {
        final NotificationOfEndorsementStatus entity = new NotificationOfEndorsementStatus(
                randomUUID(),
                null,
                Status.GENERATED,
                ZonedDateTime.now().minusSeconds(5));
        repository.save(entity);
        return entity;
    }

    private JsonEnvelope envelope(final Object payload) {
        return JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("test-envelope"),
                objectToJsonObjectConverter.convert(payload)
        );
    }
}