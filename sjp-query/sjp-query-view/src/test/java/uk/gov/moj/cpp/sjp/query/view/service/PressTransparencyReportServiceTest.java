package uk.gov.moj.cpp.sjp.query.view.service;

import static java.time.LocalDate.of;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.domain.DocumentFormat.PDF;
import static uk.gov.moj.cpp.sjp.domain.DocumentRequestType.DELTA;

import uk.gov.moj.cpp.sjp.persistence.entity.PressTransparencyReportMetadata;
import uk.gov.moj.cpp.sjp.persistence.repository.PressTransparencyReportMetadataRepository;
import uk.gov.moj.cpp.sjp.query.view.response.PressTransparencyReportMetadataView;

import java.time.LocalDateTime;
import java.time.LocalTime;

import javax.persistence.NoResultException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PressTransparencyReportServiceTest {

    @Mock
    private PressTransparencyReportMetadataRepository pressTransparencyReportMetadataRepository;

    @InjectMocks
    private PressTransparencyReportService pressTransparencyReportService;

    @Test
    public void shouldReturnMetadata() {

        final PressTransparencyReportMetadata pressTransparencyReportMetadata = new PressTransparencyReportMetadata();
        pressTransparencyReportMetadata.setFileServiceId(fromString("ad90576d-9d0d-4dd4-b670-28d61a3136f9"));
        pressTransparencyReportMetadata.setGeneratedAt(LocalDateTime.of(of(2020, 10, 10), LocalTime.of(11, 30)));
        pressTransparencyReportMetadata.setNumberOfPages(3);
        pressTransparencyReportMetadata.setSizeInBytes(712);
        pressTransparencyReportMetadata.setId(randomUUID());
        pressTransparencyReportMetadata.setLanguage("ENGLISH");
        pressTransparencyReportMetadata.setTitle("Press Transparency Report");
        pressTransparencyReportMetadata.setDocumentRequestType(DELTA.name());
        pressTransparencyReportMetadata.setDocumentFormat(PDF.name());

        when(pressTransparencyReportMetadataRepository.findLatestPressTransparencyReportMetadata(any())).thenReturn(singletonList(pressTransparencyReportMetadata));

        final PressTransparencyReportMetadataView pressTransparencyReportMetadataView = pressTransparencyReportService.getMetadata();

        assertThat(pressTransparencyReportMetadataView.getReportsMetadata().get(0).getFileId(), is("ad90576d-9d0d-4dd4-b670-28d61a3136f9"));
        assertThat(pressTransparencyReportMetadataView.getReportsMetadata().get(0).getPages(), is(3));
        assertThat(pressTransparencyReportMetadataView.getReportsMetadata().get(0).getSize(), is("712"));
        assertThat(pressTransparencyReportMetadataView.getReportsMetadata().get(0).getGeneratedAt(), is("10 October 2020 at 11:30am"));
        assertThat(pressTransparencyReportMetadataView.getReportsMetadata().get(0).getReportIn(), is("English"));
        assertThat(pressTransparencyReportMetadataView.getReportsMetadata().get(0).getTitle(), is("Press Transparency Report"));
    }

    @Test
    public void shouldReturnEmptyMetadata() {

        when(pressTransparencyReportMetadataRepository.findLatestPressTransparencyReportMetadata(any())).thenThrow(new NoResultException());

        final PressTransparencyReportMetadataView pressTransparencyReportMetadataView = pressTransparencyReportService.getMetadata();

        assertThat(pressTransparencyReportMetadataView.getReportsMetadata().size(), is(0));
    }
}
