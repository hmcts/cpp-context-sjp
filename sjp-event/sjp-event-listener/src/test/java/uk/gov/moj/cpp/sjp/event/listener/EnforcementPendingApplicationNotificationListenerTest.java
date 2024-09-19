package uk.gov.moj.cpp.sjp.event.listener;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.UUID.randomUUID;
import static org.exparity.hamcrest.date.ZonedDateTimeMatchers.after;
import static org.exparity.hamcrest.date.ZonedDateTimeMatchers.within;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.persistence.entity.EnforcementNotification.Status.GENERATED;
import static uk.gov.moj.cpp.sjp.persistence.entity.EnforcementNotification.Status.REQUIRED;
import static uk.gov.moj.cpp.sjp.persistence.entity.EnforcementNotification.Status.NOT_INITIATED;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationFailed;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationGenerationFailed;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationQueued;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationSent;
import uk.gov.moj.cpp.sjp.persistence.entity.EnforcementNotification;
import uk.gov.moj.cpp.sjp.util.fakes.FakeEnforcementPendingApplicationNotificationRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("squid:S2187")
public class EnforcementPendingApplicationNotificationListenerTest {
    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);
    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);
    @Spy
    private FakeEnforcementPendingApplicationNotificationRepository repository = new FakeEnforcementPendingApplicationNotificationRepository();
    @InjectMocks
    private EnforcementPendingApplicationNotificationListener listener;
    private static final UUID APPLICATION_ID = randomUUID();

    @Test
    public void shouldHandlerNotificationRequiredEventAndSaveToRepository() {
        final UUID applicationId = randomUUID();
        final EnforcementNotification payload =
                new EnforcementNotification(applicationId, null, REQUIRED, ZonedDateTime.now());

        listener.required(envelope(payload));

        final EnforcementNotification actual = repository.findBy(applicationId);
        assertThat(actual.getStatus(), equalTo(REQUIRED));
        assertThat(actual.getUpdated(), within(3, SECONDS, ZonedDateTime.now()));
    }

    @Test
    public void shouldHandlerNotificationNotInitiatedEventAndSaveToRepository() {

        final EnforcementNotification existingRecord = givenNoInitiationDoneForCurrentNotificationStatus();
        when(repository.findBy(APPLICATION_ID)).thenReturn(null).thenReturn(existingRecord);
        listener.generated(envelope(existingRecord));


        final EnforcementNotification actual = repository.findBy(APPLICATION_ID);
        assertThat(actual.getStatus(), equalTo(NOT_INITIATED));
        assertThat(actual.getUpdated(), within(6, SECONDS, ZonedDateTime.now()));
    }

    @Test
    public void shouldHandlerNotificationGeneratedEventAndSaveToRepository() {
        final EnforcementNotification existingRecord = givenCurrentNotificationStatusIsGenerated();

        final UUID applicationId = existingRecord.getApplicationId();
        final UUID fileId = existingRecord.getFileId();
        final EnforcementNotification payload =
                new EnforcementNotification(applicationId, fileId, GENERATED, ZonedDateTime.now());

        listener.generated(envelope(payload));

        final EnforcementNotification actual = repository.findBy(applicationId);
        assertThat(actual.getStatus(), equalTo(GENERATED));
        assertThat(actual.getUpdated(), within(3, SECONDS, ZonedDateTime.now()));
    }


    @Test
    public void shouldHandleNotificationGenerationFailedEventAndUpdateRecordIfAlreadyPresent() {
        final EnforcementNotification existingRecord = givenCurrentNotificationStatusIsGenerated();
        final EnforcementPendingApplicationNotificationGenerationFailed payload =
                new EnforcementPendingApplicationNotificationGenerationFailed(existingRecord.getApplicationId(), ZonedDateTime.now());

        listener.generationFailed(envelope(payload));

        final EnforcementNotification actual = repository.findBy(existingRecord.getApplicationId());
        assertThat(actual.getStatus(), equalTo(EnforcementNotification.Status.GENERATION_FAILED));
        assertThat(actual.getUpdated(), after(existingRecord.getUpdated()));
    }

    @Test
    public void shouldHandlerNotificationQueuedEventAndUpdateStatus() {
        final EnforcementNotification existingRecord = givenCurrentNotificationStatusIsGenerated();

        listener.notificationQueued(envelope(new EnforcementPendingApplicationNotificationQueued(existingRecord.getApplicationId(), ZonedDateTime.now())));

        final EnforcementNotification actual = repository.findBy(existingRecord.getApplicationId());
        assertThat(actual.getStatus(), equalTo(EnforcementNotification.Status.QUEUED));
        assertThat(actual.getUpdated(), after(existingRecord.getUpdated()));
    }

    @Test
    public void shouldHandleNotificationFailedEventAndUpdateStatus() {
        final EnforcementNotification existingRecord = givenCurrentNotificationStatusIsGenerated();

        listener.notificationFailed(envelope(new EnforcementPendingApplicationNotificationFailed(
                existingRecord.getApplicationId(), ZonedDateTime.now())));

        final EnforcementNotification actual = repository.findBy(existingRecord.getApplicationId());
        assertThat(actual.getStatus(), equalTo(EnforcementNotification.Status.FAILED));
        assertThat(actual.getUpdated(), after(existingRecord.getUpdated()));
    }

    @Test
    public void shouldHandlerNotificationSentEventAndUpdateStatus() {
        final EnforcementNotification existingRecord = givenCurrentNotificationStatusIsGenerated();

        listener.notificationSent(envelope(new EnforcementPendingApplicationNotificationSent(existingRecord.getApplicationId(), ZonedDateTime.now())));

        final EnforcementNotification actual = repository.findBy(existingRecord.getApplicationId());
        assertThat(actual.getStatus(), equalTo(EnforcementNotification.Status.SENT));
        assertThat(actual.getUpdated(), after(existingRecord.getUpdated()));
    }

    private EnforcementNotification givenNoInitiationDoneForCurrentNotificationStatus() {
        final EnforcementNotification entity = new EnforcementNotification(
                APPLICATION_ID,
                randomUUID(),
                NOT_INITIATED,
                ZonedDateTime.now().minusSeconds(5));
        repository.save(entity);
        return entity;
    }

    private EnforcementNotification givenCurrentNotificationStatusIsGenerated() {
        final EnforcementNotification entity = new EnforcementNotification(
                APPLICATION_ID,
                randomUUID(),
                GENERATED,
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