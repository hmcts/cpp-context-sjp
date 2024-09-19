package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyPDFReportGenerationFailed;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyPDFReportGenerationStarted;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyPDFReportMetadataAdded;
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

    public static final String TRANSPARENCY_REPORT_ID = "transparencyReportId";
    public static final String CASE_IDS = "caseIds";
    private static final String ENGLISH = "en";
    private static final String WELSH = "cy";
    public static final String LANGUAGE = "language";
    public static final String FILE_ID = "fileId";
    public static final String METADATA = "metadata";
    public static final String NUMBER_OF_PAGES = "numberOfPages";
    public static final String FILE_SIZE = "fileSize";

    @Inject
    private CasePublishStatusRepository casePublishStatusRepository;

    @Inject
    private TransparencyReportMetadataRepository transparencyReportMetadataRepository;

    /**
     * Handles the event when a transparency report is generated.
     * Use either the JSON or PDF report generation events instead. @Link{TransparencyPDFReportGenerationStarted}
     *
     * @deprecated
     */
    @Deprecated(forRemoval = true)
    @Transactional
    @Handles(TransparencyReportGenerationStarted.EVENT_NAME)
    @SuppressWarnings("squid:S1133")
    public void handleCasesArePublished(final JsonEnvelope transparencyReportGeneratedEnvelope) {
        final JsonObject transparencyReportGeneratedPayload = transparencyReportGeneratedEnvelope.payloadAsJsonObject();
        persistReportMetadata(transparencyReportGeneratedPayload);
        incrementCountersForTheExportedCases(transparencyReportGeneratedPayload);
    }

    /**
     * Handles the event when the metadata for a transparency report is added.
     * Use either the JSON or PDF report metadata added events instead. @Link{TransparencyPDFReportMetadataAdded}
     *
     * @deprecated
     */
    @Deprecated(forRemoval = true)
    @Transactional
    @Handles(TransparencyReportMetadataAdded.EVENT_NAME)
    @SuppressWarnings("squid:S1133")
    public void handleReportMetadataIsAdded(final JsonEnvelope transparencyReportMetadataAdded) {
        final JsonObject metadataAddedPayload = transparencyReportMetadataAdded.payloadAsJsonObject();
        updateReportMetadata(metadataAddedPayload);
    }

    /**
     * Handles the event when the generation of a transparency report has failed.
     * Use either the JSON or PDF report generation failed events instead.
     *
     * @deprecated
     */
    @Deprecated(forRemoval = true)
    @Transactional
    @Handles(TransparencyReportGenerationFailed.EVENT_NAME)
    @SuppressWarnings("squid:S1133")
    public void handleTransparencyReportGenerationFailed(final JsonEnvelope transparencyReportGenerationFailed) {
        final JsonObject transparencyReportGenerationFailedPayload = transparencyReportGenerationFailed.payloadAsJsonObject();
        decrementCountersForTheExportedCases(transparencyReportGenerationFailedPayload);
    }

    @Transactional
    @Handles(TransparencyPDFReportGenerationStarted.EVENT_NAME)
    public void handleCasesArePublishedPDF(final JsonEnvelope envelope) {
        final JsonObject transparencyReportGeneratedPayload = envelope.payloadAsJsonObject();
        persistReportMetadata(transparencyReportGeneratedPayload);
        incrementCountersForTheExportedCases(transparencyReportGeneratedPayload);
    }

    @Transactional
    @Handles(TransparencyPDFReportMetadataAdded.EVENT_NAME)
    public void handlePDFReportMetadataIsAdded(final JsonEnvelope envelope) {
        final JsonObject metadataAddedPayload = envelope.payloadAsJsonObject();
        updateTransparencyReportMetadata(metadataAddedPayload);
    }

    @Transactional
    @Handles(TransparencyPDFReportGenerationFailed.EVENT_NAME)
    public void handleTransparencyPDFReportGenerationFailed(final JsonEnvelope envelope) {
        final JsonObject transparencyReportGenerationFailedPayload = envelope.payloadAsJsonObject();
        decrementCountersForTheExportedCases(transparencyReportGenerationFailedPayload);
    }

    private void updateReportMetadata(final JsonObject metadataAddedPayload) {
        final UUID transparencyReportId = UUID.fromString(metadataAddedPayload.getString(TRANSPARENCY_REPORT_ID));
        final String language = metadataAddedPayload.getString(LANGUAGE);
        final JsonObject metadata = metadataAddedPayload.getJsonObject(METADATA);
        final TransparencyReportMetadata transparencyReportMetadata = transparencyReportMetadataRepository.findBy(transparencyReportId);
        if (transparencyReportMetadata != null) {
            if (language.equals(ENGLISH)) {
                transparencyReportMetadata.setEnglishFileServiceId(fromString(metadata.getString(FILE_ID)));
                transparencyReportMetadata.setEnglishNumberOfPages(metadata.getInt(NUMBER_OF_PAGES));
                transparencyReportMetadata.setEnglishSizeInBytes(metadata.getInt(FILE_SIZE));
            } else if (language.equals(WELSH)) {
                transparencyReportMetadata.setWelshFileServiceId(fromString(metadata.getString(FILE_ID)));
                transparencyReportMetadata.setWelshNumberOfPages(metadata.getInt(NUMBER_OF_PAGES));
                transparencyReportMetadata.setWelshSizeInBytes(metadata.getInt(FILE_SIZE));
            }
        }
    }

    private void updateTransparencyReportMetadata(final JsonObject metadataAddedPayload) {
        final UUID transparencyReportId = UUID.fromString(metadataAddedPayload.getString(TRANSPARENCY_REPORT_ID));
        final JsonObject metadata = metadataAddedPayload.getJsonObject(METADATA);
        final TransparencyReportMetadata transparencyReportMetadata = transparencyReportMetadataRepository.findBy(transparencyReportId);
        if (transparencyReportMetadata != null) {
            transparencyReportMetadata.setFileServiceId(fromString(metadata.getString(FILE_ID)));
            transparencyReportMetadata.setNumberOfPages(metadata.getInt(NUMBER_OF_PAGES));
            transparencyReportMetadata.setSizeInBytes(metadata.getInt(FILE_SIZE));
        }
    }

    private void persistReportMetadata(final JsonObject payload) {

        final UUID transparencyReportId = fromString(payload.getString(TRANSPARENCY_REPORT_ID));
        final String documentFormat = payload.getString("format");
        final String documentRequestType = payload.getString("requestType");
        final String title = payload.getString("title");
        final String language = payload.getString(LANGUAGE);
        final TransparencyReportMetadata transparencyReportMetadata = new TransparencyReportMetadata(
                transparencyReportId,
                documentFormat,
                documentRequestType,
                title,
                language, LocalDateTime.now());

        transparencyReportMetadataRepository.save(transparencyReportMetadata);
    }

    private void incrementCountersForTheExportedCases(final JsonObject transparencyReportGenerated) {
        final List<UUID> caseIds = transparencyReportGenerated.getJsonArray(CASE_IDS)
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
        if (!reportGenerationPrevioulsyFailed) {
            final List<UUID> caseIds = transparencyReportGenerationFailed.getJsonArray(CASE_IDS)
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
