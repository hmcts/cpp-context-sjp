package uk.gov.moj.cpp.sjp.persistence.repository;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.sjp.persistence.entity.TransparencyReportMetadata;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class TransparencyReportMetadataRepositoryTest extends BaseTransactionalTest {

    @Inject
    private TransparencyReportMetadataRepository transparencyReportMetadataRepository;

    private static final LocalDateTime earlierGeneratedAt = LocalDateTime.of(2018, 11, 26, 0, 0, 0);
    private static final LocalDateTime latestGeneratedAt = LocalDateTime.of(2018, 11, 27, 0, 0, 0);

    @Test
    public void shouldReturnTheLatestReportMetadata() {
        // given
        final UUID earlierReportId = UUID.randomUUID();
        final UUID earlierReportWelshServiceId = UUID.randomUUID();
        final UUID earlierReportEnglishServiceId = UUID.randomUUID();
        final TransparencyReportMetadata earlierTransparencyReportMetadata = populateTransparencyMetadata(earlierReportId, earlierReportWelshServiceId, earlierReportEnglishServiceId, earlierGeneratedAt, 12, 11, 2, 1);
        transparencyReportMetadataRepository.save(earlierTransparencyReportMetadata);

        final UUID latestReportId = UUID.randomUUID();
        final UUID latestReportWelshServiceId = UUID.randomUUID();
        final UUID latestReportEnglishServiceId = UUID.randomUUID();
        final TransparencyReportMetadata latestTransparencyReportMetadata = populateTransparencyMetadata(latestReportId, latestReportWelshServiceId, latestReportEnglishServiceId, latestGeneratedAt, 13, 12, 3, 2);
        transparencyReportMetadataRepository.save(latestTransparencyReportMetadata);

        // when
        final TransparencyReportMetadata latestTransparencyReportMetadataFromDB =
                transparencyReportMetadataRepository.findLatestTransparencyReportMetadata();

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

    private TransparencyReportMetadata populateTransparencyMetadata(final UUID id, final UUID welshServiceId,
                                                                    final UUID englishServiceId, final LocalDateTime earlierGeneratedAt,
                                                                    final int welshSizeInBytes, final int englishSizeInBytes,
                                                                    final int welshNumberOfPages, final int englishNumberOfPages) {
        final TransparencyReportMetadata earlierTransparencyReportMetadata = new TransparencyReportMetadata();
        earlierTransparencyReportMetadata.setId(id);
        earlierTransparencyReportMetadata.setGeneratedAt(earlierGeneratedAt);
        earlierTransparencyReportMetadata.setWelshSizeInBytes(welshSizeInBytes);
        earlierTransparencyReportMetadata.setEnglishSizeInBytes(englishSizeInBytes);
        earlierTransparencyReportMetadata.setWelshNumberOfPages(welshNumberOfPages);
        earlierTransparencyReportMetadata.setEnglishNumberOfPages(englishNumberOfPages);
        earlierTransparencyReportMetadata.setWelshFileServiceId(welshServiceId);
        earlierTransparencyReportMetadata.setEnglishFileServiceId(englishServiceId);

        return earlierTransparencyReportMetadata;
    }


}

