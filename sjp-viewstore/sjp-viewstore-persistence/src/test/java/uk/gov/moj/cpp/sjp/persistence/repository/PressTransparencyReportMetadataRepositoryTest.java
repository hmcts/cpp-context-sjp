package uk.gov.moj.cpp.sjp.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.sjp.domain.DocumentFormat.PDF;
import static uk.gov.moj.cpp.sjp.domain.DocumentRequestType.DELTA;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.sjp.persistence.entity.PressTransparencyReportMetadata;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class PressTransparencyReportMetadataRepositoryTest {

    private static final String PERSISTENCE_UNIT = "sjp-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider = new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private PressTransparencyReportMetadataRepository pressTransparencyReportMetadataRepository;

    private static final LocalDateTime earlierGeneratedAt = LocalDateTime.of(2018, 11, 26, 0, 0, 0);
    private static final LocalDateTime latestGeneratedAt = LocalDateTime.of(2018, 11, 27, 0, 0, 0);
    private static final LocalDateTime from = LocalDateTime.of(2018, 11, 25, 0, 0, 0);

    @BeforeEach
    void createRepositoryWithInjectedEntityManager() {
        pressTransparencyReportMetadataRepository = new PressTransparencyReportMetadataRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(pressTransparencyReportMetadataRepository);
    }

    @Test
    void shouldReturnTheLatestPressReportMetadata() {
        // given
        final UUID earlierReportId = randomUUID();
        final UUID earlierReportServiceId = randomUUID();
        final PressTransparencyReportMetadata earlierTransparencyReportMetadata = populatePressTransparencyMetadata(earlierReportId, earlierReportServiceId, earlierGeneratedAt, 12, 2);
        pressTransparencyReportMetadataRepository.save(earlierTransparencyReportMetadata);

        final UUID latestReportId = randomUUID();
        final UUID latestReportServiceId = randomUUID();
        final PressTransparencyReportMetadata latestTransparencyReportMetadata = populatePressTransparencyMetadata(latestReportId, latestReportServiceId, latestGeneratedAt, 13, 3);
        pressTransparencyReportMetadataRepository.save(latestTransparencyReportMetadata);

        // when
        final PressTransparencyReportMetadata latestTransparencyReportMetadataFromDB =
                pressTransparencyReportMetadataRepository.findLatestPressTransparencyReportMetadata(from).stream().findFirst().get();

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
        pressTransparencyReportMetadata.setDocumentFormat(PDF.name());
        pressTransparencyReportMetadata.setDocumentRequestType(DELTA.name());
        pressTransparencyReportMetadata.setTitle("Press Transparency Report");
        pressTransparencyReportMetadata.setLanguage("ENGLISH");
        return pressTransparencyReportMetadata;
    }

}
