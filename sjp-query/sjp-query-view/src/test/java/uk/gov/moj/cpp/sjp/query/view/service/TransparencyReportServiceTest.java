package uk.gov.moj.cpp.sjp.query.view.service;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.domain.DocumentFormat.PDF;
import static uk.gov.moj.cpp.sjp.domain.DocumentLanguage.ENGLISH;
import static uk.gov.moj.cpp.sjp.domain.DocumentRequestType.DELTA;
import static uk.gov.moj.cpp.sjp.domain.DocumentRequestType.FULL;
import static uk.gov.moj.cpp.sjp.query.view.response.TransparencyReportsMetadataView.TransparencyReportMetaDataView;

import uk.gov.moj.cpp.sjp.persistence.entity.TransparencyReportMetadata;
import uk.gov.moj.cpp.sjp.persistence.repository.TransparencyReportMetadataRepository;
import uk.gov.moj.cpp.sjp.query.view.response.TransparencyReportsMetadataView;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import javax.persistence.NoResultException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TransparencyReportServiceTest {

    @Mock
    private TransparencyReportMetadataRepository transparencyReportMetadataRepository;

    @InjectMocks
    private TransparencyReportService transparencyReportService;

    private final List<UUID> fileServiceIds = newArrayList(UUID.randomUUID(), UUID.randomUUID());

    private final List<Integer> numberOfPages = newArrayList(2, 3);

    private final List<Integer> fileSizes = newArrayList(1024 * 1024 * 22, 1024 * 1024 * 22);

    private final LocalDateTime generatedAt = LocalDateTime.of(2018, 12, 25, 4, 1, 35);


    @Test
    public void shouldReturnMetadata() {
        // given
        setUpMetadata();
        final TransparencyReportsMetadataView transparencyReportsMetaDataView = transparencyReportService.getMetaData();

        // when
        final List<TransparencyReportMetaDataView> reportsMetadata = transparencyReportsMetaDataView.getReportsMetadata();

        // then
        assertThat(reportsMetadata.size(), is(1));

        final TransparencyReportMetaDataView englishTransparencyReportMetaDataView = reportsMetadata.get(0);

        validateReportMetadata(englishTransparencyReportMetaDataView, false);
    }

    @Test
    public void shouldReturnEmptyMetadata() {
        // given
        when(transparencyReportMetadataRepository.findLatestTransparencyReportMetadata(any())).thenThrow(new NoResultException());
        final TransparencyReportsMetadataView transparencyReportsMetaDataView = transparencyReportService.getMetaData();

        // when
        final List<TransparencyReportMetaDataView> reportsMetadata = transparencyReportsMetaDataView.getReportsMetadata();

        // then
        assertThat(reportsMetadata.size(), is(0));
    }

    private void validateReportMetadata(final TransparencyReportMetaDataView transparencyReportMetaDataView, boolean welsh) {
        final int index = welsh ? 1 : 0;
        assertThat(transparencyReportMetaDataView.getFileId(), is(fileServiceIds.get(index).toString()));
        assertThat(transparencyReportMetaDataView.getPages(), is(numberOfPages.get(index)));
        assertThat(transparencyReportMetaDataView.getReportIn(), is("English"));
        assertThat(transparencyReportMetaDataView.getSize(), is("23068672"));
        assertThat(transparencyReportMetaDataView.getGeneratedAt(), is("25 December 2018 at 4:01am"));
        assertThat(transparencyReportMetaDataView.getTitle(), is("Transparency Report"));
    }

    private void setUpMetadata() {
        final TransparencyReportMetadata metadata = new TransparencyReportMetadata(UUID.randomUUID(), PDF.name(), FULL.name(), "title", ENGLISH.name(), LocalDateTime.now());

        metadata.setFileServiceId(fileServiceIds.get(0));
        metadata.setNumberOfPages(numberOfPages.get(0));
        metadata.setSizeInBytes(fileSizes.get(0));
        metadata.setGeneratedAt(generatedAt);
        metadata.setLanguage("ENGLISH");
        metadata.setTitle("Transparency Report");
        metadata.setDocumentRequestType(DELTA.name());
        metadata.setDocumentFormat(PDF.name());

        when(transparencyReportMetadataRepository.findLatestTransparencyReportMetadata(any())).thenReturn(singletonList(metadata));
    }

}