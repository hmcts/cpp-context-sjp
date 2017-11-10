package uk.gov.moj.cpp.sjp.query.view.service;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.query.view.response.CasesMissingSjpnView;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.apache.deltaspike.data.api.QueryResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseServiceFindCasesMissingSjpnTest {

    private static final LocalDate NOW = LocalDate.now();
    private static final int COUNT = 10;
    private List<CaseDetail> caseDetails;

    @Mock
    private QueryResult queryResult;

    @Mock
    private CaseRepository caseRepository;

    @InjectMocks
    private CaseService service;

    @Before
    public void init() {
        caseDetails = asList(createCaseDetails(), createCaseDetails(), createCaseDetails());
        when(queryResult.getResultList()).thenReturn(caseDetails);
    }

    @Test
    public void shouldFindAllCasesMissingSjpn() {
        when(caseRepository.findCasesMissingSjpn()).thenReturn(queryResult);
        when(caseRepository.countCasesMissingSjpn()).thenReturn(COUNT);

        final CasesMissingSjpnView casesMissingSjpnView = service.findCasesMissingSjpn(empty(), empty());

        verify(queryResult, never()).maxResults(anyInt());

        assertThat(casesMissingSjpnView.ids, equalTo(extractCaseIds(caseDetails)));
        assertThat(casesMissingSjpnView.count, equalTo(COUNT));
    }

    @Test
    public void shouldFindCasesMissingSjpnLimitedByPostingDate() {
        when(caseRepository.findCasesMissingSjpn(NOW)).thenReturn(queryResult);
        when(caseRepository.countCasesMissingSjpn(NOW)).thenReturn(COUNT);

        final CasesMissingSjpnView casesMissingSjpnView = service.findCasesMissingSjpn(empty(), Optional.of(NOW));

        verify(queryResult, never()).maxResults(anyInt());

        assertThat(casesMissingSjpnView.ids, equalTo(extractCaseIds(caseDetails)));
        assertThat(casesMissingSjpnView.count, equalTo(COUNT));
    }

    @Test
    public void shouldReturnEmptyListWhenLimitIsZero() {
        final int limit = 0;

        when(caseRepository.countCasesMissingSjpn()).thenReturn(COUNT);

        final CasesMissingSjpnView casesMissingSjpnView = service.findCasesMissingSjpn(Optional.of(limit), empty());

        verify(caseRepository, never()).findCasesMissingSjpn();
        verify(caseRepository, never()).findCasesMissingSjpn(any(LocalDate.class));
        verify(queryResult, never()).maxResults(anyInt());

        assertThat(casesMissingSjpnView.ids, hasSize(0));
        assertThat(casesMissingSjpnView.count, equalTo(COUNT));
    }


    @Test
    public void shouldFindCasesMissingSjpnWhenLimitIsPositive() {
        final int limit = 2;

        when(queryResult.maxResults(anyInt())).thenReturn(queryResult);
        when(caseRepository.findCasesMissingSjpn()).thenReturn(queryResult);
        when(caseRepository.countCasesMissingSjpn()).thenReturn(COUNT);

        final CasesMissingSjpnView casesMissingSjpnView = service.findCasesMissingSjpn(Optional.of(limit), empty());

        verify(caseRepository).findCasesMissingSjpn();
        verify(queryResult).maxResults(limit);

        assertThat(casesMissingSjpnView.ids, equalTo(extractCaseIds(caseDetails)));
        assertThat(casesMissingSjpnView.count, equalTo(COUNT));
    }

    @Test
    public void shouldFindCasesMissingSjpnWithCountAndPostingDateLimits() {
        final int limit = 2;

        when(queryResult.maxResults(anyInt())).thenReturn(queryResult);
        when(caseRepository.findCasesMissingSjpn(NOW)).thenReturn(queryResult);
        when(caseRepository.countCasesMissingSjpn(NOW)).thenReturn(COUNT);

        final CasesMissingSjpnView casesMissingSjpnView = service.findCasesMissingSjpn(Optional.of(limit), Optional.of(NOW));

        verify(caseRepository).findCasesMissingSjpn(NOW);
        verify(queryResult).maxResults(limit);

        assertThat(casesMissingSjpnView.ids, equalTo(extractCaseIds(caseDetails)));
        assertThat(casesMissingSjpnView.count, equalTo(COUNT));
    }

    private CaseDetail createCaseDetails() {
        final CaseDetail caseDetail = new CaseDetail();
        caseDetail.setId(randomUUID());
        return caseDetail;
    }

    private List<String> extractCaseIds(List<CaseDetail> casesDetails) {
        return casesDetails.stream().map(caseDetails -> caseDetails.getId().toString()).collect(toList());
    }
}
