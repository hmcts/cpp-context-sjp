package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResult;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSearchResultRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSearchResultRepositoryImpl;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RemovePersonInfoListenerTest {

    @Mock
    JsonObject jsonObject;

    @Mock
    CaseSearchResultRepository caseSearchResultRepository;

    @Mock
    CaseSearchResultRepositoryImpl caseSearchResultRepositoryImpl;

    @Mock
    JsonEnvelope envelope;

    @Mock
    CaseSearchResult caseSearchResult;

    @InjectMocks
    RemovePersonInfoListener duplicatePersonInfoListener;

    @Test
    public void shouldRemove() {
        final UUID personInfoId = randomUUID();
        when(envelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonObject.getString("personInfoId")).thenReturn(personInfoId.toString());
        when(caseSearchResultRepository.findBy(personInfoId)).thenReturn(caseSearchResult);
        when(caseSearchResult.getId()).thenReturn(personInfoId);
        duplicatePersonInfoListener.removePersonInfo(envelope);
        verify(caseSearchResultRepositoryImpl, times(1)).removePersonInfo(personInfoId.toString());
    }
}