package uk.gov.moj.cpp.sjp.query.view.service;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.query.view.response.TransparencyReportsMetadataView.TransparencyReportMetaDataView;

import uk.gov.moj.cpp.sjp.persistence.entity.TransparencyReportMetadata;
import uk.gov.moj.cpp.sjp.persistence.repository.TransparencyReportMetadataRepository;
import uk.gov.moj.cpp.sjp.query.view.response.TransparencyReportsMetadataView;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
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

    @Before
    public void setUp() {
        final TransparencyReportMetadata metadata = new TransparencyReportMetadata();

        metadata.setEnglishFileServiceId(fileServiceIds.get(0));
        metadata.setWelshFileServiceId(fileServiceIds.get(1));

        metadata.setEnglishNumberOfPages(numberOfPages.get(0));
        metadata.setWelshNumberOfPages(numberOfPages.get(1));

        metadata.setEnglishSizeInBytes(fileSizes.get(0));
        metadata.setWelshSizeInBytes(fileSizes.get(1));

        metadata.setGeneratedAt(generatedAt);

        when(transparencyReportMetadataRepository.findLatestTransparencyReportMetadata()).thenReturn(metadata);
    }

    @Test
    public void shouldReturnMetadata() {
        // given
        final TransparencyReportsMetadataView transparencyReportsMetaDataView = transparencyReportService.getMetaData();

        // when
        final List<TransparencyReportMetaDataView> reportsMetadata = transparencyReportsMetaDataView.getReportsMetadata();

        // then
        assertThat(reportsMetadata.size(), is(2));

        final TransparencyReportMetaDataView englishTransparencyReportMetaDataView = reportsMetadata.get(0);
        final TransparencyReportMetaDataView welshTransparencyReportMetaDataView = reportsMetadata.get(1);

        validateReportMetadata(englishTransparencyReportMetaDataView, false);
        validateReportMetadata(welshTransparencyReportMetaDataView, true);
    }

    private void validateReportMetadata(final TransparencyReportMetaDataView transparencyReportMetaDataView, boolean welsh) {
        final int index = welsh ? 1 : 0;
        assertThat(transparencyReportMetaDataView.getFileId(), is(fileServiceIds.get(index).toString()));
        assertThat(transparencyReportMetaDataView.getPages(), is(numberOfPages.get(index)));
        assertThat(transparencyReportMetaDataView.getReportIn(), is(welsh ? "Welsh" : "English"));
        assertThat(transparencyReportMetaDataView.getSize(), is("22MB"));
        assertThat(transparencyReportMetaDataView.getGeneratedAt(), is("25 December 2018 at 4:01am"));
    }
}