package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyReportGenerationFailed;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyReportGenerationStarted;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyReportMetadataAdded;
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

    private static final String ENGLISH = "en";
    private static final String WELSH = "cy";

    @Transactional
    @Handles(TransparencyReportGenerationStarted.EVENT_NAME)
    public void handleCasesArePublished(final JsonEnvelope transparencyReportGeneratedEnvelope) {
        final JsonObject transparencyReportGeneratedPayload = transparencyReportGeneratedEnvelope.payloadAsJsonObject();
        persistReportMetadata(transparencyReportGeneratedPayload);
        incrementCountersForTheExportedCases(transparencyReportGeneratedPayload);
    }

    @Transactional
    @Handles(TransparencyReportMetadataAdded.EVENT_NAME)
    public void handleReportMetadataIsAdded(final JsonEnvelope transparencyReportMetadataAdded) {
        final JsonObject metadataAddedPayload = transparencyReportMetadataAdded.payloadAsJsonObject();
        updateReportMetadata(metadataAddedPayload);
    }

    @Transactional
    @Handles(TransparencyReportGenerationFailed.EVENT_NAME)
    public void handleTransparencyReportGenerationFailed(final JsonEnvelope transparencyReportGenerationFailed) {
        final JsonObject transparencyReportGenerationFailedPayload = transparencyReportGenerationFailed.payloadAsJsonObject();
        decrementCountersForTheExportedCases(transparencyReportGenerationFailedPayload);
    }

    private void updateReportMetadata(final JsonObject metadataAddedPayload) {
        final UUID transparencyReportId = UUID.fromString(metadataAddedPayload.getString("transparencyReportId"));
        final String language = metadataAddedPayload.getString("language");
        final JsonObject metadata = metadataAddedPayload.getJsonObject("metadata");
        final TransparencyReportMetadata transparencyReportMetadata = transparencyReportMetadataRepository.findBy(transparencyReportId);
        if(transparencyReportMetadata!=null) {
             if(language.equals(ENGLISH)) {
                 transparencyReportMetadata.setEnglishFileServiceId(fromString(metadata.getString("fileId")));
                 transparencyReportMetadata.setEnglishNumberOfPages(metadata.getInt("numberOfPages"));
                 transparencyReportMetadata.setEnglishSizeInBytes(metadata.getInt("fileSize"));
             } else if(language.equals(WELSH)) {
                 transparencyReportMetadata.setWelshFileServiceId(fromString(metadata.getString("fileId")));
                 transparencyReportMetadata.setWelshNumberOfPages(metadata.getInt("numberOfPages"));
                 transparencyReportMetadata.setWelshSizeInBytes(metadata.getInt("fileSize"));
             }
        }
    }

    private void persistReportMetadata(final JsonObject transparencyReportGenerationStarted) {

        final UUID transparencyReportId = fromString(transparencyReportGenerationStarted.getString("transparencyReportId"));
        final TransparencyReportMetadata transparencyReportMetadata = new TransparencyReportMetadata(
                transparencyReportId, LocalDateTime.now());

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

    private void decrementCountersForTheExportedCases(final JsonObject transparencyReportGenerationFailed) {
        final boolean reportGenerationPrevioulsyFailed = transparencyReportGenerationFailed.getBoolean("reportGenerationPreviouslyFailed", false);
        if(!reportGenerationPrevioulsyFailed) {
            final List<UUID> caseIds = transparencyReportGenerationFailed.getJsonArray("caseIds")
                    .getValuesAs(JsonString.class)
                    .stream()
                    .map(e -> fromString(e.getString()))
                    .collect(toList());

            if (CollectionUtils.isNotEmpty(caseIds)) {
                casePublishStatusRepository.findByCaseIds(caseIds)
                        .forEach(this::decrementCaseCounters);
            }
        }
    }

    private void incrementCaseCounters(final CasePublishStatus casePublishStatus) {
        casePublishStatus.incrementPublishedCounters();
        casePublishStatusRepository.save(casePublishStatus);
    }

    private void decrementCaseCounters(final CasePublishStatus casePublishStatus) {
        casePublishStatus.decrementPublishedCounters();
        casePublishStatusRepository.save(casePublishStatus);
    }
}
