package uk.gov.moj.cpp.sjp.event.listener;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Boolean.TRUE;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.DocumentFormat.PDF;
import static uk.gov.moj.cpp.sjp.domain.DocumentRequestType.DELTA;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.CasePublishStatus;
import uk.gov.moj.cpp.sjp.persistence.entity.TransparencyReportMetadata;
import uk.gov.moj.cpp.sjp.persistence.repository.CasePublishStatusRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.TransparencyReportMetadataRepository;

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
public class TransparencyReportListenerTest {

    @InjectMocks
    private TransparencyReportListener transparencyReportListener;

    @Mock
    private CasePublishStatusRepository casePublishStatusRepository;

    @Mock
    private TransparencyReportMetadataRepository transparencyReportMetadataRepository;

    @Test
    public void shouldCreateReportMetadata() {
        final UUID transparencyReportId = randomUUID();
        final List<UUID> caseIds = newArrayList(randomUUID(), randomUUID());
        final JsonEnvelope eventEnvelope = envelopeFrom(metadataWithRandomUUID("sjp.events.transparency-report-generation-started"),
                createObjectBuilder()
                        .add("transparencyReportId", transparencyReportId.toString())
                        .add("caseIds", createJsonArrayWithCaseIds(caseIds))
                        .add("format", "PDF")
                        .add("requestType", "DELTA")
                        .add("language", "ENGLISH")
                        .add("title", "Pending Cases")
                        .build());

        transparencyReportListener.handleCasesArePublishedPDF(eventEnvelope);

        final ArgumentCaptor<TransparencyReportMetadata> transparencyReportMetadataArgument = ArgumentCaptor.forClass(TransparencyReportMetadata.class);
        verify(transparencyReportMetadataRepository).save(transparencyReportMetadataArgument.capture());
        final TransparencyReportMetadata reportMetadata = transparencyReportMetadataArgument.getValue();
        assertThat(reportMetadata.getId(), is(transparencyReportId));
        assertThat(reportMetadata.getGeneratedAt(), is(instanceOf(LocalDateTime.class)));
    }

    @Test
    public void shouldUpdateReportMetadata() {
        final UUID transparencyReportId = randomUUID();
        final TransparencyReportMetadata transparencyReportMetadata = new TransparencyReportMetadata(transparencyReportId, PDF.name(), DELTA.name(), "title", "ENGLISH", LocalDateTime.now());
        final UUID welshReportFileId = randomUUID();
        final int welshReportNumberOfPages = 4;
        final int welshPdfSizeInBytes = 412;

        final JsonEnvelope eventEnvelope = envelopeFrom(metadataWithRandomUUID("sjp.events.transparency-pdf-report-metadata-added"),
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
        transparencyReportListener.handlePDFReportMetadataIsAdded(eventEnvelope);
        assertThat(transparencyReportMetadata.getFileServiceId(), is(welshReportFileId));
        assertThat(transparencyReportMetadata.getNumberOfPages(), is(welshReportNumberOfPages));
        assertThat(transparencyReportMetadata.getSizeInBytes(), is(welshPdfSizeInBytes));

    }

    @Test
    public void shouldDecrementCasePublishCountersWhenGenerationFailed() {
        final UUID transparencyReportId = randomUUID();
        final List<UUID> caseIds = newArrayList(randomUUID(), randomUUID());
        final JsonEnvelope eventEnvelope = envelopeFrom(metadataWithRandomUUID("sjp.events.transparency-json-report-generation-failed"),
                createObjectBuilder()
                        .add("transparencyReportId", transparencyReportId.toString())
                        .add("templateIdentifier", "PendingCasesEnglish")
                        .add("caseIds", createArrayBuilder()
                                .add(caseIds.get(0).toString())
                                .add(caseIds.get(1).toString())
                        )
                        .add("reportGenerationPreviouslyFailed", false)
                        .build());

        final List<CasePublishStatus> publishedCases = createPublishedCases();
        when(casePublishStatusRepository.findByCaseIds(caseIds)).thenReturn(publishedCases);
        transparencyReportListener.handleTransparencyJSONReportGenerationFailed(eventEnvelope);
        final ArgumentCaptor<CasePublishStatus> argument = ArgumentCaptor.forClass(CasePublishStatus.class);
        verify(casePublishStatusRepository, times(2)).save(argument.capture());
        publishedCases.forEach(e -> assertThatDecremented(e, argument.getAllValues()));
    }

    @Test
    public void shouldNotDecrementCasePublishCountersWhenGenerationFailedPrevioulsy() {
        final UUID transparencyReportId = randomUUID();
        final List<UUID> caseIds = newArrayList(randomUUID(), randomUUID());
        final JsonEnvelope eventEnvelope = envelopeFrom(metadataWithRandomUUID("sjp.events.transparency-json-report-generation-failed"),
                createObjectBuilder()
                        .add("transparencyReportId", transparencyReportId.toString())
                        .add("templateIdentifier", "PendingCasesWelsh")
                        .add("caseIds", createArrayBuilder()
                                .add(caseIds.get(0).toString())
                                .add(caseIds.get(1).toString())
                        )
                        .add("reportGenerationPreviouslyFailed", true)
                        .build());

        final List<CasePublishStatus> publishedCases = createPublishedCases();
        transparencyReportListener.handleTransparencyJSONReportGenerationFailed(eventEnvelope);
        verify(casePublishStatusRepository, never()).save(any(CasePublishStatus.class));
    }

    private void assertThatDecremented(final CasePublishStatus casePublishStatus, final List<CasePublishStatus> decrementedCasePublishStatuses) {
        assertThat(decrementedCasePublishStatuses.stream()
                .anyMatch(e -> (casePublishStatus.getNumberOfPublishes() == 0 ? e.getNumberOfPublishes().equals(0) : e.getNumberOfPublishes().equals(casePublishStatus.getNumberOfPublishes() - 1)
                        && (casePublishStatus.getTotalNumberOfPublishes() == 0 ? e.getTotalNumberOfPublishes().equals(0) : e.getNumberOfPublishes().equals(casePublishStatus.getTotalNumberOfPublishes() - 1)))
                ), is(TRUE));
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

}