package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyReportGenerated;
import uk.gov.moj.cpp.sjp.persistence.entity.CasePublishStatus;
import uk.gov.moj.cpp.sjp.persistence.entity.TransparencyReportMetadata;
import uk.gov.moj.cpp.sjp.persistence.repository.CasePublishStatusRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.TransparencyReportMetadataRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.transaction.Transactional;

import org.apache.commons.collections.CollectionUtils;

@ServiceComponent(EVENT_LISTENER)
public class TransparencyReportListener {

    @Inject
    private CasePublishStatusRepository casePublishStatusRepository;

    @Inject
    private TransparencyReportMetadataRepository transparencyReportMetadataRepository;

    @Transactional
    @Handles(TransparencyReportGenerated.EVENT_NAME)
    public void handleCasesArePublished(final JsonEnvelope transparencyReportGeneratedEnvelope) {
        final JsonObject transparencyReportGeneratedPayload = transparencyReportGeneratedEnvelope.payloadAsJsonObject();
        persistReportMetadata(transparencyReportGeneratedPayload);
        incrementCountersForTheExportedCases(transparencyReportGeneratedPayload);
    }

    private void persistReportMetadata(final JsonObject transparencyReportGenerated) {
        final JsonObject englishReportMetadata = transparencyReportGenerated.getJsonObject("englishReportMetadata");
        final JsonObject welshReportMetadata = transparencyReportGenerated.getJsonObject("welshReportMetadata");

        final TransparencyReportMetadata transparencyReportMetadata = new TransparencyReportMetadata(
                fromString(englishReportMetadata.getString("fileId")),
                englishReportMetadata.getInt("numberOfPages"),
                englishReportMetadata.getInt("fileSize"),
                fromString(welshReportMetadata.getString("fileId")),
                welshReportMetadata.getInt("numberOfPages"),
                welshReportMetadata.getInt("fileSize"),
                LocalDateTime.now()
        );
        transparencyReportMetadataRepository.save(transparencyReportMetadata);
    }

    private void incrementCountersForTheExportedCases(final JsonObject transparencyReportGenerated) {
        final List<UUID> caseIds = transparencyReportGenerated.getJsonArray("caseIds")
                .getValuesAs(JsonString.class)
                .stream()
                .map(e -> fromString(e.getString()))
                .collect(toList());

        if (CollectionUtils.isNotEmpty(caseIds)) {
            casePublishStatusRepository.findByCaseIds(caseIds)
                    .forEach(this::incrementCaseCounters);
        }
    }

    private void incrementCaseCounters(final CasePublishStatus casePublishStatus) {
        casePublishStatus.incrementPublishedCounters();
        casePublishStatusRepository.save(casePublishStatus);
    }
}
