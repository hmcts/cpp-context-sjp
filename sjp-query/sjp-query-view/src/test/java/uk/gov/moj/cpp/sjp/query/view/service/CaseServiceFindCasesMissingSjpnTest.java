package uk.gov.moj.cpp.sjp.query.view.service;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityAccess;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityProvider;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.query.view.converter.ProsecutingAuthorityAccessFilterConverter;
import uk.gov.moj.cpp.sjp.query.view.response.CasesMissingSjpnView;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.apache.deltaspike.data.api.QueryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseServiceFindCasesMissingSjpnTest {

    private static final LocalDate NOW = LocalDate.now();
    private static final int COUNT = 10;
    private static final String TVL_FILTER_VALUE = "TVL";
    private List<CaseDetail> caseDetails;
    @Mock
    private QueryResult queryResult;
    @Mock
    private CaseRepository caseRepository;
    @Mock
    private ProsecutingAuthorityProvider prosecutingAuthorityProvider;
    @Mock
    private ProsecutingAuthorityAccessFilterConverter prosecutingAuthorityAccessFilterConverter;
    @Mock
    private ProsecutingAuthorityAccess prosecutingAuthorityAccess;
    @Mock
    private JsonEnvelope envelope;
    @InjectMocks
    private CaseService service;

    @BeforeEach
    public void init() {
        caseDetails = asList(createCaseDetails(), createCaseDetails(), createCaseDetails());

        mockProsecutingAuthorityAccess();
    }

    @Test
    public void shouldFindAllCasesMissingSjpn() {

        when(queryResult.getResultList()).thenReturn(caseDetails);
        when(caseRepository.findCasesMissingSjpn(TVL_FILTER_VALUE)).thenReturn(queryResult);
        when(caseRepository.countCasesMissingSjpn(TVL_FILTER_VALUE)).thenReturn(COUNT);

        final CasesMissingSjpnView casesMissingSjpnView = service.findCasesMissingSjpn(envelope, empty(), empty());

        verify(queryResult, never()).maxResults(anyInt());

        assertThat(casesMissingSjpnView.ids, equalTo(extractCaseIds(caseDetails)));
        assertThat(casesMissingSjpnView.count, equalTo(COUNT));
    }

    @Test
    public void shouldFindCasesMissingSjpnLimitedByPostingDate() {

        when(queryResult.getResultList()).thenReturn(caseDetails);
        when(caseRepository.findCasesMissingSjpn(TVL_FILTER_VALUE, NOW)).thenReturn(queryResult);
        when(caseRepository.countCasesMissingSjpn(TVL_FILTER_VALUE, NOW)).thenReturn(COUNT);

        final CasesMissingSjpnView casesMissingSjpnView = service.findCasesMissingSjpn(envelope, empty(), Optional.of(NOW));

        verify(queryResult, never()).maxResults(anyInt());

        assertThat(casesMissingSjpnView.ids, equalTo(extractCaseIds(caseDetails)));
        assertThat(casesMissingSjpnView.count, equalTo(COUNT));
    }

    @Test
    public void shouldReturnEmptyListWhenLimitIsZero() {
        final int limit = 0;

        when(caseRepository.countCasesMissingSjpn(TVL_FILTER_VALUE)).thenReturn(COUNT);

        final CasesMissingSjpnView casesMissingSjpnView = service.findCasesMissingSjpn(envelope, Optional.of(limit), empty());

        verify(caseRepository, never()).findCasesMissingSjpn(any(String.class));
        verify(caseRepository, never()).findCasesMissingSjpn(any(String.class), any(LocalDate.class));
        verify(queryResult, never()).maxResults(anyInt());

        assertThat(casesMissingSjpnView.ids, hasSize(0));
        assertThat(casesMissingSjpnView.count, equalTo(COUNT));
    }

    @Test
    public void shouldFindCasesMissingSjpnWhenLimitIsPositive() {
        final int limit = 2;

        when(queryResult.getResultList()).thenReturn(caseDetails);
        when(queryResult.maxResults(anyInt())).thenReturn(queryResult);
        when(caseRepository.findCasesMissingSjpn(TVL_FILTER_VALUE)).thenReturn(queryResult);
        when(caseRepository.countCasesMissingSjpn(TVL_FILTER_VALUE)).thenReturn(COUNT);

        final CasesMissingSjpnView casesMissingSjpnView = service.findCasesMissingSjpn(envelope, Optional.of(limit), empty());

        verify(caseRepository).findCasesMissingSjpn(TVL_FILTER_VALUE);
        verify(queryResult).maxResults(limit);

        assertThat(casesMissingSjpnView.ids, equalTo(extractCaseIds(caseDetails)));
        assertThat(casesMissingSjpnView.count, equalTo(COUNT));
    }

    @Test
    public void shouldFindCasesMissingSjpnWithCountAndPostingDateLimits() {
        final int limit = 2;

        when(queryResult.getResultList()).thenReturn(caseDetails);
        when(queryResult.maxResults(anyInt())).thenReturn(queryResult);
        when(caseRepository.findCasesMissingSjpn(TVL_FILTER_VALUE, NOW)).thenReturn(queryResult);
        when(caseRepository.countCasesMissingSjpn(TVL_FILTER_VALUE, NOW)).thenReturn(COUNT);

        final CasesMissingSjpnView casesMissingSjpnView = service.findCasesMissingSjpn(envelope, Optional.of(limit), Optional.of(NOW));

        verify(caseRepository).findCasesMissingSjpn(TVL_FILTER_VALUE, NOW);
        verify(queryResult).maxResults(limit);

        assertThat(casesMissingSjpnView.ids, equalTo(extractCaseIds(caseDetails)));
        assertThat(casesMissingSjpnView.count, equalTo(COUNT));
    }

    private void mockProsecutingAuthorityAccess() {

        when(prosecutingAuthorityProvider.getCurrentUsersProsecutingAuthorityAccess(envelope))
                .thenReturn(prosecutingAuthorityAccess);
        when(prosecutingAuthorityAccessFilterConverter.convertToProsecutingAuthorityAccessFilter(prosecutingAuthorityAccess))
                .thenReturn(TVL_FILTER_VALUE);
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
