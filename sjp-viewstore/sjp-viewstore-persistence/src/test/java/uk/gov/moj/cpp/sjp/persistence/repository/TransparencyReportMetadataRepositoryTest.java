package uk.gov.moj.cpp.sjp.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.sjp.domain.DocumentFormat.PDF;
import static uk.gov.moj.cpp.sjp.domain.DocumentLanguage.ENGLISH;
import static uk.gov.moj.cpp.sjp.domain.DocumentRequestType.DELTA;
import static uk.gov.moj.cpp.sjp.domain.DocumentRequestType.FULL;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.sjp.persistence.entity.TransparencyReportMetadata;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class TransparencyReportMetadataRepositoryTest {

    private static final String PERSISTENCE_UNIT = "sjp-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider = new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private TransparencyReportMetadataRepository transparencyReportMetadataRepository;

    private static final LocalDateTime earlierGeneratedAt = LocalDateTime.of(2018, 11, 26, 0, 0, 0);
    private static final LocalDateTime latestGeneratedAt = LocalDateTime.of(2018, 11, 27, 0, 0, 0);
    private static final LocalDateTime from = LocalDateTime.of(2018, 11, 25, 0, 0, 0);

    @BeforeEach
    void createRepositoryWithInjectedEntityManager() {
        transparencyReportMetadataRepository = new TransparencyReportMetadataRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(transparencyReportMetadataRepository);
    }

    @Test
    void shouldReturnTheLatestReportMetadata() {
        // given
        final UUID earlierReportId = randomUUID();
        final UUID earlierReportWelshServiceId = randomUUID();
        final UUID earlierReportEnglishServiceId = randomUUID();
        final UUID earlierReportServiceId = randomUUID();
        final TransparencyReportMetadata earlierTransparencyReportMetadata = populateTransparencyMetadata(earlierReportServiceId, earlierReportId, earlierReportWelshServiceId, earlierReportEnglishServiceId, earlierGeneratedAt, 12, 11, 2, 1);
        transparencyReportMetadataRepository.save(earlierTransparencyReportMetadata);

        final UUID latestReportId = randomUUID();
        final UUID latestReportWelshServiceId = randomUUID();
        final UUID latestReportEnglishServiceId = randomUUID();
        final UUID latestReportServiceId = randomUUID();
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
        final TransparencyReportMetadata earlierTransparencyReportMetadata = new TransparencyReportMetadata(id, PDF.name(), FULL.name(), "title", ENGLISH.name(), new UtcClock().now().toLocalDateTime());
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
