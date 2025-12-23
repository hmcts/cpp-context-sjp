package uk.gov.moj.cpp.sjp.event.listener;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.DocumentFormat.PDF;
import static uk.gov.moj.cpp.sjp.domain.DocumentLanguage.ENGLISH;
import static uk.gov.moj.cpp.sjp.domain.DocumentRequestType.DELTA;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.PressTransparencyReportMetadata;
import uk.gov.moj.cpp.sjp.persistence.repository.PressTransparencyReportMetadataRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import javax.json.JsonArrayBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PressTransparencyReportListenerTest {

    @InjectMocks
    private PressTransparencyReportListener pressTransparencyReportListener;

    @Mock
    private PressTransparencyReportMetadataRepository pressTransparencyReportMetadataRepository;

    @Test
    @SuppressWarnings("deprecation")
    public void shouldCreatePressReportMetadata() {
        final List<UUID> caseIds = newArrayList(randomUUID(), randomUUID());
        final UUID reportId = randomUUID();

        final JsonEnvelope eventEnvelope = envelopeFrom(metadataWithRandomUUID("sjp.events.press-transparency-report-generation-started"),
                createObjectBuilder()
                        .add("pressTransparencyReportId",reportId.toString())
                        .add("format", PDF.name())
                        .add("requestType", DELTA.name())
                        .add("title", "Press Transparency Report")
                        .add("language", ENGLISH.name())
                        .add("caseIds", createJsonArrayWithCaseIds(caseIds))
                        .build());

        pressTransparencyReportListener.handlePressTransparencyReportGenerated(eventEnvelope);

        final ArgumentCaptor<PressTransparencyReportMetadata> pressTransparencyReportMetadataArgument = ArgumentCaptor.forClass(PressTransparencyReportMetadata.class);
        verify(pressTransparencyReportMetadataRepository).save(pressTransparencyReportMetadataArgument.capture());
        final PressTransparencyReportMetadata pressTransparencyReportMetadata = pressTransparencyReportMetadataArgument.getValue();
        assertThat(pressTransparencyReportMetadata.getId(), is(reportId));
    }

    @Test
    public void shouldCreatePressReportPDFMetadata() {
        final List<UUID> caseIds = newArrayList(randomUUID(), randomUUID());
        final UUID reportId = randomUUID();

        final JsonEnvelope eventEnvelope = envelopeFrom(metadataWithRandomUUID("sjp.events.press-transparency-pdf-report-generation-started"),
                createObjectBuilder()
                        .add("pressTransparencyReportId", reportId.toString())
                        .add("format", PDF.name())
                        .add("requestType", DELTA.name())
                        .add("title", "Press Transparency Report")
                        .add("language", ENGLISH.name())
                        .add("caseIds", createJsonArrayWithCaseIds(caseIds))
                        .build());

        pressTransparencyReportListener.handlePressTransparencyPDFReportGenerated(eventEnvelope);

        final ArgumentCaptor<PressTransparencyReportMetadata> pressTransparencyReportMetadataArgument = ArgumentCaptor.forClass(PressTransparencyReportMetadata.class);
        verify(pressTransparencyReportMetadataRepository).save(pressTransparencyReportMetadataArgument.capture());
        final PressTransparencyReportMetadata pressTransparencyReportMetadata = pressTransparencyReportMetadataArgument.getValue();
        assertThat(pressTransparencyReportMetadata.getId(), is(reportId));
        assertThat(pressTransparencyReportMetadata.getGeneratedAt() instanceof LocalDateTime, is(true));

    }

    @Test
    @SuppressWarnings("deprecation")
    public void shouldUpdateReportMetadata() {
        final UUID reportId = randomUUID();
        final PressTransparencyReportMetadata reportMetadata = new PressTransparencyReportMetadata(reportId, LocalDateTime.now());
        final UUID reportFileId = randomUUID();
        final int reportNumberOfPages = 4;
        final int pdfSizeInBytes = 412;

        final JsonEnvelope eventEnvelope = envelopeFrom(metadataWithRandomUUID("sjp.events.press-transparency-report-metadata-added"),
                createObjectBuilder()
                        .add("pressTransparencyReportId", reportId.toString())
                        .add("metadata", createObjectBuilder()
                                .add("fileId", reportFileId.toString())
                                .add("numberOfPages", reportNumberOfPages)
                                .add("fileSize", pdfSizeInBytes)
                                .add("title", "Press Transparency Report")
                                .add("language", ENGLISH.name())
                        )
                        .build());
        when(pressTransparencyReportMetadataRepository.findBy(reportId)).thenReturn(reportMetadata);
        pressTransparencyReportListener.handleMetadataAdded(eventEnvelope);
        assertThat(reportMetadata.getFileServiceId(), is(reportFileId));
        assertThat(reportMetadata.getNumberOfPages(), is(reportNumberOfPages));
        assertThat(reportMetadata.getSizeInBytes(), is(pdfSizeInBytes));

    }

    @Test
    public void shouldUpdateReportMetadataPDF() {
        final UUID reportId = randomUUID();
        final PressTransparencyReportMetadata reportMetadata = new PressTransparencyReportMetadata(reportId, LocalDateTime.now());
        final UUID reportFileId = randomUUID();
        final int reportNumberOfPages = 4;
        final int pdfSizeInBytes = 412;

        final JsonEnvelope eventEnvelope = envelopeFrom(metadataWithRandomUUID("sjp.events.press-transparency-pdf-report-metadata-added"),
                createObjectBuilder()
                        .add("pressTransparencyReportId", reportId.toString())
                        .add("metadata", createObjectBuilder()
                                .add("fileId", reportFileId.toString())
                                .add("numberOfPages", reportNumberOfPages)
                                .add("fileSize", pdfSizeInBytes)
                                .add("title", "Press Transparency Report")
                                .add("language", ENGLISH.name())
                        )
                        .build());
        when(pressTransparencyReportMetadataRepository.findBy(reportId)).thenReturn(reportMetadata);
        pressTransparencyReportListener.handleReportMetadataAdded(eventEnvelope);
        assertThat(reportMetadata.getFileServiceId(), is(reportFileId));
        assertThat(reportMetadata.getNumberOfPages(), is(reportNumberOfPages));
        assertThat(reportMetadata.getSizeInBytes(), is(pdfSizeInBytes));

    }

    private JsonArrayBuilder createJsonArrayWithCaseIds(final List<UUID> caseIds) {
        final JsonArrayBuilder pendingCasesBuilder = createArrayBuilder();
        caseIds.forEach(caseId -> pendingCasesBuilder.add(caseId.toString()));
        return pendingCasesBuilder;
    }
}