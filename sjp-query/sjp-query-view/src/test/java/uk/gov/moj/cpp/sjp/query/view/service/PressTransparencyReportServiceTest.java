package uk.gov.moj.cpp.sjp.query.view.service;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.sjp.persistence.entity.PressTransparencyReportMetadata;
import uk.gov.moj.cpp.sjp.persistence.repository.PressTransparencyReportMetadataRepository;
import uk.gov.moj.cpp.sjp.query.view.response.PressTransparencyReportMetadataView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import javax.persistence.NoResultException;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PressTransparencyReportServiceTest {

    @Mock
    private PressTransparencyReportMetadataRepository pressTransparencyReportMetadataRepository;

    @InjectMocks
    private PressTransparencyReportService pressTransparencyReportService;

    @Test
    public void shouldReturnMetadata(){
        setupMockData();
        final PressTransparencyReportMetadataView pressTransparencyReportMetadataView = pressTransparencyReportService.getMetadata();
        assertThat(pressTransparencyReportMetadataView.getFileId(), is("ad90576d-9d0d-4dd4-b670-28d61a3136f9"));
        assertThat(pressTransparencyReportMetadataView.getPages(), is(3));
        assertThat(pressTransparencyReportMetadataView.getSize(), is("712B"));
        assertThat(pressTransparencyReportMetadataView.getGeneratedAt(), is("10 October 2020 at 11:30am"));
    }

    @Test
    public void shouldReturnEmptyMetadata(){
        setupEmptyMetadata();
        final PressTransparencyReportMetadataView pressTransparencyReportMetadataView = pressTransparencyReportService.getMetadata();
        assertThat(pressTransparencyReportMetadataView.getFileId(), is("0"));
        assertThat(pressTransparencyReportMetadataView.getPages(), is(0));
        assertThat(pressTransparencyReportMetadataView.getSize(), is("0B"));
        assertThat(pressTransparencyReportMetadataView.getGeneratedAt(), is(""));
    }

    private void setupMockData() {
        final PressTransparencyReportMetadata pressTransparencyReportMetadata = new PressTransparencyReportMetadata();
        pressTransparencyReportMetadata.setFileServiceId(fromString("ad90576d-9d0d-4dd4-b670-28d61a3136f9"));
        pressTransparencyReportMetadata.setGeneratedAt(LocalDateTime.of(LocalDate.of(2020,10, 10), LocalTime.of(11,30)));
        pressTransparencyReportMetadata.setNumberOfPages(3);
        pressTransparencyReportMetadata.setSizeInBytes(712);
        pressTransparencyReportMetadata.setId(randomUUID());
        when(pressTransparencyReportMetadataRepository.findLatestPressTransparencyReportMetadata()).thenReturn(pressTransparencyReportMetadata);
    }

    private void setupEmptyMetadata() {
        when(pressTransparencyReportMetadataRepository.findLatestPressTransparencyReportMetadata()).thenThrow(new NoResultException());
    }

}
