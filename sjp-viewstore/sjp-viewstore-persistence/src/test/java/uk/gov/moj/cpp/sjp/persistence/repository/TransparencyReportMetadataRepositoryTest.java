package uk.gov.moj.cpp.sjp.persistence.repository;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.DocumentFormat.PDF;
import static uk.gov.moj.cpp.sjp.domain.DocumentLanguage.ENGLISH;
import static uk.gov.moj.cpp.sjp.domain.DocumentRequestType.DELTA;
import static uk.gov.moj.cpp.sjp.domain.DocumentRequestType.FULL;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;
import uk.gov.moj.cpp.sjp.persistence.entity.TransparencyReportMetadata;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class TransparencyReportMetadataRepositoryTest extends BaseTransactionalJunit4Test {

    @Inject
    private TransparencyReportMetadataRepository transparencyReportMetadataRepository;

    private static final LocalDateTime earlierGeneratedAt = LocalDateTime.of(2018, 11, 26, 0, 0, 0);
    private static final LocalDateTime latestGeneratedAt = LocalDateTime.of(2018, 11, 27, 0, 0, 0);
    private static final LocalDateTime from = LocalDateTime.of(2018, 11, 25, 0, 0, 0);

    @Test
    public void shouldReturnTheLatestReportMetadata() {
        // given
        final UUID earlierReportId = UUID.randomUUID();
        final UUID earlierReportWelshServiceId = UUID.randomUUID();
        final UUID earlierReportEnglishServiceId = UUID.randomUUID();
        final UUID earlierReportServiceId = UUID.randomUUID();
        final TransparencyReportMetadata earlierTransparencyReportMetadata = populateTransparencyMetadata(earlierReportServiceId, earlierReportId, earlierReportWelshServiceId, earlierReportEnglishServiceId, earlierGeneratedAt, 12, 11, 2, 1);
        transparencyReportMetadataRepository.save(earlierTransparencyReportMetadata);

        final UUID latestReportId = UUID.randomUUID();
        final UUID latestReportWelshServiceId = UUID.randomUUID();
        final UUID latestReportEnglishServiceId = UUID.randomUUID();
        final UUID latestReportServiceId = UUID.randomUUID();
        final TransparencyReportMetadata latestTransparencyReportMetadata = populateTransparencyMetadata(latestReportServiceId, latestReportId, latestReportWelshServiceId, latestReportEnglishServiceId, latestGeneratedAt, 13, 12, 3, 2);
        transparencyReportMetadataRepository.save(latestTransparencyReportMetadata);

        // when
        final TransparencyReportMetadata latestTransparencyReportMetadataFromDB =
                transparencyReportMetadataRepository.findLatestTransparencyReportMetadata(from).stream().findFirst().get();

        // then
        assertThat(latestTransparencyReportMetadataFromDB.getGeneratedAt(), is(latestGeneratedAt));
        assertThat(latestTransparencyReportMetadataFromDB.getId(), is(latestReportId));
        assertThat(latestTransparencyReportMetadataFromDB.getWelshSizeInBytes(), is(13));
        assertThat(latestTransparencyReportMetadataFromDB.getEnglishSizeInBytes(), is(12));
        assertThat(latestTransparencyReportMetadataFromDB.getWelshNumberOfPages(), is(3));
        assertThat(latestTransparencyReportMetadataFromDB.getEnglishNumberOfPages(), is(2));
        assertThat(latestTransparencyReportMetadataFromDB.getWelshFileServiceId(), is(latestReportWelshServiceId));
        assertThat(latestTransparencyReportMetadataFromDB.getEnglishFileServiceId(), is(latestReportEnglishServiceId));
    }

    private TransparencyReportMetadata populateTransparencyMetadata(final UUID reportServiceId, final UUID id, final UUID welshServiceId,
                                                                    final UUID englishServiceId, final LocalDateTime earlierGeneratedAt,
                                                                    final int welshSizeInBytes, final int englishSizeInBytes,
                                                                    final int welshNumberOfPages, final int englishNumberOfPages) {
        final TransparencyReportMetadata earlierTransparencyReportMetadata = new TransparencyReportMetadata(id, PDF.name(), FULL.name(), "title", ENGLISH.name(), LocalDateTime.now());
        earlierTransparencyReportMetadata.setId(id);
        earlierTransparencyReportMetadata.setGeneratedAt(earlierGeneratedAt);
        earlierTransparencyReportMetadata.setWelshSizeInBytes(welshSizeInBytes);
        earlierTransparencyReportMetadata.setEnglishSizeInBytes(englishSizeInBytes);
        earlierTransparencyReportMetadata.setWelshNumberOfPages(welshNumberOfPages);
        earlierTransparencyReportMetadata.setEnglishNumberOfPages(englishNumberOfPages);
        earlierTransparencyReportMetadata.setWelshFileServiceId(welshServiceId);
        earlierTransparencyReportMetadata.setEnglishFileServiceId(englishServiceId);
        earlierTransparencyReportMetadata.setFileServiceId(reportServiceId);
        earlierTransparencyReportMetadata.setDocumentFormat(PDF.name());
        earlierTransparencyReportMetadata.setDocumentRequestType(DELTA.name());
        earlierTransparencyReportMetadata.setTitle("Transparency Report");
        earlierTransparencyReportMetadata.setLanguage("ENGLISH");

        return earlierTransparencyReportMetadata;
    }


}

