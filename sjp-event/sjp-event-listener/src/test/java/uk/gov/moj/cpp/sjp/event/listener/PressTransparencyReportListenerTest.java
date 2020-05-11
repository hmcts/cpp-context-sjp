package uk.gov.moj.cpp.sjp.event.listener;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.PressTransparencyReportMetadata;
import uk.gov.moj.cpp.sjp.persistence.repository.PressTransparencyReportMetadataRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import javax.json.JsonArrayBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PressTransparencyReportListenerTest {

    @InjectMocks
    private PressTransparencyReportListener pressTransparencyReportListener;

    @Mock
    private PressTransparencyReportMetadataRepository pressTransparencyReportMetadataRepository;

    @Test
    public void shouldCreatePressReportMetadata() {
        final List<UUID> caseIds = newArrayList(randomUUID(), randomUUID());
        final UUID reportId = randomUUID();

        final JsonEnvelope eventEnvelope = envelopeFrom(metadataWithRandomUUID("sjp.events.press-transparency-report-generation-started"),
                createObjectBuilder()
                        .add("pressTransparencyReportId",reportId.toString())
                        .add("caseIds", createJsonArrayWithCaseIds(caseIds))
                        .build());

        pressTransparencyReportListener.handlePressTransparencyReportGenerated(eventEnvelope);

        final ArgumentCaptor<PressTransparencyReportMetadata> pressTransparencyReportMetadataArgument = ArgumentCaptor.forClass(PressTransparencyReportMetadata.class);
        verify(pressTransparencyReportMetadataRepository).save(pressTransparencyReportMetadataArgument.capture());
        final PressTransparencyReportMetadata pressTransparencyReportMetadata = pressTransparencyReportMetadataArgument.getValue();
        assertThat(pressTransparencyReportMetadata.getId(), is(reportId));
        assertThat(pressTransparencyReportMetadata.getGeneratedAt(), is(LocalDateTime.class));
    }

    @Test
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
                        )
                        .build());
        when(pressTransparencyReportMetadataRepository.findBy(reportId)).thenReturn(reportMetadata);
        pressTransparencyReportListener.handleMetadataAdded(eventEnvelope);
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