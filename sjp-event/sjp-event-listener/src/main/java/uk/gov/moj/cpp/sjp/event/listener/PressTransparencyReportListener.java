package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
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

    @Inject
    private PressTransparencyReportMetadataRepository pressTransparencyReportMetadataRepository;

    @Transactional
    @Handles(PressTransparencyReportGenerationStarted.EVENT_NAME)
    public void handlePressTransparencyReportGenerated(final JsonEnvelope pressTransparencyReportGeneratedEnvelope) {
        final JsonObject eventPayload = pressTransparencyReportGeneratedEnvelope.payloadAsJsonObject();
        persistReportMetadata(eventPayload);
    }

    @Transactional
    @Handles(PressTransparencyReportMetadataAdded.EVENT_NAME)
    public void handleMetadataAdded(final JsonEnvelope pressTransparencyReportMetadataAdded) {
        final JsonObject metadataAddedPayload = pressTransparencyReportMetadataAdded.payloadAsJsonObject();
        updateReportMetadata(metadataAddedPayload);
    }

    private void updateReportMetadata(final JsonObject metadataAddedPayload) {
        final UUID reportId = fromString(metadataAddedPayload.getString("pressTransparencyReportId"));
        final JsonObject metadata = metadataAddedPayload.getJsonObject("metadata");
        final PressTransparencyReportMetadata pressTransparencyReportMetadata = pressTransparencyReportMetadataRepository.findBy(reportId);
        pressTransparencyReportMetadata.setFileServiceId(fromString(metadata.getString("fileId")));
        pressTransparencyReportMetadata.setNumberOfPages(metadata.getInt("numberOfPages"));
        pressTransparencyReportMetadata.setSizeInBytes(metadata.getInt("fileSize"));
    }

    private void persistReportMetadata(final JsonObject reportGenerationStarted) {
        final String pressTransparencyReportId = reportGenerationStarted.getString("pressTransparencyReportId");

        final PressTransparencyReportMetadata pressTransparencyReportMetadata = new PressTransparencyReportMetadata(
                fromString(pressTransparencyReportId),
                LocalDateTime.now()
        );
        pressTransparencyReportMetadataRepository.save(pressTransparencyReportMetadata);
    }
}
