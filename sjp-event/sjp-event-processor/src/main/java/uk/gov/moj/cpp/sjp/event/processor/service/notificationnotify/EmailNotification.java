package uk.gov.moj.cpp.sjp.event.processor.service.notificationnotify;

import static java.util.Optional.ofNullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class EmailNotification {

    private final UUID notificationId;
    private final UUID templateId;
    private final String sendToAddress;
    private final String replyToAddress;
    private final UUID fileId;
    private final Map<String, Object> personalisation;

    private EmailNotification(final UUID notificationId,
                              final UUID templateId,
                              final String sendToAddress,
                              final String replyToAddress,
                              final UUID fileId,
                              final Map<String, Object> personalisation) {
        this.notificationId = notificationId;
        this.templateId = templateId;
        this.sendToAddress = sendToAddress;
        this.replyToAddress = replyToAddress;
        this.fileId = fileId;
        this.personalisation = personalisation;
    }

    public static Builder emailNotification() {
        return new Builder();
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public String getSendToAddress() {
        return sendToAddress;
    }

    public String getReplyToAddress() {
        return replyToAddress;
    }

    public UUID getFileId() {
        return fileId;
    }

    public Optional<String> getSubject() {
        return ofNullable(personalisation).map(p -> p.get("subject")).map(value -> (String) value);
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public static class Builder {

        private UUID notificationId;
        private UUID templateId;
        private String sendToAddress;
        private String replyToAddress;
        private UUID fileId;
        private Map<String, Object> personalisation;

        public Builder withNotificationId(final UUID notificationId) {
            this.notificationId = notificationId;
            return this;
        }

        public Builder withTemplateId(final UUID templateId) {
            this.templateId = templateId;
            return this;
        }

        public Builder withSendToAddress(final String sendToAddress) {
            this.sendToAddress = sendToAddress;
            return this;
        }

        public Builder withReplyToAddress(final String replyToAddress) {
            this.replyToAddress = replyToAddress;
            return this;
        }

        public Builder withFileId(final UUID fileId) {
            this.fileId = fileId;
            return this;
        }

        public Builder withSubject(final String subject) {
            this.personalisation = new HashMap<>();
            this.personalisation.put("subject", subject);
            return this;
        }

        public EmailNotification build() {
            return new EmailNotification(
                    this.notificationId,
                    this.templateId,
                    this.sendToAddress,
                    this.replyToAddress,
                    this.fileId,
                    personalisation
            );
        }
    }
}
