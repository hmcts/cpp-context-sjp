package uk.gov.moj.cpp.sjp.event.listener;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResult;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSearchResultRepository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
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

    @Test
    public void shouldHandlePersonInfoAdded() {

        final String id = UUID.randomUUID().toString();
        final String caseId = UUID.randomUUID().toString();
        final String personId = UUID.randomUUID().toString();
        final String lastName = "lastName";

        final JsonEnvelope event = envelope()
                .withPayloadOf(id, "id")
                .withPayloadOf(caseId, "caseId")
                .withPayloadOf(personId, "personId")
                .withPayloadOf(lastName, "lastName")
                .build();

        caseSearchResultListener.personInfoAdded(event);

        verify(repository).save(captor.capture());
        final CaseSearchResult caseSearchResult = captor.getValue();
        assertThat(caseSearchResult.getId().toString(), is(id));
        assertThat(caseSearchResult.getCaseId().toString(), is(caseId));
        assertThat(caseSearchResult.getLastName(), is(lastName));
    }

    @Test
    public void shouldHandlePersonInfoUpdated() {
        final UUID id = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final String firstName = "Teresa";
        final String lastName = "May";
        final LocalDate dateOfBirth = LocalDate.of(1960, 10, 1);
        final String postCode = "SW1A 2AA";


        CaseSearchResult caseSearchResult = new CaseSearchResult(id, caseId, firstName, lastName, dateOfBirth, postCode);
        when(repository.findByCaseId(caseId)).thenReturn(newArrayList(caseSearchResult));

        final String updatedLastName = "June";
        final String updatedFirstName = "Theresa";
        final LocalDate updateDateOfBirth = LocalDate.now();

        final JsonEnvelope event = envelope()
                .withPayloadOf(caseId, "caseId")
                .withPayloadOf(updatedLastName, "lastName")
                .withPayloadOf(LocalDates.to(updateDateOfBirth), "dateOfBirth")
                .withPayloadOf(updatedFirstName, "firstName")
                .build();

        caseSearchResultListener.personInfoUpdated(event);

        verify(repository).save(captor.capture());
        final CaseSearchResult savedSearchResult = captor.getValue();

        assertThat(savedSearchResult, new TypeSafeMatcher<CaseSearchResult>() {
            @Override
            protected boolean matchesSafely(final CaseSearchResult item) {
                return item.getCaseId().equals(caseId)
                        && item.getLastName().equals(updatedLastName)
                        && item.getFirstName().equals(updatedFirstName)
                        && item.getDateOfBirth().equals(updateDateOfBirth)
                        && item.getPostCode().equals(postCode);
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("caseSearchResult with(caseId = ")
                        .appendValue(caseId)
                        .appendText(", lastName=")
                        .appendValue(updatedLastName)
                        .appendText(", firstName=")
                        .appendValue(updatedFirstName)
                        .appendText(", dateOfBirth=")
                        .appendValue(updateDateOfBirth)
                        .appendText(", postCode")
                        .appendValue(postCode)
                        .appendText(")");
            }

            @Override
            protected void describeMismatchSafely(final CaseSearchResult item, final Description mismatchDescription) {
                mismatchDescription.appendText("caseSearchResult with(caseId = ")
                        .appendValue(item.getCaseId())
                        .appendText(", lastName=")
                        .appendValue(item.getLastName())
                        .appendText(", firstName=")
                        .appendValue(item.getFirstName())
                        .appendText(", dateOfBirth=")
                        .appendValue(item.getDateOfBirth())
                        .appendText(", postCode")
                        .appendValue(item.getPostCode())
                        .appendText(")");
            }
        });
    }
}
