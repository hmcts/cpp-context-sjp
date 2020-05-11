package uk.gov.moj.cpp.sjp.event.listener;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Boolean.TRUE;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.CasePublishStatus;
import uk.gov.moj.cpp.sjp.persistence.entity.TransparencyReportMetadata;
import uk.gov.moj.cpp.sjp.persistence.repository.CasePublishStatusRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.TransparencyReportMetadataRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TransparencyReportListenerTest {

    @InjectMocks
    private TransparencyReportListener transparencyReportListener;

    @Mock
    private CasePublishStatusRepository casePublishStatusRepository;

    @Mock
    private TransparencyReportMetadataRepository transparencyReportMetadataRepository;

    @Test
    public void shouldCreateReportMetadataAndIncrementCasePublishedCounters() {
        final UUID transparencyReportId = randomUUID();
        final List<UUID> caseIds = newArrayList(randomUUID(), randomUUID());
        final JsonEnvelope eventEnvelope = envelopeFrom(metadataWithRandomUUID("sjp.events.transparency-report-generation-start"),
                createObjectBuilder()
                        .add("transparencyReportId", transparencyReportId.toString())
                        .add("caseIds", createJsonArrayWithCaseIds(caseIds))
                        .build());

        final List<CasePublishStatus> publishedCases = createPublishedCases();
        when(casePublishStatusRepository.findByCaseIds(caseIds)).thenReturn(createPublishedCases());

        transparencyReportListener.handleCasesArePublished(eventEnvelope);

        final ArgumentCaptor<CasePublishStatus> argument = ArgumentCaptor.forClass(CasePublishStatus.class);
        verify(casePublishStatusRepository, times(2)).save(argument.capture());
        publishedCases.forEach(e -> assertThatIncremented(e, argument.getAllValues()));

        final ArgumentCaptor<TransparencyReportMetadata> transparencyReportMetadataArgument = ArgumentCaptor.forClass(TransparencyReportMetadata.class);
        verify(transparencyReportMetadataRepository).save(transparencyReportMetadataArgument.capture());
        final TransparencyReportMetadata reportMetadata = transparencyReportMetadataArgument.getValue();
        assertThat(reportMetadata.getId(), is(transparencyReportId));
        assertThat(reportMetadata.getGeneratedAt(), is(LocalDateTime.class));
    }

    @Test
    public void shouldUpdateReportMetadata() {
        final UUID transparencyReportId = randomUUID();
        final TransparencyReportMetadata transparencyReportMetadata = new TransparencyReportMetadata(transparencyReportId, LocalDateTime.now());
        final UUID welshReportFileId = randomUUID();
        final int welshReportNumberOfPages = 4;
        final int welshPdfSizeInBytes = 412;

        final JsonEnvelope eventEnvelope = envelopeFrom(metadataWithRandomUUID("sjp.events.transparency-report-metadata-added"),
                createObjectBuilder()
                        .add("transparencyReportId", transparencyReportId.toString())
                        .add("language", "cy")
                        .add("metadata", createObjectBuilder()
                                .add("fileId", welshReportFileId.toString())
                                .add("numberOfPages", welshReportNumberOfPages)
                                .add("fileSize", welshPdfSizeInBytes)
                        )
                        .build());
        when(transparencyReportMetadataRepository.findBy(transparencyReportId)).thenReturn(transparencyReportMetadata);
        transparencyReportListener.handleReportMetadataIsAdded(eventEnvelope);
        assertThat(transparencyReportMetadata.getWelshFileServiceId(), is(welshReportFileId));
        assertThat(transparencyReportMetadata.getWelshNumberOfPages(), is(welshReportNumberOfPages));
        assertThat(transparencyReportMetadata.getWelshSizeInBytes(), is(welshPdfSizeInBytes));

    }

    private void assertThatIncremented(final CasePublishStatus casePublishStatus, final List<CasePublishStatus> incrementedCasePublishStatuses) {
        assertThat(incrementedCasePublishStatuses.stream()
                .anyMatch(e -> e.getNumberOfPublishes().equals(casePublishStatus.getNumberOfPublishes() + 1)
                        && e.getTotalNumberOfPublishes().equals(casePublishStatus.getTotalNumberOfPublishes() + 1)), is(TRUE));
    }

    private List<CasePublishStatus> createPublishedCases() {
        return newArrayList(createCasePublishStatus(0, 3),
                createCasePublishStatus(1, 1));
    }

    private CasePublishStatus createCasePublishStatus(final Integer numberOfPublishes, final Integer totalNumberOfPublishes) {
        final CasePublishStatus casePublishStatus = new CasePublishStatus();
        casePublishStatus.setTotalNumberOfPublishes(totalNumberOfPublishes);
        casePublishStatus.setNumberOfPublishes(numberOfPublishes);
        return casePublishStatus;
    }

    private JsonArrayBuilder createJsonArrayWithCaseIds(final List<UUID> caseIds) {
        final JsonArrayBuilder pendingCasesBuilder = createArrayBuilder();
        caseIds.forEach(caseId -> pendingCasesBuilder.add(caseId.toString()));
        return pendingCasesBuilder;
    }

    private JsonObject buildFileMetadataJsonObject(final String fileName,
                                                   final int pdfPageCount,
                                                   final int fileSize,
                                                   final UUID englishPdfFileUUID) {
        return createObjectBuilder()
                .add("fileName", fileName)
                .add("numberOfPages", pdfPageCount)
                .add("fileSize", fileSize)
                .add("fileId", englishPdfFileUUID.toString()).build();
    }

}