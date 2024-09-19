package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.UUID.randomUUID;
import static org.exparity.hamcrest.date.ZonedDateTimeMatchers.after;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.sjp.persistence.entity.EmailNotification.NotificationNotifyDocumentType.PARTIAL_AOCP_CRITERIA_NOTIFICATION;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.PartialAocpCriteriaNotificationProsecutorFailed;
import uk.gov.moj.cpp.sjp.event.PartialAocpCriteriaNotificationProsecutorQueued;
import uk.gov.moj.cpp.sjp.event.PartialAocpCriteriaNotificationProsecutorSent;
import uk.gov.moj.cpp.sjp.persistence.entity.EmailNotification;
import uk.gov.moj.cpp.sjp.util.fakes.FakeEmailNotificationRepository;

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
public class EmailNotificationListenerTest {
    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);
    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);
    @Spy
    private FakeEmailNotificationRepository repository;
    @InjectMocks
    private EmailNotificationListener listener;

    private static final UUID CASE_ID = randomUUID();


    @Test
    public void shouldHandlerNotificationQueuedEventAndUpdateStatus() {
        final UUID caseId = randomUUID();
        final ZonedDateTime updateDate = ZonedDateTime.now();

        listener.notificationQueued(envelope(new PartialAocpCriteriaNotificationProsecutorQueued(caseId)));

        final EmailNotification actual = repository.findByReferenceIdAndNotificationType(caseId, PARTIAL_AOCP_CRITERIA_NOTIFICATION);
        assertThat(actual.getStatus(), equalTo(EmailNotification.Status.QUEUED));
        assertThat(actual.getUpdated(), after(updateDate));
    }

    @Test
    public void shouldHandleNotificationFailedEventAndUpdateStatus() {
        final UUID caseId = randomUUID();
        final ZonedDateTime failedDate = ZonedDateTime.now();

        final EmailNotification emailNotification = new EmailNotification(randomUUID(), caseId, null, null, PARTIAL_AOCP_CRITERIA_NOTIFICATION);
        repository.save(emailNotification);

        listener.notificationFailed(envelope(new PartialAocpCriteriaNotificationProsecutorFailed(
                caseId, failedDate)));

        final EmailNotification actual = repository.findByReferenceIdAndNotificationType(caseId, PARTIAL_AOCP_CRITERIA_NOTIFICATION);
        assertThat(actual.getStatus(), equalTo(EmailNotification.Status.FAILED));
        assertThat(actual.getUpdated(), after(failedDate));
    }

    @Test
    public void shouldHandlerNotificationSentEventAndUpdateStatus() {
        final UUID caseId = randomUUID();
        final ZonedDateTime sentDate = ZonedDateTime.now();

        final EmailNotification emailNotification = new EmailNotification(randomUUID(), caseId, null, null, PARTIAL_AOCP_CRITERIA_NOTIFICATION);
        repository.save(emailNotification);

        listener.notificationSent(envelope(new PartialAocpCriteriaNotificationProsecutorSent(caseId, sentDate)));

        final EmailNotification actual = repository.findByReferenceIdAndNotificationType(caseId, PARTIAL_AOCP_CRITERIA_NOTIFICATION);
        assertThat(actual.getStatus(), equalTo(EmailNotification.Status.SENT));
        assertThat(actual.getUpdated(), after(sentDate));
    }



    private JsonEnvelope envelope(final Object payload) {
        return JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("test-envelope"),
                objectToJsonObjectConverter.convert(payload)
        );
    }
}