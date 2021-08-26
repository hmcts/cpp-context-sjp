package uk.gov.moj.cpp.sjp.event.processor.service;

import static java.lang.String.format;
import static java.util.Optional.empty;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.sjp.event.processor.service.exceptions.ContextSystemUserIdException;
import uk.gov.moj.cpp.systemidmapper.client.AdditionResponse;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMap;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapperClient;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class SystemIdMapperService {

    private static final String TARGET_TYPE = "CASE_ID";

    @Inject
    private SystemUserProvider systemUserProvider;

    @Inject
    private SystemIdMapperClient systemIdMapperClient;

    public Optional<SystemIdMapping> getSystemIdMappingForNotificationId(final UUID notificationId) {

        final Optional<SystemIdMapping> intentionToDisqualifyNoticeMapping = systemIdMapperClient.findBy(notificationId, TARGET_TYPE, getSystemUserId());
        if (intentionToDisqualifyNoticeMapping.isPresent()) {
            return intentionToDisqualifyNoticeMapping;
        }
        return empty();
    }


    @SuppressWarnings("squid:S3655") // squash sonar warnings on work in progress Optional.get(...)
    public void mapNotificationIdToCaseId(final UUID caseId, final UUID notificationId, final NotificationNotifyDocumentType documentType) {

        final SystemIdMap systemIdMap = new SystemIdMap(notificationId.toString(), documentType.name(), caseId, TARGET_TYPE);

        final AdditionResponse response = systemIdMapperClient.add(systemIdMap, getSystemUserId());

        if (!response.isSuccess()) {
            throw new IllegalStateException(format("Failed to map case Id: %s to notification id: %s with result code: %s", caseId, notificationId, response.code().toString()));
        }
    }

    private UUID getSystemUserId() {
        return systemUserProvider.getContextSystemUserId().orElseThrow(
                () -> new ContextSystemUserIdException("System user id not available for resulting context"));
    }
}




