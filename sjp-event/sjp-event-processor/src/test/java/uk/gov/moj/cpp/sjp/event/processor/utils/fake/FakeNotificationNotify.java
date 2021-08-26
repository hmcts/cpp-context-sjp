package uk.gov.moj.cpp.sjp.event.processor.utils.fake;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.service.notificationnotify.EmailNotification;
import uk.gov.moj.cpp.sjp.event.processor.service.notificationnotify.NotificationNotify;

import java.util.ArrayList;
import java.util.List;

public class FakeNotificationNotify extends NotificationNotify {

    private List<EmailNotification> emailNotifications = new ArrayList<>();

    @Override
    public void sendEmail(final EmailNotification emailNotification, final JsonEnvelope envelope) {
        this.emailNotifications.add(emailNotification);
    }

    public List<EmailNotification> getSendEmailRequests() {
        return this.emailNotifications;
    }
}
