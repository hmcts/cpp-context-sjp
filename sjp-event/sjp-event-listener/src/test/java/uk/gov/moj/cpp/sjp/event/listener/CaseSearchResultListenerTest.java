package uk.gov.moj.cpp.sjp.event.listener;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.listener.handler.CaseSearchResultService;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResult;
import uk.gov.moj.cpp.sjp.persistence.entity.ReadyCase;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSearchResultRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.ReadyCaseRepository;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseSearchResultListenerTest {

    @Spy
    @InjectMocks
    private CaseSearchResultService caseSearchResultService = new CaseSearchResultService();

    @Mock
    private ReadyCase readyCase;

    @Mock
    private CaseSearchResultRepository caseSearchResultRepository;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private ReadyCaseRepository readyCaseRepository;

    @InjectMocks
    private CaseSearchResultListener caseSearchResultListener;

    @Captor
    private ArgumentCaptor<CaseSearchResult> captor;

    @Test
    public void shouldHandleCaseAssigned() {
        final UUID caseId = UUID.randomUUID();
        final UUID assigneeId = UUID.randomUUID();
        final JsonEnvelope event = envelope()
                .withPayloadOf(caseId.toString(), "caseId")
                .withPayloadOf(assigneeId.toString(), "assigneeId")
                .build();
        final List<CaseSearchResult> searchResults = Arrays.asList(new CaseSearchResult(), new CaseSearchResult());
        when(caseSearchResultRepository.findByCaseId(caseId)).thenReturn(searchResults);

        final CaseDetail caseDetail = new CaseDetail();
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);
        when(readyCaseRepository.findBy(caseId)).thenReturn(readyCase);

        //given
        searchResults.forEach(searchResult ->
            assertThat(searchResult.isAssigned(), is(false))
        );
        assertThat(caseDetail.getAssigneeId(), nullValue());

        //when
        caseSearchResultListener.caseAssigned(event);

        //then
        searchResults.forEach(searchResult ->
            assertThat(searchResult.isAssigned(), is(true))
        );
        assertThat(caseDetail.getAssigneeId(), is(assigneeId));

        verify(readyCase).setAssigneeId(assigneeId);
    }

    @Test
    public void shouldHandleCaseAssignmentDeleted() {
        final UUID caseId = UUID.randomUUID();
        final UUID assigneeId = UUID.randomUUID();
        final JsonEnvelope event = envelope()
                .withPayloadOf(caseId.toString(), "caseId")
                .build();
        CaseSearchResult caseSearchResult1 = new CaseSearchResult();
        caseSearchResult1.setAssigned(true);
        CaseSearchResult caseSearchResult2 = new CaseSearchResult();
        caseSearchResult2.setAssigned(true);
        final List<CaseSearchResult> searchResults = Arrays.asList(caseSearchResult1, caseSearchResult2);
        when(caseSearchResultRepository.findByCaseId(caseId)).thenReturn(searchResults);

        final CaseDetail caseDetail = new CaseDetail();
        caseDetail.setAssigneeId(assigneeId);
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);
        when(readyCaseRepository.findBy(caseId)).thenReturn(readyCase);

        //given
        searchResults.forEach(searchResult ->
            assertThat(searchResult.isAssigned(), is(true))
        );
        assertThat(caseDetail.getAssigneeId(), is(assigneeId));

        //when
        caseSearchResultListener.caseUnassigned(event);

        //then
        searchResults.forEach(searchResult ->
            assertThat(searchResult.isAssigned(), is(false))
        );
        assertThat(caseDetail.getAssigneeId(), nullValue());
        verify(readyCase).setAssigneeId(null);
    }
}
