package uk.gov.moj.cpp.sjp.persistence.repository;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.sjp.persistence.entity.PressTransparencyReportMetadata;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class PressTransparencyReportMetadataRepositoryTest extends BaseTransactionalTest {

    @Inject
    private PressTransparencyReportMetadataRepository pressTransparencyReportMetadataRepository;

    private static final LocalDateTime earlierGeneratedAt = LocalDateTime.of(2018, 11, 26, 0, 0, 0);
    private static final LocalDateTime latestGeneratedAt = LocalDateTime.of(2018, 11, 27, 0, 0, 0);

    @Test
    public void shouldReturnTheLatestPressReportMetadata() {
        // given
        final UUID earlierReportId = UUID.randomUUID();
        final UUID earlierReportServiceId = UUID.randomUUID();
        final PressTransparencyReportMetadata earlierTransparencyReportMetadata = populatePressTransparencyMetadata(earlierReportId, earlierReportServiceId, earlierGeneratedAt, 12, 2);
        pressTransparencyReportMetadataRepository.save(earlierTransparencyReportMetadata);

        final UUID latestReportId = UUID.randomUUID();
        final UUID latestReportServiceId = UUID.randomUUID();
        final PressTransparencyReportMetadata latestTransparencyReportMetadata = populatePressTransparencyMetadata(latestReportId, latestReportServiceId, latestGeneratedAt, 13, 3);
        pressTransparencyReportMetadataRepository.save(latestTransparencyReportMetadata);

        // when
        final PressTransparencyReportMetadata latestTransparencyReportMetadataFromDB =
                pressTransparencyReportMetadataRepository.findLatestPressTransparencyReportMetadata();

        // then
        assertThat(latestTransparencyReportMetadataFromDB.getGeneratedAt(), is(latestGeneratedAt));
        assertThat(latestTransparencyReportMetadataFromDB.getId(), is(latestReportId));
        assertThat(latestTransparencyReportMetadataFromDB.getSizeInBytes(), is(13));
        assertThat(latestTransparencyReportMetadataFromDB.getNumberOfPages(), is(3));
        assertThat(latestTransparencyReportMetadataFromDB.getFileServiceId(), is(latestReportServiceId));
    }

    private PressTransparencyReportMetadata populatePressTransparencyMetadata(final UUID id,
                                                                    final UUID serviceId,
                                                                    final LocalDateTime generatedAt,
                                                                    final int sizeInBytes,
                                                                    final int numberOfPages) {
        final PressTransparencyReportMetadata pressTransparencyReportMetadata = new PressTransparencyReportMetadata();
        pressTransparencyReportMetadata.setId(id);
        pressTransparencyReportMetadata.setGeneratedAt(generatedAt);
        pressTransparencyReportMetadata.setSizeInBytes(sizeInBytes);
        pressTransparencyReportMetadata.setNumberOfPages(numberOfPages);
        pressTransparencyReportMetadata.setNumberOfPages(numberOfPages);
        pressTransparencyReportMetadata.setFileServiceId(serviceId);

        return pressTransparencyReportMetadata;
    }

}

