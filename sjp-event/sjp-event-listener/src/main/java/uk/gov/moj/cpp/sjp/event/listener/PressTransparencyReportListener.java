package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.transparency.PressTransparencyPDFReportGenerationStarted;
import uk.gov.moj.cpp.sjp.event.transparency.PressTransparencyPDFReportMetadataAdded;
import uk.gov.moj.cpp.sjp.event.transparency.PressTransparencyReportGenerationStarted;
import uk.gov.moj.cpp.sjp.event.transparency.PressTransparencyReportMetadataAdded;
import uk.gov.moj.cpp.sjp.persistence.entity.PressTransparencyReportMetadata;
import uk.gov.moj.cpp.sjp.persistence.repository.PressTransparencyReportMetadataRepository;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class PressTransparencyReportListener {

    private static final String REPORT_ID = "pressTransparencyReportId";

    @Inject
    private PressTransparencyReportMetadataRepository pressTransparencyReportMetadataRepository;

    /**
     * Handles the event when a press transparency report is generated.
     * Use either the JSON or PDF report generation events instead. @Link{PressTransparencyReportGenerationStarted}
     *
     * @deprecated
     */
    @Deprecated
    @Transactional
    @Handles(PressTransparencyReportGenerationStarted.EVENT_NAME)
    @SuppressWarnings("squid:S1133")
    public void handlePressTransparencyReportGenerated(final JsonEnvelope pressTransparencyReportGeneratedEnvelope) {
        final JsonObject eventPayload = pressTransparencyReportGeneratedEnvelope.payloadAsJsonObject();
        persistReportMetadata(eventPayload);
    }

    /**
     * Handles the event when the metadata for a press transparency report is added. Use either the
     * JSON or PDF report metadata added events instead.
     * @Link{PressTransparencyPDFReportMetadataAdded}
     *
     * @deprecated
     */
    @Deprecated
    @Transactional
    @Handles(PressTransparencyReportMetadataAdded.EVENT_NAME)
    @SuppressWarnings("squid:S1133")
    public void handleMetadataAdded(final JsonEnvelope pressTransparencyReportMetadataAdded) {
        final JsonObject metadataAddedPayload = pressTransparencyReportMetadataAdded.payloadAsJsonObject();
        updateReportMetadataDeprecated(metadataAddedPayload);
    }

    @Transactional
    @Handles(PressTransparencyPDFReportGenerationStarted.EVENT_NAME)
    public void handlePressTransparencyPDFReportGenerated(final JsonEnvelope jsonEnvelope) {
        final JsonObject eventPayload = jsonEnvelope.payloadAsJsonObject();
        persistReportMetadata(eventPayload);
    }

    @Transactional
    @Handles(PressTransparencyPDFReportMetadataAdded.EVENT_NAME)
    public void handleReportMetadataAdded(final JsonEnvelope pressTransparencyReportMetadataAdded) {
        final JsonObject metadataAddedPayload = pressTransparencyReportMetadataAdded.payloadAsJsonObject();
        updateReportMetadata(metadataAddedPayload);
    }

    private void updateReportMetadataDeprecated(final JsonObject metadataAddedPayload) {
        final UUID reportId = fromString(metadataAddedPayload.getString(REPORT_ID));
        final JsonObject metadata = metadataAddedPayload.getJsonObject("metadata");
        final PressTransparencyReportMetadata pressTransparencyReportMetadata = pressTransparencyReportMetadataRepository.findBy(reportId);
        pressTransparencyReportMetadata.setFileServiceId(fromString(metadata.getString("fileId")));
        pressTransparencyReportMetadata.setNumberOfPages(metadata.getInt("numberOfPages"));
        pressTransparencyReportMetadata.setSizeInBytes(metadata.getInt("fileSize"));
    }

    private void updateReportMetadata(final JsonObject metadataAddedPayload) {
        final UUID reportId = fromString(metadataAddedPayload.getString(REPORT_ID));
        final JsonObject metadata = metadataAddedPayload.getJsonObject("metadata");
        final PressTransparencyReportMetadata pressTransparencyReportMetadata = pressTransparencyReportMetadataRepository.findBy(reportId);
        pressTransparencyReportMetadata.setFileServiceId(fromString(metadata.getString("fileId")));
        pressTransparencyReportMetadata.setNumberOfPages(metadata.getInt("numberOfPages"));
        pressTransparencyReportMetadata.setSizeInBytes(metadata.getInt("fileSize"));
        pressTransparencyReportMetadataRepository.save(pressTransparencyReportMetadata);
    }

    private void persistReportMetadata(final JsonObject payload) {
        final String pressTransparencyReportId = payload.getString(REPORT_ID);
        final String documentFormat = payload.getString("format");
        final String documentRequestType = payload.getString("requestType");
        final String title = payload.getString("title");
        final String language = payload.getString("language");

        final PressTransparencyReportMetadata pressTransparencyReportMetadata = new PressTransparencyReportMetadata(
                fromString(pressTransparencyReportId),
                documentFormat,
                documentRequestType,
                title,
                language,
                LocalDateTime.now()
        );
        pressTransparencyReportMetadataRepository.save(pressTransparencyReportMetadata);
    }
}
