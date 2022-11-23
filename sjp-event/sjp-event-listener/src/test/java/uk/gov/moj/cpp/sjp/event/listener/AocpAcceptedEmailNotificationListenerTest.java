package uk.gov.moj.cpp.sjp.event.listener;


import static java.util.UUID.randomUUID;
import static org.exparity.hamcrest.date.ZonedDateTimeMatchers.after;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;


import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZonedDateTime;
import java.util.UUID;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.AocpAcceptedEmailNotificationQueued;
import uk.gov.moj.cpp.sjp.event.AocpAcceptedEmailNotificationSent;
import uk.gov.moj.cpp.sjp.persistence.entity.AocpAcceptedEmailStatus;
import uk.gov.moj.cpp.sjp.util.fakes.FakeAocpAcceptedEmailNotificationRepository;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("squid:S2187")
public class AocpAcceptedEmailNotificationListenerTest extends TestCase {
    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Spy
    @InjectMocks
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);
    @Spy
    @InjectMocks
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);
    @Spy
    private FakeAocpAcceptedEmailNotificationRepository repository = new FakeAocpAcceptedEmailNotificationRepository();
    @InjectMocks
    private AocpAcceptedEmailNotificationListener listener;
    private static final UUID CASE_ID = randomUUID();


    @Test
    public void shouldHandlerNotificationQueuedEventAndUpdateStatus() {
        final UUID caseId = randomUUID();
        final ZonedDateTime updateDate = ZonedDateTime.now();

        repository.save(new AocpAcceptedEmailStatus(caseId, null, null));

        listener.notificationQueued(envelope(new AocpAcceptedEmailNotificationQueued(caseId, ZonedDateTime.now())));

        final AocpAcceptedEmailStatus actual = repository.findBy(caseId);
        assertThat(actual.getStatus(), equalTo(AocpAcceptedEmailStatus.Status.QUEUED));
        assertThat(actual.getUpdated(), after(updateDate));
    }

    @Test
    public void shouldHandleNotificationFailedEventAndUpdateStatus() {
        final UUID caseId = randomUUID();
        final ZonedDateTime updateDate = ZonedDateTime.now();

        repository.save(new AocpAcceptedEmailStatus(caseId, null, null));

        listener.notificationFailed(envelope(new AocpAcceptedEmailNotificationQueued(
                caseId, updateDate)));

        final AocpAcceptedEmailStatus actual = repository.findBy(caseId);
        assertThat(actual.getStatus(), equalTo(AocpAcceptedEmailStatus.Status.FAILED));
        assertThat(actual.getUpdated(), after(updateDate));
    }

    @Test
    public void shouldHandlerNotificationSentEventAndUpdateStatus() {
        final UUID caseId = randomUUID();
        final ZonedDateTime updateDate = ZonedDateTime.now();

        repository.save(new AocpAcceptedEmailStatus(caseId, null, null));

        listener.notificationSent(envelope(new AocpAcceptedEmailNotificationSent(caseId, updateDate)));

        final AocpAcceptedEmailStatus actual = repository.findBy(caseId);
        assertThat(actual.getStatus(), equalTo(AocpAcceptedEmailStatus.Status.SENT));
        assertThat(actual.getUpdated(), after(updateDate));
    }



    private JsonEnvelope envelope(final Object payload) {
        return JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("test-envelope"),
                objectToJsonObjectConverter.convert(payload)
        );
    }
}