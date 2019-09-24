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
    private TransparencyReportListener casesPublishedListener;

    @Mock
    private CasePublishStatusRepository casePublishStatusRepository;

    @Mock
    private TransparencyReportMetadataRepository transparencyReportMetadataRepository;

    @Test
    public void shouldCreateReportMetadataAndIncrementCasePublishedCounters() {
        final List<UUID> caseIds = newArrayList(randomUUID(), randomUUID());
        final UUID englishReportFileId = randomUUID();
        final UUID welshReportFileId = randomUUID();
        final Integer englishReportNumberOfPages = 3;
        final Integer englishPdfSizeInBytes = 325;
        final Integer welshReportNumberOfPages = 4;
        final Integer welshPdfSizeInBytes = 412;

        final JsonEnvelope eventEnvelope = envelopeFrom(metadataWithRandomUUID("sjp.events.transparency-report-generated"),
                createObjectBuilder()
                        .add("englishReportMetadata",
                                buildFileMetadataJsonObject("transparency-report-english.pdf", englishReportNumberOfPages, englishPdfSizeInBytes, englishReportFileId))
                        .add("welshReportMetadata",
                                buildFileMetadataJsonObject("transparency-report-welsh.pdf", welshReportNumberOfPages, welshPdfSizeInBytes, welshReportFileId))
                        .add("caseIds", createJsonArrayWithCaseIds(caseIds))
                        .build());

        final List<CasePublishStatus> publishedCases = createPublishedCases();
        when(casePublishStatusRepository.findByCaseIds(caseIds)).thenReturn(createPublishedCases());

        casesPublishedListener.handleCasesArePublished(eventEnvelope);

        final ArgumentCaptor<CasePublishStatus> argument = ArgumentCaptor.forClass(CasePublishStatus.class);
        verify(casePublishStatusRepository, times(2)).save(argument.capture());
        publishedCases.forEach(e -> assertThatIncremented(e, argument.getAllValues()));

        final ArgumentCaptor<TransparencyReportMetadata> transparencyReportMetadataArgument = ArgumentCaptor.forClass(TransparencyReportMetadata.class);
        verify(transparencyReportMetadataRepository).save(transparencyReportMetadataArgument.capture());
        assertThat(transparencyReportMetadataArgument.getValue().getEnglishFileServiceId(), is(englishReportFileId));
        assertThat(transparencyReportMetadataArgument.getValue().getEnglishNumberOfPages(), is(englishReportNumberOfPages));
        assertThat(transparencyReportMetadataArgument.getValue().getEnglishSizeInBytes(), is(englishPdfSizeInBytes));
        assertThat(transparencyReportMetadataArgument.getValue().getWelshFileServiceId(), is(welshReportFileId));
        assertThat(transparencyReportMetadataArgument.getValue().getWelshNumberOfPages(), is(welshReportNumberOfPages));
        assertThat(transparencyReportMetadataArgument.getValue().getWelshSizeInBytes(), is(welshPdfSizeInBytes));
        assertThat(transparencyReportMetadataArgument.getValue().getGeneratedAt(), is(LocalDateTime.class));
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