package uk.gov.moj.cpp.sjp.event.listener;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResult;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSearchResultRepository;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseSearchResultListenerTest {

    @Mock
    private CaseSearchResultRepository repository;

    @Mock
    private CaseRepository caseRepository;

    @InjectMocks
    private CaseSearchResultListener caseSearchResultListener;

    @Captor
    private ArgumentCaptor<CaseSearchResult> captor;

    @Test
    public void caseAssignmentCreated() {
        final UUID caseId = UUID.randomUUID();
        final JsonEnvelope event = envelope()
                .withPayloadOf(caseId.toString(), "caseId")
                .build();
        final List<CaseSearchResult> searchResults = Arrays.asList(new CaseSearchResult(), new CaseSearchResult());
        when(repository.findByCaseId(caseId)).thenReturn(searchResults);

        final CaseDetail caseDetail = new CaseDetail();
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);

        //given
        searchResults.forEach(searchResult -> {
            assertThat(searchResult.isAssigned(), is(false));
        });
        assertThat(caseDetail.getAssigned(), is(false));

        //when
        caseSearchResultListener.caseAssignmentCreated(event);

        //then
        searchResults.forEach(searchResult -> {
            assertThat(searchResult.isAssigned(), is(true));
        });
        assertThat(caseDetail.getAssigned(), is(true));
    }

    @Test
    public void caseAssignmentDeleted() {
        final UUID caseId = UUID.randomUUID();
        final JsonEnvelope event = envelope()
                .withPayloadOf(caseId.toString(), "caseId")
                .build();
        CaseSearchResult caseSearchResult1 = new CaseSearchResult();
        caseSearchResult1.setAssigned(true);
        CaseSearchResult caseSearchResult2 = new CaseSearchResult();
        caseSearchResult2.setAssigned(true);
        final List<CaseSearchResult> searchResults = Arrays.asList(caseSearchResult1, caseSearchResult2);
        when(repository.findByCaseId(caseId)).thenReturn(searchResults);

        final CaseDetail caseDetail = new CaseDetail();
        caseDetail.setAssigned(true);
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);

        //given
        searchResults.forEach(searchResult -> {
            assertThat(searchResult.isAssigned(), is(true));
        });
        assertThat(caseDetail.getAssigned(), is(true));

        //when
        caseSearchResultListener.caseAssignmentDeleted(event);

        //then
        searchResults.forEach(searchResult -> {
            assertThat(searchResult.isAssigned(), is(false));
        });
        assertThat(caseDetail.getAssigned(), is(false));
    }
}
